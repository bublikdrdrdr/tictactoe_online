package com.bublik.niewymyslilemnazwy2;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.nio.channels.Pipe;
import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;


/**
 * Created by Bublik on 25-Feb-16.
 */
public class Game /*implements Parcelable*/{

   // private FrameLayout game_layout;
   // private Canvas canvas;
    private double Scale = 0.8;
    private final double maxScale = 1.0;
    private final double minScale = 0.4;
    private int CellSizeDp = 100;
    private PointF camera_position; //при масштабі 1. буде заокруглюватися
    private GraphicsView game_view;
   // int i = 0;
    private Point size;
    public GameResources gameResources;
   // private LinkedList<GameAction> actions;
    private Stack<GameAction> actions;
    private Bitmap.Config bitmapsConfig = Bitmap.Config.ARGB_4444;
    public int CurrentPlayer; // 0/1  (cross/circle)
    private int LINE_WIN_LENGTH = 5;
    private Cell[][] GAME_ARRAY;
    private Context ownerContext;
    Point[] crossed_points;
  //  private final Bitmap.Config image_profile = Bitmap.Config.ARGB_4444;

    public boolean GAME_OVER = false;
    private boolean READY_TO_DRAW = false;

    //для промальовки
    private int fullsizeX, fullsizeY;
    private int px; //розмір 1 клітинки в пікселях
    private GameMode gameMode = GameMode.HUMANvsHUMAN;
    public int human1_score = 0;
    public int human2_score = 0;
    public int phone_score = 0;
    public boolean ENABLED = true;
    public int winner = -1; //-1 - ще грають, 0 - ніхто, 1 - перший (хрестик), 2 - другий (нулик)

    public enum GameMode {HUMANvsHUMAN, HUMANvsPHONE}

   /* @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(Scale);
        dest.writeDouble(maxScale);
        dest.writeDouble(minScale);
        dest.writeInt(CellSizeDp);
        dest.writeFloat(camera_position.x);
        dest.writeFloat(camera_position.y);
    }*/


    public synchronized void setCell(int x, int y, Cell_type cell_type)
    {
        GAME_ARRAY[x][y].cell_type = cell_type;
    }


    public class GameResources{
        public Bitmap cross;
        public Bitmap circle;
        public Bitmap vertical_layout_line_start;//up part
        public Bitmap vertical_layout_line_center;//center
        public Bitmap vertical_layout_line_end;//down
        public Bitmap horizontal_layout_line_start;//left
        public Bitmap horizontal_layout_line_center;//center
        public Bitmap horizontal_layout_line_end;//right
        public Bitmap vertical_cross_line_start;
        public Bitmap vertical_cross_line_center;
        public Bitmap vertical_cross_line_end;
        public Bitmap horizontal_cross_line_start;
        public Bitmap horizontal_cross_line_center;
        public Bitmap horizontal_cross_line_end;
        public Bitmap diagonal_cross_line_start; //зображення для лінії  лівий_верхній кут  --- правий_нижній кут
        public Bitmap diagonal_cross_line_center;//для іншого перекреслення зображеня будуть зекрально розвернуті
        public Bitmap diagonal_cross_line_end;
       // public Bitmap diagonal_cross_line_connector;
    }

    public void bytesToGameArray(byte[] bytes)
    {
        int idx = 0;
        for (int i = 0; i < size.x; i++)
        {
            for (int j = 0; j < size.y; j++)
            {
                switch (bytes[idx])
                {
                    case 0:
                        GAME_ARRAY[i][j].cell_type = Cell_type.CROSS;
                        break;
                    case 1:
                        GAME_ARRAY[i][j].cell_type = Cell_type.CIRCLE;
                        break;
                    case 2:
                        GAME_ARRAY[i][j].cell_type = Cell_type.EMPTY;
                        break;
                }
                idx++;
            }
        }
    }

    public byte[] GameArrayToBytes()
    {
        byte[] tr = new byte[size.x*size.y];
        int idx = 0;
        for (int i = 0; i < size.x; i++)
        {
            for (int j = 0; j < size.y; j++)
            {
                switch (GAME_ARRAY[i][j].cell_type)
                {
                    case CROSS:
                        tr[idx] = 0;
                        break;
                    case CIRCLE:
                        tr[idx] = 1;
                        break;
                    case EMPTY:
                        tr[idx] = 2;
                        break;
                }
                idx++;
            }
        }
        return tr;
    }


    public class GameAction
    {
        public Point coordinates;
        public Cell_type cell_type;

        GameAction(Point p, Cell_type ct)
        {
            coordinates = p;
            cell_type = ct;
        }
    }

    private enum Cell_type {CROSS, CIRCLE, EMPTY}

    public class Cell
    {
        Cell_type cell_type;

        public Cell()
        {
            cell_type = Cell_type.EMPTY;
        }

        public Bitmap getBitmap()
        {
            switch (cell_type)
            {
                case CROSS:
                    return gameResources.cross;
                case CIRCLE:
                    return gameResources.circle;
                case EMPTY:
                    Bitmap tr = Bitmap.createBitmap(1,1,bitmapsConfig);
                    tr.setPixel(0,0,0x00FFFFFF);
                    return tr;
                default:
                    return null;
            }
        }



        public boolean Click(int x, int y)
        {
            if (cell_type!=Cell_type.EMPTY)
            {
                return false;
            } else
            {
                if (CurrentPlayer==0)
                {
                    cell_type = Cell_type.CROSS;
                } else
                {
                    cell_type = Cell_type.CIRCLE;
                }
                crossed_points = FindWin(new Point(x,y));

                if (crossed_points.length!=0)
                {
                    setGameOver(true);
                } else
                {
                    if (AreFreeCells()==false)
                    {
                        setGameOver(false);
                    }
                }

                return true;
            }
        }
    }

    private boolean AreFreeCells()
    {
        for (int i = 0; i < size.x; i++)
        {
            for (int j = 0; j < size.y; j++)
            {
                if (GAME_ARRAY[i][j].cell_type==Cell_type.EMPTY)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void setGameOver(boolean someone_won)
    {

        //Point[] points = FindWin();
        if (someone_won)
        {
            if (CurrentPlayer==0)
            {
                winner = 1;
                human1_score++;
            } else
            {
                winner = 2;
                human2_score++;
            }
        } else
        {
            winner = 0;
        }
        GAME_OVER = true;
        MainActivity.mthis.Game_over();
    }

    public Point[] FindWin(Point current_click_point)
    {
        ArrayList<Point> list = new ArrayList<Point>();
        Point current;
        if (GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type==Cell_type.EMPTY) return new Point[0];
        //вправо
        int rght = 0;
        int pos = current_click_point.x + 1;
        if (!((pos<0)||(pos>=size.x))) {
            while ((GAME_ARRAY[pos][current_click_point.y].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type)) {
                pos++;
                rght++;
                if ((pos < 0) && (pos > size.x)) break;
            }
        }

        int lft = 0;
        pos = current_click_point.x - 1;
        if (!((pos<0)||(pos>=size.x))) {
            while (GAME_ARRAY[pos][current_click_point.y].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                pos--;
                lft++;
                if ((pos<0)||(pos>=size.x)) break;
            }
        }

        int up = 0;
        pos = current_click_point.y - 1;
        if (!((pos<0)||(pos>=size.y))) {
            while (GAME_ARRAY[current_click_point.x][pos].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                pos--;
                up++;
                if ((pos<0)||(pos>=size.y)) break;
            }
        }

        int down = 0;
        pos = current_click_point.y + 1;
        if (!((pos<0)||(pos>=size.y))) {
            while (GAME_ARRAY[current_click_point.x][pos].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                pos++;
                down++;
                if ((pos<0)||(pos>=size.y)) break;
            }
        }

        int upleft = 0;
        int posx = current_click_point.x - 1;
        int posy = current_click_point.y - 1;
        if (!((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y))) {
            while (GAME_ARRAY[posx][posy].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                posx--;
                posy--;
                upleft++;
                if ((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y)) break;
            }
        }

        int uprght = 0;
        posx = current_click_point.x + 1;
        posy = current_click_point.y - 1;
        if (!((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y))) {
            while (GAME_ARRAY[posx][posy].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                posx++;
                posy--;
                uprght++;
                if ((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y)) break;
            }
        }

        int downleft = 0;
        posx = current_click_point.x - 1;
        posy = current_click_point.y + 1;
        if (!((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y))) {
            while (GAME_ARRAY[posx][posy].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                posx--;
                posy++;
                downleft++;
                if ((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y)) break;
            }
        }

        int downrght = 0;
        posx = current_click_point.x + 1;
        posy = current_click_point.y + 1;
        if (!((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y))) {
            while (GAME_ARRAY[posx][posy].cell_type == GAME_ARRAY[current_click_point.x][current_click_point.y].cell_type) {
                posx++;
                posy++;
                downrght++;
                if ((posx<0)||(pos>=size.x)||(posy<0)||(posy>=size.y)) break;
            }
        }

        if (lft+rght+1 >= LINE_WIN_LENGTH)
        {
            list.add(new Point(current_click_point.x-lft, current_click_point.y));
            list.add(new Point(current_click_point.x+rght, current_click_point.y));
        }
        if (up+down+1 >= LINE_WIN_LENGTH)
        {
            list.add(new Point(current_click_point.x, current_click_point.y-up));
            list.add(new Point(current_click_point.x, current_click_point.y+down));
        }
        if (upleft+downrght+1 >= LINE_WIN_LENGTH)
        {
            list.add(new Point(current_click_point.x-upleft, current_click_point.y-upleft));
            list.add(new Point(current_click_point.x+downrght, current_click_point.y+downrght));
        }
        if (downleft+uprght+1 >= LINE_WIN_LENGTH)
        {
            list.add(new Point(current_click_point.x+uprght, current_click_point.y-uprght));
            list.add(new Point(current_click_point.x-downleft, current_click_point.y+downleft));
        }
        return list.toArray(new Point[list.size()]);
    }


    public Point[] FindWin()
    { //Todo: оптимізувати для меншого квадрату
        ArrayList<Point> list = new ArrayList<Point>();
        ArrayList<Point> current;
        boolean found;
        for (int i = 0; i < size.x; i++)
        {
            for (int j = 0; j < size.y; j++) {
                if (GAME_ARRAY[i][j].cell_type != Cell_type.EMPTY) {
                    current = HasLine(new Point(i, j));
                    for (int k = 0; k < current.size(); k += 2) {
                        found = false;
                        for (int l = 0; l < list.size(); l += 2) {
                            if (((PointEq(current.get(k), list.get(l))) && (PointEq(current.get(k + 1), list.get(l + 1)))) || ((PointEq(current.get(k + 1), list.get(l))) && (PointEq(current.get(k), list.get(l + 1)))))  //надіюсь працює, поки не чіпати
                            {
                                found = true;
                                break;
                            }
                        }
                        if (found == false) {
                            list.add(current.get(k));
                            list.add(current.get(k + 1));
                        }
                    }
                }
            }
        }

       // list.add(new Point(2, 5));
        return list.toArray(new Point[list.size()]);
    }




    private boolean PointEq(Point point1, Point point2) //порівнює 2 точки
    {
        if ((point1.x == point2.x) && (point1.y==point2.y))
        {
            return true;
        } else
        {
            return false;
        }
    }

    private ArrayList<Point> HasLine(Point point)
    {
        int i;
        List<Point> list = new ArrayList<Point>();
        boolean up = false;
        boolean left = false;
        boolean right = false;
        boolean down = false;
        boolean eq;
        Cell_type ct = GAME_ARRAY[point.x][point.y].cell_type;



        if (point.x>=LINE_WIN_LENGTH-1)//точка може мати зліва від себе лінію
        {
            left = true;
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x - i][point.y].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x - LINE_WIN_LENGTH +1, point.y));
                list.add(new Point(point.x, point.y));
            }
        }



        if (point.y >=LINE_WIN_LENGTH-1)//точка може мати зверху від себе лінію
        {
            up = true;
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++) {
                if (GAME_ARRAY[point.x][point.y - i].cell_type != ct) {
                    eq = false;
                    break;
                }
            }
            if (eq == true) {
                list.add(new Point(point.x, point.y - LINE_WIN_LENGTH + 1));
                list.add(new Point(point.x, point.y));
            }
        }



        if (point.y <=size.y - LINE_WIN_LENGTH)//точка може мати знизу від себе лінію
        {
            down = true;
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x][point.y+i].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x, point.y));
                list.add(new Point(point.x, point.y + LINE_WIN_LENGTH -1));
            }
        }



        if (point.x <=size.x - LINE_WIN_LENGTH)//точка може мати справа від себе лінію
        {
            right = true;
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x+i][point.y].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x, point.y));
                list.add(new Point(point.x + LINE_WIN_LENGTH -1, point.y));
            }
        }



        if (up&&left)
        {
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x-i][point.y-i].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x - LINE_WIN_LENGTH +1, point.y - LINE_WIN_LENGTH +1));
                list.add(new Point(point.x, point.y));
            }
        }



        if (up&&right)
        {
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x+i][point.y-i].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x + LINE_WIN_LENGTH -1, point.y - LINE_WIN_LENGTH +1));
                list.add(new Point(point.x, point.y));
            }
        }



        if (down&&right)
        {
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x+i][point.y+i].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x, point.y));
                list.add(new Point(point.x + LINE_WIN_LENGTH -1, point.y + LINE_WIN_LENGTH -1));
            }
        }



        if (down&&left)
        {
            eq = true;
            for (i = 1; i < LINE_WIN_LENGTH; i++)
            {
                if (GAME_ARRAY[point.x-i][point.y+i].cell_type!=ct)
                {
                    eq = false;
                    break;
                }
            }
            if (eq == true)
            {
                list.add(new Point(point.x, point.y));
                list.add(new Point(point.x - LINE_WIN_LENGTH + 1, point.y + LINE_WIN_LENGTH - 1));
            }
        }


        return (ArrayList<Point>)list;

    }

    /*public void SetGameResources(GameResources gameR)
    {
        gameResources = gameR;
    }*/

    public Game(Context context, Point matrix_size)
    {
        game_view = new GraphicsView(context);
        size = matrix_size;
        ownerContext = context;
        ClearGameArray();
        gameResources = new GameResources();
        camera_position = new PointF(0,0);
        actions = new Stack<GameAction>();
    }

    public void Undo()
    {
        if (!ENABLED) return;
        if (GAME_OVER==false) {
            if (!actions.isEmpty()) {
                GameAction ga = actions.pop();
                if (ga != null) {
                    setCell(ga.coordinates.x, ga.coordinates.y, Cell_type.EMPTY);
                    Draw();
                }
            }
        }
    }

    public GraphicsView getView()
    {
        return game_view;

    }

    public void NewGame()
    {
        ENABLED = true;
        ClearGameArray();
        actions = new Stack<GameAction>();
        GAME_OVER = false;
        Draw();
    }

    private void ClearGameArray()
    {
        GAME_ARRAY = new Cell[size.x][];
        for (int i = 0; i < size.x; i++)
        {
            GAME_ARRAY[i] = new Cell[size.y];
            for (int j = 0; j < size.y; j++)
            {
                GAME_ARRAY[i][j] = new Cell();

                //test
                //GAME_ARRAY[i][j].cell_type = Cell_type.CROSS;
            }
        }
    }




    enum Touch_action {MOVE, ZOOM, CLICK, NONE}
    int min_move_distance_dp = 15; //  +-
    int min_move_distance_px;
    int firstX;
    int firstY;
    int lastX;
    int lastY;
    int currentX;
    int currentY;
    int zoom_start_point1X;
    int zoom_start_point1Y;
    int zoom_start_point2X;
    int zoom_start_point2Y;
    int first_fingers_distance;
    double ScaleOnStart;
    PointF start_camera_position;
    Touch_action touch_action = Touch_action.NONE;
    public void Touch(MotionEvent event)
    {
        Rect area = game_view.getOnScreenCoordinates();
        min_move_distance_px = dp2px(min_move_distance_dp);

        if ((ENABLED ==false) || (GAME_OVER))
        {
            return;
        }

        if (!PointInRect(area, new Point((int)event.getX(0), (int)event.getY(0))))
        {
            touch_action = Touch_action.NONE;
            return;
        }
        switch (event.getPointerCount())
        {
            case 1:
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if (PointInRect(area, new Point((int)event.getX(0), (int)event.getY(0))))
                        {
                            lastX = (int)event.getX(0);
                            lastY = (int)event.getY(0);
                            touch_action = Touch_action.CLICK;
                            firstX = (int)event.getX(0);
                            firstY = (int)event.getY(0);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (touch_action == Touch_action.CLICK)
                        {
                            currentX = (int)event.getX(0);
                            currentY = (int)event.getY(0);
                            if ((Math.abs(currentX-firstX)>min_move_distance_px) || (Math.abs(currentY-firstY)>min_move_distance_px))
                            {//якщо ми задалеко посунули палець
                                touch_action = Touch_action.MOVE;
                            }
                            lastX = currentX;
                            lastY = currentY;
                        } else
                        if (touch_action == Touch_action.MOVE)
                        {
                            currentX = (int)event.getX(0);
                            currentY = (int)event.getY(0);
                            int deltaX = (int)((lastX - currentX)/Scale);
                            int deltaY = (int)((lastY - currentY)/Scale);
                            camera_position.x += deltaX;
                            camera_position.y += deltaY;
                            lastX = currentX;
                            lastY = currentY;
                        }
                        CheckPosition();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (touch_action == Touch_action.MOVE)
                        {
                            currentX = (int)event.getX(0);
                            currentY = (int)event.getY(0);
                            int deltaX = (int)((lastX - currentX)/Scale);
                            int deltaY = (int)((lastY - currentY)/Scale);
                            camera_position.x += deltaX;
                            camera_position.y += deltaY;
                        } else
                        if (touch_action == Touch_action.CLICK)
                        {
                            Point p = getCellInPoint(firstX, firstY, area);
                            if (p!=null) {
                                if (MainActivity.mthis.online_mode==0) {
                                    if (GAME_ARRAY[p.x][p.y].Click(p.x, p.y)) //якщо ми натиснули і там було пусто, то змінити гравця
                                    {
                                        actions.push(new GameAction(new Point(p), GAME_ARRAY[p.x][p.y].cell_type));
                                        ChangePlayer();
                                    }
                                }
                                else
                                {
                                    if (MainActivity.mthis.online_mode==1)
                                    {
                                        if (CurrentPlayer==1)
                                        {
                                            if (GAME_ARRAY[p.x][p.y].Click(p.x, p.y)) //якщо ми натиснули і там було пусто, то змінити гравця
                                            {
                                                actions.push(new GameAction(new Point(p), GAME_ARRAY[p.x][p.y].cell_type));
                                                ChangePlayer();
                                                MainActivity.mthis.sendClick(p.x, p.y);
                                            }
                                        }
                                    } else
                                    {
                                        if (CurrentPlayer==0)
                                        {
                                            if (GAME_ARRAY[p.x][p.y].Click(p.x, p.y)) //якщо ми натиснули і там було пусто, то змінити гравця
                                            {
                                                actions.push(new GameAction(new Point(p), GAME_ARRAY[p.x][p.y].cell_type));
                                                ChangePlayer();
                                                MainActivity.mthis.sendClick(p.x, p.y);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                }
                break;
            case 2:
                switch (event.getAction()& MotionEvent.ACTION_MASK)
                {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        //записати початкові координати
                        start_camera_position = new PointF(camera_position.x, camera_position.y);
                        touch_action = Touch_action.ZOOM;
                        zoom_start_point1X = (int)event.getX(0);
                        zoom_start_point1Y = (int)event.getY(0);
                        zoom_start_point2X = (int)event.getX(1);
                        zoom_start_point2Y = (int)event.getY(1);
                        ScaleOnStart = Scale;
                        first_fingers_distance = (int)Math.sqrt((zoom_start_point2X - zoom_start_point1X) * (zoom_start_point2X - zoom_start_point1X) + (zoom_start_point2Y - zoom_start_point1Y) * (zoom_start_point2Y - zoom_start_point1Y));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //long startTime = System.currentTimeMillis(); //test
                        lastX = (int)event.getX(0);
                        lastY = (int)event.getY(0);
                        zoom_start_point1X = (int)event.getX(0);
                        zoom_start_point1Y = (int)event.getY(0);
                        zoom_start_point2X = (int)event.getX(1);
                        zoom_start_point2Y = (int)event.getY(1);
                        int current_fingers_distance =  (int)Math.sqrt((zoom_start_point2X - zoom_start_point1X)*(zoom_start_point2X - zoom_start_point1X) + (zoom_start_point2Y - zoom_start_point1Y)*(zoom_start_point2Y - zoom_start_point1Y));
                        float finger_zoom_delta = 1f;
                        Scale = ScaleOnStart*(Math.pow(((double) current_fingers_distance / first_fingers_distance), 1))*finger_zoom_delta;
                        if (Scale>maxScale) Scale = maxScale;
                        if (Scale<minScale) Scale = minScale;
                        //long timeSpent = System.currentTimeMillis() - startTime;
                        camera_position.x = (float)(start_camera_position.x - ((ScaleOnStart-Scale))*area.width()); //паше, але не так як треба
                        camera_position.y = (float)(start_camera_position.y - ((ScaleOnStart-Scale))*area.height());
                        CheckPosition();
                      //  String s = Long.toString(timeSpent);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        touch_action = Touch_action.NONE;
                        break;
                }
                break;
        }

       // long startTime = System.currentTimeMillis(); //test
        //FindWin(); //оптимізувати
       // long timeSpent = System.currentTimeMillis() - startTime;
        game_view.invalidate();

    }

    private void ChangePlayer()
    {
        if (CurrentPlayer==0)
        {
            CurrentPlayer = 1;
        } else
        {
            CurrentPlayer = 0;
        }
    }

    public void onlineClick(int x, int y)
    {
        if (GAME_ARRAY[x][y].Click(x, y)) //якщо ми натиснули і там було пусто, то змінити гравця
        {
            actions.push(new GameAction(new Point(x,y), GAME_ARRAY[x][y].cell_type));
            ChangePlayer();
        }
        Draw();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
        game_view.invalidate();
    }

    private boolean PointInRect(Rect rect, Point point)
    {
        if ((point.x >= rect.left) && (point.x <= rect.right) && (point.y >= rect.top) && (point.y <= rect.bottom))
        {
            return true;
        } else
        {
            return false;
        }
    }

    private void CheckPosition()
    {
        if (camera_position.x > fullsizeX-game_view.getOnScreenCoordinates().width()/Scale) camera_position.x = (float)(fullsizeX-game_view.getOnScreenCoordinates().width()/Scale);
        if (camera_position.y > fullsizeY-game_view.getOnScreenCoordinates().height()/Scale) camera_position.y = (float)(fullsizeY-game_view.getOnScreenCoordinates().height()/Scale);
        if (camera_position.x < 0) camera_position.x = 0;
        if (camera_position.y < 0) camera_position.y = 0;



    }

    public Point getCellInPoint(int x, int y, Rect onScreenCoord)
    {
        int tx = (int)(((x/Scale) + camera_position.x - onScreenCoord.left/Scale) / dp2px(CellSizeDp)); //поняття не маю якого фіга, але працює
        int ty = (int)(((y/Scale) + camera_position.y - onScreenCoord.top/Scale) / dp2px(CellSizeDp));
        if ((tx<0)||(tx>=size.x) || (ty<0)||(ty>=size.y))
        {
            return null;
        } else {
            return new Point(tx, ty);
        }
    }

    public void setDrawEnabled(boolean B)
    {
        READY_TO_DRAW = B;
        if (B) Draw();
    }

    public void Draw()
    {
        game_view.invalidate();
    }

    public class GraphicsView extends View {
        public GraphicsView(Context context) {
            super(context);
        }


        @Override
        protected void onDraw(Canvas canvas) {
            if ((canvas.getHeight()==0) || (canvas.getWidth()==0))
            {
                return;
            }

            if (px == 0) return;
            Point[] range = getPaintCellsRange(new Point(canvas.getWidth(), canvas.getHeight()));
            if (range[0].x <0) range[0].x = 0;
            if (range[0].y <0) range[0].y = 0;
            if (range[1].x > size.x) range[1].x = size.x;
            if (range[1].y > size.y) range[1].y = size.y;

            fullsizeX = px*size.x;
            fullsizeY = px*size.y;

            int i;
            int j;

            if (MainActivity.currentPlayerLabel!=null) {

                String g = "";
                if (CurrentPlayer == 0) {
                    g = "next step X";
                    MainActivity.currentPlayerLabel.setTextColor(0xFFFF0000);
                } else {
                    g = "next step O";
                    MainActivity.currentPlayerLabel.setTextColor(0xFF0000FF);
                }
                MainActivity.currentPlayerLabel.setText(g);
            }

            //малюємо зображення (якщо вони є)
            for (i = range[0].x; i < range[1].x; i++)
            {
                for (j = range[0].y; j < range[1].y; j++)
                {
                    if (GAME_ARRAY[i][j].cell_type!=Cell_type.EMPTY) {
                        Rect origin = new Rect(0, 0, px - 1, px - 1);
                        Rect onRender = new Rect((int) ((i * px - camera_position.x) * Scale), (int) ((j * px - camera_position.y) * Scale), (int) (((i + 1) * px - camera_position.x) * Scale), (int) (((j + 1) * px - camera_position.y) * Scale));
                        Bitmap b = GAME_ARRAY[i][j].getBitmap();
                        if (b != null) {
                            canvas.drawBitmap(b, origin, onRender, null);
                        }
                    }

                    //сітка

                    //горизонтальна
                    if ((i!=size.x-1) && (j!=size.y-1)) {
                        if (i == 0) { //перший зліва
                            Bitmap bb = gameResources.horizontal_layout_line_start;
                            Rect from = new Rect(0, 0, bb.getWidth() - 1, bb.getHeight() - 1);
                            Rect to = new Rect((int) ((i * px - camera_position.x) * Scale),
                                    (int) (((j + 1) * px - camera_position.y - ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale),
                                    (int) (((i + 1) * px - camera_position.x) * Scale),
                                    (int) (((j + 1) * px - camera_position.y + ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale));
                            if (bb != null) {
                                canvas.drawBitmap(bb, from, to, null);
                            }
                        } else {
                            if (i == size.x - 2) {
                                //передостанній
                                Bitmap bb = gameResources.horizontal_layout_line_center;
                                Rect from = new Rect(0, 0, bb.getWidth() - 1, bb.getHeight() - 1);
                                Rect to = new Rect((int) ((i * px - camera_position.x) * Scale),
                                        (int) (((j + 1) * px - camera_position.y - ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale),
                                        (int) (((i + 1) * px - camera_position.x) * Scale),
                                        (int) (((j + 1) * px - camera_position.y + ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale));
                                if (bb != null) {
                                    canvas.drawBitmap(bb, from, to, null);
                                }
                                to = new Rect((int) (((i + 1) * px - camera_position.x) * Scale),
                                        (int) (((j + 1) * px - camera_position.y - ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale),
                                        (int) (((i + 2) * px - camera_position.x) * Scale),
                                        (int) (((j + 1) * px - camera_position.y + ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale));
                                bb = gameResources.horizontal_layout_line_end;
                                if (bb != null) {
                                    canvas.drawBitmap(bb, from, to, null);
                                }
                            } else { //всі
                                Bitmap bb = gameResources.horizontal_layout_line_center;
                                Rect from = new Rect(0, 0, bb.getWidth() - 1, bb.getHeight() - 1);
                                Rect to = new Rect((int) ((i * px - camera_position.x) * Scale),
                                        (int) (((j + 1) * px - camera_position.y - ((px/bb.getWidth())*bb.getHeight() / 2.0))
                                                * Scale), (int) (((i + 1) * px - camera_position.x) * Scale),
                                        (int) (((j + 1) * px - camera_position.y + ((px/bb.getWidth())*bb.getHeight() / 2.0)) * Scale));
                                if (bb != null) {
                                    canvas.drawBitmap(bb, from, to, null);
                                }
                            }
                        }
                    }


                    //вертикальна
                    if ((i!=size.x-1) && (j!=size.y-1)) {
                        if (j == 0) { //перший зліва
                            Bitmap bb = gameResources.vertical_layout_line_start;
                            Rect from = new Rect(0, 0, bb.getWidth() - 1, bb.getHeight() - 1);
                            Rect to = new Rect((int) (((i+1) * px - camera_position.x - ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                    (int) (((j) * px - camera_position.y) * Scale),
                                    (int) (((i+1) * px - camera_position.x + ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                    (int) (((j+1) * px - camera_position.y) * Scale));
                            if (bb != null) {
                                canvas.drawBitmap(bb, from, to, null);
                            }
                        } else {
                            if (j == size.x - 2) {
                                //передостанній
                                Bitmap bb = gameResources.vertical_layout_line_center;
                                Rect from = new Rect(0, 0, bb.getWidth() - 1, bb.getHeight() - 1);
                                Rect to = new Rect((int) (((i+1) * px - camera_position.x - ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                        (int) (((j) * px - camera_position.y) * Scale),
                                        (int) (((i+1) * px - camera_position.x + ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                        (int) (((j+1) * px - camera_position.y) * Scale));
                                if (bb != null) {
                                    canvas.drawBitmap(bb, from, to, null);
                                }
                                to = new Rect((int) (((i+1) * px - camera_position.x - ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                        (int) (((j+1) * px - camera_position.y) * Scale),
                                        (int) (((i+1) * px - camera_position.x + ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                        (int) (((j+2) * px - camera_position.y) * Scale));
                                bb = gameResources.vertical_layout_line_end;
                                if (bb != null) {
                                    canvas.drawBitmap(bb, from, to, null);
                                }
                            } else { //всі
                                Bitmap bb = gameResources.vertical_layout_line_center;
                                Rect from = new Rect(0, 0, bb.getWidth() - 1, bb.getHeight() - 1);
                                Rect to = new Rect((int) (((i+1) * px - camera_position.x - ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                        (int) (((j) * px - camera_position.y) * Scale),
                                        (int) (((i+1) * px - camera_position.x + ((px/bb.getHeight())*bb.getWidth()/2.0)) * Scale),
                                        (int) (((j+1) * px - camera_position.y) * Scale));
                                if (bb != null) {
                                    canvas.drawBitmap(bb, from, to, null);
                                }
                            }
                        }
                    }
                }
            }




            if (GAME_OVER) //намалювати лінії і ту всю фігню при виграші
            {
                for (i = 0; i < crossed_points.length; i+=2)
                {
                    if (crossed_points[i].x == crossed_points[i+1].x)
                    {
                        if (crossed_points[i].y==crossed_points[i+1].y)
                        { // 1 точка? як?

                        }else
                        { // вниз
                            Bitmap cr = getBitmapCrossLine(crossed_points[i+1].y-crossed_points[i].y+1, 1);
                            Rect origin = new Rect(0, 0, cr.getWidth(), cr.getHeight());
                            Rect onRender = new Rect((int) (((crossed_points[i].x) * px - camera_position.x) * Scale), (int) ((crossed_points[i].y * px - camera_position.y) * Scale), (int) (((crossed_points[i+1].x+1) * px - camera_position.x) * Scale), (int) (((crossed_points[i+1].y+1) * px - camera_position.y) * Scale));
                            if (cr != null) {
                                canvas.drawBitmap(cr, origin, onRender, null);
                            }
                        }

                    } else
                    {
                        if (crossed_points[i].y==crossed_points[i+1].y)
                        { //вправо
                            Bitmap cr = getBitmapCrossLine(crossed_points[i+1].x-crossed_points[i].x+1, 0);
                            Rect origin = new Rect(0, 0, cr.getWidth(), cr.getHeight());
                            Rect onRender = new Rect((int) (((crossed_points[i].x) * px - camera_position.x) * Scale), (int) ((crossed_points[i].y * px - camera_position.y) * Scale), (int) (((crossed_points[i+1].x+1) * px - camera_position.x) * Scale), (int) (((crossed_points[i+1].y+1) * px - camera_position.y) * Scale));
                            if (cr != null) {
                                canvas.drawBitmap(cr, origin, onRender, null);
                            }

                        } else
                        { //по діагоналі
                            if (crossed_points[i].x > crossed_points[i+1].x)
                            {  //   /
                                Bitmap cr = getBitmapCrossLine(crossed_points[i].x-crossed_points[i+1].x+1, 3);
                                Rect origin = new Rect(0, 0, cr.getWidth(), cr.getHeight());
                                Rect onRender = new Rect((int)(((crossed_points[i+1].x) * px - camera_position.x) * Scale), (int) ((crossed_points[i].y * px - camera_position.y) * Scale), (int)(((crossed_points[i].x+1) * px - camera_position.x) * Scale), (int) (((crossed_points[i+1].y+1) * px - camera_position.y) * Scale));
                                if (cr != null) {
                                    canvas.drawBitmap(cr, origin, onRender, null);
                                }
                            } else
                            {//     \
                                Bitmap cr = getBitmapCrossLine(crossed_points[i+1].x-crossed_points[i].x+1, 2);
                                Rect origin = new Rect(0, 0, cr.getWidth(), cr.getHeight());
                                Rect onRender = new Rect((int)(((crossed_points[i].x) * px - camera_position.x) * Scale), (int) ((crossed_points[i].y * px - camera_position.y) * Scale), (int)(((crossed_points[i+1].x+1) * px - camera_position.x) * Scale), (int) (((crossed_points[i+1].y+1) * px - camera_position.y) * Scale));
                                if (cr != null) {
                                    canvas.drawBitmap(cr, origin, onRender, null);
                                }
                            }
                        }
                    }
                }
            }

            if (ENABLED==false)
            {
                canvas.drawColor(Color.argb(200,255,255,255));

            }

        }


        public Rect getOnScreenCoordinates()
        {
            int[] sz = new int[2];
            int[] loc = new int[2];
            this.getLocationOnScreen(loc);
            sz[0] = this.getWidth();
            sz[1] = this.getHeight();
            return new Rect(loc[0], loc[1], loc[0]+sz[0], loc[1] + sz[1]);
        }
    }

    private Bitmap getBitmapCrossLine(int length, int direction) /**0- hor, 1 - vert, 2 - diag \, 3 - diag /*/
    {
        Bitmap tr = null;
        Canvas c;
        Bitmap cr;
        int lpx;
        int i;
        Paint paint = null;
        switch (direction)
        {
            case 0:
                tr = Bitmap.createBitmap(px*length, px, bitmapsConfig);
                c = new Canvas(tr);
                c.drawColor(Color.argb(0,255,255,255));
                c.drawBitmap(gameResources.horizontal_cross_line_start, new Rect(0, 0, gameResources.horizontal_cross_line_start.getWidth(), gameResources.horizontal_cross_line_start.getHeight()), new Rect(0, 0, px, px), paint);
                c.drawBitmap(gameResources.horizontal_cross_line_end, new Rect(0, 0, gameResources.horizontal_cross_line_end.getWidth(), gameResources.horizontal_cross_line_end.getHeight()), new Rect((length - 1) * px,0 , (length) * px,px), paint);
                for (i = 1; i < length-1; i++)
                {
                    c.drawBitmap(gameResources.horizontal_cross_line_center, new Rect(0,0,gameResources.horizontal_cross_line_end.getWidth(),gameResources.horizontal_cross_line_end.getHeight() ), new Rect((i)*px,0, (i+1)*px, px), paint);
                }
                break;
            case 1:
                tr = Bitmap.createBitmap(px, px*length, bitmapsConfig);
                c = new Canvas(tr);
                c.drawColor(Color.argb(0, 255, 255, 255));
                c.drawBitmap(gameResources.vertical_cross_line_start, new Rect(0, 0, gameResources.vertical_cross_line_start.getWidth(), gameResources.vertical_cross_line_start.getHeight()), new Rect(0, 0, px, px), paint);
                c.drawBitmap(gameResources.vertical_cross_line_end, new Rect(0,0,gameResources.vertical_cross_line_end.getWidth(),gameResources.vertical_cross_line_end.getHeight() ), new Rect(0,(length-1)*px,px, (length)*px), paint);
                for (i = 1; i < length-1; i++)
                {
                    c.drawBitmap(gameResources.vertical_cross_line_center, new Rect(0,0,gameResources.vertical_cross_line_end.getWidth(),gameResources.vertical_cross_line_end.getHeight() ), new Rect(0,(i)*px,px, (i+1)*px), paint);
                }
                break;
            case 2:
                cr = gameResources.diagonal_cross_line_start;
                paint = new Paint();
                paint.setAntiAlias(false);
                paint.setFilterBitmap(false);
                lpx = px;
                px = cr.getWidth();
                tr = Bitmap.createBitmap(px * length, px * length, bitmapsConfig);
                c = new Canvas(tr);
                c.drawColor(0x00FFFFFF);
                c.drawBitmap(cr, new Rect(0, 0, cr.getWidth(), cr.getHeight()), new Rect(0, 0, px, px), paint);
                cr = gameResources.diagonal_cross_line_end;
                i = (length-1)*2;
                c.drawBitmap(cr, new Rect(0,0,cr.getWidth(),cr.getHeight()), new Rect((px/2)*i,(px/2)*i, (px/2)*i+px,(px/2)*i+px) , paint);
                //c.drawBitmap(cr, new Rect(0,0,cr.getWidth(),cr.getHeight()), new Rect((length-1)*px,(length-1)*px,(length)*px, (length)*px), paint);
                cr = gameResources.diagonal_cross_line_center;
                for (i = 1; i < ((length-2)*2+2); i++)
                {
                    c.drawBitmap(cr, new Rect(0,0,cr.getWidth(),cr.getHeight()), new Rect((px/2)*i,(px/2)*i, (px/2)*i+px,(px/2)*i+px) , paint);
                }
                px = lpx;
                break;
            case 3:
                cr = gameResources.diagonal_cross_line_start;
                paint = new Paint();
                paint.setAntiAlias(false);
                paint.setFilterBitmap(false);
                lpx = px;
                px = cr.getWidth();
                tr = Bitmap.createBitmap(px * length, px * length, bitmapsConfig);
                c = new Canvas(tr);
                c.drawColor(0x00FFFFFF);
                c.drawBitmap(cr, new Rect(0, 0, cr.getWidth(), cr.getHeight()), new Rect(0, 0, px, px), paint);
                cr = gameResources.diagonal_cross_line_end;
                i = (length-1)*2;
                c.drawBitmap(cr, new Rect(0,0,cr.getWidth(),cr.getHeight()), new Rect((px/2)*i,(px/2)*i, (px/2)*i+px,(px/2)*i+px) , paint);
                //c.drawBitmap(cr, new Rect(0,0,cr.getWidth(),cr.getHeight()), new Rect((length-1)*px,(length-1)*px,(length)*px, (length)*px), paint);
                cr = gameResources.diagonal_cross_line_center;
                for (i = 1; i < ((length-2)*2+2); i++)
                {
                    c.drawBitmap(cr, new Rect(0,0,cr.getWidth(),cr.getHeight()), new Rect((px/2)*i,(px/2)*i, (px/2)*i+px,(px/2)*i+px) , paint);
                }
                px = lpx;

                //дзеркально
                int cc;
                for (i = 0; i < (tr.getWidth()/2); i++) {
                    for (int j = 0; j < tr.getHeight(); j++) {
                        cc = tr.getPixel(i, j);
                        tr.setPixel(i, j, tr.getPixel(tr.getWidth() - i - 1, j));
                        tr.setPixel(tr.getWidth() - i - 1, j, cc);
                    }
                }
                break;
        }
        return tr;
    }

    private Point[] getPaintCellsRange(Point screensize) //перша точка, початок (лівий верхній кут), друга - правий нижній
    {
        Point[] tr = new Point[2];
        int left = (int)(camera_position.x / px);
        int up = (int)(camera_position.y / px);
        tr[0] = new Point(left, up);
        int down = (int)((camera_position.y + ((double)screensize.y/Scale))/px)+2;
        int right = (int)((camera_position.x + ((double)screensize.x/Scale))/px)+2;
        tr[1] = new Point(right, down);
        return tr;
    }


    public void InitDraw() //розрахунок всякої фігні перед промальовкою
    {
        px = dp2px(CellSizeDp);
    }

    public int dp2px(int dp)
    {
        Resources r = ownerContext.getResources();
        return  (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }


}
