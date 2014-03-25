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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class OnLongClickWrapper extends FrameLayout
    implements OnLongClickAgent.VersionTagGenerator{

    private OnLongClickAgent mOnLongClickAgent;
    
    public OnLongClickWrapper(Context context, AttributeSet attributeset){
        super(context, attributeset);
        super.setClickable(true);
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
        boolean bClickAble;
        if (mOnLongClickAgent != null && !mOnLongClickAgent.isClickable() || !super.isClickable()){
            bClickAble = false;
        } else {
            bClickAble = true;
        }
        return bClickAble;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean flag;
        if (!mOnLongClickAgent.onInterceptTouchEvent(ev)) {
            flag = super.onInterceptTouchEvent(ev);
        } else {
            flag = true;
        }
        return flag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mOnLongClickAgent.onTouchEvent(ev);
        boolean flag = super.onTouchEvent(ev);
        if (preventPressState() && !isClickable()) {
            setPressed(false);
            super.cancelLongPress();
        }
        return flag;
    }
    
    public boolean preventPressState() {
        return false;
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