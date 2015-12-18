package
{
	import flash.display.Sprite;
	import flash.events.Event;
	import flash.events.NetStatusEvent;
	import flash.external.ExternalInterface;
	import flash.geom.Matrix;
	import flash.geom.Point;
	import flash.media.Video;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	import flash.text.TextField;
	import flash.text.TextFormat;
	import flash.utils.setTimeout;
	
	[SWF(width=340, height=280, backgroundColor=0x000)]
	public class Subscriber extends Sprite {
		
		private var netStream:NetStream;
		private var netConnection:NetConnection;
		private var bufferLength:int = 2;
		
		private var video:Video;
		private var timeField:TextField;
		
		private var streamName:String;
		private var app:String = "live";
		private var streamHost:String = "localhost";	
		
		public function Subscriber() {
			
			super();
			
			// Optional provided values
			if(loaderInfo.parameters.stream) {
				streamName = loaderInfo.parameters.stream;
			}
			if(loaderInfo.parameters.host) {
				streamHost = loaderInfo.parameters.host;
			}
			if( loaderInfo.parameters.app) {
				app = loaderInfo.parameters.app;
			}
			if(loaderInfo.parameters.buffer) {
				bufferLength = loaderInfo.parameters.buffer
			}
			
			init();
			
		}
		
		private function init():void {
			
			var endpoint:String = "rtmp://" + streamHost + "/" + app;
			
			// Video display
			video = new Video(320, 240);
			video.x = 10;
			video.y = 30;
			addChild(video);
			
			// Time display
			var format:TextFormat = new TextFormat("_sans", 14, 0xFFFFFF, false);
			timeField = new TextField();
			timeField.setTextFormat(format);
			timeField.defaultTextFormat = format;
			timeField.text = [endpoint, bufferLength].join(" ");
			timeField.width = 310;
			timeField.height = 20;
			timeField.x = 5;
			timeField.y = 5;
			addChild(timeField);
			
			if(ExternalInterface.available) {
				try {
					ExternalInterface.addCallback("viewStream", handleInvokeViewStream);
					ExternalInterface.addCallback("resetHost", handleResetHost);
				}
				catch(e:Error) {
					// Not supported. Most likely Security issue.
				}
			}
			
			startSubscription();
			
		}
		
		private function stopSubscription():void {
			
			if(netStream != null) {
				netStream.removeEventListener(NetStatusEvent.NET_STATUS, onStatus, false);
				netStream.close();
			}
			if(netConnection != null) {
				netConnection.removeEventListener(NetStatusEvent.NET_STATUS, onStatus, false);
				netConnection.close();
			}
			
			removeEventListener(Event.ENTER_FRAME, onFrame, false);
			timeField.text = "Stream stopped.";
			netStream  = null;
			netConnection = null;
			
		}
		
		private function startSubscription():void {
			
			var endpoint:String = "rtmp://" + streamHost + "/" + app;
			
			// NetConnection instance
			netConnection = new NetConnection();
			netConnection.client = this;
			netConnection.addEventListener(NetStatusEvent.NET_STATUS, onStatus, false, 0, true);
			netConnection.connect(endpoint);
			addEventListener(Event.ENTER_FRAME, onFrame, false, 0, true);
			
		}
		
		private function restartSubscription():void {
			
			if(netConnection != null && netConnection.connected) {
				stopSubscription();
				startSubscription();
			}
			
		}
		
		private function rotateAroundCenter (videoDisplay:Video, angleDegrees:Number):void {
			
			var point:Point = new Point(videoDisplay.x + videoDisplay.width / 2, videoDisplay.y + videoDisplay.height / 2); 
			var m:Matrix = videoDisplay.transform.matrix;
			m.tx -= point.x;
			m.ty -= point.y;
			m.rotate(angleDegrees * (Math.PI / 180));
			m.tx += point.x;
			m.ty += point.y;
			videoDisplay.transform.matrix = m;
			
		}
		
		private function onFrame(event:Event):void {
			
			if(netConnection.connected && netStream) {
				timeField.text = "Time: " + netStream.time.toString();
			}
			
		}
		
		protected function onStatus(event:NetStatusEvent):void {
			
			timeField.text = event.info.code;
			
			switch(event.info.code) {
				
				case "NetConnection.Connect.Success":
					netStream = new NetStream(netConnection);
					netStream.client = this;
					netStream.bufferTime = bufferLength;
					netStream.addEventListener(NetStatusEvent.NET_STATUS, onStatus, false, 0, true);
					
					video.attachNetStream(netStream);
					netStream.play(streamName);
					break;
				
			}
			
		}
		
		public function handleInvokeViewStream(value:String):void {
			
			if(this.streamName != value ) {
				this.streamName = value;
				restartSubscription();
			}
			
		}
		
		public function handleResetHost(value:String):void {
			
			if(this.streamHost != value) {
				this.streamHost = value;
				restartSubscription();
			}
		
		}
		
		/**
		 * API invoked by Red5 Connection
		 */
		public function remoteTest(obj:Object=null):void {}
		public function onBWDone(obj:Object=null):void {}
		public function onBWCheck(obj:Object=null):void {}
		
		public function onMetaData(obj:Object):void {
			
			var t:String;
			for(t in obj ) {
				// If rotation instruction has come in on metadata, handle
				if(t == "orientation") {
					var rotation:int = parseInt(obj[t]);
					video.rotation=0;
					video.x=0;
					video.y=40;
					rotateAroundCenter(video, rotation);
				}
			}
			
		}
	}
}