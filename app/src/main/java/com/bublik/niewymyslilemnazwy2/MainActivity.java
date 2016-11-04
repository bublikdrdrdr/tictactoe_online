package com.bublik.niewymyslilemnazwy2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CSC.ActionsListener, CSS.ActionsListener{


    //settings tags:
    /*
    0 - чи завантажувати останню гру (boolean)
    1 - массив
    2 - число перемог першого
    3 - число перемог другого
    4 - гравець, який зараз буде ходити

     */
    Settings_v2 settings_v2;
    FrameLayout UILayout;
    public static MainActivity mthis;
    public static TextView currentPlayerLabel;
    Button online_button;
    public NetGame netGame;


    //  private GraphicsView game_layout;
    private Game game;

    public CSC csc;
    public CSS css;

    public int online_mode = 0; //0-offline, 1-client, 2-server

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //   game = (Game)savedInstanceState.getParcelable("game_object");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //   outState.putParcelable("game_object", (Parcelable) game);
    }

    public void setPolicy()
    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setPolicy();
        super.onCreate(savedInstanceState);
        //       game_layout = new GraphicsView(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        game = new Game(this, new Point(100, 100));
        game.gameResources.cross = BitmapFactory.decodeResource(getResources(), R.mipmap.cross);
        game.gameResources.circle = BitmapFactory.decodeResource(getResources(), R.mipmap.circle);
        game.gameResources.horizontal_layout_line_center = BitmapFactory.decodeResource(getResources(), R.mipmap.hor_c);
        game.gameResources.horizontal_layout_line_start = BitmapFactory.decodeResource(getResources(), R.mipmap.hor_s);
        game.gameResources.horizontal_layout_line_end = BitmapFactory.decodeResource(getResources(), R.mipmap.hor_e);
        game.gameResources.vertical_layout_line_center = BitmapFactory.decodeResource(getResources(), R.mipmap.vert_c);
        game.gameResources.vertical_layout_line_start = BitmapFactory.decodeResource(getResources(), R.mipmap.vert_s);
        game.gameResources.vertical_layout_line_end = BitmapFactory.decodeResource(getResources(), R.mipmap.vert_e);

        game.gameResources.vertical_cross_line_start = BitmapFactory.decodeResource(getResources(), R.mipmap.vert_cross_s);
        game.gameResources.vertical_cross_line_end = BitmapFactory.decodeResource(getResources(), R.mipmap.vert_cross_e);
        game.gameResources.vertical_cross_line_center = BitmapFactory.decodeResource(getResources(), R.mipmap.vert_cross_c);

        game.gameResources.horizontal_cross_line_start = BitmapFactory.decodeResource(getResources(), R.mipmap.hor_cross_s);
        game.gameResources.horizontal_cross_line_end = BitmapFactory.decodeResource(getResources(), R.mipmap.hor_cross_e);
        game.gameResources.horizontal_cross_line_center = BitmapFactory.decodeResource(getResources(), R.mipmap.hor_cross_c);

        game.gameResources.diagonal_cross_line_start = BitmapFactory.decodeResource(getResources(), R.mipmap.diag_cross_s);
        game.gameResources.diagonal_cross_line_end = BitmapFactory.decodeResource(getResources(), R.mipmap.diag_cross_e);
        game.gameResources.diagonal_cross_line_center = BitmapFactory.decodeResource(getResources(), R.mipmap.diag_cross_c);
        game.InitDraw();
        setContentView(R.layout.activity_main);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.game_frame_layout);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frameLayout.addView(game.getView(), 0, layoutParams);
        game.setDrawEnabled(true);
        Button blur = (Button) findViewById(R.id.blur);
        blur.setOnClickListener(onClickListener);

        Button undo_button = (Button) findViewById(R.id.undo_button);
        undo_button.setOnClickListener(onClickListener);

        settings_v2 = new Settings_v2(Settings_v2.createFileName(this, "app_settings.settings"));

        mthis = this;
        Object o = settings_v2.get(0);
        if (o == null) return;

        if ((boolean) o) {
            byte[] arr = (byte[]) settings_v2.get(1);
            if (arr != null) {
                if (arr.length > 0) {
                    game.bytesToGameArray(arr);
                }
            }
        }
        o = settings_v2.get(2);
        if (o!=null)
        {
            game.human1_score = (int)o;
        }

        o = settings_v2.get(3);
        if (o!=null)
        {
            game.human2_score = (int)o;
        }

        o = settings_v2.get(4);
        if (o!=null)
        {
            game.CurrentPlayer = (int)o;
        }


        UILayout = (FrameLayout) findViewById(R.id.UIlayout);
        Button resetButton = (Button) findViewById(R.id.reset_score);
        resetButton.setOnClickListener(onClickListener);

        Button new_game = (Button) findViewById(R.id.new_game_button);
        new_game.setOnClickListener(onClickListener);
        Button mng = (Button) findViewById(R.id.manual_new_game);
        mng.setOnClickListener(onClickListener);

        currentPlayerLabel = (TextView) findViewById(R.id.currentPlayer);
        online_button = (Button) findViewById(R.id.online);
        online_button.setOnClickListener(onClickListener);

    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.undo_button:
                    if (online_mode==0) {
                        game.Undo();
                    }
                    else
                    {
                        Toast.makeText(MainActivity.mthis.getApplicationContext(), "you can't do this on online mode", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.blur:
                    game.ENABLED = false;
                    game.Draw();
                    break;
                case R.id.reset_score:
                    game.human1_score = 0;
                    game.human2_score = 0;
                    TextView w1 = (TextView) findViewById(R.id.cross_count);
                    TextView w2 = (TextView) findViewById(R.id.circle_count);
                    w1.setText(Integer.toString(game.human1_score));
                    w2.setText(Integer.toString(game.human2_score));
                    break;
                case R.id.new_game_button:
                    UnlockGame();
                    break;
                case R.id.manual_new_game:
                    UnlockGame();
                    if (online_mode==1)
                    {
                        if (csc!=null) csc.Exit();
                    }
                    if (online_mode==2)
                    {
                        if (css!=null) css.Exit();
                    }
                    online_mode=0;
                    break;
                case R.id.online:
                    CreateOnlineGame();
                    break;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        game.Touch(event);

        return super.onTouchEvent(event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        settings_v2.set(0, !game.GAME_OVER);
        if (!game.GAME_OVER) {
            settings_v2.set(1, game.GameArrayToBytes());
            settings_v2.set(4, game.CurrentPlayer);
        }
        settings_v2.set(2, game.human1_score);
        settings_v2.set(3, game.human2_score);

        settings_v2.Save();
    }


    private Timer mTimer;
    private MyTimerTask mMyTimerTask;

    public void Game_over() {
        if (mthis.mTimer != null) {
            mthis.mTimer.cancel();
        }

        mthis.mTimer = new Timer();
        mthis.mMyTimerTask = new MyTimerTask();

        mthis.mTimer.schedule(mthis.mMyTimerTask, 1000);

    }

    private void CreateOnlineGame()
    {
        Intent intent = new Intent(this, NetGame.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {return;}
        String mode = data.getStringExtra("mode");
        if (mode!=null)
        {
            if (mode.equals("client"))
            {
                online_mode = 1;
            } else
            {
                online_mode = 2;
            }
            game.NewGame();
            game.CurrentPlayer=0;
        }
    }



    //client

    public static byte[] buffer;

    @Override
    public void OnGetMessage(byte[] bytes) {
        buffer = bytes;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (online_mode==1)
                    game.onlineClick(buffer[0], buffer[1]);
            }
        });

    }

    @Override
    public void OnServerDisconnected() {
        Looper.prepare();
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void OnException(Exception e) {

    }

    @Override
    public void OnServerConnected() {
        netGame.ConnectedListener();
    }

    //server

    @Override
    public void onGetMessage(byte[] data) {
        buffer = data;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (online_mode==2)
                    game.onlineClick(buffer[0], buffer[1]);
            }
        });
    }

    @Override
    public void onConnect() {
        netGame.ConnectedListener();
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onException(Exception e) {

    }

    public class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LockGame();
                }
            });
        }
    }

    private void LockGame() {
        game.ENABLED = false;
        game.Draw();
        TextView wn = (TextView) findViewById(R.id.winner_label);
        switch (game.winner) {
            case 0:
                wn.setText(R.string.nobody_won);
            case 1:
                wn.setText(R.string.first_won);
                break;
            case 2:
                wn.setText(R.string.second_won);
                break;
        }
        TextView w1 = (TextView) findViewById(R.id.cross_count);
        TextView w2 = (TextView) findViewById(R.id.circle_count);
        TextView sr = (TextView) findViewById(R.id.sr);
        if (online_mode==0) {
            w1.setVisibility(View.VISIBLE);
            w2.setVisibility(View.VISIBLE);
            w1.setText(Integer.toString(game.human1_score));
            w2.setText(Integer.toString(game.human2_score));
            sr.setVisibility(View.VISIBLE);
        } else
        {
            w1.setVisibility(View.INVISIBLE);
            w2.setVisibility(View.INVISIBLE);
            sr.setVisibility(View.INVISIBLE);
        }
        UILayout.setVisibility(View.VISIBLE);
    }

    private void UnlockGame() {
        UILayout.setVisibility(View.INVISIBLE);
        game.NewGame();
    }

    @Override
    protected void onDestroy() {
        if (csc!=null)
        {
            csc.Exit();
        }
        if (css!=null)
        {
            css.Exit();
        }
        super.onDestroy();
    }


    public synchronized void sendClick(int x, int y)
    {
        byte[] bytes = clickToByteArray(x,y);
        if (online_mode==1)
        {
            csc.Send(bytes);
        } else
        {
            css.Send(bytes);
        }
    }

    public static byte[] clickToByteArray(int x, int y)
    {
        byte[] TR = new byte[256];

        //tylko do 127
        TR[0] = (byte)x;
        TR[1] = (byte)y;
        return TR;
    }
}
