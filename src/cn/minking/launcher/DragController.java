package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    DragController.java
 * 创建时间：    2014
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140303: 添加两个基本操作
 * ====================================================================================
 */
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;

public class DragController {
    static interface TouchTranslator {
        public abstract void translatePosition(Rect rect);
        public abstract void translateTouch(float af[]);
    }
    
    static interface DragListener {
        public abstract void onDragEnd();
        public abstract void onDragStart(DragSource dragsource, ItemInfo iteminfo, int i);
    }


    /// M: 存储拖动目标
    private ArrayList<DropTarget> mDropTargets;
    private RectF mDeleteRegion;
    private TouchTranslator mTouchTranslater;
    
    public DragController(Context context){
        mDropTargets = new ArrayList<DropTarget>();
    }
    
    void setDeleteRegion(RectF rectf){
        mDeleteRegion = rectf;
    }
    
    /**
     * 功能： 添加拖动目标
     * @param droptarget
     */
    public void addDropTarget(DropTarget droptarget){
        mDropTargets.add(droptarget);
    }
    
    /**
     * 功能： 删除拖动目标
     */
    public void removeDropTarget(DropTarget droptarget){
        mDropTargets.remove(droptarget);
    }
    
    public void setTouchTranslator(TouchTranslator touchtranslator) {
        mTouchTranslater = touchtranslator;
    }
}
