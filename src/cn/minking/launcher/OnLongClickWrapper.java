package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    OnLongClickWrapper.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建文件及构造函数
 * ====================================================================================
 */
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class OnLongClickWrapper extends FrameLayout
    implements OnLongClickAgent.VersionTagGenerator{

    private OnLongClickAgent mOnLongClickAgent;
    
    public OnLongClickWrapper(Context context, AttributeSet attributeset){
        super(context, attributeset);
    }
    
    @Override
    public Integer getVersionTag() {
        // TODO Auto-generated method stub
        return Integer.valueOf(getWindowAttachCount());
    }
    
    public void setLauncher(Launcher launcher){
        mOnLongClickAgent = new OnLongClickAgent(this, launcher, this);
    }
}