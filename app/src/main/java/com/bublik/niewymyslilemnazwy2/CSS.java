/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bublik.niewymyslilemnazwy2;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Bublik
 */
public class CSS implements Runnable {

    private int port;
    private Thread thread;
    private ActionsListener actionsListener;
    private ServerSocket serverSocket;
    private Socket socket;
    private int timeout = 2000;
    private boolean exit = false;
    private InputStream inputStream;
    private OutputStream outputStream;
    private byte[] buffer;

    @Override
    public void run() {
        OnRun();
        try {
            if (socket != null) {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
            if (serverSocket != null) {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
            }
            actionsListener.onDisconnect();
            
        } catch (Exception e) {
            actionsListener.onException(e);
        }
    }

    private void OnRun() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(timeout);
            while (true) {
                try {
                    if (exit) {
                        return;
                    }
                    socket = serverSocket.accept();
                    break;
                } catch (SocketTimeoutException ste) {
                    //нічого не робимо
                } catch (Exception e) {
                    actionsListener.onException(e);
                    return;
                }
            }
            actionsListener.onConnect();

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            while (true) {
                while (inputStream.available() == 0) {
                    if (exit) {
                        return;
                    }
                    Thread.sleep(100);
                }
                inputStream.read(buffer);
                actionsListener.onGetMessage(buffer);
            }
        } catch (Exception e) {
            actionsListener.onException(e);
            return;
        }

    }
    
    public void Send(byte[] data)
    {
        try{
            outputStream.write(data);
            outputStream.flush();
        } catch (Exception e)
        {
            actionsListener.onException(e);
        }
    }

    public CSS(int port, ActionsListener actionsListener, int buffer_size) {
        this.port = port;
        buffer = new byte[buffer_size];
        this.actionsListener = actionsListener;
        thread = new Thread(this);
        thread.start();

    }
    
    public synchronized void Exit()
    {
        exit = true;
    }

    public interface ActionsListener {

        void onGetMessage(byte[] data);

        void onConnect();

        void onDisconnect();

        void onException(Exception e);
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4) {
                                return sAddr;
                            }
                        } else if (!isIPv4) {
                            int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                            return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

}
