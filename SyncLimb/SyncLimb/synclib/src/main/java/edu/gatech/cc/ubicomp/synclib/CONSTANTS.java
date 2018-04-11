package edu.gatech.cc.ubicomp.synclib;

/**
 * Created by arrc on 4/10/2018.
 */

public class CONSTANTS {
    //    static String IP_ADDRESS = "128.61.8.125";
    static String IP_ADDRESS = "192.168.219.103";
    //    static String IP_ADDRESS = "192.168.1.196";

    static int PORT = 12562;
    public static final int SENDING_INTERVAL = 0;

    public static final int SENSOR_INTERVAL = 7;

    public static String getIpAddress() {
        return IP_ADDRESS;
    }

    public static void setIpAddress(String ipAddress) {
        IP_ADDRESS = ipAddress;
    }

    public static int getPORT() {
        return PORT;
    }

    public static void setPORT(int PORT) {
        CONSTANTS.PORT = PORT;
    }

    public static int getSendingInterval() {
        return SENDING_INTERVAL;
    }

    public static int getSensorInterval() {
        return SENSOR_INTERVAL;
    }

    public static int getByteSize() {
        return BYTE_SIZE;
    }

    public static void setByteSize(int byteSize) {
        BYTE_SIZE = byteSize;
    }

    // variable to share with sending part
    // Gyro Acc, Rotvec
    // 4,4,4, 4,4,4, 4,4,4,4 8
    static int BYTE_SIZE = 48;



}

