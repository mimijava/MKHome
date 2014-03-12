package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    DragableScreenView.java
 * 创建时间：    2013
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: 拖动显示Screen View类
 * ====================================================================================
 */
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DragableScreenView extends ScreenView
    implements DragScroller{
    
    protected class ScrollRunnable
        implements Runnable {
        private int mDirection;
    
        @Override
        public void run() {
            if (mDirection != 0){
                scrollDragingRight();
            } else {
                scrollDragingLeft();
            }
            mDragScrollState = 0;
        }
 
        public void setDirection(int i) {
            mDirection = i;
        }
    }

    protected Handler mDragScrollHandler;
    protected ScrollRunnable mDragScrollRunnable;
    protected int mDragScrollState;

    
    public DragableScreenView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragableScreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragScrollState = 0;
        mDragScrollRunnable = new ScrollRunnable();
        mDragScrollHandler = new Handler();
    }
    
    @Override
    public boolean onEnterScrollArea(int i, int j, int k) {
        return true;
    }

    @Override
    public boolean onExitScrollArea() {
        return true;
    }

    @Override
    public void scrollDragingLeft() {
        if (!mScroller.isFinished()) {
            if (mNextScreen > 0) {
                snapToScreen(-1 + mNextScreen);
            }
        } else {
            if (mCurrentScreen > 0){
                snapToScreen(-1 + mCurrentScreen);
            }
        }
    }

    @Override
    public void scrollDragingRight() {
        if (!mScroller.isFinished()) {
            if (mNextScreen < -1 + getScreenCount()){
                snapToScreen(1 + mNextScreen);
            }
        } else {
            if (mCurrentScreen < -1 + getScreenCount()){
                snapToScreen(1 + mCurrentScreen);
            }
        }
    }

    @Override
    public void onSecondaryPointerDown(MotionEvent motionEvent, int point_id) {
        super.onSecondaryPointerDown(motionEvent, point_id);
    }

    @Override
    public void onSecondaryPointerMove(MotionEvent motionEvent, int point_id) {
        super.onSecondaryPointerMove(motionEvent, point_id);
    }

    @Override
    public void onSecondaryPointerUp(MotionEvent motionEvent, int point_id) {
        super.onSecondaryPointerUp(motionEvent, point_id);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        final int action = motionEvent.getAction();
        int j = 0;
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_MOVE:
            if (getTouchState() == 5) {
                int i = (int)motionEvent.getX(motionEvent.findPointerIndex(mActivePointerId));
                if (i >= 30 && i <= -30 + getWidth()) {
                    if (mDragScrollState == 1) {
                        mDragScrollState = 0;
                        mDragScrollHandler.removeCallbacks(mDragScrollRunnable);
                    }
                } else {                
                    if (mDragScrollState == 0) {
                        mDragScrollState = 1;
                        ScrollRunnable scrollrunnable = mDragScrollRunnable;
                        if (i >= 30){
                            j = 1;
                        }
                        scrollrunnable.setDirection(j);
                        mDragScrollHandler.postDelayed(mDragScrollRunnable, 600L);
                    }
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            break;
        case MotionEvent.ACTION_CANCEL:
            break;
        }
        return true;
    }
    
    
    
}
