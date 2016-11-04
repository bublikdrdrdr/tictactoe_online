package com.bublik.niewymyslilemnazwy2;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 * Created by Bublik on 15-Mar-16.
 * very very bad test v0.0.0.0.0.-1 alpha^(-2)
 */
public class CSC implements Runnable{

    private Runnable runnable;
    private Thread thread;
    private Socket socket;
    private InetAddress inetAddress;
    private int port;
    private int max_start_time = 2000;
    private long max_wait_time = 3000;
    private  boolean exit = false;
    private InputStream inputStream;
    private OutputStream outputStream;
    private byte[] buffer;
    private int buffer_size;
    private ActionsListener actionsListener;
    private AppCompatActivity appCompatActivity;


    @Override
    public void run() {
        OnRun();
        if (socket!=null)
        {
            if (socket.isConnected())
            {
                try {
                    socket.close();
                } catch (IOException e) {
                    OnException(e);
                }
            }
        }
        OnDisconnected();
    }

    private void OnRun()
    {
        boolean connected = false;
   //     socket = new Socket();
        boolean STE = false;
        while (!connected) {
            if (exit) return;
            try {
                STE = false;
                socket = new Socket();
              //  socket = new Socket(inetAddress, port);
                socket.connect(new InetSocketAddress(inetAddress, port), max_start_time);
                //socket.connect(new InetSocketAddress(inetAddress, port));
                socket.setSoTimeout(max_start_time);
                OnConnect();
                connected = true;
            } catch (SocketTimeoutException e) {
                //
                STE = true;
            }
            catch (Exception e)
            {
                String s = e.getMessage();
                if (!STE) {
                    OnException(e);
                    return;
                }
            }
        }

        try
        {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (Exception e)
        {
            OnException(e);
            return;
        }

        while (!exit)
        {
            try {
               /* long startTime = System.currentTimeMillis();
                long timeSpent = System.currentTimeMillis() - startTime;*/
                while (inputStream.available()==0)
                {
                    Thread.sleep(50);
                    if (exit) return;
                    if (!socket.isConnected()) return;
                }
                inputStream.read(buffer);
                OnGetMessage(buffer);
            } catch (Exception e)
            {
                OnException(e);
                return;
            }
        }
    }

    public CSC(ActionsListener listener, String IP, int port, int packed_size)
    {
        actionsListener = listener;
        this.port = port;
        buffer_size = packed_size;
        buffer = new byte[buffer_size];
        try {
            inetAddress = InetAddress.getByName(IP);
        } catch (Exception e)
        {
            OnException(e);
            return;
        }
        runnable = this;
        thread = new Thread(runnable);
        thread.start();
    }

    public void SetActivity(AppCompatActivity appCompatActivity)
    {
        this.appCompatActivity = appCompatActivity;
    }

    public void Send(byte[] data)
    {
        if (socket!=null)
        {
            if (socket.isConnected())
            {
                try {
                    outputStream.write(data);
                    outputStream.flush();
                } catch (Exception e)
                {
                    actionsListener.OnException(e);
                }
            }
        }
    }

    private void OnConnect()
    {
        actionsListener.OnServerConnected();
    }

    private void OnException(Exception e)
    {
        actionsListener.OnException(e);
    }

    private void OnDisconnected()
    {
        actionsListener.OnServerDisconnected();
    }

    private void OnGetMessage(byte[] data)
    {
        actionsListener.OnGetMessage(data);
    }



    public interface ActionsListener
    {
        /**
         *  Methods must be synchronized, static or executed after UI thread
         *
         * public void OnServerConnected() {
         * doSomething();
         * }
         *
         * private static void doSomething()
         * {
         * ...
         * }
         *
         * private synchronized void doSomething()
         * {
         * ...
         * }
         *
         * private void doSomething{
         * activity.runOnUiThread(new Runnable() {
         * @Override
         * public void run() {
         * textView.setText(text);
         * }
         * });
         * }
         */
        void OnGetMessage(byte[] bytes);
        void OnServerDisconnected();
        void OnException(Exception e);
        void OnServerConnected();
    }

    public synchronized void Exit()
    {
        exit = true;
    }

    public void SetUIUpdate()
    {
       /* if (appCompatActivity!=null)
        {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionsListener.OnUpdateUi();
                }
            });
        }*/
    }

}
