package cn.minking.launcher;

/**
 * 作者：      minking
 * 文件名称:    DeleteZone.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建桌面的删除（DeleteZone）区域， DeleteZone采用动画显示
 * ====================================================================================
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.FrameLayout;

public class DeleteZone extends FrameLayout 
    implements android.view.animation.Animation.AnimationListener{
    public DeleteZone(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // TODO Auto-generated method stub
        
    }
    
    
}