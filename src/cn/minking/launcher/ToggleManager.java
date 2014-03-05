package cn.minking.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * 作者：      minking
 * 文件名称:    ToggleManager.java
 * 创建时间：    2014-03-04
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140304 : 小图标下拉后的快捷功能图标
 * ====================================================================================
 */
public class ToggleManager{
    private static Context sContext;
    
    private ToggleManager(Context context){
        sContext = context;
    }
    
//  private static int getImage(int i){
//        return sToggleImages[i];
//    }
//  
//  public static Drawable getImageDrawable(int i){
//        Drawable drawable = sContext.getResources().getDrawable(getImage(i));
//        drawable.setAlpha(sToggleAlpha[i]);
//        return drawable;
//    }
    
}