package com.dylantaylor.ttb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

/**
 *
 * @author Dylan Taylor
 */
public class TTBView extends Activity implements OnTouchListener {

    private PowerManager.WakeLock wl;
    private Bundle data = getIntent().getExtras();
    //short integers used to determine where the user pressed on the screen
    private short x = 0; //finger x coordinate
    private short y = 0; //finger y coordinate
    //short integers containing the current location of the ball on the screen
    private short bx; //ball x coordinate (upper left corner)
    private short by; //ball y coordinate (upper left corner)
    private short bcx; //ball center x coordinate
    private short bcy; //ball center y coordinate
    //short integers containing the screen resolution. set during first draw.
    private short sHeight = 0; //screen height in pixels
    private short sWidth = 0; //screen width in pixels
    //create null Bitmap resources
    Bitmap block = null; //block image for the border
    Bitmap bg = null; //tiled background image
    Bitmap ball = null; //ball image
    //boolean values
    private boolean firstdraw = true; //whether the screen is being drawn for the first time. used for optimization
    private boolean newBall = true; //the previous ball was successfully clicked; redraw a new one at a random location within the border
    //sizes of resources; checked only once for optimization
    private short blockHeight;
    private short blockWidth;
    private short bgHeight;
    private short bgWidth;
    private short ballHeight;
    private short ballWidth;
    //Boundaries of the area within the border; changes later
    private short tb = 0; //top boundary
    private short lb = 0; //left boundary
    private short bb = 0; //bottom boundary
    private short rb = 0; //right boundary
    //Gameplay related variables
    private short timer = data.getShort("timer");
    private short level = 1; //current level; defaults to 1.
    private int delay = 810; //delay in miliseconds before updating ball location
    private float deltaX;
    private float deltaY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Hide the "Touch The Ball" title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Put the application in fullscreen mode.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Prevent the screen from dimming during the game.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
        //Load Bitmap resources
        block = BitmapFactory.decodeResource(getResources(), R.drawable.block); //block image for the border
        bg = BitmapFactory.decodeResource(getResources(), R.drawable.bmmini); //tiled background image
        ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball); //ball image
        //Calculate sizes
        blockHeight = (short) block.getHeight();
        blockWidth = (short) block.getWidth();
        bgHeight = (short) bg.getHeight();
        bgWidth = (short) bg.getWidth();
        ballHeight = (short) ball.getHeight();
        ballWidth = (short) ball.getWidth();
        //Set top and left boundaries
        tb = blockHeight;
        lb = blockWidth;
        setContentView(new Panel(this));
    }

    @Override
    public void onPause() {
        super.onPause();
        wl.release(); //release the wake lock
    }

    @Override
    public void onResume() {
        super.onResume();
        wl.acquire();
    }

    private void levelUp() {
        level++;
        if (level >= 100) {
            //congratulations message will go here...
        }
        delay = (short) (1000 - (9.5 * level)); //allows delay to range from 990 to 50 over 100 levels
    }

    private short rand(int min, int max) { //random number generator
        return (short) ((Math.random() * max) + min);
    }

    public boolean onTouch(View v, MotionEvent e) {
        //Only record it as a press if they press down, not if they let go.
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            this.x = (short) e.getX();
            this.y = (short) e.getY();
        }
        return true;
    }

    class Panel
            extends View {

        public Panel(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            canvas.drawColor(Color.BLACK);
            if (firstdraw) {
                //Determine screen resolution
                sHeight = (short) this.getHeight();
                sWidth = (short) this.getWidth();
                firstdraw = false;
            }
            //draw background
            for (short i = 0; i < sHeight; i += bgHeight) {
                for (short j = 0; j < sWidth; j += bgWidth) {
                    canvas.drawBitmap(bg, j, i, null);
                }
            }
            //draw top border and right side
            for (short i = 0; i < sHeight; i += blockWidth) {
                canvas.drawBitmap(block, i, 0, null);
                if (i + blockHeight >= sWidth) {
                    rb = (short) ((i * blockWidth) - blockWidth); //right boundary
                    for (int j = 0; j < sHeight; j += blockHeight) {
                        canvas.drawBitmap(block, i, j, null);
                    }
                }
            }
            //draw left side and bottom border
            for (short i = 0; i < sHeight; i += blockHeight) {
                canvas.drawBitmap(block, 0, i, null);
                if (i + blockHeight >= sHeight) {
                    bb = (short) (i * blockHeight - blockHeight);
                    for (int j = 0; j < sWidth; j += blockWidth) {
                        canvas.drawBitmap(block, j, i, null);
                    }
                }
            }
            if (newBall) {
                /* Draw ball in center of the screen; deprecated:
                 * canvas.drawBitmap(ball, ((sWidth / 2) - (ballWidth / 2)), ((sHeight / 2) - (ballHeight / 2)), null);
                 */
                //determine which direction the ball will be traveling in
                deltaX = (rand(0, 1) == 0) ? -1 : 1;
                deltaY = (rand(0, 1) == 0) ? -1 : 1;
                //spawn the ball in a random location, making sure it's not on top of the border
                bx = rand((lb + 1), (rb - ballWidth - 1));
                by = rand((tb + 1), (bb - ballHeight - 1));
                canvas.drawBitmap(ball, bx, by, null);
            } else {
                //updateBall();
                canvas.drawBitmap(ball, bx, by, null);
            }
        }
    }
}