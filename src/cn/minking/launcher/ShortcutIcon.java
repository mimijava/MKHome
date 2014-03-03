package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ShortcutIcon.java
 * 创建时间：    2014-02-27
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: APP快捷图标 文件创建
 * ====================================================================================
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class ShortcutIcon extends ItemIcon
    implements DropTarget {
    public ShortcutIcon(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
    }
    static ShortcutIcon fromXml(int i, Launcher launcher, 
            ViewGroup viewgroup, ShortcutInfo shortcutinfo){
        ShortcutIcon shortcutIcon = (ShortcutIcon)LayoutInflater.from(launcher).inflate(i, viewgroup, false);
        return shortcutIcon;
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }
}