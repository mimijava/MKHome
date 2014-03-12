package cn.minking.launcher;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.Animation;

public class ThumbnailView extends DragableScreenView{
    private class ThumbnailViewAdapterObserver extends DataSetObserver{

        @Override
        public void onChanged() {
            refreshThumbnails();
            mLayoutRequested = true;
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            onChanged();
        }
    }
    
    protected static final int LONG_PRESS_DELAY = 100 + ViewConfiguration.getLongPressTimeout();
    protected final android.view.animation.Animation.AnimationListener ENTER_PREVIEW_ANIMATION_LISTENER;
    protected final android.view.animation.Animation.AnimationListener EXIT_PREVIEW_ANIMATION_LISTENER;
    protected ThumbnailViewAdapter mAdapter;
    protected ThumbnailViewAdapterObserver mAdapterObserver;
    protected int mAnimationDuration;
    protected int mColumnCountPerScreen;
    private boolean mDataRefreshmentRequested;
    private int mDragingSource;
    private boolean mEnableThumbnailPositionChanging;
    private boolean mEnterAnimationRequested;
    private boolean mExitAnimationRequested;
    private boolean mLayoutRequested;
    private boolean mMeasurmentRequested;
    private int mRequesetedColumnCountPerScreen;
    private int mRequesetedRowCountPerScreen;
    private int mRequesetedThumbnailHeight;
    private int mRequesetedThumbnailWidth;
    protected int mRowCountPerScreen;
    private boolean mShowing;
    protected int mThumbnailCountPerScreen;
    protected int mThumbnailHeight;
    private boolean mThumbnailPositionChanged;
    private int mThumbnailPositionMapping[];
    protected int mThumbnailWidth;
    private Context mContext;
    
    public ThumbnailView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public ThumbnailView(Context context, AttributeSet attributeset, int i){
        super(context, attributeset, i);
        mMeasurmentRequested = true;
        mLayoutRequested = true;
        mDataRefreshmentRequested = true;
        mEnterAnimationRequested = false;
        mExitAnimationRequested = false;
        mRequesetedRowCountPerScreen = 0;
        mRequesetedColumnCountPerScreen = 0;
        mRequesetedThumbnailWidth = android.view.View.MeasureSpec.makeMeasureSpec(0, 0);
        mRequesetedThumbnailHeight = mRequesetedThumbnailWidth;
        mAdapterObserver = new ThumbnailViewAdapterObserver();
        mShowing = false;
        mEnableThumbnailPositionChanging = false;
        mContext = context;
        
        ENTER_PREVIEW_ANIMATION_LISTENER = new android.view.animation.Animation.AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                if (mAdapter != null) {
                    android.view.animation.Animation.AnimationListener animationlistener =
                            mAdapter.getEnterAnimationListener();
                    if (animationlistener != null){
                        animationlistener.onAnimationStart(animation);
                    }
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                if (mAdapter != null) {
                    android.view.animation.Animation.AnimationListener animationlistener =
                            mAdapter.getEnterAnimationListener();
                    if (animationlistener != null){
                        animationlistener.onAnimationEnd(animation);
                    }
                }
                clearSwitchingAnimation();              
            }
        };
        
        EXIT_PREVIEW_ANIMATION_LISTENER = new android.view.animation.Animation.AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                if (mAdapter != null) {
                    android.view.animation.Animation.AnimationListener animationlistener = 
                            mAdapter.getExitAnimationListener();
                    if (animationlistener != null)
                        animationlistener.onAnimationStart(animation);
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                if (mAdapter != null) {
                    android.view.animation.Animation.AnimationListener animationlistener = 
                            mAdapter.getExitAnimationListener();
                    if (animationlistener != null){
                        animationlistener.onAnimationEnd(animation);
                    }
                }
                clearSwitchingAnimation();
            }
        };
        mAnimationDuration = context.getResources().getInteger(R.integer.config_animDuration);
    }
    
    private void prepareSwitchingAnimation(boolean flag) {
        if (!mMeasurmentRequested 
                && !mLayoutRequested 
                && !mDataRefreshmentRequested) {
            if (getWidth() != 0) {
                setCurrentScreen(mAdapter.getFocusedItemPosition() / mThumbnailCountPerScreen);
                scrollToScreen(mCurrentScreen);
                startSwitchingAnimation(flag);
            }
        } else{
            if (!flag){
                mExitAnimationRequested = true;
            } else {
                mEnterAnimationRequested = true;
            }
        }
    }
    
    private void updateScreenGridSize(){
        
    }
    
    private void updateThumbnailPositionMapping(int i, int j){
        
    }
    
    protected void clearSwitchingAnimation(){
        
    }
    
    protected ThumbnailScreen createScreen(Context context, int i, int j, int k, int l){
        return new ThumbnailScreen(mContext, mRowCountPerScreen, mColumnCountPerScreen, mThumbnailWidth, mThumbnailHeight, true);
    }
    
    public void enableThumbnailPositionChanging(boolean flag) {
        mEnableThumbnailPositionChanging = flag;
    }
    
    public boolean isShowing() {
        return mShowing;
    }
    
    public boolean onInterceptTouchEvent(MotionEvent motionevent){
        boolean flag = false;
        return flag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // TODO Auto-generated method stub
        return super.onTouchEvent(motionEvent);
    }

    @Override
    protected void onLayout(boolean changed, int i, int j, int k, int l) {
        // TODO Auto-generated method stub
        super.onLayout(changed, i, j, k, l);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    
    protected void refreshThumbnails() {
        
    }
    
    public void setAdapter(ThumbnailViewAdapter thumbnailviewadapter) {
        if (mAdapter != null){
            mAdapter.unregisterDataSetObserver(mAdapterObserver);
        }
        mAdapter = thumbnailviewadapter;
        mAdapter.registerDataSetObserver(mAdapterObserver);
        mAdapterObserver.onInvalidated();
    }
    
    public void setAnimationDuration(int i) {
        mAnimationDuration = Math.max(0, i);
    }
    
    public void setOnLongClickListener(android.view.View.OnLongClickListener onlongclicklistener) {
        
    }
    
    public void setScreenGridSize(int i, int j) {
        mRequesetedRowCountPerScreen = i;
        mRequesetedColumnCountPerScreen = j;
        mMeasurmentRequested = true;
        mDataRefreshmentRequested = true;
        mLayoutRequested = true;
        requestLayout();
    }

    public void setScreenPadding(Rect rect) {
        super.setScreenPadding(rect);
        mMeasurmentRequested = true;
        mDataRefreshmentRequested = true;
        mLayoutRequested = true;
        requestLayout();
    }

    public void setThumbnailMeasureSpec(int i, int j) {
        mRequesetedThumbnailWidth = i;
        mRequesetedThumbnailHeight = j;
        mMeasurmentRequested = true;
        mDataRefreshmentRequested = true;
        mLayoutRequested = true;
        requestLayout();
    }

    public void show(boolean flag) {
        if (mAdapter != null && mShowing != flag) {
            mShowing = flag;
            if (!mShowing) {
                prepareSwitchingAnimation(false);
            } else {
                prepareSwitchingAnimation(true);
            }
        }
    }

    public void snapToScreen(int i) {
        int j = Math.max(0, Math.min(i, -1 + getScreenCount()));
        if (getTouchState() == 5 && j != mDragingSource / mThumbnailCountPerScreen) {
            ThumbnailScreen thumbnailscreen = (ThumbnailScreen)getScreen(j);
            int i1 = mDragingSource / mThumbnailCountPerScreen;
            boolean flag;
            if (i1 >= j) {
                flag = false;
            } else {
                flag = true;
            }
            int intoThumb = thumbnailscreen.moveThumbnailInto(flag, (ThumbnailScreen)getScreen(i1), mDragingSource % mThumbnailCountPerScreen);
            int k = thumbnailscreen.moveThumbnailTo(mAnimationDuration, intoThumb, (int)mLastMotionX, (int)mLastMotionY) + j * mThumbnailCountPerScreen;
            updateThumbnailPositionMapping(mDragingSource, k);
            mDragingSource = k;
        }
        super.snapToScreen(j);
    }

    protected void startSwitchingAnimation(boolean flag) {
    }
}