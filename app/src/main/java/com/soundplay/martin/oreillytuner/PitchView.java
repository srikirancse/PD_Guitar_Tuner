package com.soundplay.martin.oreillytuner;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;



public class PitchView extends View {

    private static final String TAG = "Pitch View";
    private static int viewCounter = 0;
    private float centerPitch;
    private float currentPitch;
    private Paint paint= new Paint();
    private int height, width;

    public PitchView(Context context){
        super(context);
    }

    public PitchView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
    }

    public PitchView(Context context, AttributeSet attributeSet, int defStyle){
        super(context, attributeSet, defStyle);
    }

    public void setCenterPitch(float centerPitch){
        this.centerPitch = centerPitch;
    }

    public void setCurrentPitch(float currentPitch){
        this.currentPitch = currentPitch;
    }


    @Override
    protected void onSizeChanged(int newHeight, int newWidth, int oldHeight, int oldWidth){
        super.onSizeChanged(newHeight, newWidth, oldHeight, oldWidth);
        height = newHeight;
        width = newWidth;
    }

    @Override
    protected void onDraw(Canvas canvas){
        float halfWidth = width/2;
        paint.setStrokeWidth(10.0f);
        paint.setColor(Color.GREEN);
        if(viewCounter==5){
            viewCounter =0;
        }else{
            viewCounter++;
        }
        //canvas.drawLine(halfWidth, height/2, width*viewCounter/5, height/2, paint);

        canvas.drawLine(halfWidth, 0, halfWidth, height, paint);

        float dx = currentPitch-centerPitch;
        Log.d(TAG, "dx: "+dx);
        if(-1<dx&&dx<1){
            paint.setStrokeWidth(2.0f);
            paint.setColor(Color.BLUE);
        }else{
            paint.setStrokeWidth(8.0f);
            paint.setColor(Color.RED);
            dx = (dx<0) ?-1:1;
        }
        double phi = dx*Math.PI/4;
        canvas.drawLine(halfWidth, height, halfWidth+(float)Math.sin(phi)*height*0.9f, height-(float)Math.cos(phi)*height*0.9f, paint);

    }

}