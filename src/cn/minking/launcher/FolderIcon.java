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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FolderIcon extends ItemIcon
    implements DropTarget {
    
    private ImageView mCover;
    private AnimationDrawable mCoverClose;
    private AnimationDrawable mCoverHalfClose;
    private AnimationDrawable mCoverHalfOpen;
    private AnimationDrawable mCoverOpen;
    private Drawable mFolderBackground;
    private IconCache mIconCache;
    private FolderInfo mInfo;
    private ImageView mItemIcons[];
    private Launcher mLauncher;
    private LinearLayout mPreviewIconContainer;
    private static Context mContext;
    
    public FolderIcon(Context context, AttributeSet attributeset){
        super(context, attributeset);
        mContext = context;
        mCoverOpen = null;
        mCoverClose = null;
        mCoverHalfOpen = null;
        mCoverHalfClose = null;
        mIconCache = ((LauncherApplication)context.getApplicationContext()).getIconCache();
    }
    public static FolderIcon fromXml(int i, Launcher launcher, 
            ViewGroup viewgroup, FolderInfo folderinfo){
        FolderIcon foldericon = (FolderIcon)LayoutInflater.from(launcher).inflate(i, viewgroup, false);
        foldericon.setIcon(foldericon.mFolderBackground);
        foldericon.setTitle(folderinfo.title);
        foldericon.setTag(folderinfo);
        foldericon.mInfo = folderinfo;
        foldericon.mLauncher = launcher;
        folderinfo.icon = foldericon;
        folderinfo.notifyDataSetChanged();
        return foldericon;
    }
    
    private boolean isDropable(ItemInfo iteminfo) {
        boolean flag = true;
        if (iteminfo.itemType != 0 && iteminfo.itemType != 1 
                || iteminfo.container == -1L 
                || mInfo.opened)
            flag = false;
        return flag;
    }

    void loadItemIcons() {
        for (int i = 0; i < mItemIcons.length; i++) {
            if (mInfo.count() <= i) {
                mItemIcons[i].setImageBitmap(null);
            } else {
                ShortcutInfo shortcutinfo = mInfo.getAdapter(mLauncher).getItem(i);
                shortcutinfo.ensureToggleIcon(getContext());
                mItemIcons[i].setImageBitmap(shortcutinfo.getIcon(mIconCache));
            }
        }
        mLauncher.updateFolderMessage(mInfo);
    }
    
    @Override
    public boolean isDropEnabled() {
        return true;
    }
    
    public void deleteSelf() {
        if (mInfo.contents.isEmpty()) {
            mLauncher.preRemoveItem(this);
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation anim) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mLauncher.removeFolder(mInfo);
                        }
                    });
                }

                public void onAnimationRepeat(Animation anim) {
                }

                public void onAnimationStart(Animation anim) {
                }

            
            });
            startAnimation(animation);
        }
    }
    
    public final static Bitmap loadFolderIconBitmap(Context context) {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_folder);
    }

    public void getHitRect(Rect rect) {
        //rect.set(mLeft + mIcon.getLeft(), mTop, mLeft + mIcon.getWidth() + mIcon.getLeft(), mBottom);
        return;
    }
    
    public void onClose() {
        if (mCoverClose != null) {
            mCoverClose.unscheduleSelf(null);
            mCover.setImageDrawable(mCoverClose);
            mCoverClose.start();
        }
    }

    @Override
    public boolean acceptDrop(DragObject dragobject) {
        return isDropable(dragobject.dragInfo);
    }
    @Override
    public DropTarget getDropTargetDelegate(DragObject dragobject) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void onDragEnter(DragObject dragobject) {
        if (isDropable(dragobject.dragInfo) && mCoverHalfOpen != null) {
            mCoverHalfOpen.unscheduleSelf(null);
            mCover.setImageDrawable(mCoverHalfOpen);
            mCoverHalfOpen.start();
        }
    }
    @Override
    public void onDragExit(DragObject dragobject) {
        if (mCoverHalfClose != null) {
            mCoverHalfClose.unscheduleSelf(null);
            mCover.setImageDrawable(mCoverHalfClose);
            mCoverHalfClose.start();
        }
    }
    @Override
    public void onDragOver(DragObject dragobject) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public boolean onDrop(DragObject dragobject) {
        ShortcutInfo shortcutinfo = (ShortcutInfo)dragobject.dragInfo;
        if (dragobject.dragInfo.container != mInfo.id) {
            shortcutinfo.cellX = mInfo.getAdapter(mContext).getCount();
            mInfo.add(shortcutinfo);
            LauncherModel.addOrMoveItemInDatabase(mLauncher, (ItemInfo)shortcutinfo, 
                    mInfo.id, -1, shortcutinfo.cellX, 0);
        }
        mInfo.notifyDataSetChanged();
        return true;
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ImageView aimageview[] = new ImageView[4];
        aimageview[0] = (ImageView)findViewById(R.id.item1);
        aimageview[1] = (ImageView)findViewById(R.id.item2);
        aimageview[2] = (ImageView)findViewById(R.id.item3);
        aimageview[3] = (ImageView)findViewById(R.id.item4);
        mItemIcons = aimageview;
        Resources resources = mContext.getResources();
        mPreviewIconContainer = (LinearLayout)findViewById(R.id.preview_icons_container);
        mCover = (ImageView)findViewById(R.id.folder_cover);
        int j = ResConfig.getIconWidth();
        int i = ResConfig.getIconHeight();
        Bitmap bitmap1 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder_icon_cover_01);
        
        if (bitmap1 != null)
        {
            mCover.setVisibility(0);
            mCover.setImageBitmap(bitmap1);
            Bitmap bitmap2 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder_icon_cover_02);
            Bitmap bitmap3 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder_icon_cover_03);
            Bitmap bitmap4 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder_icon_cover_04);
            Bitmap bitmap5 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder_icon_cover_05);
            Bitmap bitmap6 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder_icon_cover_06);
            j = resources.getInteger(R.integer.config_folder_animation_duration);
            mCoverOpen = new AnimationDrawable();
            mCoverOpen.setOneShot(true);
            mCoverOpen.addFrame(new BitmapDrawable(resources, bitmap1), j);
            mCoverOpen.addFrame(new BitmapDrawable(resources, bitmap2), j);
            mCoverOpen.addFrame(new BitmapDrawable(resources, bitmap3), j);
            mCoverOpen.addFrame(new BitmapDrawable(resources, bitmap4), j);
            mCoverOpen.addFrame(new BitmapDrawable(resources, bitmap5), j);
            mCoverOpen.addFrame(new BitmapDrawable(resources, bitmap6), j);
            mCoverClose = new AnimationDrawable();
            mCoverClose.setOneShot(true);
            mCoverClose.addFrame(new BitmapDrawable(resources, bitmap6), j);
            mCoverClose.addFrame(new BitmapDrawable(resources, bitmap5), j);
            mCoverClose.addFrame(new BitmapDrawable(resources, bitmap4), j);
            mCoverClose.addFrame(new BitmapDrawable(resources, bitmap3), j);
            mCoverClose.addFrame(new BitmapDrawable(resources, bitmap2), j);
            mCoverClose.addFrame(new BitmapDrawable(resources, bitmap1), j);
            mCoverHalfOpen = new AnimationDrawable();
            mCoverHalfOpen.setOneShot(true);
            mCoverHalfOpen.addFrame(new BitmapDrawable(resources, bitmap1), j);
            mCoverHalfOpen.addFrame(new BitmapDrawable(resources, bitmap2), j);
            mCoverHalfOpen.addFrame(new BitmapDrawable(resources, bitmap3), j);
            mCoverHalfOpen.addFrame(new BitmapDrawable(resources, bitmap4), j);
            mCoverHalfClose = new AnimationDrawable();
            mCoverHalfClose.setOneShot(true);
            mCoverHalfClose.addFrame(new BitmapDrawable(resources, bitmap4), j);
            mCoverHalfClose.addFrame(new BitmapDrawable(resources, bitmap3), j);
            mCoverHalfClose.addFrame(new BitmapDrawable(resources, bitmap2), j);
            mCoverHalfClose.addFrame(new BitmapDrawable(resources, bitmap1), j);
        }
        Bitmap bitmapFolder = loadFolderIconBitmap(mContext);
        if (bitmapFolder == null) {
            mFolderBackground = new BitmapDrawable(resources, 
                    Utilities.createIconBitmap(resources.getDrawable(R.drawable.icon_folder), mContext));
        } else {
            mFolderBackground = new BitmapDrawable(resources, bitmapFolder);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int k = mIconContainer.getMeasuredWidth();
        int l = (int)(0.76F * (float)k);
        FrameLayout.LayoutParams layoutparams1 = (FrameLayout.LayoutParams)mIconContainer.getLayoutParams();
        FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams)mPreviewIconContainer.getLayoutParams();
        mPreviewIconContainer.measure(View.MeasureSpec.makeMeasureSpec(l, MeasureSpec.EXACTLY), 
                View.MeasureSpec.makeMeasureSpec(l, MeasureSpec.EXACTLY));
        if (!isCompact()) {
            layoutparams.gravity = 49;
            layoutparams.setMargins(0, layoutparams1.topMargin + (k - l) / 2, 0, 0);
        } else {
            layoutparams.gravity = 17;
            layoutparams.setMargins(0, 0, 0, 0);
        }
        mPreviewIconContainer.setLayoutParams(layoutparams);
        if (mCover.getVisibility() == View.VISIBLE){
            setupIconMargin(mCover);
        }
    }
    
    public void onOpen() {
        if (mCoverOpen != null) {
            mCoverOpen.unscheduleSelf(null);
            mCover.setImageDrawable(mCoverOpen);
            mCoverOpen.start();
        }
    }
}