#File Conversion
This demo shows an application adapter overriding the streamBroadcastClose method in order to trigger post processing.
The post process uses ffmpeg installed on the host system path to convert a recorded FLV to an MP4 file.
##Edits
The demo app needs to be compiled with a couple of edits. Occurence of these variables are in the streamBroadcastClose method.

'PATH_TO_RED5_ROOT' should reflect the full path of the server root on the drive which is passed to ffmpeg.

'ToolPath' should reflect the full ffmpeg path. 'c:/ffmpeg/bin/ffmpeg' or similar.

##Running
Install the recompiled application by copying the 'live' folder to your red5 server 'webapps' directory, removing any other 'live' directory first. Start or restart the server.

Configure a publisher to record mode and begin streaming.
After some time, stop streaming. After a short pause, the red5 server will call ffmpeg to convert the file to mp4.

