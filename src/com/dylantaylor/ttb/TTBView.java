package com.dylantaylor.ttb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 *
 * @author Dylan Taylor
 */
public class TTBView extends Activity {

    private final boolean debug = true;
    private Paint drawColor = new Paint();
    private PowerManager.WakeLock wakeLock;
    private Context context; //application context
    private int duration = Toast.LENGTH_SHORT; //duration for "Toast" message
    private Bitmap environment; //used to store a pre-rendered version of the background and border
    private Toast toast; //toast message object
    private RefreshHandler refreshHandler = new RefreshHandler();
    private static Panel gamePanel;
    //private Bundle data = getIntent().getExtras();
    //short integers used to determine where the user pressed on the screen
    private short x = 0; //finger x coordinate
    private short y = 0; //finger y coordinate
    //short integers containing the current location of the ball on the screen
    private int bulX; //ball x coordinate (upper left corner)
    private int bulY; //ball y coordinate (upper left corner)
    private int blrX; //ball x coordinate (lower right corner)
    private int blrY; //ball y coordinate (lower right corner)
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
    private boolean firstDraw = true; //whether the screen is being drawn for the first time. used for optimization
    private boolean newBall = true; //the previous ball was successfully clicked; redraw a new one at a random location within the border
    private final boolean drawScenery = true; //whether or not to draw the background and borders
    private final boolean showNotifications = true; //whether or not to show toast notifications
    private final boolean forceLevelUp = true; //force the level to increase when search is pressed; used for debugging.
    private boolean gamePaused = false;
    private boolean hitX = false; //whether the ball hit a boundary on the x axis
    private boolean hitY = false; //whether the ball hit a boundary on the y axis
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
    private int deltaX = 0;
    private int deltaY = 0;
    private final int startingSpeed = 1; //the speed the ball starts at on the first level
    private final int deltaCap = 8; //maximum speed; prevents the ball from getting too hard to catch
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
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
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
        gamePaused = true;
        super.onStop();
        wakeLock.release(); //release the wake lock
    }

    @Override
    public void onResume() {
        gamePaused = false;
        super.onResume();
        wakeLock.acquire();
    }

    private void levelUp() {
        level++;
        //delay = (int) (1000 - (9.5 * level)); //allows delay to range from 990 to 50 over 100 levels
        newBall = true;
        gamePanel.invalidate();
        displayMessage(new StringBuilder("Level ").append(level).toString());
        if (level >= 100) {
            //congratulations message will go here...
        }
    }

    public void update() {
        //contains some code borrowed from snake example
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate >= delay) {
            if (!gamePaused) {
                gamePanel.invalidate();
                lastUpdate = currentTime;
            }
        }
        refreshHandler.sleep(delay);
    }

    private static int random(int min, int max) { //random number generator
        return (int) ((Math.random() * max) + min);
    }

    public void showDebugInfo() {
        //for debugging purposes

        displayMessage(new StringBuilder("Last touched: ").append(x).append(",").append(y).append("\nBall location:").append(bulX).append(",").append(bulY).append("\nScreen size: ").append(sWidth).append(",").append(sHeight).append("\nRight Border: ").append(rb).append("\nBottom Border: ").append(bb).toString());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { //returns to menu
            finish(); //destroy this activity
            //return super.onKeyDown(keyCode, msg);
        } else if (keyCode == KeyEvent.KEYCODE_MENU) { //displays debugging info
            if (debug) {
                duration = Toast.LENGTH_LONG;
                showDebugInfo();
                duration = Toast.LENGTH_SHORT;
            }
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) { //respawns ball
            if (debug) {
                if (forceLevelUp) {
                    levelUp();
                } else {
                    newBall = true;
                    gamePanel.invalidate();
                }
            }
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
            if ((withinRange(x, (bulX - Math.abs(deltaX)), (blrX + Math.abs(deltaX)))) && (withinRange(y, (bulY - Math.abs(deltaY)), (blrY + Math.abs(deltaY))))) {
                levelUp();
            } else {
                if (debug) {
//                    showDebugInfo();
                } else {
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
        if (showNotifications) {
            toast = Toast.makeText(context, text, duration);
            toast.cancel(); //close previous message if one exists
            toast.show();
        }
    }

    public class Panel extends View {

        public Panel(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (firstDraw) {
                //Determine screen resolution
                sHeight = (short) this.getHeight();
                sWidth = (short) this.getWidth();
                environment = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Config.RGB_565); //allows the background and border rendering to only be done once
                canvas = new Canvas(environment);
                canvas.drawColor(Color.BLACK);
                //draw background
                for (short i = 0; i < sHeight; i += bgHeight) {
                    for (short j = 0; j < sWidth; j += bgWidth) {
                        if (drawScenery) {
                            canvas.drawBitmap(bg, j, i, null);
                        }
                    }
                }
                //draw top border and right side
                for (short i = 0; i < sWidth; i += blockWidth) {
                    if (drawScenery) {
                        canvas.drawBitmap(block, i, 0, null);
                    }
                    if (i + blockHeight >= sWidth) {
                        rb = (short) (i - ballWidth); //right boundary
                        for (int j = 0; j < sHeight; j += blockHeight) {
                            if (drawScenery) {
                                canvas.drawBitmap(block, i, j, null);
                            }
                        }
                    }
                }
                //draw left side and bottom border
                for (short i = 0; i < sHeight; i += blockHeight) {
                    if (drawScenery) {
                        canvas.drawBitmap(block, 0, i, null);
                    }
                    if (i + blockHeight >= sHeight) {
                        bb = (short) (i - ballHeight);
                        for (int j = 0; j < sWidth; j += blockWidth) {
                            if (drawScenery) {
                                canvas.drawBitmap(block, j, i, null);
                            }
                        }
                    }
                }
                File file = new File(Environment.getExternalStorageDirectory(), "ttb_environment.png");
                try {
                    OutputStream outStream = new FileOutputStream(file);
                    environment.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                } catch (Exception e) {
                    /* do nothing */
                }
                canvas = new Canvas();
            }
            canvas.drawBitmap(environment, 0, 0, drawColor);
            if (newBall) {
                /* Draw ball in center of the screen, works perfectly; deprecated:
                 * bx = ((sWidth / 2) - (ballWidth / 2));
                 * by = ((sHeight / 2) - (ballHeight / 2));
                 */
                //determine which direction the ball will be traveling in
                deltaX = (((random(1, 10000)) % 2) == 0) ? 1 : -1;
                deltaY = (((random(1, 10000)) % 2) == 0) ? 1 : -1;
                if (level > 9) { //start increasing the speed of the ball
                    deltaX *= ((level - (level % 10)) / 10) + 1;
                    deltaY *= ((level - (level % 10)) / 10) + 1;
                    //cap the deltas to prevent excessive speed
                    deltaX = (deltaX > deltaCap) ? deltaCap : deltaX;
                    deltaY = (deltaY > deltaCap) ? deltaCap : deltaY;
                } else { //make the ball move at the starting speed
                    deltaX *= startingSpeed;
                    deltaY *= startingSpeed;
                }
                //spawn the ball in a random location, making sure it's not on top of the border or within 5 pixels of it
                if (!firstDraw) {
                    boldx = bulX;
                    boldy = bulY;
                    //ensures the ball spawns reasonably far away from the old location
                    do {
                        bulX = random((lb + 5), (rb - ballWidth - 5));
                        bulY = random((tb + 5), (bb - ballHeight - 5));
                    } while ((Math.abs(bulX - boldx) < tWidth) && (Math.abs(bulY - boldy) < tHeight));
                } else {
                    //spawn the ball for the first time
                    bulX = random((lb + 5), (rb - ballWidth - 5));
                    bulY = random((tb + 5), (bb - ballHeight - 5));
                    //it's no longer the first draw
                    firstDraw = false;
                }
                newBall = false;
            } else {
                hitX = false;
                hitY = false;
                //make sure the ball touches the x boundaries if it would otherwise exceed them
                if ((bulX + deltaX) < lb) { //if the ball exceeds the left boundary
                    bulX = lb; //make the ball touch the left boundary
                    hitX = true;
                } else if ((bulX + deltaX) > rb) { //if the ball exceeds the right boundary
                    bulX = rb; //make the ball touch the right boundary
                    hitX = true;
                }
                //make sure the ball hits the y boundaries if it would otherwise exceed them
                if ((bulY + deltaY) < tb) { //if the ball exceeds the top boundary
                    bulY = tb; //make the ball touch the top boundary
                    hitY = true;
                } else if ((bulY + deltaY) > bb) { //if the ball exceeds the bottom boundary
                    bulY = bb; //make the ball touch the bottom boundary
                    hitY = true;
                }
                //if we hit one of the boundaries, reverse the direction of the ball
                deltaX = (((bulX + deltaX) >= lb) && ((bulX + deltaX) <= rb)) ? deltaX : (deltaX * -1);
                deltaY = (((bulY + deltaY) >= tb) && ((bulY + deltaY) <= bb)) ? deltaY : (deltaY * -1);
                if (!hitX) {
                    bulX += deltaX; //update the ball's location on the X axis
                }
                if (!hitY) {
                    bulY += deltaY; //update the ball's location on the Y axis
                }
            }
            blrX = bulX + (ballWidth - 1);
            blrY = bulY + (ballHeight - 1);
            canvas.drawBitmap(ball, bulX, bulY, null); //draws the ball
            if (debug) {
                //Now calculate the new center of the ball
                bxc = (bulX + (ballWidth / 2));
                byc = (bulY + (ballHeight / 2));
                //draw a point at the approximate center of the ball
                canvas.drawPoint(bxc, byc, drawColor);
                //draw a square around the ball's clickable area
                for (int i = bulY; i < blrY; i++) {
                    canvas.drawPoint(bulX, i, drawColor);
                    canvas.drawPoint(blrX, i, drawColor);
                }
                for (int i = bulX; i < blrX; i++) {
                    canvas.drawPoint(i, bulY, drawColor);
                    canvas.drawPoint(i, blrY, drawColor);
                }
            }
        }
    }
}
