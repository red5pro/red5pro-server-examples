package com.infrared5.red5pro.studio.feed;


import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;

import org.red5.io.mp3.impl.MP3Header;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.message.Header;

import org.slf4j.Logger;

/**
 * @author Wittawas Nakkasem (vittee@hotmail.com)
 * @author Andy Shaules (bowljoman@hotmail.com)
 *
 */
public class AudioParser {

	private static Logger log = Red5LoggerFactory.getLogger(AudioParser.class);


	public static final int[] AAC_SAMPLERATES = { 96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000,
		11025, 8000, 7350 };

	private int saveAACfrequency = -1;

	private int saveAACsamplesPerFrame = -1;

	private int aacTimecodeOffset = 0;

	private int lastSample = 0;

	private boolean isFirst;

	public int lastTimecode = 0;

	private byte[] tail = null;

	private boolean frameSynched;

	private int profile = 0;

	private int sampleRateIndex = 0;

	private int channels = 0;

	private int currentFrameLeft = 0;

	private int raw_data_block = 0;

	private IoBuffer buffer = null;

	public Receiver output;

	public double totalTime=0;

	public int frameTimeOffset=0;

	private char[] prev_bits=new char[0];

	private MP3Header testHead;
	private MP3Header goodHead;

	private boolean isSync;


	private long lastConfig;

	public int getDeltaTime()
	{
		return frameTimeOffset;
	}
	public AudioParser(Receiver outputSink) {
		output = outputSink;
		isFirst=true;
	}

	public void reset() {

		isSync=false;
		goodHead=null;
		testHead=null;
		tail = null;
		buffer = null;
		currentFrameLeft = 0;
		frameSynched = false;
		isFirst = true;
		saveAACfrequency = -1;
		saveAACsamplesPerFrame = -1;
		aacTimecodeOffset = 0;
		lastTimecode = 0;
		lastSample = 0;
		totalTime=0;
	}

	public void onAACData(byte[] feed, int offset2) {

		if(feed.length==0)
			return;

		byte[] data;
		// merge previous tail
		if (tail != null) {
			IoBuffer bb = IoBuffer.allocate(tail.length + feed.length);
			bb.put(tail);
			for (int i = 0; i < feed.length; i++) {
				bb.put((byte) feed[i]);
			}

			data = bb.array();
			tail = null;
		} else {
			IoBuffer bb = IoBuffer.allocate(feed.length);
			for (int i = 0; i < feed.length; i++) {
				bb.put((byte) feed[i]);
			}

			data = bb.array();
		}

		// parse ISO 14496-3 != esds box from an MP4/F4V
		int offset = 0;
		//int adtsSkipped = 0;
		while ((data.length - offset) > 7) {
			if (!frameSynched) {
				if ((data[offset++] & 0xff) == 0xff) {
					if ((data[offset++] & 0xf6) == 0xf0) {
						
 						profile = (data[offset] & 0xC0) >> 6;
						sampleRateIndex = (data[offset] & 0x3C) >> 2;
						channels = ((data[offset] & 0x01) << 2) | ((data[offset + 1] & 0xC0) >> 6);

			// frame length, ADTS excluded
			currentFrameLeft = (((data[offset + 1] & 0x03) << 8) | ((data[offset + 2] & 0xff) << 3) | ((data[offset + 3] & 0xff) >>> 5)) - 7;
			raw_data_block = data[(offset + 4)] & 0x3;
			offset += 5; // skip ADTS		
			//adtsSkipped += 7;
			frameSynched = true;
					}
				}

			} else {
				int remain = (data.length - offset);
				int bytesToRead = currentFrameLeft;
				if (bytesToRead > remain)
					bytesToRead = remain;

				if (buffer == null) {
					buffer = IoBuffer.allocate(2);
					buffer.setAutoExpand(true);
					buffer.put(new byte[] { (byte) 0xAF, (byte) 0x01 });
				}

				buffer.put(data, offset, bytesToRead);

				offset += bytesToRead;
				currentFrameLeft -= bytesToRead;

				if (currentFrameLeft == 0) {
					try {
						buffer.flip();
						IoBuffer newBuffer = IoBuffer.allocate(buffer.limit());
						
						newBuffer.put(buffer);
						if (AAC_SAMPLERATES.length <= sampleRateIndex) {
							isFirst = false;
							buffer = null;
							frameSynched = false;
							return;
						}
						newBuffer.flip();
						deliverAACFrame(newBuffer, AAC_SAMPLERATES[sampleRateIndex], (raw_data_block + 1) * 1024, offset2);
					}catch(Exception e){
						e.printStackTrace();
					} finally {
						isFirst = false;
						buffer = null;
						frameSynched = false;
					}
				}
			}
		}

		// keep tail
		int remain = data.length - offset;
		if (remain > 0) {
			try {
				tail = Arrays.copyOfRange(data, offset, data.length);
			} catch (Exception e) {
				log.error("Failed to copy tail {}" ,e.getCause().getMessage());
			}
		}
	}

	private void deliverAACFrame(IoBuffer buffer, int sampleRate, int sampleCount,int offset) {

		if (saveAACfrequency == -1) {
			saveAACfrequency = sampleRate;
			saveAACsamplesPerFrame = sampleCount;
		}

		if ((saveAACfrequency != sampleRate) || (saveAACsamplesPerFrame != sampleCount)) {
			saveAACfrequency = sampleRate;
			saveAACsamplesPerFrame = sampleCount;
			aacTimecodeOffset = lastTimecode;
			lastSample = 0;
		}

		long timeSpan = 0;
		if (isFirst) {
			log.debug("first frame @ sample rate:"+ AAC_SAMPLERATES[sampleRateIndex] );
			//	System.out.println("first frame @ delta:"+ time+":"+offset );
			
			timeSpan = 0;
			lastSample = 0;
			lastTimecode = 0;
			totalTime=0;
			sendAACConfig();
			lastConfig=System.currentTimeMillis();

		} else {
			lastSample += sampleCount;
			timeSpan = aacTimecodeOffset + sample2TimeCode(lastSample, sampleRate) - lastTimecode;
			//timeSpan+=frameTimeOffset;
			//clockCurrentTime=System.currentTimeMillis();
			totalTime+=timeSpan;
			lastTimecode=(int) totalTime;
			if(System.currentTimeMillis()-lastConfig > 5000){
				sendAACConfig();
				lastConfig=System.currentTimeMillis();
			}
		}

		IRTMPEvent audio = new AudioData(buffer);
		audio.setTimestamp((int) lastTimecode);
		audio.setHeader(new Header());
		audio.getHeader().setTimer( (int) lastTimecode );
		audio.getHeader().setTimerDelta(0);
		audio.getHeader().setTimerBase(lastTimecode);

		//System.out.println("@audio frt delta:"+(frameTime-lastTimecode));
		//		audio.setTimestamp((int) frameTime);
		//		audio.setHeader(new Header());
		//		audio.getHeader().setTimer( (int) frameTime );
		//		audio.getHeader().setTimerDelta(0);
		//		audio.getHeader().setTimerBase(frameTime);		


		output.dispatchEvent(audio);
	}
	public void sendAACConfig(){

		log.debug("sendAACConfig" );
		IoBuffer buffer = IoBuffer.allocate(10);
		buffer.setAutoExpand(true);
		buffer.put((byte) 0xaf);
		buffer.put((byte) 0x00);
		buffer.put(getAACSpecificConfig());

		buffer.flip();
		AudioData data = new AudioData(buffer);
		data.setHeader(new Header());
		data.getHeader().setTimer(lastTimecode);
		data.setTimestamp(lastTimecode);

		output.dispatchEvent(data);
	}
	public byte[] getAACSpecificConfig() {
		return new byte[] { (byte) (/*0x10 |*/((profile > 2) ? 2 : profile << 3) | ((sampleRateIndex >> 1) & 0x03)),
				(byte) (((sampleRateIndex & 0x01) << 7) | ((channels & 0x0F) << 3)) };

	}

	private long sample2TimeCode(long time, int sampleRate) {
		return (time * 1000L / sampleRate);
	}

	public void onMP3Data(int[] dta, int timestamp, int timeOffset) {

		otherParse(dta);	
	}

	private void save(char[] ba, int from)
	{
		int y=0;
		prev_bits = new char[ba.length-from];

		for(int i=from; i<ba.length; i++ )
		{
			prev_bits[y++]= ba[i];
		}


	}

	public synchronized void otherParse(int[] dta)
	{
		char[]ba=new char[prev_bits.length+dta.length];

		for(int g=0 ; g < prev_bits.length ; g++ )
		{
			ba[g]= prev_bits[g];
		}


		for(int m = 0 ; m <dta.length ;m++ )
		{
			ba[ m +prev_bits.length ]= (char) (dta[m] & 0xff );
		}

		if(prev_bits.length>0)
		{
			prev_bits=new char[0];
		}

		int nextFrame=0;
		int i=0;

		for( i=0; i < ba.length - 3 ; i++ )
		{//need 4 				


			if (ba[i] != 0xff)
			{//first frame byte?

				continue;
			}
			else
			{
				if ((ba[i+1] & 0xe0) == 0xe0)
				{ 	//possibly frame byte?					
					MP3Header header = null;

					int vall=(ba[i]<<24) |  (ba[i+1]<<16) |  (ba[i+2]<<8) |  (ba[i+3]);
					try
					{						

						header = new MP3Header(vall);

						if(goodHead != null)
						{//huh? should be resync.
							if(header.getBitRate()!= goodHead.getBitRate() ||  header.getSampleRate()!=goodHead.getSampleRate())
							{								

								continue;
							}

						}						

						if(testHead == null)
						{							
							testHead=header;
						}
					}
					catch ( Exception e)
					{
						continue;
					}

					if(header.getSampleRate()<=0 || header.getBitRate()<=0 ||header.frameDuration()<=0 || header.frameSize()<=0 )
					{							
						nextFrame=0;	
						continue;
					}					

					if(!isSync)
					{						
						nextFrame= i + header.frameSize();

						if(nextFrame != 0)
						{					
							if(nextFrame < ba.length )
							{ 
								if( ba[nextFrame] == 0xff)
								{						
									isSync=true;									
									goodHead=testHead;
								}
							}				
							nextFrame=0;
						}

						if(!isSync)
							continue;
					}				

					int rem = ba.length - i ;


					if( isSync &&  rem  <  header.frameSize()  )
					{					
						break;
					}

					byte rtmpHead= 2<<4;//type
					rtmpHead|=3<<2;//sample rate
					rtmpHead|=1<<1;//bits per sample
					rtmpHead|=header.isStereo()?1:0;
					IoBuffer buff=IoBuffer.allocate(header.frameSize()+1);
					buff.put(rtmpHead);

					for(int u=0;u<header.frameSize();u++ )
					{
						buff.put((byte)( ba[u + i ]& 0xff ));
					}

					buff.flip();
					IRTMPEvent audio = new AudioData(buff);
					audio.setTimestamp((int) lastTimecode);
					audio.setHeader(new Header());
					audio.getHeader().setTimer((int) lastTimecode );
					audio.getHeader().setTimerDelta(0);
					audio.getHeader().setTimerBase(lastTimecode);

					output.dispatchEvent(audio);

					totalTime+=header.frameDuration();;
					lastTimecode=(int) totalTime;
					i += header.frameSize()-1;
				}	
			}				
		}

		if(ba.length-i<=0)
			return;		

		save( ba,i);				
	}

	public int getCurrentStreamTime() {

		return lastTimecode;
	}
}
