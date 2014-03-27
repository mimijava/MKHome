package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ShortcutIcon.java
 * 创建时间：    2014-02-27
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: APP快捷图标 文件创建
 * 20140326: 完善快捷图标的拖动处理
 * ====================================================================================
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ShortcutIcon extends ItemIcon
    implements DropTarget {
    
    private ImageView mFolderCreationBg;
    private Animation mFolderCreationBgEnter;
    private Animation mFolderCreationBgExit;
    private Launcher mLauncher;
    private Context mContext;
    
    public ShortcutIcon(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        mContext = context;
        mFolderCreationBgEnter = null;
        mFolderCreationBgExit = null;
    }
    
    /**
     * 功能： 更新ShortcutInfo信息
     * @param launcher
     * @param shortcutinfo
     */
    public void updateInfo(Launcher launcher, ShortcutInfo shortcutinfo){
        // 设置标签
        setTag(shortcutinfo);
        if (shortcutinfo.mIconType != LauncherSettings.Favorites.ICON_TYPE_CUSTOMIZE) {
            // 设置默认图标
            setIcon(shortcutinfo.getIcon(launcher.getIconCache()));
        } 
        
        // 标题
        setTitle(shortcutinfo.title);
        launcher.bindAppMessage(this, shortcutinfo.intent.getComponent());
        if (mFolderCreationBgEnter == null) {
            mFolderCreationBgEnter = AnimationUtils.loadAnimation(launcher, R.anim.folder_creation_bg_enter);
            mFolderCreationBgExit = AnimationUtils.loadAnimation(launcher, R.anim.folder_creation_bg_exit);
        }
        mLauncher = launcher;
    }
    
    /**
     * 功能： 从XML中的得到快捷图标的布局 
     * @param layout
     * @param launcher
     * @param viewgroup
     * @param shortcutinfo
     * @return
     */
    static ShortcutIcon fromXml(int layout, Launcher launcher, 
            ViewGroup viewgroup, ShortcutInfo shortcutinfo){
        ShortcutIcon shortcutIcon = (ShortcutIcon)LayoutInflater.from(launcher).inflate(layout, viewgroup, false);
        shortcutIcon.updateInfo(launcher, shortcutinfo);
            
        return shortcutIcon;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFolderCreationBg = (ImageView)findViewById(R.id.icon_folder_creation_bg);
        if (mFolderCreationBg != null){
            Bitmap bitmap = FolderIcon.loadFolderIconBitmap(mContext);
            if (bitmap != null) {
                mFolderCreationBg.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mFolderCreationBg != null) {
            FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams)mFolderCreationBg.getLayoutParams();
            layoutparams.topMargin = (int)(0.08333334F * (float)mFolderCreationBg.getMeasuredWidth());
            mFolderCreationBg.setLayoutParams(layoutparams);
        }
    }

    @Override
    public boolean isDropEnabled() {
        boolean flag;
        if (isCompact()) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }

    /**
     * 功能： 判断是否为APP或快捷方式，非则无法拖动
     * @param dragobject
     * @return
     */
    private boolean isDropable(DropTarget.DragObject dragobject) {
        boolean flag = true;
        if (dragobject.dragInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION 
                && dragobject.dragInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
            flag = false;
        }
        return flag;
    }
    
    
    @Override
    public boolean acceptDrop(DragObject dragobject) {
        return isDropable(dragobject);
    }

    @Override
    public DropTarget getDropTargetDelegate(DragObject dragobject) {
        return null;
    }

    @Override
    public void onDragEnter(DragObject dragobject) {
        mFolderCreationBg.startAnimation(mFolderCreationBgEnter);
        invalidate();
    }

    @Override
    public void onDragExit(DragObject dragobject) {
        mFolderCreationBg.startAnimation(mFolderCreationBgExit);
        invalidate();
    }

    @Override
    public void onDragOver(DragObject dragobject) {
        
    }

    @Override
    public boolean onDrop(DragObject dragobject) {
        boolean flag;
        if (!isDropable(dragobject)) {
            flag = false;
        } else {
            mFolderCreationBg.startAnimation(mFolderCreationBgExit);
            mLauncher.getWorkspace().createUserFolderWithDragOverlap(
                    (ShortcutInfo)dragobject.dragInfo, (ShortcutInfo)getTag());
            flag = true;
        }
        return flag;
    }
}