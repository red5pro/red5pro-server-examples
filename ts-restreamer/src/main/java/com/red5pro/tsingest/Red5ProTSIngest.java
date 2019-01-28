package com.red5pro.tsingest;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.mpegts.MPEGTSTransport;
import com.red5pro.mpegts.plugin.TSIngestEndpoint;
import com.red5pro.mpegts.plugin.TSIngestPlugin;

/**
 * This application adapter allows for manipulation of the MPEG-TS Ingest plugin.
 *
 * @author Paul Gregoire
 */
public class Red5ProTSIngest extends MultiThreadedApplicationAdapter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void appStop(IScope scope) {
        // call super stop!
        super.appStop(scope);
        // kill this scope with a separate thread so there's no odd behavior
        new Thread(() -> {
            try {
                ((WebScope) scope).destroy();
            } catch (Exception e) {
                logger.warn("Exception destroying {}", scope.getName(), e);
            }
        }).start();
    }

    /**
     * Returns a collection of active end-points.
     *
     * @return end-points
     */
    public ConcurrentMap<String, TSIngestEndpoint> list() {
        ConcurrentMap<String, TSIngestEndpoint> endPoints = null;
        Optional<MPEGTSTransport> opt = Optional.ofNullable(TSIngestPlugin.getTransport());
        if (opt.isPresent()) {
            endPoints = opt.get().getEndPoints();
        } else {
            endPoints = new ConcurrentHashMap<>();
        }
        return endPoints;
    }

    /**
     * Creates an end-point for consuming mpeg-ts (ingest).
     *
     * @param unicast if true it'll be unicast, if false it'll be multicast
     * @param ipAddress unicast IP address or multicast group name
     * @param port the port to bind
     * @return streams path
     */
    public String createListenerEndpoint(boolean unicast, String ipAddress, int port) {
        // get the transport
        MPEGTSTransport transport = TSIngestPlugin.getTransport();
        // get current first before attempting to create one
        ConcurrentMap<String, TSIngestEndpoint> endPoints = transport.getEndPoints();
        // create identifier (also used as stream name since its not passed in here)
        String id = String.format("%s%sp%d", (unicast ? "u" : "m"), ipAddress, port);
        if (endPoints.containsKey(id)) {
            logger.warn("End-point already exists for {}:{} {}", ipAddress, port, (unicast ? "unicast" : "multicast"));
            return null;
        }
        // no scope is specified here so get default
        String streamPath = String.format("%s/%s", TSIngestPlugin.getDefaultScope().getContextPath(), id);
        if (unicast) {
            transport.buildUnicastListener(ipAddress, port);
        } else {
            // multicast
            transport.buildListener(ipAddress, port);
        }
        return streamPath;
    }

    /**
     * Creates an end-point for consuming mpeg-ts (ingest).
     *
     * @param unicast if true it'll be unicast, if false it'll be multicast
     * @param ipAddress unicast IP address or multicast group name
     * @param port the port to bind
     * @param streamName
     * @return streams path
     */
    public String createListenerEndpoint(boolean unicast, String ipAddress, int port, String streamName) {
        // get the transport
        MPEGTSTransport transport = TSIngestPlugin.getTransport();
        // get current first before attempting to create one
        ConcurrentMap<String, TSIngestEndpoint> endPoints = transport.getEndPoints();
        // create identifier (also used as stream name since its not passed in here)
        String id = String.format("%s%sp%d", (unicast ? "u" : "m"), ipAddress, port);
        if (endPoints.containsKey(id)) {
            logger.warn("End-point already exists for {}:{} {}", ipAddress, port, (unicast ? "unicast" : "multicast"));
            return null;
        }
        // no scope is specified here so get default
        String streamPath = String.format("%s/%s", TSIngestPlugin.getDefaultScope().getContextPath(), streamName);
        if (unicast) {
            transport.buildUnicastListener(ipAddress, port, streamName);
        } else {
            // multicast
            transport.buildListener(ipAddress, port, streamName);
        }
        return streamPath;
    }

    /**
     * Stops and disposes of an end-point matching the given end-point identifier.
     *
     * @param endPointId
     * @return true if successful and false otherwise
     */
    public boolean disposeEndpoint(String endPointId) {
        // get the transport
        MPEGTSTransport transport = TSIngestPlugin.getTransport();
        // get current first before attempting to create one
        ConcurrentMap<String, TSIngestEndpoint> endPoints = transport.getEndPoints();
        endPoints.forEach((id, endPoint) -> {
            if (id == endPointId) {
                transport.disposeEndpoint(endPoint);
                return;
            }
        });
        return true;
    }

    /**
     * Stops and disposes of an end-point matching the given ipAddress and port.
     *
     * @param ipAddress
     * @param port
     * @return true if successful and false otherwise
     */
    public boolean disposeEndpoint(String ipAddress, int port) {
        // get the transport
        MPEGTSTransport transport = TSIngestPlugin.getTransport();
        // dispose of it
        transport.disposeEndpoint(ipAddress, port);
        // no cast prefix
        String endPointId = String.format("%sp%d", ipAddress.replaceAll("\\.", ""), port);
        ConcurrentMap<String, TSIngestEndpoint> endPoints = transport.getEndPoints();
        // not knowing the whether or not its unicast or multicast, just look at the suffix
        Optional<String> disposed = endPoints.keySet().stream().filter(id -> id.endsWith(endPointId)).findFirst();
        // if its present dispose failed
        return !disposed.isPresent();
    }

}
