package cn.minking.launcher.upsidescene;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ScrollableScreen extends FrameLayout{
    private boolean mIsBeingDragged;
    private int mActivePointerId;
    private int mScreenCount;
    
    public ScrollableScreen(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mIsBeingDragged = false;
        mActivePointerId = -1;
        mScreenCount = -1;
    }

    public ScrollableScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollableScreen(Context context) {
        this(context, null);
    }
    
}