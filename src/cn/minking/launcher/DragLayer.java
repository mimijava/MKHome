package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    DragLayer.java
 * 创建时间：    2014-02-26
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140226: 桌面背景显示
 * ====================================================================================
 */

import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.view.*;

public class DragLayer extends FrameLayout {
    private static final String TAG = "MKHome.DragLayer";
    
    // DragLayer应用与Launcher及使用的CONTROLLER
    private Launcher mLauncher;
    private DragController mDragController;
    
    private Runnable OffsetUpdater;
    private boolean mOffsetChanged = false;
    private int mOldOffsetX = 0;
    
    // 屏幕显示大小
    private Point mScreenSize;
    private Context mContext;
    
    // 桌面背景
    private Bitmap mWallpaper;
    private WallpaperManager mWallpaperManager;
    private Paint mWallpaperPaint;
    private int mWpHeight = 0;
    private float mWpOffsetX = 0F;
    private float mWpOffsetY = 0F;
    private boolean mWpScrolling = true;
    private float mWpStepX = 0F;
    private float mWpStepY = 0F;
    private int mWpWidth = 0;
    
    public DragLayer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        
        mContext = context;
        mWallpaperManager = WallpaperManager.getInstance(context);
        mWallpaperPaint = new Paint();
        mScreenSize = new Point();
        OffsetUpdater = new Runnable() {
            
            @Override
            public void run() {
                updateWallpaperOffset();
            }
        };
    }
    public void setDragController(DragController dragcontroller){
        mDragController = dragcontroller;
    }
    
    public void setLauncher(Launcher launcher){
        mLauncher = launcher;
        mLauncher.getWindowManager().getDefaultDisplay().getSize(mScreenSize);
    }
    
    /**
     *  功能： 桌面背景显示
     *  调用： 使用于Launcher.java
     */
    public void updateWallpaper() {
        mLauncher.getWindow().setFormat(PixelFormat.TRANSPARENT);
        mWallpaper = null;
        updateWallpaperOffset();
    }
    
    public void updateWallpaperOffset() {
        if (mWallpaper == null) {
            mWallpaperManager.setWallpaperOffsetSteps(mWpStepX, mWpStepY);
            if (getWindowToken() == null) {
                removeCallbacks(OffsetUpdater);
                postDelayed(OffsetUpdater, 50L);
            }else {
                //ViewRootImpl.getWindowSession(mContext.getMainLooper()).setWallpaperPosition(getWindowToken(), mWpOffsetX, mWpOffsetY, mWpStepX, mWpStepY);   
            }
        }else {
            int offsetX = (int)((float)(mWpWidth - mScreenSize.x) * mWpOffsetX);
            if (mOldOffsetX != offsetX) {
                mOffsetChanged = true;
            }
            mOldOffsetX = offsetX;
        }
    }
    
    public void updateWallpaperOffset(float xStep, float yStep, float xOffset, float yOffset) {
        if (mWpScrolling && mWpOffsetX != xOffset) {
            mWpStepX = xStep;
            mWpStepY = yStep;
            mWpOffsetX = xOffset;
            mWpOffsetY = yOffset;
            updateWallpaperOffset();
        }
    }
    
    public void updateWallpaperOffsetAnimate(final float xStep, final float yStep, final float xOffset, final float yOffset) {
        final float xStepDelta = xStep - mWpStepX;
        final float yStepDelta = yStep - mWpStepY;
        final float mWpOffsetXDelta = xOffset - mWpOffsetX;
        final float mWpOffsetYDelta = yOffset - mWpOffsetY;
        
        float af[] = new float[]{1F, 0F};
        
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(af);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            
//          final float mWpOffsetXDelta;
//            final float mWpOffsetYDelta;
//            final float xOffset;
//            final float xStep;
//            final float xStepDelta;
//            final float yOffset;
//            final float yStep;
//            final float yStepDelta;
            
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float f = ((Float)animation.getAnimatedValue()).floatValue();
                
                updateWallpaperOffset(xStep - f * xStepDelta, yStep - f * yStepDelta, 
                        xOffset - f * mWpOffsetXDelta, yOffset - f * mWpOffsetYDelta);
            }
        });
        
        valueAnimator.start();
    }
    
    public void getLocationInDragLayer(View view, int[] loc){
        loc[0] = 0;
        loc[1] = 0;
        getDescendantCoordRelativeToSelf(view, loc);
    }
    
    /**
     * Given a coordinate relative to the descendant, find the coordinate in this DragLayer's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param coord The coordinate that we want mapped.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord){
        float scale = 1.0f;
        float[] pt = {coord[0], coord[1]};
        
        descendant.getMatrix().mapPoints(pt);
        scale *= descendant.getScaleX();
        pt[0] += descendant.getLeft();
        pt[1] += descendant.getTop();
        ViewParent viewParent = descendant.getParent();
        while (viewParent instanceof View && viewParent != this) {
            final View view = (View)viewParent;
            view.getMatrix().mapPoints(pt);
            scale *= view.getScaleX();
            pt[0] += view.getLeft() - view.getScrollX();
            pt[1] += view.getTop() - view.getScrollY();
            viewParent = view.getParent();
        }
        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }
    
    public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];

        v.getLocationInWindow(loc);
        int vX = loc[0];
        int vY = loc[1];

        int left = vX - x;
        int top = vY - y;
        r.set(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
    }
    
    public float getWpOffsetX(){
        return mWpOffsetX;
    }

    public float getWpStepX(){
        return mWpStepX;
    }
    
    
    
    
    
    
    
    
}
