package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    HotSeatButton.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建文件及构造函数
 * ====================================================================================
 */
import android.content.Context;
import android.util.AttributeSet;

public class HotSeatButton extends OnLongClickWrapper{
    private ItemIcon mIcon;

    public HotSeatButton(Context context, AttributeSet attributeset)
    {
        super(context, attributeset);
        mIcon = null;
    }
    
    public void bind(ItemIcon itemicon, DragController dragcontroller){
        mIcon = itemicon;
        addView(itemicon, 0);
        if (itemicon instanceof DropTarget) {
            dragcontroller.addDropTarget((DropTarget)itemicon);
        }
    }
    
    /**
     * 功能：  得到ItemIcon
     * @return
     */
    public ItemIcon getIcon() {
        return mIcon;
    }
    
    public void unbind(DragController dragController) {
        if ((mIcon instanceof DropTarget) && ((DropTarget)mIcon).isDropEnabled())
            dragController.removeDropTarget((DropTarget)mIcon);
        removeAllViews();
        mIcon = null;
    }
}