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
    private ViewGroup mClientView;
    private Launcher mLauncher;
    private VersionTagGenerator mVersionTagGenerator;
    private boolean mHasPerformedLongPress;
    private boolean mIsLongPressCheckPending;
    private CheckForLongPress mPendingCheckForLongPress;
    
    public OnLongClickAgent(ViewGroup viewgroup, Launcher launcher, VersionTagGenerator versiontaggenerator){
        mClientView = viewgroup;
        mLauncher = launcher;
        mVersionTagGenerator = versiontaggenerator;
    }

    private OnLongClickListener mOnLongClickListener;
    
    public void setOnLongClickListener(OnLongClickListener onlongclicklistener){
        mOnLongClickListener = onlongclicklistener;
    }
    
    public void cancelCustomziedLongPress(){
        mHasPerformedLongPress = false;
        mIsLongPressCheckPending = false;
        if (mPendingCheckForLongPress != null)
            mClientView.removeCallbacks(mPendingCheckForLongPress);
    }
    
    public boolean isClickable(){
        boolean flag;
        if (mLauncher != null && mLauncher.isInEditing())
            flag = false;
        else
            flag = true;
        return flag;
    }
}