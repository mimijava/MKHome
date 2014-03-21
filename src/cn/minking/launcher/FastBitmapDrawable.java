package cn.minking.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

class FastBitmapDrawable extends Drawable{
    private Bitmap mBitmap;
    private int mHeight;
    private int mWidth;
    
    FastBitmapDrawable(Bitmap bitmap) {
        mBitmap = bitmap;
        if (bitmap == null) {
            mHeight = 0;
            mWidth = 0;
        } else {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0F, 0F, null);
    }

    public int getIntrinsicHeight() {
        return mHeight;
    }

    public int getIntrinsicWidth() {
        return mWidth;
    }

    public int getMinimumHeight() {
        return mHeight;
    }

    public int getMinimumWidth() {
        return mWidth;
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int i) {
    }

    public void setColorFilter(ColorFilter colorfilter) {
    }
}