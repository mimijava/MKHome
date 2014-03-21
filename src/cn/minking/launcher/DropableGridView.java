package cn.minking.launcher;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.GridView;


public class DropableGridView extends GridView
    implements DropTarget{
    private ShortcutsAdapter mAdapter;
    private ShortcutInfo mDragItem;
    private View mLastHit;
    private HashMap<ShortcutInfo, Rect> mLastPosMap;
    private Runnable mStayConfirm;
    private Rect mTmpRect;
    
    public DropableGridView(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        mAdapter = null;
        mTmpRect = new Rect();
        mLastHit = null;
        mDragItem = null;
        mStayConfirm = new Runnable() {
            @Override
            public void run() {
                if (mLastHit == null || mDragItem != mLastHit.getTag()) {
                    makePositionSnapShot();
                    ShortcutsAdapter shortcutsadapter = mAdapter;
                    ShortcutInfo shortcutinfo1 = mDragItem;
                    ShortcutInfo shortcutinfo;
                    if (mLastHit != null) {
                        shortcutinfo = (ShortcutInfo)mLastHit.getTag();
                    } else {
                        shortcutinfo = null;
                    }
                    shortcutsadapter.reorderItemByInsert(shortcutinfo1, shortcutinfo);
                }
            }
        };
        setFocusable(false);
    }

    private void makePositionSnapShot() {
        mLastPosMap = new HashMap<ShortcutInfo, Rect>();
        for (int i = 0; i < getChildCount(); i++) {
            Rect rect = new Rect();
            View view = getChildAt(i);
            view.getHitRect(rect);
            mLastPosMap.put((ShortcutInfo)view.getTag(), rect);
        }
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragobject) {
        boolean flag = true;
        if ((dragobject.dragInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION 
                && dragobject.dragInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) 
            || dragobject.dragInfo.container == -1L)
            flag = false;
        return flag;
    }

    @Override
    public void addFocusables(ArrayList<View> arraylist, int i, int j) {
        if (isEnabled()){
            super.addFocusables(arraylist, i, j);
        }
    }

    @Override
    public DropTarget getDropTargetDelegate(DropTarget.DragObject dragobject) {
        return null;
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public void onDragEnter(DropTarget.DragObject dragobject) {
        if (mAdapter == null) {
            mAdapter = (ShortcutsAdapter)getAdapter();
        }
        mDragItem = (ShortcutInfo)dragobject.dragInfo;
    }

    @Override
    public void onDragExit(DropTarget.DragObject dragobject) {
        mLastHit = null;
        mDragItem = null;
        mLastPosMap = null;
        mAdapter = null;
        getHandler().removeCallbacks(mStayConfirm);
    }

    @Override
    public void onDragOver(DropTarget.DragObject dragobject) {
        View view = null;
        
        for (int i = 0; i < getChildCount(); i++) {
            View view1 = getChildAt(i);
            view1.getHitRect(mTmpRect);
            if (mTmpRect.contains(dragobject.x, dragobject.y)){
                view = view1;   
            }
        }
        if (view != mLastHit) {
            mLastHit = view;
            getHandler().removeCallbacks(mStayConfirm);
            postDelayed(mStayConfirm, 300L);
        }
    }
    
    @Override
    public boolean onDrop(DropTarget.DragObject dragobject) {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mLastPosMap == null) return;
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view.getVisibility() == View.VISIBLE) {
                view.clearAnimation();
                ShortcutInfo shortcutInfo = (ShortcutInfo)view.getTag();
                if (mLastPosMap.containsKey(shortcutInfo)) {
                    Rect rect = mLastPosMap.get(shortcutInfo);
                    if (rect.left != view.getLeft() || rect.right != view.getRight()) {
                        Animation anim = new TranslateAnimation(rect.left - view.getLeft(), 
                                0F, rect.top - view.getTop(), 0F);
                        anim.setDuration(300L);
                        view.startAnimation(anim);
                    }
                }
            }
        }
        mLastPosMap = null;
    }

    

}