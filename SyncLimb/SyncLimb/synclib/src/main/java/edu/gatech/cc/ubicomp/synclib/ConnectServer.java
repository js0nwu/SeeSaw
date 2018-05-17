package edu.gatech.cc.ubicomp.synclib;


import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

/**
 * Created by arrc on 3/20/2018.
 */

public class ConnectServer extends AsyncTask<Void, Void, Void> {

    private Boolean isGLASS = false;

    long last_t, start_t, done_t;
    private ByteBuffer msgBuffer;
    private BlockingQueue bufferQueue;
    private Boolean isSending = false;

    private Boolean isWriting = false;

    //    String baseDir = "/mnt/sdcard/";
//    String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SyncLimb/";

    String baseDir_str;
    File baseDir;
    FileOutputStream file_stream;
    DecimalFormat df = new DecimalFormat("0.00##");
    DecimalFormat no_df = new DecimalFormat("0");

    long writing_start_t = 0;
    private final int write_file_term = 1000 * 60 * 10;
//    int write_file_term = 1000;


    public ConnectServer() {
        super();

        // store float values as byte array
        msgBuffer = ByteBuffer.allocate(CONSTANTS.BYTE_SIZE);



    }

    public ConnectServer(BlockingQueue q) {
        super();
        this.bufferQueue = q;
        this.isGLASS = false;

        baseDir_str = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SyncLimb/";


    }

    public ConnectServer(BlockingQueue q, Boolean isGLASS) {

        super();
        this.bufferQueue = q;
        this.isGLASS = isGLASS;
        if(isGLASS){
            baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        }else{
            baseDir_str = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SyncLimb/";
        }




    }

    public void setIsSending(boolean isSending) {
        this.isSending = isSending;
    }

    public void setWriting(String name) {
        isWriting = true;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        String currentDateTime = sdf.format(new Date());


        File dir;
        String file_name;
        File file;
        if(isGLASS){
            dir = baseDir;
            file_name = "/Camera/"+currentDateTime + "_" + name + ".csv";
            file = new File(baseDir, file_name);
            if (!dir.exists()) {
                dir.mkdirs();
                Log.d("sendToServer", "Folder created: " + file_name);
            }
        }else{
            dir = new File(baseDir_str);
            file_name = baseDir_str + currentDateTime + "_" + name + ".csv";
            file = new File(file_name);
            if (!dir.exists()) {
                dir.mkdirs();
                Log.d("sendToServer", "Folder created: " + baseDir);
            }
        }


        try {
            file_stream = new FileOutputStream(file);
            writing_start_t = System.currentTimeMillis();
            Log.d("sendToServer", "File Created:  " + file_name);
        } catch (FileNotFoundException e) {
            Log.e("sendToServer", "File not found: " + e.toString());
        }
    }

    public Boolean getSending() {
        return isSending;
    }

    public Boolean getWriting() {
        return isWriting;
    }

    @Override
    protected Void doInBackground(Void... params) {
        sendToServer();
        return null;
    }

    public void sendToServer() {

        try {
            DatagramSocket socket = new DatagramSocket(CONSTANTS.PORT);
            InetAddress IPAddress = InetAddress.getByName(CONSTANTS.IP_ADDRESS);
            int PORT = CONSTANTS.PORT;
            int interval = CONSTANTS.SENDING_INTERVAL;
            String one_line = "";
            Log.d("sendToServer", "loop starting");

            long last_time = 0;
            while (this.isSending || this.isWriting) {
                //String str = new String(msgBuffer.array());
                start_t = System.currentTimeMillis();
//                Log.d("sending", ""+ start_t + "|" +last_t +"="+(start_t - last_t));

                try {
                    while (bufferQueue.size() > 0) {
                        msgBuffer = (ByteBuffer) bufferQueue.take();
                        if(last_time == msgBuffer.getLong(40))
                            continue;


                        if (this.isSending) {
                            DatagramPacket pkt = new DatagramPacket(msgBuffer.array(),
                                    msgBuffer.capacity(),
                                    IPAddress, PORT);

                            socket.send(pkt);
                        }
                        if (this.isWriting) {
                            one_line += df.format(msgBuffer.getFloat(0));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(4));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(8));
                            one_line += ", ";

                            one_line += df.format(msgBuffer.getFloat(12));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(16));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(20));
                            one_line += ", ";

                            one_line += df.format(msgBuffer.getFloat(24));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(28));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(32));
                            one_line += ", ";
                            one_line += df.format(msgBuffer.getFloat(36));
                            one_line += ", ";

//                            one_line += no_df.format(msgBuffer.getLong(40));
                            one_line += msgBuffer.getLong(40);



                            one_line += "\n";
//                            Log.d("sendToServer", one_line);
                            file_stream.write(one_line.getBytes());
                            one_line = "";

                            if ((System.currentTimeMillis() - writing_start_t) > write_file_term) {
                                this.file_stream.close();
                                this.setWriting("testing");
                                Log.d("sendToServer", "created new file for divide saving");
                            }
                        }

                        last_time = msgBuffer.getLong(40);
                    }
                } catch (InterruptedException ex) {
                    Log.d("sendToServer", "Error on take");
                }

                done_t = System.currentTimeMillis();
                if ((done_t - start_t) > interval) {
//                    Log.e("send", "NO NEED INTERVAL  interval:" + (start_t-last_t) + "| sending time:" + (done_t-start_t) );
                } else {
                    Thread.sleep(interval - (done_t - start_t));
//                    Log.d("send", "interval:" + (start_t-last_t) + "| sending time:" + (done_t-start_t) );
                }
                last_t = done_t;


            }
            socket.close();
        } catch (Exception e) {
            Log.e("socket", "" + e.toString());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (this.isWriting) {
            try {
                this.file_stream.close();
            } catch (IOException ex) {
                Log.d("sendToServer", "Can not close file: " + ex.toString());
            }
            this.isSending = false;
        }

        this.isWriting = false;


    }
}