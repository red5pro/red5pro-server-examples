package com.infrared5.red5pro.studio.feed;

import java.util.Map;

import org.red5.server.net.rtmp.event.IRTMPEvent;

public interface Receiver {

	void onMetaData(Map<String, Object> meta);

	void reset(String string, String string2);

	void onRawData(byte[] pushIt);

	void onDisconnected();

	void dispatchEvent(IRTMPEvent media);

}
