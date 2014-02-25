package cn.minking.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class Background extends View{
    Drawable mCurrentDrawable;
    Drawable mStatusbarDrawable;
    boolean mNeedShowStatusbar;
    
    public Background(Context context, AttributeSet attributeset){
        super(context, attributeset);
    }
    
    private void setNormalMode(){
        mNeedShowStatusbar = true;
        mCurrentDrawable = getContext().getResources().getDrawable(R.drawable.wallpaper_mask);
        mCurrentDrawable.setBounds(0, getHeight() - mCurrentDrawable.getIntrinsicHeight(), 
                getWidth(), getHeight());
        invalidate();
    }

    protected void dispatchDraw(Canvas canvas){
        mCurrentDrawable.draw(canvas);
        if (mNeedShowStatusbar){
            mStatusbarDrawable.draw(canvas);
        }
    }

    protected void onLayout(boolean flag, int i, int j, int k, int l){
        super.onLayout(flag, i, j, k, l);
        if (mStatusbarDrawable == null){
            mStatusbarDrawable = getContext().getResources().getDrawable(R.drawable.statusbar_bg);
            mStatusbarDrawable.setBounds(0, 0, getWidth(), mStatusbarDrawable.getIntrinsicHeight());
            setNormalMode();
        }
    }

    public void setEnterEditingMode(){
        mNeedShowStatusbar = false;
        mCurrentDrawable = getContext().getResources().getDrawable(R.drawable.editing_bg);
        mCurrentDrawable.setBounds(0, 0, getWidth(), getHeight());
        invalidate();
    }

    public void setEnterPreviewMode(){
        mNeedShowStatusbar = false;
        mCurrentDrawable = getContext().getResources().getDrawable(R.color.preview_background);
        mCurrentDrawable.setBounds(0, 0, getWidth(), getHeight());
        invalidate();
    }

    public void setExitEditingMode(){
        setNormalMode();
    }

    public void setExitPreviewMode(){
        setNormalMode();
    }
}