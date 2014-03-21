package cn.minking.launcher;

/**
 * 作者：      minking
 * 文件名称:    DeleteZone.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建桌面的删除（DeleteZone）区域， DeleteZone采用动画显示
 * ====================================================================================
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class DeleteZone extends FrameLayout 
    implements Animation.AnimationListener{
    private TextView mEditingTips;
    private boolean mErrorShowing;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private Launcher mLauncher;
    private RetainedList mRetainedList;
    private Animation mShrinkToTop;
    private Animation mStretchFromTop;
    private TransitionDrawable mTransition;
    private ImageView mTrashIcon;
    private final Paint mTrashPaint;
    private Context mContext;

    public DeleteZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mTrashPaint = new Paint();
        mErrorShowing = false;
        mRetainedList = new RetainedList();
        int j = context.getResources().getColor(R.color.delete_color_filter);
        mTrashPaint.setColorFilter(new PorterDuffColorFilter(j, android.graphics.PorterDuff.Mode.SRC_ATOP));
        mTrashPaint.setAlpha(context.getResources().getInteger(R.integer.delete_color_alpha));
        setAnimationCacheEnabled(false);
    }

    public DeleteZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    private void deletePackage(String s, ShortcutInfo shortcutinfo) {
        //mContext.getPackageManager().deletePackage(s, new DeleteObserver(shortcutinfo), 0);
    }

    private boolean isSystemPackage(String s) {
        boolean flag = false;
        try {
            int i = mContext.getPackageManager().getApplicationInfo(s, 0).flags;
            if ((i & 1) != 0){
                flag = true;
            }
        } catch (Exception e) {  }
        
        return flag;
    }

    private void removeItem(DropTarget.DragObject dragobject, boolean flag) {
        if (dragobject.dragSource instanceof Folder) {
            ((Folder)dragobject.dragSource).removeItem((ShortcutInfo)dragobject.dragInfo);
        }
        if (flag) {
            LauncherModel.deleteItemFromDatabase(mLauncher, dragobject.dragInfo);
        }
    }

    private void startUninstallDialog(final ShortcutInfo info)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(new FastBitmapDrawable(info.getIcon(mLauncher.getIconCache())));
        builder.setTitle(info.title);
        
        Context context = mContext;
        Object aobj[] = new Object[]{info.title};
        
        builder.setMessage(context.getString(R.string.uninstall_body_format, aobj));
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialoginterface) {
                mLauncher.addItem(info, false);
            }

        });
        
        builder.setNegativeButton(mContext.getString(R.string.cancel_action), 
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialoginterface, int i) {
                mLauncher.addItem(info, false);
            }
        });
        builder.setPositiveButton(mContext.getString(R.string.uninstall), 
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialoginterface, int i) {
                deletePackage(info.intent.getComponent().getPackageName(), info);
            }
        });
        builder.create().show();
    }

    private void startUninstallPresetDialog(final DropTarget.DragObject d) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final ShortcutInfo info = (ShortcutInfo)d.dragInfo;
        builder.setIcon(new FastBitmapDrawable(info.getIcon(mLauncher.getIconCache())));
        builder.setTitle(info.title);
        Context context = mContext;
        Object aobj[] = new Object[]{info.title};
        
        builder.setMessage(context.getString(R.string.uninstall_body_format, aobj));
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialoginterface) {
                removeItem(d, false);
                mLauncher.addItem(info, false);
            }
        });
        
        builder.setNegativeButton(mContext.getString(R.string.cancel_action), 
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialoginterface, int i) {
                removeItem(d, false);
                mLauncher.addItem(info, false);
            }
        });
        
        builder.setPositiveButton(mContext.getString(R.string.uninstall), 
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialoginterface, int i) {
                removeItem(d, true);
            }
        });
        builder.create().show();
    }

    private void startUninstallWidgetDialog(final String packageName) {
        PackageManager packageManager = mContext.getPackageManager();;
        try {
            ApplicationInfo applicationinfo = packageManager.getApplicationInfo(packageName, 0);
            if (applicationinfo != null) {
                CharSequence charsequence = applicationinfo.loadLabel(packageManager);
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setIcon(applicationinfo.loadIcon(packageManager));
                builder.setTitle(charsequence);
                Context context = mContext;
                Object aobj[] = new Object[]{charsequence};
                
                builder.setMessage(context.getString(R.string.uninstall_body_format, aobj));
                builder.setCancelable(true);
                builder.setNegativeButton(mContext.getString(R.string.cancel_action), null);
                builder.setPositiveButton(mContext.getString(R.string.uninstall), 
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialoginterface, int i) {
                        deletePackage(packageName, null);
                    }

                });
                builder.create().show();
            }
        } catch (NameNotFoundException e) { }
        return;
    }

    public boolean acceptDrop(DropTarget.DragObject dragobject) {
        boolean flag;
        if (dragobject.dragInfo.container == -1L && dragobject.dragInfo.itemType == 2) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }

    public DropTarget getDropTargetDelegate(DropTarget.DragObject dragobject) {
        return null;
    }

    public boolean isDropEnabled() {
        return true;
    }

    public void onAnimationEnd(Animation animation) {
        mEditingTips.setVisibility(View.VISIBLE);
    }

    public void onAnimationRepeat(Animation animation)
    {
    }

    public void onAnimationStart(Animation animation)
    {
    }

    public void onDragEnd() {
        if (mLauncher.getEditingState() != 8) {
            mEditingTips.setVisibility(View.INVISIBLE);
        } else {
            Animation animation = mFadeIn;
            long l;
            if (!mErrorShowing){
                l = 0L;
            } else {
                l = getContext().getResources().getInteger(R.integer.error_notification_duration);
            }
            animation.setStartOffset(l);
            mErrorShowing = false;
            mEditingTips.startAnimation(mFadeIn);
            mEditingTips.setVisibility(View.VISIBLE);
        }
        mTrashIcon.startAnimation(mShrinkToTop);
        mTrashIcon.setVisibility(View.INVISIBLE);
    }

    public void onDragEnter(DropTarget.DragObject dragobject) {
        if (acceptDrop(dragobject)) {
            mTrashIcon.setImageResource(R.drawable.delete_zone_in);
            ((AnimationDrawable)mTrashIcon.getDrawable()).start();
            mTransition.reverseTransition(250);
            dragobject.dragView.setPaint(mTrashPaint);
        }
    }

    public void onDragExit(DropTarget.DragObject dragobject) {
        if (acceptDrop(dragobject)) {
            mTrashIcon.setImageResource(R.drawable.delete_zone_out);
            ((AnimationDrawable)mTrashIcon.getDrawable()).start();
            mTransition.reverseTransition(250);
            dragobject.dragView.setPaint(null);
        }
    }

    public void onDragOver(DropTarget.DragObject dragobject) {
    }

    public void onDragStart(DragSource dragsource, ItemInfo iteminfo, int i) {
        if (mLauncher.getEditingState() != 8) {
            mEditingTips.setVisibility(View.INVISIBLE);
        } else {
            mEditingTips.startAnimation(mFadeOut);
            mEditingTips.setVisibility(View.INVISIBLE);
        }
        mTrashIcon.startAnimation(mStretchFromTop);
        mTrashIcon.setVisibility(View.VISIBLE);
        mTransition.resetTransition();
    }

    public boolean onDrop(DropTarget.DragObject dragobject) {
        boolean flag = false;
        if (!dragobject.dragInfo.isRetained 
                && (!(dragobject.dragInfo instanceof ShortcutInfo) 
                        || !mRetainedList.contain(((ShortcutInfo)dragobject.dragInfo).intent))) {
            if (dragobject.dragInfo.container != -1L || dragobject.dragInfo.itemType == 6) {
                if (dragobject.dragInfo.container == -100L && (dragobject.dragInfo instanceof LauncherAppWidgetInfo)){
                    mLauncher.removeAppWidget((LauncherAppWidgetInfo)dragobject.dragInfo);
                }
                if (dragobject.dragInfo.itemType != 2) {
                    if (dragobject.dragInfo.itemType == 4) {
                        LauncherAppWidgetInfo launcherappwidgetinfo = (LauncherAppWidgetInfo)dragobject.dragInfo;
                        LauncherAppWidgetHost launcherappwidgethost = mLauncher.getAppWidgetHost();
                        if (launcherappwidgethost != null){
                            launcherappwidgethost.deleteAppWidgetId(launcherappwidgetinfo.appWidgetId);
                        }
                    }
                } else {
                    FolderInfo folderInfo = (FolderInfo)dragobject.dragInfo;
                    if (folderInfo.count() != 0) {
                        mLauncher.showError(R.string.cant_remove_folder);
                        flag = false;
                    }
                    LauncherModel.deleteUserFolderContentsFromDatabase(mLauncher, folderInfo);
                    mLauncher.removeFolder(folderInfo);
                }
                if (dragobject.dragInfo.itemType != 0) {
                    if (dragobject.dragInfo.itemType != 6) {
                        if (dragobject.dragInfo.itemType == 5){
                            mLauncher.removeGadget(dragobject.dragInfo);
                        }
                        if (!dragobject.dragInfo.isPresetApp()){
                            removeItem(dragobject, true);
                        } else {
                            startUninstallPresetDialog(dragobject);
                        }
                    } else {
                        String sInfo = ((LauncherAppWidgetProviderInfo)dragobject.dragInfo).providerInfo.provider.getPackageName();
                        if (!isSystemPackage(sInfo)) {
                            startUninstallWidgetDialog(sInfo);
                        } else {
                            mLauncher.showError(R.string.cant_remove_system_app);
                            flag = false;
                        }
                    }
                } else {
                    ShortcutInfo sInfo = (ShortcutInfo)dragobject.dragInfo;
                    if (sInfo.intent != null && sInfo.intent.getComponent() != null) {
                        if (mContext.getPackageManager().resolveActivity(sInfo.intent, 0) != null) {
                            if (!isSystemPackage(sInfo.intent.getComponent().getPackageName())) {
                                removeItem(dragobject, false);
                                startUninstallDialog(sInfo);
                            } else {
                                mLauncher.showError(R.string.cant_remove_system_app);
                                flag = false;
                            }
                        } else {
                            removeItem(dragobject, true);
                        }
                    } else {
                        LauncherModel.deleteItemFromDatabase(mLauncher, dragobject.dragInfo);
                    }
                }
                if (flag) {
                    flag = true;
                } else {
                    mLauncher.showError(R.string.failed_to_delete_temporary);
                    flag = false;
                }
            } else {
                flag = true;
            }
        } else {
            mLauncher.showError(R.string.cant_remove_retained_app);
            flag = false;
        }
        return flag;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mTrashIcon = (ImageView)findViewById(R.id.trash);
        mEditingTips = (TextView)findViewById(R.id.editing_tips);
        mEditingTips.setDrawingCacheEnabled(true);
        mFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        mFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        mShrinkToTop = AnimationUtils.loadAnimation(getContext(), R.anim.shrink_to_top);
        mShrinkToTop.setAnimationListener(this);
        mStretchFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.stretch_from_top);
        mTransition = (TransitionDrawable)mTrashIcon.getBackground();
    }

    
    public void onShowError() {
        mErrorShowing = true;
    }
    
    void setDragController(DragController dragcontroller) {
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }
}