package cn.minking.launcher;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * 作者：      minking
 * 文件名称:    DropTarget.java
 * 创建时间：    2014-02-27
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 桌面拖动接口
 * ====================================================================================
 */
public interface DropTarget{
    public static class DragObject{
        public boolean cancelled;
        public boolean dragComplete;
        public ItemInfo dragInfo;
        public DragSource dragSource;
        public DragView dragView;
        public Bitmap outline;
        public Runnable postAnimationRunnable;
        public int x;
        public int y;
        public int xOffset;
        public int yOffset;

        public DragObject() {
            x = -1;
            y = -1;
            xOffset = -1;
            yOffset = -1;
            dragComplete = false;
            dragView = null;
            dragInfo = null;
            dragSource = null;
            postAnimationRunnable = null;
            cancelled = false;
        }
    }
    
    public abstract boolean acceptDrop(DragObject dragobject);

    public abstract DropTarget getDropTargetDelegate(DragObject dragobject);

    public abstract void getHitRect(Rect rect);

    public abstract int getLeft();

    public abstract void getLocationOnScreen(int ai[]);

    public abstract int getTop();

    public abstract boolean isDropEnabled();

    public abstract void onDragEnter(DragObject dragobject);

    public abstract void onDragExit(DragObject dragobject);

    public abstract void onDragOver(DragObject dragobject);

    public abstract boolean onDrop(DragObject dragobject);
}