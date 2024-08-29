package com.infrared5.red5pro.live;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.red5.codec.AACAudio;
import org.red5.codec.AVCVideo;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.IVideoStreamCodec;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.Red5;
import org.red5.server.api.listeners.AbstractConnectionListener;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.AbstractClientStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.red5pro.canvas.DefaultFrameProvider;
import com.red5pro.canvas.FrameProvider;
import com.red5pro.canvas.Red5ProCanvas;
import com.red5pro.cluster.streams.Provision;
import com.red5pro.interstitial.api.IInterstitialRequestHandler;
import com.red5pro.media.sdp.SessionDescription;
import com.red5pro.override.IProStream;
import com.red5pro.override.api.ProStreamTerminationEventListener;
import com.red5pro.server.IProScopeHandler;
import com.red5pro.server.stream.IAliasProvider;
import com.red5pro.server.stream.auxout.WaveWriter;
import com.red5pro.server.stream.webrtc.IRTCCapableConnection;
import com.red5pro.server.stream.webrtc.IRTCStreamSession;


/**
 * This example application adapter illustrates some of the most commonly used
 * overrides.
 *
 * @author Andy Shaules
 */
public class Red5ProLive extends MultiThreadedApplicationAdapter implements IStreamListener, IProScopeHandler {

    private static Logger log = LoggerFactory.getLogger(Red5ProLive.class);

    private static boolean isDebug = log.isDebugEnabled();

    private boolean useABRNames;

    ConcurrentHashMap<String, IProStream> liveStreams = new ConcurrentHashMap<>();

    private Red5ProCanvas canvas;

    private boolean doCanvasTest;

    private boolean doCanvasForwarderTest;

    private long canvasTestDuration = 240000;

    private long canvasTestDelay = 10000;

    private int canvasTestWidth = 640;

    private int canvasTestHeight = 480;

    private int canvasTestFrameRate = 15;

    private int canvasTestSampleRate = 16000;

    private String canvasTestName = "canvasTest";

    private byte color = 0;

    private byte colorTime = 0;

    private byte colorStyle = 0;

    private boolean isInterstitialHandlerEnabled = true;

    private String canvasTestForwardHost = "localhost";

    private boolean interceptOfferSDP, interceptAnswerSDP;

    private static String webhookEndpoint;

    private static IAliasProvider streamNameAliasProvider;

    /**
     * Application life-cycle begins here
     */
    @Override
    public boolean appStart(IScope scope) {
        log.info("========================= >>> Red5ProLive APP START <<< =========================");
        // register publish security
        registerStreamPublishSecurity(new IStreamPublishSecurity() {

            @Override
            public boolean isPublishAllowed(IScope scope, String name, String mode) {
                log.info("isPublishAllowed {} {}", scope.getContextPath(), name);
                if (name.contains("rejectme")) {
                    return false;
                }
                return true;
            }

        });
        // register playback security
        registerStreamPlaybackSecurity(new IStreamPlaybackSecurity() {

            @Override
            public boolean isPlaybackAllowed(IScope scope, String name, int start, int length, boolean flushPlaylist) {
                log.info("isPlaybackAllowed {} {}", scope.getContextPath(), name);
                return true;
            }

        });
        if (doCanvasTest) {
            new Thread(() -> {
                try {
                    Thread.sleep(canvasTestDelay);
                } catch (InterruptedException e) {
                    log.warn("Canvas test delay interrupted", e);
                }
                testCanvas();
                try {
                    Thread.sleep(canvasTestDuration);
                } catch (InterruptedException e) {
                    log.warn("Canvas test duration interrupted", e);
                }
                canvas.stop();
            }).start();
        }
        // Interstitial
        if (isInterstitialHandlerEnabled) {
            log.info("Register new InterstitialRequestHandlerImpl");
            IInterstitialRequestHandler.handlers.add(new InterstitialRequestHandlerImpl(this));
        } else {
            log.debug("Interstitial disabled, skipping");
        }
        // provision-based alias provider
        //streamNameAliasProvider = new ProvisionAliasProvider(scope);
        // pattern based alias provider
        streamNameAliasProvider = new PatternAliasProvider();
        return true;
    }

    @Override
    public void appStop(IScope scope) {
        log.info("========================= >>> Red5ProLive APP STOP <<< =========================");
        // Interstitial
        if (isInterstitialHandlerEnabled) {
            log.info("Unregister InterstitialRequestHandlerImpl");
            IInterstitialRequestHandler.handlers.remove(new InterstitialRequestHandlerImpl(this));
        } else {
            log.debug("Interstitial disabled, skipping");
        }
        super.appStop(scope);
    }

    /**
     * Called when a client connects to the application.
     */
    @Override
    public boolean appConnect(IConnection conn, Object[] params) {
        // debug for publish security at this stage
        log.debug("Publish security implementations: {}", getStreamPublishSecurity());
        log.debug("Playback security implementations: {}", getStreamPlaybackSecurity());
        // show type of client encoding / protocol
        Encoding encoding = conn.getEncoding();
        log.debug("Connection encoding: {}", encoding);
        // WebSocket or DataChannel == WebRTC
        if (EnumSet.of(Encoding.WEBSOCKET, Encoding.DATACHANNEL).contains(encoding)) {
            log.info("WebRTC UA: {}", ((IRTCCapableConnection) conn).getUserAgent());
        }
        // add a connection listener for the property changes
        conn.addListener(new AbstractConnectionListener() {
            @Override
            public void notifyConnected(IConnection conn) {
                // XXX nate: this event has already fired by the time we add this listener, so we will never recieve it here..
            }

            @Override
            public void notifyDisconnected(IConnection conn) {
                // no-op
            }

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                log.debug("Connection propertyChange: {}", evt);
                Object src = evt.getSource();
                // only supporting RTC capable connections at this time
                if (src != null && src instanceof IRTCCapableConnection) {
                    final IRTCCapableConnection conn = (IRTCCapableConnection) src;
                    String propName = evt.getPropertyName();
                    Object oldValue = evt.getOldValue();
                    Object newValue = evt.getNewValue();
                    switch (propName) {
                        // an offer sdp from a publishing source will arrive here for information prior to server parsing
                        // modifications to the sdp will not be processed by the server
                        case "sdpPrOffer":
                            // consider this a Pre-Offer wherein manipulation here will not affect server parsing
                            log.info("PrOffer: {}", newValue);
                            break;
                        // an answer sdp from a subscriber will arrive here for information prior to server parsing
                        // modifications to the sdp will not be processed by the server
                        case "sdpPrAnswer":
                            // consider this a Pre-Answer wherein manipulation here will not affect server parsing
                            log.info("PrAnswer: {}", newValue);
                            break;
                        // an offer sdp will arrive here destined for a subscriber/consumer after server generation
                        case "sdpOffer":
                            // get sdp
                            String offerSdpStr = null;
                            // check the data value; if its a string or session description
                            if (newValue instanceof SessionDescription) {
                                SessionDescription offerSdp = (SessionDescription) newValue;
                                // ensure its a string before we try to manipulate it
                                offerSdpStr = offerSdp.toString();
                            } else {
                                offerSdpStr = (String) newValue;
                            }
                            log.info("SDP offer: {}", offerSdpStr);
                            if (interceptOfferSDP) {
                                log.info("Intercepting offer");
                                // here we do a replacement to indicate a change which can be found in the log
                                offerSdpStr = offerSdpStr.replace("o=red5pro", "o=intercept");
                                conn.setLocalSDP(offerSdpStr);
                            }
                            break;
                        // an answer sdp will arrive here destined for a publisher/source after server manipulation
                        case "sdpAnswer":
                            // get sdp
                            String answerSdpStr = null;
                            // check the data value; if its a string or session description
                            if (newValue instanceof SessionDescription) {
                                SessionDescription answerSdp = (SessionDescription) newValue;
                                // ensure its a string before we try to manipulate it
                                answerSdpStr = answerSdp.toString();
                            } else {
                                answerSdpStr = (String) newValue;
                            }
                            log.info("SDP answer: {}", answerSdpStr);
                            if (interceptAnswerSDP) {
                                log.info("Intercepting answer");
                                // here we do a replacement to indicate a change which can be found in the log
                                answerSdpStr = answerSdpStr.replace("o=red5pro", "o=intercept");
                                conn.setLocalSDP(answerSdpStr);
                            }
                            break;
                        case "iceCandidate":
                            log.info("ICE candidate: {}", newValue);
                            break;
                        case "publish":
                            log.info("Publish: {}", newValue);
                            // for testing we only want the io session for webrtc; no connection type cast or check, its RTC only
                            try {
                                // log out the mina io session for QA purposes
                                log.info("Publishing connection: {}", conn.getSession().getRtcStream().getIoSession());
                            } catch (Exception e) {
                                log.warn("Exception getting io session", e);
                            }
                            break;
                        case "unpublish":
                            log.info("Unpublish: {}", newValue);
                            break;
                        case "subscribe":
                            log.info("Subscribe: {}", newValue);
                            break;
                        case "unsubscribe":
                            log.info("Unsubscribe: {}", newValue);
                            break;
                        case "packetLossAudio":
                            log.info("Audio packet loss - prev total: {} increase: {}", oldValue, newValue);
                            break;
                        case "packetLossVideo":
                            log.info("Video packet loss - prev total: {} increase: {}", oldValue, newValue);
                            break;
                        case "packetStats":
                            // perform some logic to determine if some action is appropriate like sending an REMB
                            // {"type":"video","ssrc":4151326586,"lastREMB":1678818230690,"bitrate":750300,"recv":630,"dropped":0}
                            JsonObject json = JsonParser.parseString((String) newValue).getAsJsonObject();
                            log.info("Packet stats: {}", json);
                            long lastREMB = json.get("lastREMB").getAsLong();
                            // cheap and dirty to demo; ensure send every 3s only at DEBUG level
                            long delta = System.currentTimeMillis() - lastREMB;
                            log.debug("Delta time: {}", delta);
                            if (isDebug && delta > 3000L) {
                                int ssrc = json.get("ssrc").getAsInt();
                                int bitrate = json.get("bitrate").getAsInt();
                                int scaledUpBitrate = bitrate + 100;
                                log.debug("Requesting bitrate increase from {} to {} ssrc: {}", bitrate, scaledUpBitrate,
                                        Integer.toUnsignedString(ssrc));
                                //conn.sendRemb(ssrc, scaledUpBitrate);
                            }
                            break;
                        default:
                            log.debug("Unhandled property change for: {}", propName);
                    }
                }
            }

        });
        return super.appConnect(conn, params);
    }

    /**
     * Called when the client disconnects. This is where clean-up of client
     * associated objects should occur.
     */
    @Override
    public void appDisconnect(IConnection conn) {
        IClient client = conn.getClient();
        log.debug("Connections client removed on appDisconnect: {}", client);
        super.appDisconnect(conn);
    }

    @Override
    public void streamPublishStart(IBroadcastStream stream) {
        log.info("Stream publish start: {}", stream);
        super.streamPublishStart(stream);
    }

    /**
     * Called when a client begins to publish media or data.
     */
    @Override
    public void streamBroadcastStart(IBroadcastStream stream) {
        log.info("Stream broadcast start: {}", stream);
        // check the stream first for its connection
        IConnection connection = ((AbstractClientStream) stream).getConnection();
        if (connection == null) {
            log.info("Stream had no associated connection, checking thread local");
            connection = Red5.getConnectionLocal();
        }
        if (connection != null && stream != null) {
            connection.setAttribute("streamStart", System.currentTimeMillis());
            connection.setAttribute("streamName", stream.getPublishedName());
        }
        IProStream pro = (IProStream) stream;
        String streamName = pro.getBroadcastStreamPublishName();
        log.info("========================== >>>> Stream Broadcast Start {} / {} <<<< ==========================", //
                pro.getScope().getContextPath(), streamName);
        String key = Provision.makeGuid(pro.getScope().getContextPath(), streamName);
        log.info("adding key {}", key);
        liveStreams.put(key, pro);

        // for webhook receiver to tell who made the call
        final String nodeType = System.getProperty("clusterNodeType", "off");
        
        final IRTCStreamSession session = IRTCCapableConnection.class.isInstance(connection)
                ? ((IRTCCapableConnection) connection).getSession()
                : null;
        final String userAgent = (session == null) ? "" : session.getUserAgent();
        final String username = (connection == null) ? "" : (String) connection.getAttribute("username");
        // XXX add a listener to get notifications in the packetReceived method below
        // pro.addStreamListener(this);
        if (stream instanceof IProStream && stream.getPublishedName().contains("_wav")) {
            WaveWriter writer16 = new WaveWriter(stream.getPublishedName().concat("-16khz.wav"), 1, 16000, 16);
            writer16.setFlushDuration(1);
            ((IProStream) stream).addAuxOut(writer16);
        }
        pro.addTerminationEventListener(new ProStreamTerminationEventListener() {
            @Override
            public void streamStopped(IProStream stream) {
                if (stream != null) {
                    // for webhook receiver to tell who made the call
                    final String streamName = stream.getBroadcastStreamPublishName();
                    IScope streamScope = stream.getScope();
                    if (streamScope != null) {
                        String path = streamScope.getContextPath();
                        String key = Provision.makeGuid(path, streamName);
                        log.info("removing key {}", key);
                        liveStreams.remove(key);                        
                    } else {
                        log.warn("Streams scope missing at close for {}, forcing removal", streamName);
                        liveStreams.forEach((key, ps) -> {
                            if (streamName.equals(ps.getBroadcastStreamPublishName())) {
                                log.info("removing key {}", key);
                                liveStreams.remove(key);
                            }
                        });
                    }
                }
            }
        });
        super.streamBroadcastStart(stream);
    }

    @Override
    public void streamBroadcastClose(IBroadcastStream stream) {
        log.info("Stream broadcast close: {}", stream);
        // check the stream first for its connection
        IConnection connection = ((AbstractClientStream) stream).getConnection();
        if (connection == null) {
            log.info("Stream had no associated connection, checking thread local");
            connection = Red5.getConnectionLocal();
        }
    }

    @Override
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        if (isDebug && packet.getDataType() == Constants.TYPE_VIDEO_DATA) {
            log.debug("***** VIDEO PACKET RECIEVED ******* {}", System.currentTimeMillis());
        }
    }

    @Override
    public void streamRecordStart(IBroadcastStream stream) {
        log.info("Stream record start: {}", stream);
        super.streamRecordStart(stream);
    }

    @Override
    public void streamRecordStop(IBroadcastStream stream) {
        log.info("Stream record stop: {}", stream);
        super.streamRecordStop(stream);
    }

    public IProStream getStream(String path, String name) {
        String key = Provision.makeGuid(path, name);
        log.debug("sanitize {}", key);
        return liveStreams.get(key);
    }

    @Override
    public void streamSubscriberStart(ISubscriberStream stream) {
        log.info("Stream subscriber start: {}", stream);
        String streamName = stream.getBroadcastStreamPublishName();
        IConnection conn = Red5.getConnectionLocal();
        conn.setAttribute("broadcastId", streamName);
        conn.setAttribute("sessionAge", System.currentTimeMillis());
        super.streamSubscriberStart(stream);
    }

    @Override
    public void streamSubscriberClose(ISubscriberStream stream) {
        log.info("Stream subscriber close: {}", stream);
        // this should match broadcastId
        String streamName = stream.getBroadcastStreamPublishName();
        IConnection conn = Red5.getConnectionLocal();
        long age = conn.hasAttribute("sessionAge") ? Long.valueOf(conn.getAttribute("sessionAge").toString()) : -1L;

        // XXX nate: there is no `userId` that i can find. maybe this was supposed to be `username`?
        if (conn.hasAttribute("userId")) {
            String userId = conn.getAttribute("userId").toString();
            String broadcastId = conn.getAttribute("broadcastId").toString();
            String type = conn.getAttribute("type").toString();
            if (age > 0) {
                long duration = System.currentTimeMillis() - age;
                log.info("User id {}  watched broadcast {}  type:{} for {} seconds.",
                        new Object[] { userId, broadcastId, type, duration / 1000.0f });
            }
        } else {
            if (age > 0) {
                long duration = System.currentTimeMillis() - age;
                log.info("unknown user watched broadcast {} for {} seconds.", new Object[] { streamName, duration / 1000.0f });
            }
        }
    }

    public void sendMessageToPublisher(Map<Object, Object> message) {
        IConnection conn = Red5.getConnectionLocal();
        String thePublisher = null;
        if (conn.hasAttribute("broadcastId")) {
            thePublisher = conn.getAttribute("broadcastId").toString();
            Set<IConnection> conns = conn.getScope().getClientConnections();
            for (IConnection clientConn : conns) {
                if (clientConn.hasAttribute("streamName")) {
                    String theStream = clientConn.getAttribute("streamName").toString();
                    // this is a publisher. we set this above.
                    if (theStream.equals(thePublisher) && clientConn instanceof IServiceCapableConnection) {
                        ((IServiceCapableConnection) clientConn).invoke("subscriberMessage", new Object[] { message });
                        log.info("sent message");
                        return;
                    }
                }
            }
        }
    }

    public void sendMessageToPublisher(String message) {
        IConnection conn = Red5.getConnectionLocal();
        String thePublisher = null;
        if (conn.hasAttribute("broadcastId")) {
            thePublisher = conn.getAttribute("broadcastId").toString();
            Set<IConnection> conns = conn.getScope().getClientConnections();
            for (IConnection clientConn : conns) {
                if (clientConn.hasAttribute("streamName")) {
                    String theStream = clientConn.getAttribute("streamName").toString();
                    // this is a publisher. we set this above.
                    if (theStream.equals(thePublisher) && clientConn instanceof IServiceCapableConnection) {
                        ((IServiceCapableConnection) clientConn).invoke("subscriberMessage", new Object[] { message });
                        log.info("sent message");
                        return;
                    }
                }
            }
        }
    }

    public IProStream getLiveStream(String streamName) {
    	IProStream stream = null;
        IBroadcastScope broadcastScope = scope.getBroadcastScope(streamName);
        if (broadcastScope != null) {
            stream = (IProStream) broadcastScope.getClientBroadcastStream();
        }
        if (stream == null && log.isDebugEnabled()) {
            log.debug("Stream {} not found in {}", streamName, scope.getContextPath());
        }
        return stream;
    }

    public List<String> getLiveStreams() {
        log.info("getLiveStreams()");
        if (!useABRNames) {
            // create a list of available stream names
            final Set<String> streams = new HashSet<>();
            // any non-parent node

                // run through the stream names for the scope
                getBroadcastStreamNames(scope).forEach(streamName -> {
                    IProStream stream = getLiveStream(streamName);
                    if (stream != null) {
                        // check availability
                        if (isAvailable(stream)) {
                            // add any play aliases
                            if (streamNameAliasProvider instanceof ProvisionAliasProvider) {
                                Set<String> aliases = ((ProvisionAliasProvider) streamNameAliasProvider).getAllPlayAliases(streamName);
                                if (aliases != null && !aliases.isEmpty()) {
                                    aliases.forEach(alias -> {
                                        log.debug("Adding alias: {}", alias);
                                        streams.add(alias);
                                    });
                                } else {
                                    log.debug("No play aliases found for {}", streamName);
                                }
                            } else {
                                // add the identifying name regardless of alias provider
                                streams.add(stream.getPublishedName());
                            }
                        } else {
                            log.info("Stream exists but is not yet available: {} on {}", streamName, scope.getName());
                        }
                    } else {
                        log.info("Stream {} doesnt exist in {} right now", streamName, scope.getName());
                    }
                });
                log.info("Streams from local: {}", streams);
            
            return streams.stream().collect(Collectors.toCollection(ArrayList::new));
        }
        return Collections.emptyList();
    }

    public Notify onMetaData(String streamName) {
        String key = Provision.makeGuid(Red5.getConnectionLocal().getScope().getContextPath(), streamName);
        IProStream stream = liveStreams.get(key);
        if (stream != null) {
            return stream.getMetaData();
        }
        return null;
    }

    public void testCanvas() {
        FrameProvider javaImple = new FrameProvider() {

            double fCyclePosition = 0;

            @Override
            public void drawVideo(byte[] arg0, long milliseconds) {
                int lPoint = (canvasTestWidth * canvasTestHeight / 4) + (canvasTestWidth * canvasTestHeight);
                color += (byte) ((milliseconds / 1000) % 10);
                colorTime += (byte) ((milliseconds / 2000) % 20);
                colorStyle += (byte) ((milliseconds / 4000) % 20);
                for (int i = 0; i < arg0.length; i++) {
                    if (i < canvasTestWidth * canvasTestHeight) {
                        arg0[i] = color;
                    } else if (i < lPoint) {
                        arg0[i] = colorTime;
                    } else {
                        arg0[i] = colorStyle;
                    }
                }
            }

            @Override
            public void fillSound(byte[] arg0, long arg1) {
                int bps = 2; // mono 16bit;
                double frqInc = 440.0 / canvasTestSampleRate;
                for (int i = 0; i < arg0.length; i += bps) {
                    short val = (short) (Short.MAX_VALUE * (Math.sin(2 * Math.PI * fCyclePosition)) * 0.33);
                    arg0[i] = (byte) (val & 0xFF);
                    arg0[i + 1] = (byte) ((val >> 8) & 0xFF);
                    fCyclePosition += frqInc;
                    if (fCyclePosition > 1) {
                        fCyclePosition -= 1;
                    }
                }
            }

            @Override
            public void startCanvas(long arg0) {
                log.info("Canvas start {}", arg0);
            }

            @Override
            public void stopCanvas() {
                log.info("Canvas stop");
            }

        };
        // create canvas
        canvas = DefaultFrameProvider.factory.createCanvas(canvasTestWidth, canvasTestHeight, canvasTestFrameRate, canvasTestSampleRate, 1);
        canvas.setFrameProvider(javaImple);
        canvas.setBitrate(750000);
        canvas.start();
        try {
            if (doCanvasForwarderTest) {
                log.info("Starting forwarder test");
                // String host, int port, String path, String name, Map<String, Object> params
                canvas.forward(canvasTestForwardHost, 1935, "live", canvasTestName, new HashMap<String, Object>());
            } else {
                // start loopback, it blocks!
                // IScope scope, String name, boolean record, boolean append
                canvas.loopBack(scope, canvasTestName, false, false);
            }
        } catch (Throwable t) {
            log.warn("CanvasTest exception", t);
        }
    }

    public boolean isDoCanvasTest() {
        return doCanvasTest;
    }

    public void setDoCanvasTest(boolean doCanvasTest) {
        this.doCanvasTest = doCanvasTest;
    }

    public boolean isDoCanvasForwarderTest() {
        return doCanvasForwarderTest;
    }

    public void setDoCanvasForwarderTest(boolean doCanvasForwarderTest) {
        this.doCanvasForwarderTest = doCanvasForwarderTest;
    }

    public long getCanvasTestDuration() {
        return canvasTestDuration;
    }

    public void setCanvasTestDuration(long canvasTestDuration) {
        this.canvasTestDuration = canvasTestDuration;
    }

    public long getCanvasTestDelay() {
        return canvasTestDelay;
    }

    public void setCanvasTestDelay(long canvasTestDelay) {
        this.canvasTestDelay = canvasTestDelay;
    }

    public int getCanvasTestWidth() {
        return canvasTestWidth;
    }

    public void setCanvasTestWidth(int canvasTestWidth) {
        this.canvasTestWidth = canvasTestWidth;
    }

    public int getCanvasTestHeight() {
        return canvasTestHeight;
    }

    public void setCanvasTestHeight(int canvasTestHeight) {
        this.canvasTestHeight = canvasTestHeight;
    }

    public int getCanvasTestFrameRate() {
        return canvasTestFrameRate;
    }

    public void setCanvasTestFrameRate(int canvasTestFrameRate) {
        this.canvasTestFrameRate = canvasTestFrameRate;
    }

    public int getCanvasTestSampleRate() {
        return canvasTestSampleRate;
    }

    public void setCanvasTestSampleRate(int canvasTestSampleRate) {
        this.canvasTestSampleRate = canvasTestSampleRate;
    }

    public String getCanvasTestName() {
        return canvasTestName;
    }

    public void setCanvasTestName(String canvasTestName) {
        this.canvasTestName = canvasTestName;
    }

    public boolean isInterstitialHandlerEnabled() {
        return isInterstitialHandlerEnabled;
    }

    public void setIsInterstitialHandlerEnabled(boolean isInterstitialHandlerEnabled) {
        this.isInterstitialHandlerEnabled = isInterstitialHandlerEnabled;
    }

    public String getCanvasTestForwardHost() {
        return canvasTestForwardHost;
    }

    public void setCanvasTestForwardHost(String canvasTestForwardHost) {
        this.canvasTestForwardHost = canvasTestForwardHost;
    }

    public void setInterceptOfferSDP(boolean interceptOfferSDP) {
        this.interceptOfferSDP = interceptOfferSDP;
    }

    public void setInterceptAnswerSDP(boolean interceptAnswerSDP) {
        this.interceptAnswerSDP = interceptAnswerSDP;
    }

    @Override
    public String getWebhookEndpoint() {
        return webhookEndpoint;
    }

    @Override
    public void setWebhookEndpoint(String webhookEndpoint) {
        Red5ProLive.webhookEndpoint = webhookEndpoint;
    }

    @Override
    public void setStreamNameAliasProvider(IAliasProvider provider) {
        streamNameAliasProvider = provider;
    }

    @Override
    public IAliasProvider getStreamNameAliasProvider() {
        return streamNameAliasProvider;
    }
    public static boolean isAvailable(IProStream stream) {
        
        // check for audio or video codecs and their expected configs based on type
        IStreamCodecInfo info = stream.getCodecInfo();
        // get has first
        boolean audio = info.hasAudio(), video = info.hasVideo();
        // get codecs
        IAudioStreamCodec acodec = info.getAudioCodec();
        IVideoStreamCodec vcodec = info.getVideoCodec();
        log.trace("Audio codec: {} video codec: {}", acodec, vcodec);
        // whether or not config private data is required
        boolean needsAudioConfig = (acodec instanceof AACAudio), needsVideoConfig = (vcodec instanceof AVCVideo);
        // whether or not config data exists
        boolean hasAudioConfig = acodec != null ? (acodec.getDecoderConfiguration() != null) : false;
        boolean hasVideoConfig = vcodec != null ? (vcodec.getDecoderConfiguration() != null) : false;
        log.debug("isAvailable? {} has audio config? {} needed? {} has video config? {} needed? {}", stream.getPublishedName(), hasAudioConfig,
                needsAudioConfig, hasVideoConfig, needsVideoConfig);
        // has audio and audio requires codec, and has no video
        if (audio && !video) {
            // check private data for aac
            if (needsAudioConfig && hasAudioConfig) {
                return true;
            } else if (!needsAudioConfig) {
                // other audio codecs have no decoder config
                return true;
            }
        }
        // has video and video config but no audio
        if (video && !audio) {
            // check private data for h264/avc
            if (needsVideoConfig && hasVideoConfig) {
                return true;
            } else if (!needsVideoConfig) {
                // other video codecs have no decoder config
                return true;
            }
        }
        // has video and has audio but missing one or both configs
        if (audio && video) {
            if (needsAudioConfig && hasAudioConfig && needsVideoConfig && hasVideoConfig) {
                // needs both configs
                return true;
            } else if (needsAudioConfig && !needsVideoConfig && hasAudioConfig) {
                // only needs audio config
                return true;
            } else if (!needsAudioConfig && needsVideoConfig && hasVideoConfig) {
                // only needs video config
                return true;
            } else if (!needsAudioConfig && !needsVideoConfig) {
                // needs no configs
                return true;
            }
        }        
    return false;
}
}
