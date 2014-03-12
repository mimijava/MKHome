package cn.minking.launcher;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

public class LauncherAppWidgetHostView extends AppWidgetHostView
    implements OnLongClickAgent.VersionTagGenerator{
    private LayoutInflater mInflater;
    private Launcher mLauncher;
    private OnLongClickAgent mOnLongClickAgent;

    public LauncherAppWidgetHostView(Context context, Launcher launcher) {
        super(context);
        mInflater = (LayoutInflater)context.getSystemService("layout_inflater");
        mOnLongClickAgent = new OnLongClickAgent(this, launcher, this);
        mLauncher = launcher;
    }

    public void cancelLongPress() {
        mOnLongClickAgent.cancelCustomziedLongPress();
    }

    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    public Object getVersionTag() {
        return Integer.valueOf(getWindowAttachCount());
    }

    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        boolean flag = true;
        if (!mOnLongClickAgent.onInterceptTouchEvent(motionevent) && !mLauncher.isInEditing())
            flag = super.onInterceptTouchEvent(motionevent);
        return flag;
    }

    public boolean onTouchEvent(MotionEvent motionevent)
    {
        boolean flag;
        if (!mOnLongClickAgent.onTouchEvent(motionevent))
            flag = super.onTouchEvent(motionevent);
        else
            flag = true;
        return flag;
    }

    public void setOnLongClickListener(android.view.View.OnLongClickListener onlongclicklistener)
    {
        mOnLongClickAgent.setOnLongClickListener(onlongclicklistener);
    }
}