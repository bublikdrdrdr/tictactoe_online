package com.bublik.niewymyslilemnazwy2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import org.w3c.dom.Text;

public class NetGame extends AppCompatActivity {

    RadioButton serverRB;
    RadioButton clientRB;
    Button connectB;
    Button start_game_b;
    Button cancel_button;
    LinearLayout client_layout;
    LinearLayout server_layout;
    TextView statusView;
    EditText serverPort;
    EditText clientPort;
    EditText clientIP;
    TextView serverIP;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_game);

        MainActivity.mthis.netGame = this;

        serverRB = (RadioButton) findViewById(R.id.server_radio);
        clientRB = (RadioButton) findViewById(R.id.client_radio);
        connectB = (Button) findViewById(R.id.connect_button);
        cancel_button = (Button) findViewById(R.id.cancel_button);
        start_game_b = (Button) findViewById(R.id.start_game_b);
        client_layout = (LinearLayout) findViewById(R.id.client_layout);
        server_layout = (LinearLayout) findViewById(R.id.server_layout);
        statusView = (TextView) findViewById(R.id.statusView);

        serverRB.setOnClickListener(onClickListener);
        clientRB.setOnClickListener(onClickListener);
        connectB.setOnClickListener(onClickListener);
        cancel_button.setOnClickListener(onClickListener);
        start_game_b.setOnClickListener(onClickListener);

        clientIP = (EditText) findViewById(R.id.ipEdit);
        clientPort = (EditText) findViewById(R.id.portEdit);
        serverPort = (EditText) findViewById(R.id.serverPortEdit);

        serverIP = (TextView) findViewById(R.id.serverIPtextView);

        if (MainActivity.mthis.online_mode==0) {
            setStatus(ConnectionStatus.offline);
        } else
        {
            setStatus(ConnectionStatus.connected);
        }
    }

    private void setRetResult()
    {
        Intent intent = new Intent();
        if (serverRB.isChecked()) {
            intent.putExtra("mode", "server");
        } else
        {
            intent.putExtra("mode", "client");
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId())
            {
                case R.id.server_radio:
                    setLayoutsVis(true);
                    break;
                case R.id.client_radio:
                    setLayoutsVis(false);
                    break;
                case R.id.cancel_button:
                    Intent intent = new Intent();
                    setResult(RESULT_CANCELED, intent);
                    finish();
                    break;
                case R.id.start_game_b:
                    setRetResult();
                    break;
                case R.id.connect_button:
                    if (serverRB.isChecked())
                    {
                        tryConnect(true,"", Integer.valueOf(serverPort.getText().toString()));
                    } else
                    {
                        tryConnect(false, clientIP.getText().toString(), Integer.valueOf(clientPort.getText().toString()));
                    }
            }
        }
    };

    private void setLayoutsVis(boolean server)
    {
        if (server)
        {
            server_layout.setVisibility(View.VISIBLE);
            client_layout.setVisibility(View.GONE);
        } else
        {
            server_layout.setVisibility(View.GONE);
            client_layout.setVisibility(View.VISIBLE);
        }
    }

    private void tryConnect(boolean server, String ip, int port)
    {
        if (MainActivity.mthis.online_mode == 1)
        {
            if(MainActivity.mthis.csc!=null)
            {
                MainActivity.mthis.csc.Exit();
            }
        } else
        {
            if(MainActivity.mthis.css!=null)
            {
                MainActivity.mthis.css.Exit();
            }
        }
        int bsize = 256;
        if (server) {
            serverIP.setText(CSS.getIPAddress(true));
            MainActivity.mthis.css = new CSS(port, (CSS.ActionsListener) MainActivity.mthis, bsize);
        } else
        {
            MainActivity.mthis.csc = new CSC(MainActivity.mthis, ip, port, bsize);
        }
        setStatus(ConnectionStatus.connecting);
    }

    public enum ConnectionStatus{offline, connecting, waitingforclient, connected};

    private void setStatus(ConnectionStatus connectionStatus)
    {
        switch (connectionStatus)
        {
            case connected:
                statusView.setText("connected");
                statusView.setTextColor(0xFF00FF00);
                break;
            case offline:
                statusView.setText("offline");
                statusView.setTextColor(0xFFFF0000);
                break;
            case connecting:
                statusView.setText("connecting");
                statusView.setTextColor(0xFFEFD300);
                break;
            case waitingforclient:
                statusView.setText("waiting for a client");
                statusView.setTextColor(0xFFEFD300);
                break;
        }
    }

    public synchronized void ConnectedListener()
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setStatus(ConnectionStatus.connected);
            }
        });
        /*try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        this.finish();*/
    }
}
