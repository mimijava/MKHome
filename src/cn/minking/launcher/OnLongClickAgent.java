package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    OnLongClickAgent.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建文件及构造函数， 接口VersionTagGenerator
 * ====================================================================================
 */
import android.view.View.OnLongClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class OnLongClickAgent{
    
    public static interface VersionTagGenerator{
        public abstract Object getVersionTag();
    }
    
    class CheckForLongPress implements Runnable{
        private Object zOriginalVersionTag;
        
        public void rememberVersionTag(){
            zOriginalVersionTag = mVersionTagGenerator.getVersionTag();
        }
    
        public void run() {
            if (mIsLongPressCheckPending 
                    && mClientView.hasWindowFocus() 
                    && mClientView.getParent() != null 
                    && zOriginalVersionTag == mVersionTagGenerator.getVersionTag()) {
                if (mOnLongClickListener != null)
                    mOnLongClickListener.onLongClick(mClientView);
                mHasPerformedLongPress = true;
                mIsLongPressCheckPending = false;
            }
        }
    }
    
    private static int MOVE_THRESHOLD = 15;
    private ViewGroup mClientView;
    private float mDonwX;
    private float mDonwY;
    private long mEditingTimeout;
    private long mNormalTimeout;
    
    private Launcher mLauncher;
    private VersionTagGenerator mVersionTagGenerator;
    private boolean mHasPerformedLongPress;
    private boolean mIsLongPressCheckPending;
    private CheckForLongPress mPendingCheckForLongPress;
    private View.OnLongClickListener mOnLongClickListener;
    
    public OnLongClickAgent(ViewGroup viewgroup, Launcher launcher, VersionTagGenerator versiontaggenerator){
        mEditingTimeout = 200L;
        mNormalTimeout = 800L;
        mClientView = viewgroup;
        mLauncher = launcher;
        mVersionTagGenerator = versiontaggenerator;
    }

    private boolean handleTouchEvent(MotionEvent motionevent){
        boolean flag = false;
        
        if (!mHasPerformedLongPress) {
            if (mIsLongPressCheckPending)
                switch (0xff & motionevent.getAction()) {
                case MotionEvent.ACTION_MOVE: // '\002'
                    if (Math.abs(mDonwX - motionevent.getX()) < (float)MOVE_THRESHOLD 
                            && Math.abs(mDonwY - motionevent.getY()) < (float)MOVE_THRESHOLD)
                        break;
                    // fall through

                case MotionEvent.ACTION_UP: // '\001'
                case MotionEvent.ACTION_CANCEL: // '\003'
                    cancelCustomziedLongPress();
                    break;
                }
        } else {
            flag = true;
        }
        
        return flag;
    }
    
    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        boolean flag = false;
        int i = 0xff & motionevent.getAction();
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            if (i == 0){
                switch (i)
                {
                default:
                    flag = handleTouchEvent(motionevent);
                    break;

                case MotionEvent.ACTION_DOWN: // '\0'
                    mDonwX = motionevent.getX();
                    mDonwY = motionevent.getY();
                    postCheckForLongClick();
                    break;
                }
                flag = flag;
            }
            flag = true;
        }
        return flag;
    }
    
    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress == null){
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberVersionTag();
        ViewGroup viewgroup = mClientView;
        CheckForLongPress checkforlongpress = mPendingCheckForLongPress;
        long l;
        if (!mLauncher.isInEditing())
            l = mNormalTimeout;
        else
            l = mEditingTimeout;
        viewgroup.postDelayed(checkforlongpress, l);
        mIsLongPressCheckPending = true;
    }

    public void cancelCustomziedLongPress() {
        mHasPerformedLongPress = false;
        mIsLongPressCheckPending = false;
        if (mPendingCheckForLongPress != null) {
            mClientView.removeCallbacks(mPendingCheckForLongPress);
        }
    }

    public boolean isClickable() {
        boolean flag;
        if (mLauncher != null && mLauncher.isInEditing())
            flag = false;
        else
            flag = true;
        return flag;
    }
    
    public void setOnLongClickListener(OnLongClickListener onlongclicklistener){
        mOnLongClickListener = onlongclicklistener;
    }
    
    public boolean onTouchEvent(MotionEvent motionevent) {
        return handleTouchEvent(motionevent);
    }

    public void setEditingTimeout(long timeOut) {
        mEditingTimeout = timeOut;
    }
    
}