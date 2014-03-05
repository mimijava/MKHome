package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    OnLongClickWrapper.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建文件及构造函数
 * 20140304： Wrap布局 -------------------------------
 *                  | |----| |----| |----| |----| |
 *                  | |    | |    | |    | |    | |
 *                  | |----| |----| |----| |----| |
 *                  -------------------------------
 * ====================================================================================
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class OnLongClickWrapper extends FrameLayout
    implements OnLongClickAgent.VersionTagGenerator{

    private OnLongClickAgent mOnLongClickAgent;
    
    public OnLongClickWrapper(Context context, AttributeSet attributeset){
        super(context, attributeset);
    }
    
    public OnLongClickWrapper(Launcher launcher){
        super(launcher);
        super.setClickable(true);
        setLauncher(launcher);
    }
    
    public void setOnLongClickListener(android.view.View.OnLongClickListener onlongclicklistener){
        mOnLongClickAgent.setOnLongClickListener(onlongclicklistener);
    }
    
    public void setLauncher(Launcher launcher){
        mOnLongClickAgent = new OnLongClickAgent(this, launcher, this);
    }
    
    public void cancelLongPress(){
        mOnLongClickAgent.cancelCustomziedLongPress();
    }
    
    public boolean isClickable(){
        boolean flag;
        if (mOnLongClickAgent != null && !mOnLongClickAgent.isClickable() || !super.isClickable())
            flag = false;
        else
            flag = true;
        return flag;
    }
    
    @Override
    public Integer getVersionTag() {
        return Integer.valueOf(getWindowAttachCount());
    }
    
    @Override
    public void addView(View view) {
        super.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }
    
}