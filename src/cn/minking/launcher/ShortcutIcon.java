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
    
    public ShortcutIcon(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        mFolderCreationBgEnter = null;
        mFolderCreationBgExit = null;
    }
    
    public void updateInfo(Launcher launcher, ShortcutInfo shortcutinfo){
        setTag(shortcutinfo);
        if (shortcutinfo.mIconType != 3) {
            setIcon(shortcutinfo.getIcon(launcher.getIconCache()));
        } 
        setTitle(shortcutinfo.title);
        launcher.bindAppMessage(this, shortcutinfo.intent.getComponent());
        if (mFolderCreationBgEnter == null) {
            mFolderCreationBgEnter = AnimationUtils.loadAnimation(launcher, R.anim.folder_creation_bg_enter);
            mFolderCreationBgExit = AnimationUtils.loadAnimation(launcher, R.anim.folder_creation_bg_exit);
        }
        mLauncher = launcher;
    }
    
    static ShortcutIcon fromXml(int layout, Launcher launcher, 
            ViewGroup viewgroup, ShortcutInfo shortcutinfo){
        ShortcutIcon shortcutIcon = (ShortcutIcon)LayoutInflater.from(launcher).inflate(layout, viewgroup, false);
        shortcutIcon.updateInfo(launcher, shortcutinfo);
            
        return shortcutIcon;
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFolderCreationBg = (ImageView)findViewById(R.id.icon_folder_creation_bg);
        if (mFolderCreationBg != null){
            android.graphics.Bitmap bitmap = FolderIcon.loadFolderIconBitmap();
            if (bitmap != null)
                mFolderCreationBg.setImageBitmap(bitmap);
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