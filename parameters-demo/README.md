Red5 Parameters Demo
===
The parameters-demo application responds to a user provided password by allowing or disallowing connection to the server. Flash clients must pass the value 'some_secret_value' in the net connection connect call, and Red5 Pro rtsp clients must set 'some_secret_value;' in the client configuration. Without them, the sever will respond by rejecting the connection.
The application also registers itself as the IStreamPlaybackSecurity and IStreamPublushSecurity handler.

The more advanced parameters demo uses a sha 256 digest to create a one-way encoded cookie.
It requires 5 paramters to be passed from the clients during connection.

user-id, broadcast-name, stream-action(play or publish ), unitx-time milliseconds, 256 sha digest.

THe application first checks the age, and then recreates the digest with the provided parameters.
If the digest given by the user does not match the recreated digest, then the client is tampered with.
Creating a secure session with a full log-in is out of the scope of this demo.
The secure session should provide the 5 paramters via an ssl web call or embedded in the html page for web clients.
This demo provides a simple jsp to create a digest with timestamp. 

http://localhost:5080/live/gettoken.jsp?type=publish&userId=andy&broadcastId=publisher1

The return should look like  "digest-time", and should be split by omitting the dash.

Use the generation paramters together with the time and digest returned to complete the connection set.

TODO
===
set up ssl cert for your server.

connect red5 app to sql or nosql services.

build a log-in page, and provide a way for the client to receive the allowed action parameters.

look up the user id and stream permissions during the digest creation. 

optionally edit the digest computation and recompile the so it is unique. 
