package com.dylantaylor.ttb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 *
 * @author Dylan Taylor
 */
public class TTBView extends Activity {

    private final boolean debug = true;
    private Paint drawColor = new Paint();
    private PowerManager.WakeLock wl;
    private Context context; //application context
    private int duration = Toast.LENGTH_LONG; //duration for "Toast" message
    private Toast toast; //toast message object
    private RefreshHandler refreshHandler = new RefreshHandler();
    private static Panel gamePanel;
    //private Bundle data = getIntent().getExtras();
    //short integers used to determine where the user pressed on the screen
    private short x = 0; //finger x coordinate
    private short y = 0; //finger y coordinate
    //short integers containing the current location of the ball on the screen
    private int bulx; //ball x coordinate (upper left corner)
    private int buly; //ball y coordinate (upper left corner)
    private int blrx; //ball x coordinate (lower right corner)
    private int blry; //ball y coordinate (lower right corner)
    //short integers containing the previous location of the ball on the screen
    private int boldx; //previous x coordinate
    private int boldy; //previous y coordinate
    private int bxc; //ball approximate center x coordinate
    private int byc; //ball approximate center y coordinate
    //short integers containing the screen resolution. set during first draw.
    public static short sHeight = 0; //screen height in pixels
    public static short sWidth = 0; //screen width in pixels
    //integers containing approximately 1/3 the screen height (truncated)
    public static short tHeight = 0;
    public static short tWidth = 0;
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
    private boolean gameover = false;
    //private short timer = data.getShort("timer");
    private short level = 0; //current level; changes to 1 upon game starting.
    private int delay = 0; //delay in miliseconds before updating ball location
    private float deltaX = 0;
    private float deltaY = 0;
    //variables related to delays and timing
    private long lastUpdate;

    class RefreshHandler extends Handler { //Makes animation possible

        @Override
        public void handleMessage(Message msg) {
            TTBView.this.update();
            TTBView.gamePanel.invalidate();
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };

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
        //Set top and left boundaries; ball tends to get stuck on these
        tb = blockHeight;
        lb = blockWidth;
        //set up the application context for toast notifications
        context = getApplicationContext();
        gamePanel = new Panel(this);
        drawColor.setColor(Color.GREEN);
        setContentView(gamePanel);
        lastUpdate = System.currentTimeMillis();
        update();
        levelUp();
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
        //delay = (int) (1000 - (9.5 * level)); //allows delay to range from 990 to 50 over 100 levels
        newBall = true;
        gamePanel.invalidate();
        duration = Toast.LENGTH_LONG;
        displayMessage(new StringBuilder("Level ").append(level).toString());
        if (level >= 100) {
            //congratulations message will go here...
        }
    }

    public void update() {
        //contains some code borrowed from snake example
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate >= delay) {
            gamePanel.invalidate();
            lastUpdate = currentTime;
        }
        refreshHandler.sleep(delay);
    }

    private static int random(int min, int max) { //random number generator
        return (int) ((Math.random() * max) + min);
    }

    public void showDebugInfo() {
        //for debugging purposes
        duration = Toast.LENGTH_LONG;
        displayMessage(new StringBuilder("Last touched: ").append(x).append(",").append(y).append("\nBall location:").append(bulx).append(",").append(buly).append("\nScreen size: ").append(sWidth).append(",").append(sHeight).append("\nRight Border: ").append(rb).append("\nBottom Border: ").append(bb).toString());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { //returns to menu
            return super.onKeyDown(keyCode, msg);
        } else if (keyCode == KeyEvent.KEYCODE_MENU) { //displays debugging info
            duration = Toast.LENGTH_SHORT;
            showDebugInfo();
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) { //respawns ball
            newBall = true;
            gamePanel.invalidate();
        } else {
            /* do nothing */
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //Only record it as a press if they press down, not if they let go.
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            x = (short) e.getX();
            y = (short) e.getY();
            if ((withinRange(x, bulx, blrx)) && (withinRange(y, buly, blry))) {
                levelUp();
            } else {
                if (debug) {
                    showDebugInfo();
                } else {
                    duration = Toast.LENGTH_SHORT;
                    displayMessage("Miss");
                }
            }
        }
        return true;
    }

    public boolean withinRange(int n, int r1, int r2) {
        if ((n >= r1) && (n <= r2)) {
            return true;
        } else {
            return false;
        }
    }

    public void displayMessage(CharSequence text) {
        toast = Toast.makeText(context, text, duration);
        toast.cancel(); //close previous message if one exists
        toast.show();
    }

    public class Panel extends View {

        public Panel(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (firstdraw) {
                canvas.drawColor(Color.BLACK);
                //Determine screen resolution
                sHeight = (short) this.getHeight();
                sWidth = (short) this.getWidth();
            }
            //draw background
            for (short i = 0; i < sHeight; i += bgHeight) {
                for (short j = 0; j < sWidth; j += bgWidth) {
                    canvas.drawBitmap(bg, j, i, null);
                }
            }
            //draw top border and right side
            for (short i = 0; i < sWidth; i += blockWidth) {
                canvas.drawBitmap(block, i, 0, null);
                if (i + blockHeight >= sWidth) {
                    rb = (short) (i - ballWidth); //right boundary
                    for (int j = 0; j < sHeight; j += blockHeight) {
                        canvas.drawBitmap(block, i, j, null);
                    }
                }
            }
            //draw left side and bottom border
            for (short i = 0; i < sHeight; i += blockHeight) {
                canvas.drawBitmap(block, 0, i, null);
                if (i + blockHeight >= sHeight) {
                    bb = (short) (i - ballHeight);
                    for (int j = 0; j < sWidth; j += blockWidth) {
                        canvas.drawBitmap(block, j, i, null);
                    }
                }
            }
            if (newBall) {
                /* Draw ball in center of the screen, works perfectly; deprecated:
                 * bx = ((sWidth / 2) - (ballWidth / 2));
                 * by = ((sHeight / 2) - (ballHeight / 2));
                 */
                //determine which direction the ball will be traveling in
                deltaX = (random(0, 1) == 0) ? -1 : 1;
                deltaY = (random(0, 1) == 0) ? -1 : 1;
                //spawn the ball in a random location, making sure it's not on top of the border or within 5 pixels of it
                if (!firstdraw) {
                    boldx = bulx;
                    boldy = buly;
                    //ensures the ball spawns reasonably far away from the old location
                    do {
                        bulx = random((lb + 5), (rb - ballWidth - 5));
                        buly = random((tb + 5), (bb - ballHeight - 5));
                    } while ((Math.abs(bulx - boldx) < tWidth) || (Math.abs(buly - boldy) < tHeight));
                } else {
                    //spawn the ball for the first time
                    bulx = random((lb + 5), (rb - ballWidth - 5));
                    buly = random((tb + 5), (bb - ballHeight - 5));
                    //it's no longer the first draw
                    firstdraw = false;
                }
                newBall = false;
            } else {
                //if we hit one of the boundaries, reverse the direction of the ball
                deltaX = (((bulx + deltaX) >= lb) && ((bulx + deltaX) <= rb)) ? deltaX : (deltaX * -1);
                deltaY = (((buly + deltaY) >= tb) && ((buly + deltaY) <= bb)) ? deltaY : (deltaY * -1);
                //update the location of the ball
                bulx += deltaX;
                buly += deltaY;
            }
            blrx = bulx + (ballWidth - 1);
            blry = buly + (ballHeight - 1);
            canvas.drawBitmap(ball, bulx, buly, null); //draws the ball
            if (debug) {
                //Now calculate the new center of the ball
                bxc = (bulx + (ballWidth / 2));
                byc = (buly + (ballHeight / 2));
                //draw a point at the approximate center of the ball
                canvas.drawPoint(bxc, byc, drawColor);
                //draw a square around the ball's clickable area
                for (int i = buly; i < blry; i++) {
                    canvas.drawPoint(bulx, i, drawColor);
                    canvas.drawPoint(blrx, i, drawColor);
                }
                for (int i = bulx; i < blrx; i++) {
                    canvas.drawPoint(i, buly, drawColor);
                    canvas.drawPoint(i, blry, drawColor);
                }
            }
        }
    }
}
