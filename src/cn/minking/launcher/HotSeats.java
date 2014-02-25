package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    HotSeats.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建桌面的HOTSEAT， HOTSEAT支持长按拖动图标
 * ====================================================================================
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class HotSeats extends LinearLayout
    implements android.view.View.OnLongClickListener{
    // HOTSEAT 支持长按拖动图标
    public HotSeats(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean onLongClick(View v) {
        // TODO Auto-generated method stub
        return false;
    }
    
    
}