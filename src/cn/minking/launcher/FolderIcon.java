package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    FolderIcon.java
 * 创建时间：    2014-02-27
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 文件夹图标 文件创建
 * ====================================================================================
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class FolderIcon extends ItemIcon
    implements DropTarget {
    public FolderIcon(Context context, AttributeSet attributeset){
        super(context, attributeset);
    }
    public static FolderIcon fromXml(int i, Launcher launcher, 
            ViewGroup viewgroup, FolderInfo folderinfo){
        FolderIcon folderIcon = (FolderIcon)LayoutInflater.from(launcher).inflate(i, viewgroup, false);
        return folderIcon;
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }
    
    public static final Bitmap loadFolderIconBitmap()    {
        return null;
    }
    @Override
    public boolean acceptDrop(DragObject dragobject) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public DropTarget getDropTargetDelegate(DragObject dragobject) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void onDragEnter(DragObject dragobject) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onDragExit(DragObject dragobject) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onDragOver(DragObject dragobject) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public boolean onDrop(DragObject dragobject) {
        // TODO Auto-generated method stub
        return false;
    }
    
    
}