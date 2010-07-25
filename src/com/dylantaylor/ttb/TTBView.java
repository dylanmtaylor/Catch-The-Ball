package com.dylantaylor.ttb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 *
 * @author Dylan Taylor
 */
public class TTBView extends Activity {

    //Display display = getWindowManager().getDefaultDisplay();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new Panel(this));
    }

    class Panel extends View {

        public Panel(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            Bitmap block = BitmapFactory.decodeResource(getResources(), R.drawable.block);
            Bitmap bg = BitmapFactory.decodeResource(getResources(), R.drawable.bmmini);
            Bitmap ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
            canvas.drawColor(Color.BLACK);
            int longer = (this.getWidth() > this.getHeight()) ? this.getWidth() : this.getHeight();
            int tb = block.getHeight(); //top boundary
            int lb = block.getWidth(); //left boundary
            int bb = block.getHeight(); //bottom boundary
            int rb = block.getWidth(); //right boundary
            //draw background
            for (int i = 0; i < this.getHeight(); i += bg.getHeight()) {
                for (int j = 0; j < this.getWidth(); j += bg.getWidth()) {
                    canvas.drawBitmap(bg, j, i, null);
                }
            }
            //draw top border and right side
            for (int i = 0; i < longer; i += block.getWidth()) {
                canvas.drawBitmap(block, i, 0, null);
                if (i + block.getHeight() >= this.getWidth()) {
                    rb = (i * block.getWidth()) - block.getWidth(); //right boundary
                    for (int j = 0; j < longer; j += block.getHeight()) {
                        canvas.drawBitmap(block, i, j, null);
                    }
                }
            }
            //draw left side and bottom border
            for (int i = 0; i < longer; i = i + block.getHeight()) {
                canvas.drawBitmap(block, 0, i, null);
                if (i + block.getHeight() >= this.getHeight()) {
                    bb = i * block.getHeight() - block.getHeight();
                    for (int j = 0; j < longer; j += block.getWidth()) {
                        canvas.drawBitmap(block, j, i, null);
                    }
                }
            }
            //draw ball at the center of the screen
            canvas.drawBitmap(ball, ((this.getWidth() / 2) - (ball.getWidth() / 2)), ((this.getHeight() / 2) - ball.getHeight() / 2), null);
        }
    }
}
