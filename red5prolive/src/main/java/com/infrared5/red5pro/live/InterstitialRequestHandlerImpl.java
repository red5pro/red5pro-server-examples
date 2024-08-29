package com.infrared5.red5pro.live;

import java.util.List;

import org.red5.server.api.stream.IClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.interstitial.api.FLVInterstitial;
import com.red5pro.interstitial.api.IInterstitialConfiguration;
import com.red5pro.interstitial.api.IInterstitialRequestHandler;
import com.red5pro.interstitial.api.IInterstitialStream;
import com.red5pro.interstitial.api.InterstitialDurationControl;
import com.red5pro.interstitial.api.InterstitialDurationControlType;
import com.red5pro.interstitial.api.InterstitialException;
import com.red5pro.interstitial.api.InterstitialInsert;
import com.red5pro.interstitial.api.InterstitialSession;
import com.red5pro.interstitial.api.LiveInterstitial;
import com.red5pro.override.IProStream;


/**
 * InterstitialRequestHandlerImpl is an example implementation of IInterstitialRequestHandler
 * which inserts static files or other live streams into a live InterstitialStream.
 *
 * @author Andy
 * @author Nate Roe
 */
public class InterstitialRequestHandlerImpl implements IInterstitialRequestHandler, IInterstitialConfiguration {
    Logger log = LoggerFactory.getLogger(InterstitialRequestHandlerImpl.class);

    private Red5ProLive app;

    public InterstitialRequestHandlerImpl(Red5ProLive app) {
        this.app = app;
    }

    private String getStreamName(String contextPath) {
        // chop off stream name
        String streamName = contextPath;
        int i = streamName.lastIndexOf('/');
        if (i >= 0) {
            streamName = streamName.substring(i + 1);
        }
        return streamName;
    }

    /* (non-Javadoc)
     * @see com.red5pro.interstitial.api.IInterstitialRequestHandler#newRequest(java.lang.String, java.lang.String, java.util.List)
     */
    @Override
    public void newRequest(String user, String digest, List<InterstitialInsert> inserts) throws InterstitialException {
        log.info("New interstitial request. num inserts: {}", inserts.size());
        for (InterstitialInsert insert : inserts) {
            log.info("insert: {}", insert);

            // determine if static file or live stream
            if (insert.interstitial.contains(".flv")) {
                log.trace("FLVInterstitial request");
                FLVInterstitial interstitial = new FLVInterstitial(app.getScope(), insert.interstitial, insert.isInterstitialAudio,
                        insert.isInterstitialVideo);
                interstitial.id = insert.id;
                interstitial.sessionControl = new InterstitialDurationControl(insert.type, insert.duration, insert.start);
                interstitial.sessionControl.setLoop(insert.loop);
                String liveStreamTarget = insert.target;
                IClientBroadcastStream broadcastStream = app.getLiveStream(getStreamName(liveStreamTarget));
                if (broadcastStream != null) {
                    try {
                    	IInterstitialStream stream = (IInterstitialStream) broadcastStream;
                        if (stream != null) {
                            log.info("Adding insert to live program {}", liveStreamTarget);
                            stream.getInterstitialEngine().addInterstitial(interstitial);
                        }
                    } catch (ClassCastException cce) {
                        log.error(
                                "Stream not an InterstitialStream. Config conf/red5-common.xml to com.red5pro.interstitial.InterstitialStream");
                        throw new InterstitialException("Stream not an InterstitialStream");
                    }
                } else {
                    String message = "Stream not found: " + liveStreamTarget;
                    log.info(message);
                    throw new InterstitialException(message);
                }
            } else {
                String liveStreamTarget = insert.target;
                log.info("LiveInterstitial request for target {}", liveStreamTarget);

                if (log.isDebugEnabled()) {
                    StringBuilder builder = new StringBuilder();
                    boolean isFirst = true;
                    for (String key : app.getLiveStreams()) {
                        if (!isFirst) {
                            builder.append(", ");
                        } else {
                            isFirst = false;
                        }
                        builder.append(key);
                    }
                    log.debug("Live streams: {}", builder.toString());
                }

                // locate live stream if possible
                String streamName = getStreamName(liveStreamTarget);
                log.debug("Locate stream: {}", streamName);
                IProStream proStream = (IProStream) app.getLiveStream(streamName);

                if (proStream != null) {
                    try {
                        IInterstitialStream stream = (IInterstitialStream) proStream;
                        IInterstitialStream newStream = (IInterstitialStream) app.getLiveStream(getStreamName(insert.interstitial));
                        if (newStream != null) {
                            LiveInterstitial interstitial = new LiveInterstitial(stream, newStream, insert.isInterstitialAudio,
                                    insert.isInterstitialVideo);
                            interstitial.id = insert.id;
                            if (insert.type == InterstitialDurationControlType.INDEFINITE) {
                                interstitial.sessionControl = new InterstitialDurationControl(insert.type, 0, 0);
                            } else {
                                if (insert.duration == null) {
                                    throw new InterstitialException("Duration is missing but required.");
                                }
                                if (insert.start == null) {
                                    throw new InterstitialException("Start is missing but required.");
                                }
                                interstitial.sessionControl = new InterstitialDurationControl(insert.type, insert.duration, insert.start);
                            }
                            interstitial.sessionControl.setLoop(insert.loop);
                            log.info("Ignite stream via cluster");
                            int lastSlash = insert.interstitial.lastIndexOf('/');
                            String path = insert.interstitial.substring(0, lastSlash);
                            String name = insert.interstitial.substring(lastSlash + 1, insert.interstitial.length());                            
                            log.info("Switching streams on live program {}", liveStreamTarget);
                            stream.getInterstitialEngine().addInterstitial(interstitial);

                        } else {
                            log.info("newStream not found");
                            throw new InterstitialException("Interstitial stream " + insert.interstitial + " not found");
                        }
                    } catch (ClassCastException cce) {
                        log.error(
                                "Stream not an InterstitialStream. Config red5-common.xml to com.red5pro.interstitial.InterstitialStream");
                        throw new InterstitialException("Stream not an InterstitialStream.");
                    } catch (Exception e) {
                        // log any exceptions we don't expect
                        if (!(e instanceof InterstitialException)) {
                            log.warn("unexpected", e);
                        }
                        throw e;
                    }
                } else {
                    log.error("stream not found");
                    throw new InterstitialException("Stream not found");
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.red5pro.interstitial.api.IInterstitialRequestHandler#resume(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void resume(String user, String digest, String path) throws InterstitialException {
        IInterstitialStream stream = (IInterstitialStream) app.getLiveStream(getStreamName(path));
        if (stream != null) {
            log.info("Resume {}", path);
            if (stream instanceof IInterstitialStream) {
                stream.getInterstitialEngine().resumeProgram();
            } else {
                log.error("Stream not an InterstitialStream. Config red5-common.xml to com.red5pro.interstitial.InterstitialStream");
            }
        } else {
            throw new InterstitialException("Resume failed. Stream " + path + " not found.");
        }
    }

    /* (non-Javadoc)
     * @see com.red5pro.interstitial.api.IInterstitialConfiguration#configure(com.red5pro.interstitial.api.IInterstitialStream, com.red5pro.interstitial.api.InterstitialSession)
     */
    @Override
    public InterstitialSession configure(IInterstitialStream stream, InterstitialSession session) {
        return session;
    }

    /* (non-Javadoc)
     * @see com.red5pro.interstitial.api.IInterstitialConfiguration#queueSession(com.red5pro.interstitial.api.InterstitialSession)
     */
    @Override
    public void queueSession(InterstitialSession session) {
        session.queue();
    }
}
