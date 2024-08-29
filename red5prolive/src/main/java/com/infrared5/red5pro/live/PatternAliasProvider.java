package com.infrared5.red5pro.live;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.red5.net.websocket.WebSocketConnection;
import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.stream.AliasProviderAdapter;

/**
 * Utilizes a simple pattern based approach to provide alias support.
 *
 * @author Paul Gregoire
 */
public class PatternAliasProvider extends AliasProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(PatternAliasProvider.class);

    // published name pattern (stream alias) ie: pub_streamA_1
    private static final Pattern PUBLISH_PATTERN = Pattern.compile("^(pub)_([a-zA-Z0-9]+)_\\d{1,3}");

    // play name pattern (play alias) ie: play_streamA_42
    private static final Pattern PLAY_PATTERN = Pattern.compile("^(play)_([a-zA-Z0-9]+)_\\d{1,3}");

    public PatternAliasProvider() {
        log.info("PatternAliasProvider");
    }

    @Override
    public boolean isAlias(String alias) {
        boolean result = false;
        Matcher matcher = PUBLISH_PATTERN.matcher(alias);
        if (matcher.find()) {
            result = true;
        } else {
            result = PLAY_PATTERN.matcher(alias).find();
        }
        log.debug("isAlias - alias: {} result: {}", alias, result);
        return result;
    }

    @Override
    public String resolvePlayAlias(String alias, IConnection webSocket) {
        log.debug("resolvePlayAlias - alias: {}", alias);
        Matcher matcher = PLAY_PATTERN.matcher(alias);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            log.debug("No stream found for play alias: {}", alias);
        }
        return null;
    }

    @Override
    public String resolvePlayAlias(String alias, WebSocketConnection webSocket) {
        log.debug("resolvePlayAlias - alias: {}", alias);
        Matcher matcher = PLAY_PATTERN.matcher(alias);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            log.debug("No stream found for play alias: {}", alias);
        }
        return null;
    }

    @Override
    public String resolveStreamAlias(String alias, IConnection conn) {
        log.debug("resolveStreamAlias - alias: {}", alias);
        Matcher matcher = PUBLISH_PATTERN.matcher(alias);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            log.debug("No stream found for stream alias: {}", alias);
        }
        return null;
    }

    @Override
    public String resolveStreamAlias(String alias, WebSocketConnection webSocket) {
        log.debug("resolveStreamAlias - alias: {}", alias);
        Matcher matcher = PUBLISH_PATTERN.matcher(alias);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            log.debug("No stream found for stream alias: {}", alias);
        }
        return null;
    }

}
