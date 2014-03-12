package cn.minking.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;

public class WorkspaceThumbnailView extends ThumbnailView{
    private Context mContext; 
    
    public WorkspaceThumbnailView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public WorkspaceThumbnailView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        enableThumbnailPositionChanging(true);
        mContext = context;
    }

    @Override
    protected void startSwitchingAnimation(boolean flag) {
        // TODO Auto-generated method stub
        super.startSwitchingAnimation(flag);
    }

    @Override
    public void onPinchOut(ScaleGestureDetector detector) {
        super.onPinchOut(detector);
        finishCurrentGesture();
        int i = ((ThumbnailScreen)getScreen(mCurrentScreen)).
                getThumbnailIndex((int)detector.getFocusX(), (int)detector.getFocusY());
        if (i >= 0) {
            i += mThumbnailCountPerScreen * mCurrentScreen;
            if (i != -1 + mAdapter.getCount()){
                mAdapter.onThumbnailClick(i);
            }
        }
    }

    protected void startDeletedAnimation(int i){
        ThumbnailScreen thumbnailscreen = (ThumbnailScreen)getCurrentScreen();
        thumbnailscreen.startMovingAnimation(mContext.getResources().getInteger(R.integer.config_animDuration), 
                i % mThumbnailCountPerScreen, thumbnailscreen.getChildCount());
    }
    
}