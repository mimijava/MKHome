package cn.minking.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class FolderCling extends FrameLayout
    implements View.OnClickListener, DropTarget{
    
    /******* 状态  *******/
    // 文件夹是否处于打开状态
    private boolean mOpened;
    
    /******* 内容  *******/
    private DragController mDragController;
    private Folder mFolder;
    private Launcher mLauncher;
    
    /******* 动作  *******/
    private Runnable mOnFinishClose;
    private Runnable mCloseConfirm;

    /******* 数据  *******/
    private int mTmpPos[];

    
    public FolderCling(final Context context, AttributeSet attributeset){
        super(context, attributeset);
        mOpened = false;
        mTmpPos = new int[2];
        mOnFinishClose = new Runnable() {
            @Override
            public void run() {
                setVisibility(View.GONE);
                mOpened = false;
            }

        };
        
        mCloseConfirm = new Runnable() {
            @Override
            public void run() {
                mLauncher.closeFolder();
                FolderInfo folderinfo = mFolder.getInfo();
                ShortcutInfo shortcutinfo = mFolder.getDragedItem();
                if (shortcutinfo != null 
                    && folderinfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                    && folderinfo.getAdapter(context).getCount() == 1) {
                    mFolder.removeItem(shortcutinfo);
                    folderinfo.icon.deleteSelf();
                    shortcutinfo.copyPosition(folderinfo);
                }
            }
        };
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void bind(FolderInfo folderinfo) {
        mFolder.bind(folderinfo);
    }

    @Override
    public void onClick(View v) {
         mLauncher.closeFolder();
    }

    @Override
    public boolean acceptDrop(DragObject dragobject) {
        boolean flag = true;
        if (dragobject.dragInfo.itemType != 0 && dragobject.dragInfo.itemType != 1){
            flag = false;
        }
        return flag;
    }

    @Override
    public DropTarget getDropTargetDelegate(DragObject dragobject) {
        return null;
    }

    @Override
    public boolean isDropEnabled() {
        return isOpened();
    }

    @Override
    public void onDragEnter(DragObject dragobject) {
        postDelayed(mCloseConfirm, 200L);
    }

    @Override
    public void onDragExit(DragObject dragobject) {
        removeCallbacks(mCloseConfirm);
    }

    @Override
    public void onDragOver(DragObject dragobject) {
        
    }

    @Override
    public boolean onDrop(DragObject dragobject) {
        return false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFolder = (Folder)findViewById(R.id.folder);
        setOnClickListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        if (mOpened) {
            super.onLayout(changed, left, top, right, bottom);
            FolderIcon foldericon = mFolder.getInfo().icon;
            foldericon.getLocationInWindow(mTmpPos);
            int i1 = mTmpPos[1];
            int l1 = i1 + foldericon.getMeasuredHeight();
            int j1 = i1 - mFolder.getTop();
            int k1 = (mFolder.getTop() + mFolder.getLastAtMostMeasureHeight()) - l1;
            
            if (j1 < mFolder.getMeasuredHeight()) {
                if (k1 < mFolder.getMeasuredHeight()) {
                    if (j1 >= k1) {
                        mFolder.showOpenAnimation();                
                    }
                    i1 = mFolder.getLastAtMostMeasureHeight() - mFolder.getMeasuredHeight();
                } else {
                    i1 = l1 - mFolder.getTop();
                }
            } else {
                i1 -= mFolder.getBottom();
            }
            i1 = Math.min(i1, mFolder.getLastAtMostMeasureHeight() - mFolder.getMeasuredHeight());
            mFolder.layout(mFolder.getLeft(), i1 + mFolder.getTop(), mFolder.getRight(), i1 + mFolder.getBottom());
        }
    }
    
    /**
     * 功能： 打开文件夹
     */
    public void open() {
        setVisibility(View.VISIBLE);
        mDragController.addDropTarget(this);
        mFolder.onOpen(true);
        mOpened = true;
    }
    
    /**
     * 功能： 关闭文件夹
     * @param bClose
     */
    public void close(boolean bClose) {
        mDragController.removeDropTarget(this);
        mFolder.onClose(bClose, mOnFinishClose);
    }

    /**
     * 功能： 文件夹是否打开
     * @return
     */
    public boolean isOpened() {
        return mOpened;
    }

    public void setDragController(DragController dragcontroller) {
        mFolder.setDragController(dragcontroller);
        mDragController = dragcontroller;
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
        mFolder.setLauncher(launcher);
    }
}
    
