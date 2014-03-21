package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ScreenView.java
 * 创建时间：    2013
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: 
 * 20140317: 屏幕布局onLayout
 * ====================================================================================
 */
import java.security.InvalidParameterException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Scroller;

@SuppressLint("NewApi")
public class ScreenView extends ViewGroup {

    /*
     * 使用parcel存储state, state中包含当前屏幕 ID： currentScreen
     */
    public static class SavedState extends BaseSavedState{
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<ScreenView.SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }
            
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        
        int currentScreen = INVALID_SCREEN;
        
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(currentScreen);
        }
        
        private SavedState(Parcel parcel) {
            super(parcel);
            currentScreen = INVALID_SCREEN;
            currentScreen = parcel.readInt();
        }
        
        public SavedState(Parcelable parcelable) {
            super(parcelable);
            currentScreen = INVALID_SCREEN;
        }
    }
    
    /*
     * 缩放监听
     */
    private class ScaleDetectorListener
        implements OnScaleGestureListener{

        private static final float VALID_PINCH_IN_RATIO = 0.8F;
        private static final float VALID_PINCH_OUT_RATIO = 1.2F;
        private static final float VALID_PINCH_RATIO = 0.95F;
        private static final float VALID_PINCH_TIME = 200F;
        
        private ScaleDetectorListener() {
        }
        
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            boolean bFlag = true;
            float f = detector.getScaleFactor();
            if (mTouchState == 0 && ((float)detector.getTimeDelta() > VALID_PINCH_TIME || f < VALID_PINCH_RATIO || f > 1.052632F)) {
                setTouchState(null, 4);
            }
            if (f >= VALID_PINCH_IN_RATIO) {
                if (f <= VALID_PINCH_OUT_RATIO) {
                    bFlag = false;
                }else {
                    onPinchOut(detector);
                }
            }else {
                onPinchIn(detector);
            }
            return bFlag;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            boolean bFlag = true;
            if (mTouchState != 0) {
                bFlag = false;
            }else {
                bFlag = true;
            }
            return bFlag;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            finishCurrentGesture();
        }
        
    }
    
    /*
     * 触摸滑动监听
     */
    private class SliderTouchListener
        implements OnTouchListener{

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int i = view.getWidth();
            float f = Math.max(0F, Math.min(motionEvent.getX(), i - 1));
            int j = getScreenCount();
            int k = (int)Math.floor((f * (float)j) / (float)i);
            switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                setTouchState(motionEvent, 3);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                snapToScreen(k);
                updateSeekPoints(mCurrentScreen, mNextScreen);
                break;
            case MotionEvent.ACTION_MOVE:
                setCurrentScreenInner(k);
                scrollTo((int)((f * (float)(j * mChildScreenWidth)) / (float)i - (float)(mChildScreenWidth / 2)), 0);
                break;
                
            default:
                break;
            }
            return false;
        }
        private SliderTouchListener() {
        }
    }
    
    /*
     * 滑动条 
     */
    protected class SlideBar extends FrameLayout
        implements Indicator{

        private Rect mPadding;
        private Rect mPos;
        private NinePatch mSlidePoint;
        private Bitmap mSlidePointBmp;
        
        @Override
        public void fastOffset(int offset) {
            mRight = (offset + mRight) - mLeft;
            mLeft = offset;
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (mSlidePoint != null) {
                mSlidePoint.draw(canvas, mPos);
            }
        }
        
        @Override
        protected boolean setFrame(int left, int right, int top, int bottom) {
            boolean flag = super.setFrame(left, right, top, bottom);
            if (mSlidePoint != null) {
                mPos.bottom = bottom - top - mPadding.bottom;
                mPos.top = mPos.bottom - mSlidePoint.getHeight();
            }
            return flag;
        }

        public int getSlideWidth() {
            return getMeasuredWidth() - mPadding.left - mPadding.right;
        }
        
        public void setPosition(int left, int right) {
            mPos.left = left + mPadding.left;
            mPos.right = right + mPadding.left;
        }
        
        public SlideBar(Context context) {
            super(context);
            mPos = new Rect();
            mPadding = new Rect();
            
            // 从资源中获得滑动条图片
            mSlidePointBmp = BitmapFactory.decodeResource(context.getResources(), 
                    R.drawable.screen_view_slide_bar);
            if (mSlidePointBmp != null) {
                byte abyte[] = mSlidePointBmp.getNinePatchChunk();
                if (abyte != null) {
                    mSlidePoint = new NinePatch(mSlidePointBmp, abyte, null);
                    FrameLayout frameLayout = new FrameLayout(mContext);
                    frameLayout.setBackgroundResource(R.drawable.screen_view_slide_bar_bg);
                    addView(frameLayout, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 
                            FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
                    mPadding.left = frameLayout.getPaddingLeft();
                    mPadding.top = frameLayout.getPaddingTop();
                    mPadding.right = frameLayout.getPaddingRight();
                    mPadding.bottom = frameLayout.getPaddingBottom();
                    mPos.top = mPadding.top;
                    mPos.bottom = mPos.top + mSlidePointBmp.getHeight();
                }
            }
        }
    }
    
    /*
     * 页面指示器 .....
     */
    protected class SeekBarIndicator extends LinearLayout
        implements Indicator{
    
        public SeekBarIndicator(Context context) {
            super(context);
            setDrawingCacheEnabled(true);
        }
    
        public void fastOffset(int offset) {
            mRight = offset + mRight - mLeft;
            mLeft = offset;
        }
    }
    
    /*
     * 方向标
     */
    protected class ArrowIndicator extends ImageView
        implements Indicator{
        public ArrowIndicator(Context context) {
            super(context);
        }

        @Override
        public void fastOffset(int offset) {
            mRight = (offset + mRight) - mLeft;
            mLeft = offset;
        }
    }
    
    private static abstract interface Indicator{
        public abstract void fastOffset(int offset);
    }
    
    /*
     * 动画变化率
     */
    private class ScreenViewOverShootInterpolator
        implements Interpolator{
        private float mTension; // 拉力
        
        public ScreenViewOverShootInterpolator() {
            mTension = mOvershootTension;
        }
        
        public void disableSettle() {
            mTension = 0F;
        }
        
        public void setDistance(int i, int j) {
            float f;
            if (i <= 0) {
                f = mOvershootTension;
            }else {
                f = mOvershootTension / (float)i;
            }
            mTension = f;
        }
        
        /*
         * 返回动画变化率
         * @see android.animation.TimeInterpolator#getInterpolation(float)
         */
        @Override
        public float getInterpolation(float f) {
            float f1 = f - 1F;
            return 1F + f1 * f1 * (f1 * (1F + mTension) + mTension);
        }
        
    }
    
    /*
     * 手势速率监测
     */
    private class GestureVelocityTracker{
        public static final int FLING_LEFT = 1;
        public static final int FLING_RIGHT = 2;
        public static final int FLING_CANCEL = 3;
        public static final int FLING_ALIGN = 4;
        private static final float MIN_FOLD_DIST = 3.0F;
        private int mPointerId = INVALID_POINTER;
        private float mFoldX = -1.0F;
        private float mPreX = -1.0F;
        private float mStartX = -1.0F;
        private VelocityTracker mVelocityTracker;
        
        public GestureVelocityTracker() {
        
        }
        
        private void reset(){
            mPointerId = INVALID_POINTER;
            mStartX = -1.0F;
            mStartX = -1.0F;
            mPreX = -1.0F;
        }
        
        public void addMovement(MotionEvent motionEvent) {
            float motionPointer = motionEvent.getX();
            
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(motionEvent);
            }
            if (mPointerId != INVALID_POINTER) {
                int i = motionEvent.findPointerIndex(mPointerId);
                if (i == -1) {
                    mPointerId = INVALID_POINTER;
                    return;
                }
                motionPointer = motionEvent.getX(i);
            }
            if (mStartX < 0.0F) {
                mStartX = motionPointer;
                return;
            }
            if (mPreX < 0.0F) {
                mPreX = motionPointer;
                return;
            }
            if (mFoldX < 0.0F) {
                if (((mPreX <= mStartX) || (motionPointer >= mPreX))
                        && ((mPreX >= mStartX) || (motionPointer <= mPreX)|| (Math.abs(motionPointer - mStartX) <= MIN_FOLD_DIST))) {
                    mFoldX = mPreX;
                    mStartX = mFoldX;
                    if ((mFoldX == mPreX) 
                            || (((mPreX <= mFoldX) || (motionPointer >= mPreX)) 
                                    && ((mPreX > mFoldX) || (motionPointer <= mPreX) || (Math.abs(motionPointer - mFoldX) <= MIN_FOLD_DIST)))) {
                        mPreX = motionPointer;
                    }
                    
                }
            }
        }
        
        public int getFlingDirection(float paramFloat) {
            if (paramFloat > 300.0F) {
                if (mFoldX < 0.0F) {
                    if (mPreX <= mStartX) {
                        if (mPreX < mFoldX) {
                            if (mScrollX < getCurrentScreen().getLeft()) {
                                return FLING_CANCEL;
                            }else {
                                return FLING_RIGHT;
                            }
                        }
                        return FLING_LEFT;
                    }
                    return FLING_RIGHT;
                }
            }
            return FLING_ALIGN;
        }
        
        public float getXVelocity(int paramInt1, int paramInt2, int paramInt3) {
            mVelocityTracker.computeCurrentVelocity(paramInt1, paramInt2);
            return mVelocityTracker.getXVelocity(paramInt3);
        }
        
        public void init(int pointer_id) {
            // 创建一个速率监测器
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
                reset();
                mPointerId = pointer_id;
                mVelocityTracker.clear();
            }
        }
        
        public void recycle() {
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
            reset();
        }
    }

    /*
     * 常量定义
     */
    private static final String TAG = "ScreenView";

    private static final Boolean BOOL_ALWAYS = true;
    private static final Boolean BOOL_CLIP_TO_PADDINGS = true;
    private static final float BASELINE_FLING_VELOCITY = 2500F;
    private static final float DEFAULT_OVER_SHOOT_TENSION = 1.3F;
    private static final int DEFAULT_SCREEN_SNAP_DURATION = 300;
    private static final float FLING_VELOCITY_INFLUENCE = 0.4F;
    protected static final int INDICATOR_MEASURE_SPEC = MeasureSpec.makeMeasureSpec(0, 0);
    private static final int INVALID_POINTER = -1;
    protected static final int INVALID_SCREEN = -1; 
    protected static final int MINIMAL_SLIDE_BAR_POINT_WIDTH = 48;
    private static final float NANOTIME_DIV = 1000000000.0F+009F;
    /*
     * 触摸的四个状态
     */
    public static final int MAX_TOUCH_STATE = 4;
    protected static final int TOUCH_STATE_REST = 0;
    protected static final int TOUCH_STATE_SCROLLING = 1;
    protected static final int TOUCH_STATE_SLIDING = 2;
    protected static final int TOUCH_STATE_PINCHING = 3;
    /*
     * 屏幕对其四种方式
     */
    public static final int SCREEN_ALIGN_CUSTOMIZED = 0;
    public static final int SCREEN_ALIGN_LEFT = 1;
    public static final int SCREEN_ALIGN_CENTER = 2;
    public static final int SCREEN_ALIGN_RIGHT = 3;
    /*
     * 屏幕切换的10种特效
     */
    public static final int SCREEN_TRANSITION_TYPE_CLASSIC = 0;
    public static final int SCREEN_TRANSITION_TYPE_CLASSIC_NO_OVER_SHOOT = 1;
    public static final int SCREEN_TRANSITION_TYPE_CROSSFADE = 2;
    public static final int SCREEN_TRANSITION_TYPE_FALLDOWN = 3;
    public static final int SCREEN_TRANSITION_TYPE_CUBE = 4;
    public static final int SCREEN_TRANSITION_TYPE_LEFTPAGE = 5;
    public static final int SCREEN_TRANSITION_TYPE_RIGHTPAGE = 6;
    public static final int SCREEN_TRANSITION_TYPE_STACK = 7;
    public static final int SCREEN_TRANSITION_TYPE_ROTATE = 8;
    public static final int SCREEN_TRANSITION_TYPE_CUSTOM = 9;
    
    /*********** 内容  *********/
    // 指示器
    protected static final LinearLayout.LayoutParams SEEK_POINT_LAYOUT_PARAMS = 
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1F);
    protected SeekBarIndicator mScreenSeekBar;
    private int mSeekPointResId = R.drawable.screen_view_seek_point_selector;
    private int mIndicatorCount = 0;
    
    private static final float SMOOTHING_CONSTANT = (float)(0.016D / Math.log(0.75D));
    private static final float SMOOTHING_SPEED = 0.75F;
    private static final int SNAP_VELOCITY = 300;
    
    private boolean isFromcomputeScroll = false;
    private ArrowIndicator mArrowLeft = null;
    private ArrowIndicator mArrowRight = null;
    private int mArrowLeftOffResId = R.drawable.screen_view_arrow_left_gray; // 方向标图片资源
    private int mArrowLeftOnResId = R.drawable.screen_view_arrow_left;
    private int mArrowRightOffResId = R.drawable.screen_view_arrow_right_gray;
    private int mArrowRightOnResId = R.drawable.screen_view_arrow_right;
    private float mConfirmHorizontalScrollRatio = 0.5F;
    private boolean mCurrentGestureFinished = false;
    protected int mCurrentScreen = INVALID_SCREEN;
    protected int mNextScreen = INVALID_SCREEN;
    
    private Context mContext = null;
    private int mScreenCounter = 0;
    
    private int mTouchState = TOUCH_STATE_REST;
    protected Scroller mScroller = null;
    private int mRight = 0;
    private int mLeft = 0;
    private float mOvershootTension = DEFAULT_OVER_SHOOT_TENSION;
    private ScreenViewOverShootInterpolator mScrollInterpolator = null;
    private int mTouchSlop = 0; // 触摸触发事件的最短距离
    private ScaleGestureDetector mScaleGestureDetector = null; // 缩放手势检测
    GestureVelocityTracker mGestureVelocityTracker = null;
    private int mMaximumVelocity = 0;
    protected int mScrollX = 0;
    protected int mScrollY = 0;
    private float mTouchX = 0.0F;
    private int mScreenTransitionType = SCREEN_TRANSITION_TYPE_CLASSIC_NO_OVER_SHOOT;
    protected int mVisibleRange = 1;
    private Camera mCamera = null;
    protected int mChildScreenWidth = 0;
    protected SlideBar mSlideBar = null;
    private float mSmoothingTime = 0.0F;
    protected int mActivePointerId = INVALID_POINTER;
    private boolean mAllowLongPress = true;
    protected boolean mFirstLayout = true;
    protected int mWidthMeasureSpec = 0;
    protected int mHeightMeasureSpec = 0;
    protected OnLongClickListener mLongClickListener = null;
    protected int mScreenAlignment = 0;
    protected int mScreenOffset = 0;
    protected int mScreenPaddingBottom = 0;
    protected int mScreenPaddingTop = 0;
    private int mScreenSnapDuration = DEFAULT_SCREEN_SNAP_DURATION;
    protected int mScreenWidth = 0;
    protected int mScrollLeftBound = 0;
    protected int mScrollOffset = 0;
    protected int mScrollRightBound = 0;
    protected float mOverScrollRatio = 0.3333333F;
    protected boolean mScrollWholeScreen = false;
    private boolean mTouchIntercepted = false;
    public float mLastMotionX = 0.0F;
    public float mLastMotionY = 0.0F;
    private int mPaddingLeft = 0;
    private int mPaddingRight = 0;
    private int mPaddingTop = 0;
    private int mPaddingBottom = 0;

    
    public ScreenView(Context context) {
        super(context);
        mContext = context;
        mGestureVelocityTracker = new GestureVelocityTracker();
        mCamera = new Camera();
        initScreenView();
    }

    public ScreenView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public ScreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mGestureVelocityTracker = new GestureVelocityTracker();
        mCamera = new Camera();
        initScreenView();
    }

    /**
     * 功能： 布局显示
     * @see android.view.ViewGroup#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int left, int right, int top, int bottom) {
        this.setFrame(left, right, top, bottom);
        
        updateIndicatorPositions(mScrollX);
        int width = 0;
        
        for (int screenId = 0; screenId < getScreenCount(); screenId++) {
            View view = getChildAt(screenId);
            if (view.getVisibility() != View.GONE) {
                view.layout(width, mPaddingTop + mScreenPaddingTop, 
                        width + view.getMeasuredWidth(), 
                        mPaddingTop + mScreenPaddingTop + view.getMeasuredHeight());
                width += view.getMeasuredWidth();
            }
        }
        if (mScrollWholeScreen && ((mCurrentScreen % mVisibleRange) > 0)) {
            setCurrentScreen(mCurrentScreen - (mCurrentScreen % mVisibleRange));
        }
    }
    
    /**
     * 获得个屏幕上每个字控件的所占用的空间大小
     */
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidthMeasureSpec = widthMeasureSpec;
        mHeightMeasureSpec = heightMeasureSpec;
        int iHeight = 0;
        int iWidth = 0;
        int screeenCount = getScreenCount();
        
        for (int i = 0; i < mIndicatorCount; i++) {
            View view = getChildAt(i + getScreenCount());
            ViewGroup.LayoutParams lParams = view.getLayoutParams();
            view.measure(
                    getChildMeasureSpec(mWidthMeasureSpec, 
                        mPaddingLeft + mPaddingRight, lParams.width), 
                    getChildMeasureSpec(mHeightMeasureSpec, 
                        mPaddingTop + mScreenPaddingTop + mPaddingBottom + mScreenPaddingBottom, 
                        lParams.height));
            iWidth = Math.max(iWidth, view.getMeasuredWidth());
            iHeight = Math.max(iHeight, view.getMeasuredHeight());
        }
        
        int mWidth = 0;
        int mHeight = 0;
        
        for (int i = 0; i < screeenCount; i++) {
            View view = getChildAt(i);
            ViewGroup.LayoutParams lParams = view.getLayoutParams();
            view.measure(
                    getChildMeasureSpec(mWidthMeasureSpec, 
                            mPaddingLeft + mPaddingRight, lParams.width), 
                    getChildMeasureSpec(mHeightMeasureSpec, 
                            mPaddingTop + mScreenPaddingTop + mPaddingBottom + mScreenPaddingBottom, 
                            lParams.height));
            mWidth = Math.max(mWidth, view.getMeasuredWidth());
            mHeight = Math.max(mHeight, view.getMeasuredHeight());
        }
        
        iWidth = Math.max(mWidth, iWidth);
        iHeight = Math.max(mHeight, iHeight);
        
        iWidth += mPaddingLeft + mPaddingRight;
        iHeight += mPaddingTop + mScreenPaddingTop + mPaddingBottom + mScreenPaddingBottom;
        setMeasuredDimension(resolveSize(iWidth, mWidthMeasureSpec), 
                resolveSize(iHeight, mHeightMeasureSpec));
        if (screeenCount > 0) {
            mChildScreenWidth = mWidth;
            mScreenWidth = View.MeasureSpec.getSize(mWidthMeasureSpec) - mPaddingLeft - mPaddingRight;
            updateScreenOffset();
            setOverScrollRatio(mOverScrollRatio);
            if (mChildScreenWidth > 0){
                mVisibleRange = Math.max(1, (mScreenWidth + mChildScreenWidth / 2) / mChildScreenWidth);
            }
        }
        if (mFirstLayout && mVisibleRange > 0) {
            mFirstLayout = false;
            setHorizontalScrollBarEnabled(false);
            setCurrentScreen(mCurrentScreen);
            setHorizontalScrollBarEnabled(true);
        }
    }
    
    public void onPause() {
        if (!mScroller.isFinished()) {
            setCurrentScreen((int)Math.floor((mScroller.getCurrX() + mChildScreenWidth / 2) / mChildScreenWidth));
            mScroller.abortAnimation();
        }
    }
    
    public void onResume() {
        
    }

    /*
     * 界面返回时获得状态
     * @see android.view.View#onRestoreInstanceState(android.os.Parcelable)
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.currentScreen != INVALID_SCREEN) {
            setCurrentScreen(savedState.currentScreen);
        }
    }

    /*
     * 界面退出时保存状态
     * @see android.view.View#onSaveInstanceState()
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.currentScreen = mCurrentScreen;
        return savedState;
    }

    /*
     * 第二个手指按下
     */
    public void onSecondaryPointerDown(MotionEvent motionEvent, int point_id) {
        mLastMotionX = motionEvent.getX(motionEvent.findPointerIndex(point_id));
        mTouchX = mScrollX;
        mSmoothingTime = (float)System.nanoTime() / NANOTIME_DIV;
        mGestureVelocityTracker.init(point_id);
        mGestureVelocityTracker.addMovement(motionEvent);
        mTouchState = TOUCH_STATE_SCROLLING;
    }
    
    /*
     * 第二个手指移动
     */
    public void onSecondaryPointerMove(MotionEvent motionEvent, int point_id) {
        float f = motionEvent.getX(motionEvent.findPointerIndex(point_id));
        float f1 = mLastMotionX - f;
        mLastMotionX = f;
        if (f1 == 0F) {
            awakenScrollBars();
        }else {
            scrollTo((int)(f1 + mTouchX), 0);
        }
        mGestureVelocityTracker.addMovement(motionEvent);
    }
    
    /*
     * 第二个手指抬起
     */
    public void onSecondaryPointerUp(MotionEvent motionEvent, int point_id) {
        snapByVelocity(point_id);
        mGestureVelocityTracker.recycle();
        mTouchState = TOUCH_STATE_REST;
    }
    
    /*
     * 触摸事件处理
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float f = 0;
        if (!mCurrentGestureFinished) {
            if (mTouchIntercepted) {
                onTouchEventUnique(motionEvent);
            }
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                {
                    // 滑动到ACTIVE POINTER对应的页面，并设置TOUCH STATE为 RESET
                    if (mTouchState == TOUCH_STATE_SCROLLING) {
                        snapByVelocity(mActivePointerId);
                    }
                    setTouchState(motionEvent, TOUCH_STATE_REST);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                {
                    // 如果状态为RESET并移动了足够的距离则状态设置为SCROLLING
                    if (mTouchState == TOUCH_STATE_REST && scrolledFarEnough(motionEvent)) {
                        setTouchState(motionEvent, TOUCH_STATE_SCROLLING);
                    }
                    if (mTouchState != TOUCH_STATE_SCROLLING) {
                        break;
                    }
                    int point_id = motionEvent.findPointerIndex(mActivePointerId);
                    f = motionEvent.getX(point_id);
                    float f1 = mLastMotionX - f;
                    mLastMotionX = f;
                    if (f1 == 0) {
                        awakenScrollBars();
                    }else {
                        scrollTo(Math.round(f1 + mTouchX), 0);
                    }
                }   
                break;
            case MotionEvent.ACTION_POINTER_UP:
                {
                    // 此处为多点触摸处理抬起事件
                    int point_id = 0;
                    int i = (MotionEvent.ACTION_POINTER_INDEX_MASK & motionEvent.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                    if (motionEvent.getPointerId(i) != mActivePointerId) {
                        break;
                    }
                    if (i == 0) {
                        point_id = 1;
                    }
                    mLastMotionX = motionEvent.getX(point_id);
                    mActivePointerId = motionEvent.getPointerId(point_id);
                    mGestureVelocityTracker.init(mActivePointerId);
                }
                break;
            case MotionEvent.ACTION_DOWN:
            default:
                break;
            }
            mTouchIntercepted = true;
        }
        return true;
    }

    /**
     * 功能： 移除所有屏幕
     */
    public void removeAllScreens() {
        removeScreensInLayout(0, getScreenCount());
        requestLayout();
        invalidate();
    }
    
    /**
     * 删除所有的屏幕
     */
    @Override
    public void removeAllViewsInLayout() {
        mIndicatorCount = 0;
        mScreenCounter = 0;
        if (mScreenSeekBar != null) {
            mScreenSeekBar.removeAllViewsInLayout();
        }
        super.removeAllViewsInLayout();
    }
    
    /**
     * 描述： 从现有的布局中删除掉一些View
     * @param start
     * @param count
     */
    public void removeScreensInLayout(int start, int count) {
        if (start >= 0 && start < getScreenCount()) {
            
            // 
            int k = Math.min(count, getScreenCount() - start);
            if (mScreenSeekBar != null) {
                mScreenSeekBar.removeViewsInLayout(start, k);
            }
            
            // 最后剩余的屏幕数
            mScreenCounter = getScreenCount() - k;
            super.removeViewsInLayout(start, k);
        }
    }
    
    /**
     * 描述： 删除指定的屏幕
     * @param screen_id
     */
    public void removeScreen(int screen_id) {
        if (screen_id < getScreenCount()) {
            // 如果删除的屏幕为当前的显示的屏幕
            if (screen_id == mCurrentScreen) {
                if (mScrollWholeScreen) {
                    if ((screen_id != 0) && (screen_id == getScreenCount() - 1)) {
                        snapToScreen(screen_id - 1);
                    }
                } else {
                    setCurrentScreen(Math.max(0, screen_id - 1));
                }
            }
            
            // 删除指示器
            if (mScreenSeekBar != null) {
                mScreenSeekBar.removeViewAt(screen_id);
            }
            mScreenCounter = mScreenCounter - 1;
            removeViewAt(screen_id);
            return;
        }else {
            throw new InvalidParameterException("The view specified by the index must be a screen.");
        }
    }
    
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean b_flag) {
        boolean bFlag = false;
        if (indexOfChild(view) >= getScreenCount()) {
            bFlag = false;
        }else {
            if ((indexOfChild(view) == mCurrentScreen) && mScroller.isFinished()) {
                bFlag = false;
            }else {
                snapToScreen(indexOfChild(view));
                bFlag = true;
            }
        }
        return bFlag;
    }
    
    /**
     * 功能： 添加指示器
     * @param view
     * @param layoutParams
     */
    public void addIndicator(View view, FrameLayout.LayoutParams layoutParams){
        mIndicatorCount = mIndicatorCount + 1;
        super.addView(view, -1, layoutParams);
    }
    
    /**
     * 功能： 指定位置添加指示器
     * @param view
     * @param layoutParams
     */
    public void addIndicatorAt(View view, FrameLayout.LayoutParams layoutParams, int pos) {
        int j = Math.max(-1, Math.min(pos, mIndicatorCount));
        if (j >= 0) {
            j += getScreenCount();
        }
        mIndicatorCount = mIndicatorCount + 1;
        super.addView(view, j, layoutParams);
    }
    
    /**
     * 描述： 删除指示器
     * @param view
     */
    public void removeIndicator(View view){
        int i = indexOfChild(view);
        if (i >= getScreenCount()) {
            mIndicatorCount = mIndicatorCount - 1;
            super.removeViewAt(i);
            return;
        }else {
            throw new InvalidParameterException("The view passed through the parameter must be indicator.");
        }
    }
    
    
    /**
     * 功能： 给指示器分配布局
     * @param layoutParams
     */
    public void setSeekBarPosition(FrameLayout.LayoutParams layoutParams){
        if (layoutParams == null) {
            // 如果分配的布局为空， 则删除指示器
            if (mScreenSeekBar != null) {
                removeIndicator(mScreenSeekBar);
                mScreenSeekBar = null;
            }
        } else { 
            if (mScreenSeekBar != null) {
                mScreenSeekBar.setLayoutParams(layoutParams);
            }else {
                mScreenSeekBar = new SeekBarIndicator(mContext);
                mScreenSeekBar.setGravity(Gravity.CENTER_VERTICAL);
                mScreenSeekBar.setAnimationCacheEnabled(false);
                addIndicator(mScreenSeekBar, layoutParams);
            }
        }
    }
    
    /**
     * 描述： 更新指示器选择
     * @param cur_screen
     * @param next_screen
     */
    public void updateSeekPoints(int cur_screen, int next_screen) {
        if (mScreenSeekBar == null) return;
        int screenCount = getScreenCount();
        
        for (int i = 0; ((i < mVisibleRange) && ((cur_screen + i) < screenCount)); i++) {
            mScreenSeekBar.getChildAt(cur_screen + i).setSelected(false);
        }
        
        for (int m = 0; ((m < mVisibleRange) && ((next_screen + m) < screenCount)); m++) {
            mScreenSeekBar.getChildAt(next_screen + m).setSelected(true);
        }
    }
    /**
     * 功能： 创建指示器图
     * @return
     */
    private ImageView createSeekPoint() {
        ImageView imageView = new ImageView(mContext);
        imageView.setScaleType(ScaleType.CENTER);
        imageView.setImageResource(mSeekPointResId);
        return imageView;
    }
    
    /**
     * 描述： 显示指示器
     * @param b_visibility
     */
    public void setSeekBarVisibility(int b_visibility) {
        if (mScreenSeekBar != null) {
            mScreenSeekBar.setVisibility(b_visibility);
        }
    }
    
    /**
     * 描述： 指示器使用资源
     * @param res_id
     */
    public void setSeekPointResource(int res_id) {
        mSeekPointResId = res_id;
    }
    
    
    /**
     * 功能： 初始化
     */
    private void initScreenView(){
        // 使用DRAWING CACHE来显示VIEW上所有控件，在拖动时也能显示
        setAlwaysDrawnWithCacheEnabled(BOOL_ALWAYS);
        setClipToPadding(BOOL_CLIP_TO_PADDINGS);
        
        // 滑动条
        mScrollInterpolator = new ScreenViewOverShootInterpolator();
        mScroller = new Scroller(mContext, mScrollInterpolator);
        
        // 默认screen 0
        setCurrentScreenInner(0);
        
        // 获取触摸触发事件的最短距离
        ViewConfiguration viewConfiguration = ViewConfiguration.get(mContext);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        
        // 获得允许执行一个fling手势动作的最大速度值
        setMaximumSnapVelocity(viewConfiguration.getScaledMaximumFlingVelocity());
        
        mScaleGestureDetector = new ScaleGestureDetector(mContext, new ScaleDetectorListener());
    }

    private void onTouchEventUnique(MotionEvent motionEvent){
        mGestureVelocityTracker.addMovement(motionEvent);
        if (mTouchState == TOUCH_STATE_REST || mTouchState == TOUCH_STATE_PINCHING) {
            mScaleGestureDetector.onTouchEvent(motionEvent);
        }
    }
    
    private void refreshScrollBound(){
        mScrollLeftBound = (int)((float)(-mChildScreenWidth) * mOverScrollRatio) - mScrollOffset;
        if (mScrollWholeScreen) {
            mScrollRightBound = (int)((float)(((getScreenCount() - 1) / mVisibleRange) * mScreenWidth) 
                    + (float)mChildScreenWidth * mOverScrollRatio);
        }else {
            mScrollRightBound = (int)((float)mChildScreenWidth * ((float)getScreenCount() + mOverScrollRatio) 
                    - (float)mScreenWidth) + mScrollOffset;
        }
    }
    
    /*
     * 手势判断，移动足够距离
     * 根据当前X, Y坐标与最后坐标X,Y的距离来判断
     */
    private boolean scrolledFarEnough(MotionEvent motionEvent){
        boolean bFlag = false;
        float distance = Math.abs(motionEvent.getX(0) - mLastMotionX);
        if ((distance > Math.abs(motionEvent.getY(0) - mLastMotionY) * mConfirmHorizontalScrollRatio) 
                && (distance > (float)(mTouchSlop * motionEvent.getPointerCount()))){
            bFlag = true;
        }
        return bFlag;
    }
    
    /*
     * 根据传递的速率滑动页面
     */
    private void snapByVelocity(int i) {
        if (mChildScreenWidth > 0 && getCurrentScreen() != null) {
            int j = (int)mGestureVelocityTracker.getXVelocity(1000, mMaximumVelocity, i);
            int k = mGestureVelocityTracker.getFlingDirection(Math.abs(j));
            if (k != 1 || mCurrentScreen <= 0) {
                if (k != 2 || mCurrentScreen >= getScreenCount() - 1) {
                    if (k != 3) {
                        int visibleRang;
                        j = mChildScreenWidth;
                        if (!mScrollWholeScreen) {
                            visibleRang = 1;
                        }else {
                            visibleRang = mVisibleRange;
                        }
                        j *= visibleRang;
                        snapToScreen((mScrollX + (j >> 1)) / mChildScreenWidth, 0, true);
                    }else {
                        snapToScreen(mCurrentScreen + mVisibleRange, j, true);
                    }
                }else {
                    snapToScreen(mCurrentScreen + mVisibleRange, j, true);
                }
            }else {
                snapToScreen(mCurrentScreen - mVisibleRange, j, true);
            }
        }
    }
    
    private void updateArrowIndicatorResource(int scrollX) {
        if (mArrowLeft != null) {
            ArrowIndicator arrowIndicator = mArrowLeft;
            int arrowImageId;
            if (scrollX > 0) {
                arrowImageId = mArrowLeftOnResId;
            }else {
                arrowImageId = mArrowLeftOffResId;
            }
            arrowIndicator.setImageResource(arrowImageId);
            arrowIndicator = mArrowRight;
            if (scrollX < getScreenCount() * mChildScreenWidth - mScreenWidth - mScrollOffset) {
                arrowImageId = mArrowRightOnResId;
            }else {
                arrowImageId = mArrowRightOffResId;
            }
            arrowIndicator.setImageResource(arrowImageId);
        }
    }
    
    private void updateIndicatorPositions(int pos){
        if (getWidth() <= 0) return;
        
        int width = getWidth();
        int height = getHeight();
        
        for (int i = 0; i < mIndicatorCount; i++) {
            View view = getChildAt(i + getScreenCount());
            if (view != null && (view instanceof SeekBarIndicator)) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)view.getLayoutParams();
                int measureWidth = view.getMeasuredHeight();
                int measureHeight = view.getMeasuredHeight();
                int gravity = layoutParams.gravity;
                int childLeft = 0;
                int childTop = 0;
                if (gravity != -1) {
                    switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.LEFT:
                        childLeft = layoutParams.leftMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = width - measureHeight - layoutParams.rightMargin;
                        break;
                        
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = ((width - measureWidth) / 2) + layoutParams.leftMargin - layoutParams.rightMargin;
                        break;

                    case Gravity.AXIS_PULL_BEFORE:
                    case Gravity.AXIS_PULL_AFTER:
                    default:
                        childLeft = layoutParams.leftMargin;
                        break;
                    }
                    switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.CENTER:
                        childTop = ((height - measureHeight) / 2) + layoutParams.topMargin - layoutParams.bottomMargin;
                        break;
                    case Gravity.TOP:
                        childTop = layoutParams.topMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = height - measureHeight - layoutParams.bottomMargin;
                        break;
                    default:
                        childTop = layoutParams.topMargin;
                        break;
                    }
                }
                if ((view.isLayoutRequested() || view.getHeight() <= 0 || view.getWidth() <= 0) 
                        && !isFromcomputeScroll) {
                    view.layout(pos + childLeft, childTop, pos + childLeft + measureWidth, childTop + measureHeight);
                }else {
                    ((Indicator)view).fastOffset(pos + childLeft);
                }
            }
        }
    }
    
    private void updateScreenOffset(){
        switch (mScreenAlignment) {
        case SCREEN_ALIGN_CUSTOMIZED:
            mScrollOffset = mScreenOffset;
            break;
        case SCREEN_ALIGN_LEFT:
            mScrollOffset = 0;
            break;
        case SCREEN_ALIGN_CENTER:
            mScrollOffset = (mScreenWidth - mChildScreenWidth) / 2;
            break;
        case SCREEN_ALIGN_RIGHT:
            mScrollOffset = mScreenWidth - mChildScreenWidth;
            break;
        }
        mScrollOffset = mScrollOffset + mPaddingLeft;
    }
    
    private void updateSlidePointPosition(int pos) {
        int screenCount = getScreenCount();
        if (mSlideBar != null && screenCount > 0) {
            int slideWidth = mSlideBar.getSlideWidth();
            int range = Math.max((slideWidth / screenCount) * mVisibleRange, MINIMAL_SLIDE_BAR_POINT_WIDTH);
            screenCount *= mChildScreenWidth;
            if (screenCount > slideWidth) {
                slideWidth = (pos * (slideWidth - range)) / (screenCount - slideWidth);
            }else {
                slideWidth = 0;
            }
            mSlideBar.setPosition(slideWidth, slideWidth + range);
            if (isHardwareAccelerated()) {
                mSlideBar.invalidate();
            }
        }
    }
    
    public void onPinchOut(ScaleGestureDetector detector) {
        // TODO Auto-generated method stub
        
    }

    public void onPinchIn(ScaleGestureDetector detector) {
        // TODO Auto-generated method stub
        
    }

    protected void finishCurrentGesture() {
        mCurrentGestureFinished = true;
        setTouchState(null, TOUCH_STATE_REST);
    }

    /*
     * 各页面间切换特效
     * @see android.view.ViewGroup#getChildStaticTransformation(android.view.View, android.view.animation.Transformation)
     */
    
    @Override
    protected boolean getChildStaticTransformation(View child, Transformation trans) {
        boolean bFlag;
        if (mScreenTransitionType == SCREEN_TRANSITION_TYPE_CLASSIC_NO_OVER_SHOOT
                || mScreenTransitionType == SCREEN_TRANSITION_TYPE_CLASSIC 
                || indexOfChild(child) >= getScreenCount()) {
            bFlag = false;
        }else {
            float ratio = 0.0F; // f4
            float childWidth = child.getMeasuredWidth(); // f
            float childHeight = child.getMeasuredHeight(); // flag
            float screenWidth = (float)getMeasuredWidth() / 2F; // f1
            float childHalfWidth = childWidth / 2F; // f2
            float childHalfHeight = childHeight / 2F; // f3
            
            ratio = ((screenWidth + (float)mScrollX) - (float)child.getLeft() - childHalfWidth) / (screenWidth + childHalfWidth);
            Matrix matrix = trans.getMatrix();
            
            switch (mScreenTransitionType) {
            case SCREEN_TRANSITION_TYPE_CLASSIC:
            case SCREEN_TRANSITION_TYPE_CLASSIC_NO_OVER_SHOOT:
                bFlag = false;
                break;
            case SCREEN_TRANSITION_TYPE_CROSSFADE:
                if (ratio != 0F && Math.abs(ratio) <= 1F) {
                    trans.setAlpha(0.3F + 0.7F * (1F - Math.abs(ratio)));
                    trans.setTransformationType(Transformation.TYPE_ALPHA);
                }else {
                    bFlag = false;
                }
                break;
            case SCREEN_TRANSITION_TYPE_FALLDOWN:
                if (ratio != 0F && Math.abs(ratio) <= 1F) {
                    trans.getMatrix().setRotate(45F * (-ratio), childHalfWidth, childHalfHeight);
                    trans.setTransformationType(Transformation.TYPE_MATRIX);
                }else {
                    bFlag = false;
                }
                break;
            case SCREEN_TRANSITION_TYPE_CUBE:
                if (ratio != 0F && Math.abs(ratio) <= 1F) {
                    trans.setAlpha(1F - Math.abs(ratio));
                    mCamera.save();
                    mCamera.translate(0F, 0F, childHalfWidth);
                    mCamera.rotateY(-90F * ratio);
                    mCamera.translate(0F, 0F, -childHalfWidth);
                    mCamera.getMatrix(matrix);
                    mCamera.restore();
                    matrix.preTranslate(-childHalfWidth, -childHalfHeight);
                    matrix.postTranslate(childHalfWidth * (1F + 2F * ratio), childHalfHeight);
                    trans.setTransformationType(Transformation.TYPE_BOTH);
                }else {
                    bFlag = false;
                }
                break;
            case SCREEN_TRANSITION_TYPE_LEFTPAGE:
                if (ratio != 0F && Math.abs(ratio) <= 1F) {
                    trans.setAlpha(1F - Math.abs(ratio));
                    mCamera.save();
                    mCamera.translate(((-childHalfWidth) * Math.abs(ratio) / 3F), childHalfHeight, ratio * (-childHalfWidth));
                    mCamera.rotateY(30F * (-ratio));
                    mCamera.getMatrix(matrix);
                    mCamera.restore();
                    matrix.postTranslate(childWidth * ratio, childHalfHeight);
                    trans.setTransformationType(Transformation.TYPE_BOTH);
                }else {
                    bFlag = false;
                }
                break;
            case SCREEN_TRANSITION_TYPE_RIGHTPAGE:
                bFlag = false;
                break;
            case SCREEN_TRANSITION_TYPE_STACK:
                if (ratio != 0F && Math.abs(ratio) <= 1F) {
                    trans.setAlpha(1F - ratio);
                    childHalfWidth = 0.6F + FLING_VELOCITY_INFLUENCE * (1F - ratio);
                    matrix.setScale(childHalfWidth, childHalfHeight);
                    matrix.postTranslate(3F * (childWidth * (1F - childHalfWidth)), 0.5F * (childHeight * (1F - childHalfWidth)));
                    trans.setTransformationType(Transformation.TYPE_BOTH);
                }else {
                    bFlag = false;
                }
                break;
            case SCREEN_TRANSITION_TYPE_ROTATE:
                if (ratio != 0F || Math.abs(ratio) <= 1F) {
                    trans.setAlpha(1F - Math.abs(ratio));
                    mCamera.save();
                    mCamera.translate(childWidth * ratio, 0F, 0F);
                    mCamera.rotateY(45F * ratio);
                    mCamera.getMatrix(matrix);
                    mCamera.restore();
                    matrix.preTranslate(-childHalfWidth, -childHalfHeight);
                    matrix.postTranslate(childHalfWidth, childHalfHeight);
                    trans.setTransformationType(Transformation.TYPE_BOTH);
                }else {
                    bFlag = false;
                }
                break;
            }
            bFlag = super.getChildStaticTransformation(child, trans);
        }
        
        return bFlag;
    }
    
    protected boolean getChildStaticTransformationByScreen(View view, Transformation transformation, float f) {
        return false;
    }

    /*
     * 移动屏幕
     */
    private void snapToScreen(int screen, int j, boolean bEffect) {
        if (mScreenWidth > 0) {
            if (!mScrollWholeScreen) {
                mNextScreen = Math.max(0, Math.min(screen, getScreenCount() - mVisibleRange));
            }else {
                mNextScreen = Math.max(0, Math.min(screen, getScreenCount() - 1));
                mNextScreen = mNextScreen - (mNextScreen % mVisibleRange);
            }
            
            int k = Math.max(1, Math.abs(mNextScreen - mCurrentScreen));
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            
            int j1 = Math.abs(j);
            // 如果不需要特效，将动画变化率设置为0.0F，否则则按算法计算
            if (!bEffect) {
                mScrollInterpolator.disableSettle();
            }else {
                mScrollInterpolator.setDistance(k, j1);
            }
            
            int l = mNextScreen * mChildScreenWidth - mScrollOffset - mScrollX;
            if (l != 0) {
                int i1 = (Math.abs(l) * mScreenSnapDuration) / mScreenWidth;
                if (j1 > 0) {
                    i1 += (int)(FLING_VELOCITY_INFLUENCE * ((float)i1 / ((float)j1 / BASELINE_FLING_VELOCITY)));
                }
                i1 = Math.max(mScreenSnapDuration, i1);
                if (k <= 1) {
                    i1 = Math.min(i1, 2 * mScreenSnapDuration);
                }
                mScroller.startScroll(mScrollX, 0, l, 0, i1);
                invalidate();
            }
        }
        
    }
    
    public void snapToScreen(int screen) {
        snapToScreen(screen, 0, false);
    }

    public void scrollTo(int x, int y){
        mTouchX = Math.max(mScrollLeftBound, Math.min(x, mScrollRightBound));
        mSmoothingTime = ((float)System.nanoTime()/NANOTIME_DIV);
    }
    
    public void scrollToScreen(int cur_screen) {
        if (mScrollWholeScreen) {
            cur_screen -= cur_screen % mVisibleRange;
        }
        measure(mWidthMeasureSpec, mHeightMeasureSpec);
        scrollTo(cur_screen * mChildScreenWidth - mScrollOffset, 0);
        
    }
    
    public void setAllowLongPress(boolean bFlag) {
        mAllowLongPress = bFlag;
    }
    
    public void setArrowIndicatorMarginRect(Rect rect) {
        if (rect == null) {
            if (mArrowLeft != null) {
                removeIndicator(mArrowLeft);
                removeIndicator(mArrowRight);
                mArrowLeft = null;
                mArrowRight = null;
            }
        }else {
            FrameLayout.LayoutParams layoutParams;
            FrameLayout.LayoutParams layoutParams2;
            if (mArrowLeft != null) {
                layoutParams = (FrameLayout.LayoutParams)mArrowRight.getLayoutParams();
                layoutParams2 = (FrameLayout.LayoutParams)mArrowLeft.getLayoutParams();
            }else {
                layoutParams2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER | Gravity.CLIP_HORIZONTAL);
                mArrowLeft = new ArrowIndicator(mContext);
                mArrowLeft.setImageResource(mArrowLeftOnResId);
                addIndicator(mArrowLeft, layoutParams2);
                layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.CLIP_HORIZONTAL | Gravity.CENTER_HORIZONTAL);
                mArrowRight = new ArrowIndicator(mContext);
                mArrowRight.setImageResource(mArrowRightOnResId);
                addIndicator(mArrowRight, layoutParams);
            }
            layoutParams2.setMargins(rect.left, rect.top, 0, rect.bottom);
            layoutParams.setMargins(0, rect.top, rect.right, rect.bottom);
        }
    }
    
    public void setArrowIndicatorResource(int left_on_resId, int left_off_resId, int right_on_resId, int right_off_resId) {
        mArrowLeftOnResId = left_on_resId;
        mArrowLeftOffResId = left_off_resId;
        mArrowRightOnResId = right_on_resId;
        mArrowRightOffResId = right_off_resId;
    }
    
    public void setConfirmHorizontalScrollRatio(float ratio) {
        mConfirmHorizontalScrollRatio = ratio;
    }
    
    /*
     * 设置当前页面
     */
    public void setCurrentScreen(int cur_screen) {
        int j;
        if (!mScrollWholeScreen) {
            j = Math.max(0, Math.min(cur_screen, getScreenCount() - mVisibleRange));
        }else {
            j = Math.max(0, Math.min(cur_screen, getScreenCount() - 1));
            j -= j % mVisibleRange;
        }
        setCurrentScreenInner(j);
        if (!mFirstLayout) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollToScreen(mCurrentScreen);
            invalidate();
        }
    }
    
    protected void setCurrentScreenInner(int cur_screen){
        updateSeekPoints(mCurrentScreen, cur_screen);
        mCurrentScreen = cur_screen;
        mNextScreen = INVALID_SCREEN;
    }
    
    public void setIndicatorBarvisibility(int visibility_bar) {
        setSeekBarVisibility(visibility_bar);
        setSlideBarVisibility(visibility_bar);
    }
    
    public View getCurrentScreen() {
        return getScreen(mCurrentScreen);
    }
    
    public int getCurrentScreenIndex() {
        if (mNextScreen != INVALID_SCREEN) {
            mCurrentScreen = mNextScreen;
        }
        return mCurrentScreen;
    }
    
    public View getScreen(int cur_screen) {
        View view;
        
        if (cur_screen >= 0 && cur_screen < getScreenCount()) {
            view = getChildAt(cur_screen);
        }else {
            view = null;
        }
        return view;
    }
    
    public void setIndicatorBarVisibility(int i) {
        setSeekBarVisibility(i);
        setSlideBarVisibility(i);
    }

    /*
     * 返回当前的显示的页号
     */
    int getCurrentPage() {
        return mCurrentScreen;
    }
    
    /*
     * 返回屏幕总数
     */
    public final int getScreenCount() {
        return mScreenCounter;
    }
    
    /*
     * 返回屏幕切换特效类型
     */
    public int getScreenTransitionType() {
        return mScreenTransitionType;
    }
    
    /*
     * 返回触摸类型
     */
    protected int getTouchState() {
        return mTouchState;
    }
    
    public int getVisibleRange() {
        return mVisibleRange;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        computeScroll();
    }

    
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean bFlag = false;
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            motionEvent.setAction(MotionEvent.ACTION_CANCEL);
            mScaleGestureDetector.onTouchEvent(motionEvent);
            motionEvent.setAction(MotionEvent.ACTION_DOWN);
            mCurrentGestureFinished = false;
            mTouchIntercepted = false;
            mLastMotionX = motionEvent.getX();
            mLastMotionY = motionEvent.getY();
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
                setTouchState(motionEvent, TOUCH_STATE_SCROLLING);
            }else {
                mAllowLongPress = true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            onTouchEventUnique(motionEvent);
            if (mTouchState == TOUCH_STATE_REST && scrolledFarEnough(motionEvent)) {
                setTouchState(motionEvent, TOUCH_STATE_SCROLLING);
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            setTouchState(motionEvent, TOUCH_STATE_REST);
            break;
        }
        if (MotionEvent.ACTION_MOVE != (motionEvent.getAction() & MotionEvent.ACTION_MASK)) {
            onTouchEventUnique(motionEvent);
        }
        if (mCurrentGestureFinished || (mTouchState != TOUCH_STATE_REST && mTouchState != TOUCH_STATE_PINCHING)) {
            bFlag = true;
        }
        return bFlag;
    }

    public void setSlideBarPosition(FrameLayout.LayoutParams layoutParams) {
        if (layoutParams == null) {
            if (mSlideBar != null) {
                removeIndicator(mSlideBar);
                mSlideBar = null;
            }
        }else {
            if (mSlideBar != null) {
                mSlideBar.setLayoutParams(layoutParams);
            }else {
                mSlideBar = new SlideBar(mContext);
                mSlideBar.setOnTouchListener(new SliderTouchListener());
                mSlideBar.setAnimationCacheEnabled(false);
                addIndicator(mSlideBar, layoutParams);
            }
        }
    }
    
    public void setSlideBarVisibility(int visibility) {
        if (mSlideBar != null) {
            mSlideBar.setVisibility(visibility);
        }
    }
    
    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        mLongClickListener = onLongClickListener;
        for (int i = 0; i < getScreenCount(); i++) {
            getChildAt(i).setOnLongClickListener(onLongClickListener);
        }
    }
    
    public void setOverScrollRatio(float ratio) {
        mOverScrollRatio = ratio;
        refreshScrollBound();
    }
    
    public void setOvershootTension(float over_shoot_tension) {
        mOvershootTension = over_shoot_tension;
        if (mScrollInterpolator != null) {
            mScrollInterpolator.mTension = over_shoot_tension;
        }
    }
    
    public void setScreenAlignment(int align) {
        mScreenAlignment = align;
    }
    
    public void setScreenOffset(int screen_offset) {
        mScreenOffset = screen_offset;
        mScreenAlignment = 0;
        requestLayout();
    }
    
    public void setScreenPadding(Rect rect) {
        if (rect != null) {
            mScreenPaddingTop = rect.top;
            mScreenPaddingBottom = rect.bottom;
            setPadding(rect.left, 0, rect.right, 0);
            return;
        }else {
            throw new InvalidParameterException("The padding parameter can not be null.");
        }
    }
    
    /*
     * 页面切换过程时间
     */
    public void setScreenSnapDuration(int snap_duration) {
        mScreenSnapDuration = snap_duration;
    }
    
    /*
     * 设置页面切换使用的特效
     */
    public void setScreenTransitionType(int type) {
        boolean bFlag = true;
        mScreenTransitionType = type;
        if (type == 0) {
            bFlag = false;
        }
        setStaticTransformationsEnabled(bFlag);
        switch (mScreenTransitionType) {
        case SCREEN_TRANSITION_TYPE_CLASSIC:
        case SCREEN_TRANSITION_TYPE_FALLDOWN:
        case SCREEN_TRANSITION_TYPE_ROTATE:
            setOvershootTension(DEFAULT_OVER_SHOOT_TENSION);
            setScreenSnapDuration(DEFAULT_SCREEN_SNAP_DURATION);
            break;
        case SCREEN_TRANSITION_TYPE_CLASSIC_NO_OVER_SHOOT:
        case SCREEN_TRANSITION_TYPE_CROSSFADE:
        case SCREEN_TRANSITION_TYPE_STACK:
            setOvershootTension(0F);
            setScreenSnapDuration(270);
            break;
        case SCREEN_TRANSITION_TYPE_CUBE:
        case SCREEN_TRANSITION_TYPE_RIGHTPAGE:
        case SCREEN_TRANSITION_TYPE_LEFTPAGE:
        case SCREEN_TRANSITION_TYPE_CUSTOM:
            setOvershootTension(0F);
            setScreenSnapDuration(330);
            break;
            
        default:
            break;
        }
    }
    
    public void setScrollWholeScreen(boolean bFlag) {
        mScrollWholeScreen = bFlag;
    }
    
    public void setTouchSlop(int slop) {
        mTouchSlop = slop;
    }
    
    protected void setTouchState(MotionEvent motionEvent, int state) {
        mTouchState = state;
        ViewParent viewParent = getParent();
        boolean bFlag;
        if (mTouchState == 0) {
            bFlag = false;
        }else {
            bFlag = true;
        }
        viewParent.requestDisallowInterceptTouchEvent(bFlag);
        if (mTouchState != 0) {
            if (motionEvent != null) {
                mActivePointerId = motionEvent.getPointerId(0);
            }
            if (mAllowLongPress) {
                mAllowLongPress = false;
                View view = getChildAt(mCurrentScreen);
                if (view != null) {
                    view.cancelLongPress();
                }
            }
            if (mTouchState == 1) {
                mLastMotionX = motionEvent.getX();
                mTouchX = mScrollX;
                mSmoothingTime = (float)System.nanoTime() / NANOTIME_DIV;
            }
        }else {
            mActivePointerId = INVALID_POINTER;
            mAllowLongPress = false;
            mGestureVelocityTracker.recycle();
        }
    }
    
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        int screenCount = getScreenCount();
        if (index >= 0) {
            screenCount = Math.min(index, screenCount);
        }
        if (mScreenSeekBar != null) {
            mScreenSeekBar.addView(createSeekPoint(), screenCount, SEEK_POINT_LAYOUT_PARAMS);
        }
        mScreenCounter = mScreenCounter + 1;
        refreshScrollBound();
        super.addView(child, screenCount, params);
    }

    public boolean allowLongPress() {
        return mAllowLongPress;
    }
    
    /*
     * 计算位置，在屏幕显示之前完成
     */
    public void computeScroll() {
        isFromcomputeScroll = true;
        if (!mScroller.computeScrollOffset()) {
            if (mNextScreen == INVALID_SCREEN) {
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    float f1 = (float)System.nanoTime() / NANOTIME_DIV;
                    float f2 = (float)Math.exp((f1 - mSmoothingTime) / SMOOTHING_CONSTANT);
                    float f = mTouchX - (float)mScrollX;
                    mScrollX = (int)((float)mScrollX + f * f2);
                    mSmoothingTime = f1;
                    if (f > 1F || f < -1F) {
                        postInvalidate();
                    }
                }
            }else {
                setCurrentScreenInner(Math.max(0, Math.min(mNextScreen, (getScreenCount() - 1))));
                mNextScreen = INVALID_SCREEN;
            }
        }else {
            int i = mScroller.getCurrX();
            mScrollX = i;
            mTouchX = i;
            mSmoothingTime = (float)System.nanoTime() / NANOTIME_DIV;
            mScrollY = mScroller.getCurrY();
            postInvalidate();
        }
        updateIndicatorPositions(mScrollX);
        updateSlidePointPosition(mScrollX);
        updateArrowIndicatorResource(mScrollX);
        isFromcomputeScroll = false;
    }
    
    public boolean dispatchUnhandledMove(View view, int gravity) {
        boolean bFlag = true;
        
        if ((gravity == (Gravity.TOP | Gravity.CENTER_HORIZONTAL | Gravity.CENTER)) && (mCurrentScreen < getScreenCount() - 1)) {
            snapToScreen(mCurrentScreen + 1);
            return true;
        }else if(gravity == Gravity.CENTER) {
            if (mCurrentScreen > 0) {
                snapToScreen(mCurrentScreen - 1);
                return true;
            }
        }
        bFlag = dispatchUnhandledMove(view, gravity);
        return bFlag;
    }
    
    /*
     * 设置最大Snap速率
     */
    public void setMaximumSnapVelocity(int snap_velocity) {
        mMaximumVelocity = snap_velocity;
    }
}
