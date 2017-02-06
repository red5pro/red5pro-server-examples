package com.infrared5.red5pro.studio.switching;

import org.red5.server.api.stream.IStreamPacket;

public interface ISwitchStream {

	void dispatch(MediaSource mediaSource, IStreamPacket arg1);

}
