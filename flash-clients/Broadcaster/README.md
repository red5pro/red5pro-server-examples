Broadcaster
===
The source of the flash Broadcaster used in the server front-end pages can be found here.

The swf file itself can be found in the [front-end server repo](https://github.com/red5pro/red5pro-server-frontend/tree/master/src/webapps/live).

You can integrate this broadcaster in your html5 web application using the javascript call-outs.
The swf notifies the webpage for broadcast event life cycle and status.

Below is a simple implementation of a javascript handler with all three methods used by the swf.

```javascript
  var broadcast = {
  	start : function(info){
  	console.log("start "+ info.host+"/"+info.streamName);
  	},
  	
  	stop : function(){
  	console.log("broadcast stop");
  	},
  	
  	onStatus : function (status){
  	console.log("onStatus "+ info.host+"/"+info.streamName+"  "+info.status);
  	} 
  };
```

broadcast.start
====
Called when the user presses the broadcast button to begin capturing media and sending to the server.

broadcast.stop
====
Called when the broadcast is terminated via server error or the user presses the button to end capturing media.

broadcast.onStatus
====
Called any time the client receives a net-status message, such as connection fail/success.
