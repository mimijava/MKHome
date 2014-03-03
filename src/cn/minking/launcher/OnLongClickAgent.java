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

    private ViewGroup mClientView;
    private Launcher mLauncher;
    private VersionTagGenerator mVersionTagGenerator;
    
    public OnLongClickAgent(ViewGroup viewgroup, Launcher launcher, VersionTagGenerator versiontaggenerator){
        mClientView = viewgroup;
        mLauncher = launcher;
        mVersionTagGenerator = versiontaggenerator;
    }

    private OnLongClickListener mOnLongClickListener;
    
    public void setOnLongClickListener(OnLongClickListener onlongclicklistener){
        mOnLongClickListener = onlongclicklistener;
    }
}