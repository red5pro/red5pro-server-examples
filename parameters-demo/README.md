Red5 Parameters Demo
===
The parameters-demo application responds to a user provided password by allowing or disallowing connection to the server. Flash clients must pass the value 'some_secret_value' in the net connection connect call, and Red5 Pro rtsp clients must set 'some_secret_value;' in the client configuration. Without them, the sever will respond by rejecting the connection.
The application also registers itself as the IStreamPlaybackSecurity and IStreamPublushSecurity handler.