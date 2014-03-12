package cn.minking.launcher;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class DragView extends View{
    ValueAnimator mAnim;
    private Bitmap mBitmap;
    private Paint mCustomPaint;
    private DragLayer mDragLayer;
    private Rect mDragRegion;
    private Point mDragVisualizeOffset;
    private boolean mHasDrawn;
    private DragLayer.LayoutParams mLayoutParams;
    private float mOffsetX;
    private float mOffsetY;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;
    
    public DragView(Launcher launcher, Bitmap bitmap, 
            int i, int j, int k, int l, int i1, int j1) {
        super(launcher);
        mDragVisualizeOffset = null;
        mDragRegion = null;
        mDragLayer = null;
        mHasDrawn = false;
        mOffsetX = 0F;
        mOffsetY = 0F;
        mDragLayer = launcher.getDragLayer();
        Resources resources = getResources();
        final int extraPixel = resources.getInteger(R.integer.config_dragViewExtraPixels);
        Matrix matrix = new Matrix();
        float offsetT = (float)((i1 + extraPixel) / i1);
        if (offsetT != 1F){
            matrix.setScale(offsetT, offsetT);
        }
        
        final int offsetX = resources.getDimensionPixelSize(R.dimen.dragViewOffsetX);
        final int offsetY = resources.getDimensionPixelSize(R.dimen.dragViewOffsetY);
        
        float af[] = new float[]{0F, 1F};
        mAnim = ValueAnimator.ofFloat(af);
        mAnim.setDuration(110L);
        mAnim.setInterpolator(new DecelerateInterpolator(2.5F));
        
        mAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            
            @Override
            public void onAnimationUpdate(ValueAnimator valueanimator) {
                 float f = ((Float)valueanimator.getAnimatedValue()).floatValue();
                 int l1 = (int)(f * (float)offsetX - mOffsetX);
                 int i2 = (int)(f * (float)offsetY - mOffsetY);
            }
        });
        
        mBitmap = Bitmap.createBitmap(bitmap, k, l, i1, j1, matrix, true);
        setDragRegion(new Rect(0, 0, i1, j1));
        mRegistrationX = i;
        mRegistrationY = j;
        int k1 = android.view.View.MeasureSpec.makeMeasureSpec(0, 0);
        measure(k1, k1);
    }
    
    public Rect getDragRegion(){
        return mDragRegion;
    }
    
    public boolean hasDrawn(){
        return mHasDrawn;
    }
    
    void move(int i, int j){
        DragLayer.LayoutParams layoutparams = mLayoutParams;
        layoutparams.x = (i - mRegistrationX) + (int)mOffsetX;
        layoutparams.y = (j - mRegistrationY) + (int)mOffsetY;
        mDragLayer.requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        mHasDrawn = true;
        Bitmap bitmap = mBitmap;
        Paint paint;
        if (mCustomPaint != null){
            paint = mCustomPaint;
        } else {
            paint = mPaint;
        }
        canvas.drawBitmap(bitmap, 0F, 0F, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBitmap.getWidth(), mBitmap.getHeight());
    }
    
    void remove(){
        post(new Runnable() {
            
            @Override
            public void run() {
                mDragLayer.removeView(DragView.this);
            }
        });
    }
    
    public void setAlpha(float f){
        setAlpha(f);
        if (mPaint == null){
            mPaint = new Paint();
        }
        mPaint.setAlpha((int)(255F * f));
        invalidate();
    }
    
    public void setDragRegion(Rect rect){
        mDragRegion = rect;
    }
    
    public void setDragVisualizeOffset(Point point){
        mDragVisualizeOffset = point;
    }
    
    public void setPaint(Paint paint){
        mCustomPaint = paint;
        invalidate();
    }
    
    public void show(int i, int j){
        mDragLayer.addView(this);
        DragLayer.LayoutParams layoutparams = new DragLayer.LayoutParams(0, 0);
        layoutparams.width = mBitmap.getWidth();
        layoutparams.height = mBitmap.getHeight();
        layoutparams.x = i - mRegistrationX;
        layoutparams.y = j - mRegistrationY;
        layoutparams.customPosition = true;
        setLayoutParams(layoutparams);
        mLayoutParams = layoutparams;
        mAnim.start();
    }
}