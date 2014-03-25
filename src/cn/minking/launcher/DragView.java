package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    DragView.java
 * 创建时间：    2014-02-24
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140324: 修改可拖动项目的视图View
 * ====================================================================================
 */
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
    private static final String TAG = "DragView";
    ValueAnimator mAnim;
    private Bitmap mBitmap;
    private Paint mCustomPaint;
    private DragLayer mDragLayer;
    private Rect mDragRegion = null;
    private Point mDragVisualizeOffset = null;
    private boolean mHasDrawn = false;
    private DragLayer.LayoutParams mLayoutParams;
    private float mOffsetX = 0.0F;
    private float mOffsetY = 0.0F;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;
    
    public DragView(Launcher launcher, Bitmap bitmap, 
            int registrationX, int registrationY, int left, int top, int width, int height) {
        super(launcher);
        mDragLayer = launcher.getDragLayer();
        
        Resources res = getResources();
        final int offsetX = res.getDimensionPixelSize(R.dimen.dragViewOffsetX);
        final int offsetY = res.getDimensionPixelSize(R.dimen.dragViewOffsetY);
        final int extraPixel = res.getInteger(R.integer.config_dragViewExtraPixels);
        
        // 被拖动的目标放大显示
        Matrix matrix = new Matrix();
        float offsetT = (float)((width + extraPixel) / width);
        if (offsetT != 1.0F){
            matrix.setScale(offsetT, offsetT);
        }
        
        // 放大过程动画
        mAnim = ValueAnimator.ofFloat(0.0F, 1.0F);
        mAnim.setDuration(110L);
        mAnim.setInterpolator(new DecelerateInterpolator(2.5F));
        
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            
            @Override
            public void onAnimationUpdate(ValueAnimator valueanimator) {
                float value = ((Float)valueanimator.getAnimatedValue()).floatValue();
                int deltaX = (int)(value * (float)offsetX - mOffsetX);
                int deltaY = (int)(value * (float)offsetY - mOffsetY);
                
                if (getParent() == null) {
                    valueanimator.cancel();
                }else {
                    DragLayer.LayoutParams localLayoutParams = mLayoutParams;
                    localLayoutParams.x = (deltaX + localLayoutParams.x);
                    localLayoutParams.y = (deltaY + localLayoutParams.y);
                    mDragLayer.requestLayout();
                }
            }
        });
        
        // 创建位图
        mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height, matrix, true);
        setDragRegion(new Rect(0, 0, width, height));
        mRegistrationX = registrationX;
        mRegistrationY = registrationY;
        int ms = MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        measure(ms, ms);
    }
    
    void move(int touchX, int touchY){
        DragLayer.LayoutParams layoutparams = mLayoutParams;
        layoutparams.x = (touchX - mRegistrationX) + (int)mOffsetX;
        layoutparams.y = (touchY - mRegistrationY) + (int)mOffsetY;
        mDragLayer.requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        final boolean debug = false;
        if (debug) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66ffffff);
            canvas.drawRect(0, 0, getWidth(), getHeight(), p);
        }
        
        mHasDrawn = true;
        Paint paint;
        if (mCustomPaint != null){
            paint = mCustomPaint;
        } else {
            paint = mPaint;
        }
        canvas.drawBitmap(mBitmap, 0F, 0F, paint);
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
    
    @Override
    public void setAlpha(float alpha){
        super.setAlpha(alpha);
        if (mPaint == null){
            mPaint = new Paint();
        }
        mPaint.setAlpha((int)(255.0F * alpha));
        invalidate();
    }
    
    public boolean hasDrawn(){
        return mHasDrawn;
    }
    
    public void setDragRegion(Rect rect){
        mDragRegion = rect;
    }
       
    public Rect getDragRegion(){
        return mDragRegion;
    }
    
    public void setDragVisualizeOffset(Point point){
        mDragVisualizeOffset = point;
    }
    
    public Point getDragVisualizeOffset() {
        return mDragVisualizeOffset;
    }
    
    public void setPaint(Paint paint){
        mCustomPaint = paint;
        invalidate();
    }
    
    public void show(int mMotionDownX, int mMotionDownY){
        mDragLayer.addView(this);
        DragLayer.LayoutParams lParams = new DragLayer.LayoutParams(0, 0);
        lParams.width = mBitmap.getWidth();
        lParams.height = mBitmap.getHeight();
        lParams.x = mMotionDownX - mRegistrationX;
        lParams.y = mMotionDownY - mRegistrationY;
        lParams.customPosition = true;
        setLayoutParams(lParams);
        mLayoutParams = lParams;
        mAnim.start();
    }
}