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

import java.util.ArrayList;
import java.util.Iterator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class DragLayer extends FrameLayout {
    public static class LayoutParams extends FrameLayout.LayoutParams {

        public boolean customPosition;
        public int x;
        public int y;

        public LayoutParams(int i, int j) {
            super(i, j);
            customPosition = false;
        }
    }
    
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
    private int mXDown;
    private int mYDown;

    private TimeInterpolator mCubicEaseOutInterpolator;
    private AppWidgetResizeFrame mCurrentResizeFrame;
    private ValueAnimator mDropAnim;
    private View mDropView;
    private float mDropViewAlpha;
    private int mDropViewPos[];
    private float mDropViewScale;
    private ValueAnimator mFadeOutAnim;
    private float mOldPositions[];
    private final ArrayList<AppWidgetResizeFrame> mResizeFrames = new ArrayList();
    private int mScaledUpsideScreenOutTouch;
    private int mTmpPos[];
    private float mTmpPosF[];
    private Rect mTmpRect;
        
    public DragLayer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
        mWpStepX = 0F;
        mWpStepY = 0F;
        mWpOffsetX = 0F;
        mWpOffsetY = 0F;
        mWpWidth = 0;
        mWpHeight = 0;
        mOldOffsetX = 0;
        mOffsetChanged = false;
        mWpScrolling = true;
        mScaledUpsideScreenOutTouch = 40;
        mDropAnim = null;
        mFadeOutAnim = null;
        mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5F);
        mDropView = null;
        mDropViewPos = new int[2];
        mTmpPos = new int[2];
        mTmpPosF = new float[2];
        mTmpRect = new Rect();
        OffsetUpdater = new Runnable() {
            @Override
            public void run() {
                updateWallpaperOffset();
            }
        };
        mWallpaperManager = WallpaperManager.getInstance(context);
        mWallpaperPaint = new Paint();
        mScreenSize = new Point();
        mScaledUpsideScreenOutTouch = (int)(getResources().getDisplayMetrics().density * (float)mScaledUpsideScreenOutTouch);
    }
    
    private void animateViewIntoPosition(View view, int i, int j, int k, int l, float f, Runnable runnable, 
            boolean flag, int i1) {
        Rect rect = new Rect(i, j, i + view.getMeasuredWidth(), j + view.getMeasuredHeight());
        Rect rect1 = new Rect(k, l, k + view.getMeasuredWidth(), l + view.getMeasuredHeight());
        DragController.TouchTranslator touchtranslator = mLauncher.getTouchTranslator();
        if (touchtranslator != null) {
            touchtranslator.translatePosition(rect1);
        }
        animateView(view, rect, rect1, 1F, (f * (float)rect1.height()) / (float)rect.height(), i1, null, null, runnable, false);
    }

    private void fadeOutDragView() {
        mFadeOutAnim = new ValueAnimator();
        mFadeOutAnim.setDuration(150L);
        ValueAnimator valueanimator = mFadeOutAnim;
        float af[] = new float[2];
        af[0] = 0F;
        af[1] = 1F;
        valueanimator.setFloatValues(af);
        mFadeOutAnim.removeAllUpdateListeners();
        mFadeOutAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueanimator1) {
                float f = ((Float)valueanimator1.getAnimatedValue()).floatValue();
                mDropViewAlpha = 1F - f;
                int mWidth = mDropView.getMeasuredWidth();
                int mHeight = mDropView.getMeasuredHeight();
                invalidate(mDropViewPos[0], mDropViewPos[1], mWidth + mDropViewPos[0], mHeight + mDropViewPos[1]);
            }
        });
        mFadeOutAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator)
            {
                mDropView = null;
            }
        });
        mFadeOutAnim.start();
    }

    public void addResizeFrame(ItemInfo iteminfo, 
            LauncherAppWidgetHostView launcherappwidgethostview, CellLayout celllayout) {
        AppWidgetResizeFrame appwidgetresizeframe = 
                new AppWidgetResizeFrame(getContext(), iteminfo, 
                        launcherappwidgethostview, celllayout, this);
        LayoutParams layoutparams = new LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutparams.customPosition = true;
        addView(appwidgetresizeframe, layoutparams);
        mResizeFrames.add(appwidgetresizeframe);
        appwidgetresizeframe.snapToWidget(false);
    }

    public void animateView(final View view, final Rect from, final Rect to, 
            final float finalAlpha, final float finalScale, 
            int i, final Interpolator motionInterpolator, 
            final Interpolator alphaInterpolator, 
            final Runnable onCompleteRunnable, final boolean fadeOut) {
        float f = (float)Math.sqrt(Math.pow(to.left - from.left, 2D) + Math.pow(to.top - from.top, 2D));
        Resources resources = getResources();
        final float initialAlpha = resources.getInteger(R.integer.config_dropAnimMaxDist);
        if (i < 0) {
            i = resources.getInteger(R.integer.config_dropAnimMaxDuration);
            if (f < initialAlpha){
                i = (int)((float)i * mCubicEaseOutInterpolator.getInterpolation(f / initialAlpha));
            }
        }
        if (mDropAnim != null){
            mDropAnim.cancel();
        }
        if (mFadeOutAnim != null){
            mFadeOutAnim.cancel();
        }
        mDropView = view;
        mDropAnim = new ValueAnimator();
        if (alphaInterpolator == null || motionInterpolator == null){
            mDropAnim.setInterpolator(mCubicEaseOutInterpolator);
        }
        mDropAnim.setDuration(i);
       
        float af[] = new float[]{0F,1F};
        mDropAnim.setFloatValues(af);
        mDropAnim.removeAllUpdateListeners();
        mDropAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueanimator) {
                float f2 = ((Float)valueanimator.getAnimatedValue()).floatValue();
                int j = view.getMeasuredWidth();
                int k = view.getMeasuredHeight();
                invalidate(mDropViewPos[0], mDropViewPos[1], j + mDropViewPos[0], k + mDropViewPos[1]);
                float f1;
                if (alphaInterpolator != null){
                    f1 = alphaInterpolator.getInterpolation(f2);
                } else {
                    f1 = f2;
                }
                float f3;
                if (motionInterpolator != null){
                    f3 = motionInterpolator.getInterpolation(f2);
                } else {
                    f3 = f2;
                }
                mDropViewPos[0] = from.left + Math.round(f3 * (float)(to.left - from.left));
                mDropViewPos[1] = from.top + Math.round(f3 * (float)(to.top - from.top));
                mDropViewScale = f2 * finalScale + (1F - f2);
                mDropViewAlpha = f1 * finalAlpha + (1F - f1) * initialAlpha;
                invalidate(mDropViewPos[0], mDropViewPos[1], j + mDropViewPos[0], k + mDropViewPos[1]);
            }
        });
        mDropAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (onCompleteRunnable != null){
                    onCompleteRunnable.run();
                }
                if (!fadeOut) {
                    mDropView = null;
                } else {
                    fadeOutDragView();
                }
            }
        });
        mDropAnim.start();
    }

    public void animateViewIntoPosition(DragView dragview, final View child,
            int i, final Runnable onFinishAnimationRunnable) {
        if (!(child.getParent() instanceof CellLayout)) {
            child.setVisibility(View.VISIBLE);
            if (onFinishAnimationRunnable != null){
                onFinishAnimationRunnable.run();
            }
        } else {
            ((CellLayout)child.getParent()).measureChild(child);
            CellLayout.LayoutParams layoutparams = (CellLayout.LayoutParams)child.getLayoutParams();
            mTmpPos[0] = layoutparams.x;
            mTmpPos[1] = layoutparams.y;
            Rect rect = mTmpRect;
            getViewRectRelativeToSelf(dragview, rect);
            float f = getDescendantCoordRelativeToSelf((View)child.getParent(), mTmpPos);
            int l = mTmpPos[0];
            int j = mTmpPos[1];
            int k = rect.left;
            int i1 = rect.top;
            child.setVisibility(View.INVISIBLE);
            animateViewIntoPosition(((View) (dragview)), k, i1, l, j, f, new Runnable() {
                @Override
                public void run() {
                    child.setVisibility(View.VISIBLE);
                    if (onFinishAnimationRunnable != null) {
                        onFinishAnimationRunnable.run();
                    }
                }
            }, false, i);
        }
    }

    public void animateViewIntoPosition(DragView dragview, View view, Runnable runnable) {
        animateViewIntoPosition(dragview, view, -1, runnable);
    }
    
    public void clearAllResizeFrames() {
        if (mResizeFrames.size() <= 0) return;
        Iterator<AppWidgetResizeFrame> iterator = mResizeFrames.iterator();
        while(iterator.hasNext()){
            removeView(iterator.next());
        }
        mResizeFrames.clear();
        return;
    }

    protected void dispatchDraw(Canvas canvas) {
        if (mWallpaper != null) {
            canvas.drawBitmap(mWallpaper, -mOldOffsetX, 0F, mWallpaperPaint);
        }
        super.dispatchDraw(canvas);
        if (mDropView != null) {
            canvas.save(1);
            int j = mDropViewPos[0] - mDropView.getScrollX();
            int i = mDropViewPos[1] - mDropView.getScrollY();
            canvas.translate(j, i);
            canvas.scale(mDropViewScale, mDropViewScale);
            mDropView.setAlpha(mDropViewAlpha);
            mDropView.draw(canvas);
            canvas.restore();
        }
    }

    public boolean dispatchKeyEvent(KeyEvent keyevent) {
        boolean flag;
        if (!mDragController.dispatchKeyEvent(keyevent) && !super.dispatchKeyEvent(keyevent)) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }

    public boolean dispatchUnhandledMove(View view, int i) {
        return mDragController.dispatchUnhandledMove(view, i);
    }

    public boolean gatherTransparentRegion(Region region) {
        region.setEmpty();
        return false;
    }

    public ViewParent invalidateChildInParent(int ai[], Rect rect) {
        if (mWallpaper != null && mOffsetChanged) {
            //rect.set(mLeft, mTop, mRight, mBottom);
            mOffsetChanged = false;
        }
        return super.invalidateChildInParent(ai, rect);
    }
    
    public void setDragController(DragController dragcontroller){
        mDragController = dragcontroller;
    }
    
    public void setLauncher(Launcher launcher){
        mLauncher = launcher;
        mLauncher.getWindowManager().getDefaultDisplay().getSize(mScreenSize);
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            FrameLayout.LayoutParams lParams = (FrameLayout.LayoutParams)view.getLayoutParams();
            if (lParams instanceof LayoutParams) {
                if (((LayoutParams)lParams).customPosition){
                    view.layout(((LayoutParams) lParams).x, ((LayoutParams) lParams).y, 
                            ((LayoutParams) lParams).x + ((LayoutParams) lParams).width, 
                            ((LayoutParams) lParams).y + ((LayoutParams) lParams).height);
                }
            }
        }
    }

    /**
     * 功能： DragLayer层处理TP按下操作
     * @param motionevent
     * @param flag
     * @return
     */
    private boolean handleTouchDown(MotionEvent motionevent, boolean flag) {
        Rect rect = new Rect();
        int x = (int)motionevent.getX();
        int y = (int)motionevent.getY();
        Iterator<AppWidgetResizeFrame> iterator = mResizeFrames.iterator();
        while(iterator.hasNext()) {
            AppWidgetResizeFrame appwidgetresizeframe = (AppWidgetResizeFrame)iterator.next();
            appwidgetresizeframe.getHitRect(rect);
            if (rect.contains(x, y) 
                && appwidgetresizeframe.beginResizeIfPointInRegion(
                        x - appwidgetresizeframe.getLeft(), 
                        y - appwidgetresizeframe.getTop())){
                mCurrentResizeFrame = appwidgetresizeframe;
                mXDown = x;
                mYDown = y;
                // 如果触摸在DragLayer处理了，则不传入父级VIEW
                requestDisallowInterceptTouchEvent(flag);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 功能： 是否截断触摸事件
     */
    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (mOldPositions == null || !disallowIntercept){
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean flag = true;
        
        // 处理DOWN事件
        if (ev.getAction() == MotionEvent.ACTION_DOWN && handleTouchDown(ev, flag)) {
            return flag;
        }
            
        // 判断是否为两指触摸
        if (ev.getPointerCount() != 2) {
            if (mOldPositions != null) {
                mOldPositions = null;
            }
        } else {
            if (mOldPositions != null) {
                if (!mLauncher.isSceneAnimating()) {
                    if (mLauncher.isSceneShowing()) {
                        if (!mLauncher.isFolderShowing() 
                            && (ev.getY(0) - mOldPositions[0] < (float)(-mScaledUpsideScreenOutTouch))
                            && (ev.getY(1) - mOldPositions[1] < (float)(-mScaledUpsideScreenOutTouch))) {
                            
                            mOldPositions = null;
                            mLauncher.hideSceneScreen();
                            return true;
                        }
                    } else {
                        if (!mLauncher.isInEditing() 
                            && !mLauncher.isFolderShowing() 
                            && !mLauncher.isPreviewShowing() 
                            && (ev.getY(0) - mOldPositions[0] > (float)mScaledUpsideScreenOutTouch)
                            && (ev.getY(1) - mOldPositions[1] > (float)mScaledUpsideScreenOutTouch)
                            && mLauncher.getUpsideScene() != null 
                            && mLauncher.getWorkspace().isTouchStateNotInScroll()) {
                            
                            mOldPositions = null;
                            mLauncher.getWorkspace().finishCurrentGesture();
                            mLauncher.showSceneScreen();
                            return true;
                        }
                    }
                }
            } else {
                float af[] = new float[2];
                af[0] = ev.getY(0);
                af[1] = ev.getY(1);
                mOldPositions = af;
            }
        }
        clearAllResizeFrames();
        flag = mDragController.onInterceptTouchEvent(ev);
        return flag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean flag1 = true;
        boolean flag = false;
        
        int x = (int)ev.getX();
        int y = (int)ev.getY();
        if (ev.getAction() != MotionEvent.ACTION_DOWN || !handleTouchDown(ev, false)) {
            if (mCurrentResizeFrame != null) {
                flag = true;
                switch (ev.getAction()) {
                case MotionEvent.ACTION_UP: // '\001'
                case MotionEvent.ACTION_CANCEL: // '\003'
                    mCurrentResizeFrame.commitResizeForDelta(x - mXDown, y - mYDown);
                    mCurrentResizeFrame = null;
                    break;

                case MotionEvent.ACTION_MOVE: // '\002'
                    mCurrentResizeFrame.visualizeResizeForDelta(x - mXDown, y - mYDown);
                    break;
                }
            }
            if (!flag){
                flag1 = mDragController.onTouchEvent(ev);
            }
        }
        return flag1;
    }
    
    /**
     *  功能： 桌面背景显示， 从配置表中读取壁纸的显示方式
     *  调用： 使用于Launcher.java
     */
    public void updateWallpaper() {
        String s = PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_key_wallpaper_scroll_type", "byTheme");
        if (s.equals("byTheme")){
            s = getResources().getString(R.string.wallpaper_scrolling);
        }
        mWpScrolling = false;
        if (!s.equals("left")) {
            if (!s.equals("center")) {
                if (!s.equals("right")) {
                    mWpScrolling = true;
                } else {
                    mWpOffsetX = 1F;
                }
            } else {
                mWpOffsetX = 0.5F;
            }
        } else {
            mWpOffsetX = 0F;
        }
        mLauncher.getWindow().setFormat(PixelFormat.TRANSPARENT);
        mWallpaper = null;
        updateWallpaperOffset();
    }
    
    /**
     *  功能： 壁纸显示起点
     */
    public void updateWallpaperOffset() {
        if (mWallpaper == null) {
            mWallpaperManager.setWallpaperOffsetSteps(mWpStepX, mWpStepY);
            if (getWindowToken() == null) {
                removeCallbacks(OffsetUpdater);
                postDelayed(OffsetUpdater, 50L);
            }else {
                try {
                    WindowManagerGlobal.getWindowSession(mContext.getMainLooper()).
                        setWallpaperPosition(getWindowToken(), mWpOffsetX, mWpOffsetY, mWpStepX, mWpStepY); 
                } catch (RemoteException e) { }
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
