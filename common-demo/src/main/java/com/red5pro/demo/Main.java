package com.red5pro.demo;

import com.red5pro.util.ConnectionUtils.ConnectionType;

/**
 * Example class to demo the use of the red5pro-common dependency library.
 * 
 * @author Paul Gregoire
 */
public class Main {

    public static void main(String[] args) {
        // make a simple call to an enum in the library
        Enum<ConnectionType> type = ConnectionType.findByClassName("com.red5pro.webrtc.RTCConnection");
        System.out.println("Connection type: " + type);
    }

}
