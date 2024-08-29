package com.infrared5.red5pro.live;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.red5.codec.AACAudio;
import org.red5.codec.AVCVideo;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.IVideoStreamCodec;
import org.red5.net.websocket.WebSocketConnection;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.cluster.streams.Provision;
import com.red5pro.cluster.streams.ProvisionIndex;
import com.red5pro.override.IProStream;
import com.red5pro.server.stream.AliasProviderAdapter;

/**
 * Utilizes our Provision system to provide alias support. The resolve methods are meant to mostly mimic the Wowza
 * example to make implementation easier.
 *
 * @author Paul Gregoire
 */
public class ProvisionAliasProvider extends AliasProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProvisionAliasProvider.class);

    // for provision access, we need the scope associated with this instance
    private final IScope scope;

    public ProvisionAliasProvider(IScope scope) {
        log.info("ProvisionAliasProvider - scope: {}", scope);
        this.scope = scope;
    }

    public Provision resolveProvision(String context, String withName) {
    	
        Iterator<ProvisionIndex> provisions = ProvisionIndex.providers.iterator();
        if (provisions.hasNext()) {
            ProvisionIndex provider = provisions.next();            
            Provision provision = provider.resolveProvision(context, withName);
            if (provision != null) {
            	return provision;
            }
        }
        return null;
    }
    
    
    @Override
    public boolean addPlayAlias(String alias, String streamName) {
        log.debug("addPlayAlias - alias: {} streamName: {}", alias, streamName);
        boolean result = false;
        return result;
    }

    @Override
    public boolean addStreamAlias(String alias, String streamName) {
        boolean result = false;

        return result;
    }

    @Override
    public boolean isAlias(String alias) {
        boolean result = false;
        return result;
    }

    @Override
    public boolean removeAllAliases(String streamName) {
        log.debug("removeAllAliases - streamName: {}", streamName);
        boolean result = false;
        Provision prov = resolveProvision(scope.getName(), streamName);
        if (prov != null) {
            prov.setStreamNameAlias(null);
            prov.setAliases(null);
            result = true;
            log.debug("Results: {} stream name: {}", prov.getStreamNameAlias(), streamName);
        } else {
            log.debug("No provision found for removals, stream name: {}", streamName);
        }
        return result;
    }

    @Override
    public boolean removePlayAlias(String alias) {
        return false;
    }

    @Override
    public boolean removeStreamAlias(String alias) {

        return false;
    }

    @Override
    public String resolvePlayAlias(String alias, IConnection conn) {

        return null;
    }

    @Override
    public String resolvePlayAlias(String alias, WebSocketConnection webSocket) {

        return null;
    }

    @Override
    public String resolveStreamAlias(String alias, IConnection conn) {

        return null;
    }

    @Override
    public String resolveStreamAlias(String alias, WebSocketConnection webSocket) {

        return null;
    }

    public Set<String> getAllPlayAliases(String streamName) {
        log.debug("getAllPlayAliases - streamName: {}", streamName);
        Set<String> aliases = new HashSet<>();
        Provision prov = resolveProvision(scope.getName(), streamName);
        if (prov != null) {
            aliases = prov.getAliases();
        } else {
            log.debug("No provision found for play aliases, stream name: {}", streamName);
        }
        return aliases;
    }
}
