package cn.minking.launcher.upsidescene;

import cn.minking.launcher.Launcher;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

public class SceneScreen extends FrameLayout{
    private FixedScreen mForegroundScreen;
    private boolean mIsEditMode;
    private boolean mIsLongClicked;
    private boolean mIsStarted;
    private float mLastMotionX;
    private float mLastMotionY;
    private Launcher mLauncher;
    private float mOldWpOffsetX;
    private float mOldWpStepX;
    private ScaleGestureDetector mScaleDetector;
    private SceneContentView mSceneContent;
    private SceneData mSceneData;
    private ScrollableScreen mScrollableScreen;
    private boolean mShowHideAnimating;
    private int mTouchSlop;
    private SpriteView mTouchedSprite;
    private Context mContext;
    
    private BroadcastReceiver mExitSceneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context1, Intent intent) {
            mLauncher.hideSceneScreen();
        }
    };
    
    public SceneScreen(Context context) {
        this(context, null, 0);
    }

    public SceneScreen(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public SceneScreen(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        mIsEditMode = false;
        mContext = context;
    }
    
    public boolean isShowing() {
        boolean flag;
        if (getVisibility() != View.VISIBLE) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    public void onHideAnimationStart() {
        mShowHideAnimating = true;
        mLauncher.updateWallpaperOffsetAnimate(mOldWpStepX, 0F, mOldWpOffsetX, 0F);
    }
    
    public void onHideAnimationEnd() {
        setVisibility(View.GONE);
        onStop();
        mShowHideAnimating = false;
    }
    
    public void onStart() {
        if (!mIsStarted) {
            mContext.registerReceiver(mExitSceneReceiver, new IntentFilter("cn.minking.launcher.upsidescene.SceneScreen.EXIT"));
            mIsStarted = true;
        }
    }

    public void onStop() {
        if (mIsStarted){
            mContext.unregisterReceiver(mExitSceneReceiver);
            mIsStarted = false;
        }
    }
}