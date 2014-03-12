package cn.minking.launcher;

import android.view.MotionEvent;

public interface DragScroller {

    public abstract boolean onEnterScrollArea(int i, int j, int k);

    public abstract boolean onExitScrollArea();

    public abstract void onSecondaryPointerDown(MotionEvent motionevent, int i);

    public abstract void onSecondaryPointerMove(MotionEvent motionevent, int i);

    public abstract void onSecondaryPointerUp(MotionEvent motionevent, int i);

    public abstract void scrollDragingLeft();

    public abstract void scrollDragingRight();
}