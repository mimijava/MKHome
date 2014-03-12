package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    LauncherApplication.java
 * 创建时间：    2013-XX-XX
 * 描述：      MKHome的主入口， 装载Launcher.xml及LauncherModel的调用
 * 更新内容
 * ====================================================================================
 * 2014-02-18： 实现LauncherModel读取手机中的APP及WIDGET
 * ====================================================================================
 */
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import cn.minking.launcher.gadget.GadgetInfo;
import cn.minking.launcher.upsidescene.SceneData;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Point;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

public class Launcher extends Activity implements OnClickListener,
        OnLongClickListener, LauncherModel.Callbacks{
    
    private class WallpaperChangedIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mDragLayer.updateWallpaper();
        }
    }

    private class AppWidgetResetObserver extends ContentObserver {
        @Override
        public void onChange(boolean flag){
            onAppWidgetReset();
        }

        public AppWidgetResetObserver() {
            super(new Handler());
        }
    }

    private class ScreenOnOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
                LauncherModel.flashDelayedUpdateItemFlags(context);
            }
        }
    }

    class Padding {
        int bottom;
        int left;
        int right;
        int top;

        Padding() {
            left = 0;
            right = 0;
            top = 0;
            bottom = 0;
        }
    }

    private static class LocaleConfiguration {

        public String locale;
        public int mcc;
        public int mnc;

        private LocaleConfiguration() {
            mcc = -1;
            mnc = -1;
        }
    }
    
    /******* 常量 ********/
    static final private String TAG = "ZwLauncher";
    static final int APPWIDGET_HOST_ID = 1024;
    
    /******* 状态  ********/
    
    /// M: WORKSPACE是否处于装载
    private boolean mWorkspaceLoading;
    
    /// M: 用于强制重载WORKSPACE
    private boolean mIsLoadingWorkspace;
    
    private static boolean mIsHardwareAccelerated = false;

    private boolean mPaused = true;
    private boolean mRestoring = false;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
        
    /// M: 跟踪用户是否离开Launcher的行为状态
    private static boolean sPausedFromUserAction = false;
    
    /******* 桌面内容  ********/
    private DragLayer mDragLayer;
    private DragController mDragController;
    private Workspace mWorkspace;
    private ValueAnimator mDimAnim;
    private View mScreen;
    
    // HOTSEAT区域
    private HotSeats mHotSeats;
    private Animation mHotseatEditingEnter;
    private Animation mHotseatEditingExit;
    
    // 删除区
    private Animation mDeleteZoneEditingEnter;
    private Animation mDeleteZoneEditingExit;
    
    // WIDGET区域
    private Animation mWidgetEditingEnter;
    private Animation mWidgetEditingExit;
    
    // 桌面背景
    private Background mDragLayerBackground;
    private Point mTmpPoint = new Point();
    
    // 桌面缩略图
    private WorkspaceThumbnailView mWorkspacePreview;
    
    /// M: 静态变量标识本地信息是否变更
    private static boolean sLocaleChanged = false;
    
    /******* 数据 ********/
    private LauncherModel mModel;
    private LauncherAppWidgetHost mAppWidgetHost;
    private static HashMap mFolders = new HashMap();
    private IconCache mIconCache;
    private ItemInfo mLastAddInfo;
    private ApplicationsMessage mApplicationsMessage;
    
    // 桌面内容
    private ArrayList<ItemInfo> mDesktopItems = new ArrayList();

    /******* 其他 ********/
    
    // 桌面错误是打印消息
    static final ArrayList<String> sDumpLogs = new ArrayList<String>();
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();
    private final ContentObserver mScreenChangeObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean flag) {
            Log.d("Launcher", "onContentChange");
            mWorkspace.loadScreens(false);
            if (mLastAddInfo instanceof LauncherAppWidgetProviderInfo){
                addAppWidget((LauncherAppWidgetProviderInfo)mLastAddInfo);
            }
        }
    };
    
    public Launcher(){
        mWorkspaceLoading = true;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        Window localWindow = getWindow();
        localWindow.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        localWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //mIsHardwareAccelerated = ((Window)(localWindow)).getWindowManager().isHardwareAccelerated();
        LauncherApplication launcherApplication = (LauncherApplication)getApplication();
        
        // MODEL与 LAUNCHER APP 绑定
        mModel = launcherApplication.setLauncher(this);
        mIconCache = launcherApplication.getIconCache();

        // 分配拖动控制器 
        mDragController = new DragController(this);
        registerContentObservers();
        
        // 应用消息
        mApplicationsMessage = new ApplicationsMessage(this);
        
        setWallpaperDimension();
        
        
        mPaused = false;
        
        // 设置Launcher布局
        setContentView(R.layout.launcher);
        setupViews();
        
        if (!mRestoring) {
            /// M: 如果本地信息变更了，设置为重新装载所有信息
            if (sLocaleChanged) {
                mModel.resetLoadedState(true, true);
                sLocaleChanged = false;
            }
            mIsLoadingWorkspace = true;
            if (sPausedFromUserAction) {
                // 如果用户离开了launcher， 只需要在回到launcher的时候异步的完成装载
                mModel.startLoader(getApplicationContext(), true);
            } else {
                // 如果用户旋转屏幕或更改配置，则同步装载
                mModel.startLoader(getApplicationContext(), true);
            }
        }
    }

    private void registerContentObservers(){
        ContentResolver contentResolver = getContentResolver();
        contentResolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true, mWidgetObserver);
        contentResolver.registerContentObserver(LauncherSettings.Screens.CONTENT_URI, true, mScreenChangeObserver);
    }
    
    public static final boolean isHardwareAccelerated() {
        return mIsHardwareAccelerated;
    }
    
    private void setWallpaperDimension() {
        WallpaperManager wallpaperManager = (WallpaperManager)getSystemService("wallpaper");
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int width = 0;
        int height = 0;
        boolean bFlag = false;
        
        if (rotation != Surface.ROTATION_0 && rotation != Surface.ROTATION_180) {
            bFlag = false;
        }else {
            bFlag = true;
        }
        display.getSize(mTmpPoint);
        
        if (!bFlag) {
            width = mTmpPoint.y;
            height = mTmpPoint.x;
        }else {
            width = mTmpPoint.x;
            height = mTmpPoint.y;
        }
        wallpaperManager.suggestDesiredDimensions(width * 2, height);
    }
    
    private void setupAnimations(){
        mDeleteZoneEditingEnter = AnimationUtils.loadAnimation(this, R.anim.deletezone_editing_enter);
        mDeleteZoneEditingExit = AnimationUtils.loadAnimation(this, R.anim.deletezone_editing_exit);
        mHotseatEditingEnter = AnimationUtils.loadAnimation(this, R.anim.hotseat_editing_enter);
        mHotseatEditingExit = AnimationUtils.loadAnimation(this, R.anim.hotseat_editing_exit);
        mWidgetEditingEnter = AnimationUtils.loadAnimation(this, R.anim.widget_editing_enter);
        mWidgetEditingExit = AnimationUtils.loadAnimation(this, R.anim.widget_editing_exit);
        mDimAnim = new ValueAnimator();
        mDimAnim.setInterpolator(new LinearInterpolator());
        mDimAnim.setDuration(getResources().getInteger(R.integer.config_animDuration));
        mDimAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueanimator) {
                  float f = ((Float)valueanimator.getAnimatedValue()).floatValue();
                  mScreen.setAlpha(f);
            }
        });

    }
    
    private void onAppWidgetReset() {
        mAppWidgetHost.startListening();
    }
    
    /**
     * 功能： 滑动更新墙纸
     */
    public void updateWallpaperOffset(float xStep, float yStep, float xOffset, float yOffset){
        mDragLayer.updateWallpaperOffset(xStep, yStep, xOffset, yOffset);
    }
    
    /**
     * 功能： 滑动更新墙纸动画
     */
    public void updateWallpaperOffsetAnimate(float xStep, float yStep, float xOffset, float yOffset){
        mDragLayer.updateWallpaperOffsetAnimate(xStep, yStep, xOffset, yOffset);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        
        mPaused = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 显示桌面背景
        mDragLayer.updateWallpaper();        
        mPaused = false;
        
        sPausedFromUserAction = false;
        
    }

    @Override
    protected void onStart() {
        super.onStart();
        mWorkspace.onStart();
        mApplicationsMessage.requestUpdateMessages();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sPausedFromUserAction = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDragLayer.updateWallpaperOffset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mModel.stopLoaderAsync();
        LauncherApplication launcherapplication = (LauncherApplication)getApplication();
        mModel.stopLoader();
        launcherapplication.setLauncher(null);
        unbindDesktopItems();
        
        mWorkspace.onDestory();
        mApplicationsMessage.destory();
        finishLoading();
    }

    @Override
    public boolean onLongClick(View paramView) {
        return false;
    }

    @Override
    public void onClick(View paramView) {
        // TODO Auto-generated method stub

    }
    
    public void startActivityEx(Intent intent, Bundle options) {
        startActivity(intent, options);
    }
    
    public void startActivity(Intent intent, Object obj) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (obj instanceof ShortcutInfo) {
                ShortcutInfo shortcutinfo = (ShortcutInfo)obj;
                shortcutinfo.onLaunch();
                LauncherModel.updateItemFlagsInDatabaseDelayed(this, shortcutinfo);
                mApplicationsMessage.onLaunchApplication(intent.getComponent());
            }
            startActivity(intent);
        } catch (NullPointerException e) {
            Toast.makeText(this, R.string.start_activity_failed, 0).show();
            Log.e(TAG, (new StringBuilder()).append("Launcher cannot start this activity(app2sd?)tag=").
                    append(obj).append(" intent=").append(intent).toString(), e);
        }
    }

    private void setupViews() {
        // Drag 控制器
        DragController dragController = mDragController;
        
        // Drag 层
        mDragLayer = (DragLayer)findViewById(R.id.drag_layer);
        mDragLayerBackground = (Background)findViewById(R.id.drag_layer_background);
        mDragLayer.setDragController(dragController);
        mDragLayer.setLauncher(this);
        
        mScreen = findViewById(R.id.screen);
        
        mWorkspace = (Workspace)mDragLayer.findViewById(R.id.workspace);
        Workspace workspace = mWorkspace;
        
        mWorkspacePreview = (WorkspaceThumbnailView)mDragLayer.findViewById(R.id.workspace_preview);
        workspace.setHapticFeedbackEnabled(false);
        workspace.setOnLongClickListener(this);
        workspace.setDragController(dragController);
        workspace.setLauncher(this);
        workspace.setThumbnailView(mWorkspacePreview);
        
        // HOTSEAT
        mHotSeats = (HotSeats)mDragLayer.findViewById(R.id.hot_seats);
        mHotSeats.setLauncher(this);
        mHotSeats.setDragController(dragController);
        
        setupAnimations();
    }

    public IconCache getIconCache(){
        return mIconCache;
    }
    
    int addAppWidget(LauncherAppWidgetProviderInfo launcherappwidgetproviderinfo){
        return 0;
    }
    
    public void bindAppMessage(ShortcutIcon shortcuticon, ComponentName componentname){
        mApplicationsMessage.addApplication(shortcuticon, componentname);
    }
    
    public Workspace getWorkspace() {
        return mWorkspace;
    }
    
    /**
     * 功能：  获取Drag控制器
     * @return
     */
    public DragController getDragController(){
        return mDragController;
    }
    
    /**
     * 功能：  获取拖动的图层
     * @return
     */
    public DragLayer getDragLayer(){
        return mDragLayer;
    }
    private FolderIcon createFolderIcon(ViewGroup viewgroup, FolderInfo folderinfo){
        return FolderIcon.fromXml(R.layout.folder_icon, this, viewgroup, folderinfo);
    }

    public FolderIcon getFolderIcon(FolderInfo folderinfo){
        FolderIcon foldericon = null;
        if (folderinfo != null)
            if (folderinfo.container != LauncherSettings.Favorites.CONTAINER_DESKTOP)
            {
                if (folderinfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT)
                    foldericon = (FolderIcon)mHotSeats.getItemIcon(folderinfo);
            } else {
                foldericon = (FolderIcon)mWorkspace.findViewWithTag(folderinfo);
            }
        return foldericon;
    }
    
    /**
     * 功能： 以Layout application为样式创建快捷方式的布局
     * @param viewgroup
     * @param shortcutinfo
     * @return
     */
    private ShortcutIcon createShortcutIcon(ViewGroup viewgroup, ShortcutInfo shortcutinfo){
        return ShortcutIcon.fromXml(R.layout.application, this, viewgroup, shortcutinfo);
    }
    
    /**
     * 功能： 创建ITEM图标
     * @param viewgroup
     * @param iteminfo
     * @return
     */
    public ItemIcon createItemIcon(ViewGroup viewgroup, ItemInfo iteminfo){
        ItemIcon itemIcon;
        if (iteminfo instanceof FolderInfo) {
            itemIcon = createFolderIcon(viewgroup, (FolderInfo)iteminfo);
        }else {
            itemIcon = createShortcutIcon(viewgroup, (ShortcutInfo)iteminfo);
        }
        if (itemIcon != null) {
            itemIcon.setOnClickListener(this);
        }
        return itemIcon;
    }
    
    /**
     * 功能：  将各ITEM添加至相应的位置
     * @param iteminfo
     * @param flag
     */
    public void addItem(ItemInfo iteminfo, boolean flag){
        if (iteminfo.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT){
            // 文件夹
            if (iteminfo instanceof FolderInfo){
                mWorkspace.addInScreen(createItemIcon(mWorkspace.getCurrentCellLayout(), 
                        iteminfo), iteminfo.screenId, iteminfo.cellX, iteminfo.cellY, 1, 1, false);
            } else {
                // 快捷方式
                addShortcut((ShortcutInfo)iteminfo, flag);
            }
        } else {
            // 添加HOTSEAT
            mHotSeats.pushItem(iteminfo);
        }
    }
    
    public FolderIcon getParentFolderIcon(ShortcutInfo shortcutinfo){
        return getFolderIcon(getParentFolderInfo(shortcutinfo));
    }
    
    public FolderInfo getParentFolderInfo(ShortcutInfo shortcutinfo){
        FolderInfo folderinfo;
        if (shortcutinfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP 
                || shortcutinfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
            folderinfo = null;
        } else {
            folderinfo = (FolderInfo)mFolders.get(Long.valueOf(shortcutinfo.container));
        }
        return folderinfo;
    }
    
    void addShortcut(ShortcutInfo shortcutinfo){
        addShortcut(shortcutinfo, false);
    }

    void addShortcut(ShortcutInfo shortcutinfo, boolean flag){
        if (getParentFolderIcon(shortcutinfo) == null){
            //mWorkspace.addInScreen(createItemIcon(mWorkspace.getCurrentCellLayout(), shortcutinfo), shortcutinfo.screenId, shortcutinfo.cellX, shortcutinfo.cellY, 1, 1, flag);
        } else {
            FolderInfo folderinfo = getParentFolderInfo(shortcutinfo);
            if (folderinfo == null || !(folderinfo instanceof FolderInfo)){
                Log.e("Launcher", (new StringBuilder()).append("Can't find user folder of id ").append(shortcutinfo.container).toString());
            } else {
                folderinfo.add(shortcutinfo);
                folderinfo.notifyDataSetChanged();
                mApplicationsMessage.updateFolderMessage(folderinfo);
            }
        }
    }
    
    public boolean isWorkspaceLocked() {
        boolean flag;
        if (!mWorkspaceLoading && !mWaitingForResult){
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    public void showPreview(final boolean show){
        if (!isWorkspaceLocked()){
            if (show) {
                mDragLayerBackground.setEnterPreviewMode();
                mHotSeats.setVisibility(View.INVISIBLE);
                mHotSeats.startAnimation(mHotseatEditingEnter);
            } else {
                mDragLayerBackground.setExitPreviewMode();
                mHotSeats.setVisibility(View.VISIBLE);
                mHotSeats.startAnimation(mHotseatEditingExit);
            }
            mWorkspace.showPreview(show);
        }
    }
    
    public boolean isInEditing(){
        boolean flag = false;
        
        return flag;
    }
    
    private void unbindDesktopItems() {
        Iterator<ItemInfo> iterator = mDesktopItems.iterator();
        while(iterator.hasNext()){
            iterator.next().unbind();
        }
    }
    
    @Override
    public void bindAppWidget(LauncherAppWidgetInfo launcherappwidgetinfo) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bindAppsAdded(final ArrayList<ShortcutInfo> arraylist) {
        mWorkspace.post(new Runnable() {
            final ArrayList<ShortcutInfo> apps = arraylist;
            @Override
            public void run() {
                Iterator<ShortcutInfo> iterator = apps.iterator();
                while (iterator.hasNext()) {
                    ShortcutInfo shortcutInfo = iterator.next();
                    addItem(shortcutInfo, false);
                }
            }
        });
    }

    @Override
    public void bindAppsRemoved(ArrayList arraylist) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bindFolders(HashMap hashmap) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bindGadget(GadgetInfo gadgetinfo) {
        // TODO Auto-generated method stub
        
    }

    /**
     * 功能： 将读取的项放入到桌面及放入数据库中存储
     */
    @Override
    public void bindItems(ArrayList<ItemInfo> arraylist, int start, int end) {
        if (mWorkspace == null) return;
        
        for (int i = start; i < end; i++) {
            ItemInfo iteminfo = arraylist.get(i);
            
            if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                mDesktopItems.add(iteminfo);
            }
            switch (iteminfo.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION: // APP
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT: // 快捷方式
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER: // 文件夹
                addItem(iteminfo, false);
                break;
            }
        }
        mWorkspace.requestLayout();
    }

    @Override
    public void bindUpsideScene(SceneData scenedata) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isAllAppsVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void finishBindingMissingItems() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void finishBindingSavedItems() {
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()){
                mWorkspace.getCurrentScreen().requestFocus();
            }
            mSavedState = null;
        }
        if (mSavedInstanceState != null) {
            super.onRestoreInstanceState(mSavedInstanceState);
            mSavedInstanceState = null;
        }
        mWorkspaceLoading = false;
        mApplicationsMessage.requestUpdateMessages();
        mHotSeats.finishBinding();
    }

    @Override
    public void finishLoading() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getCurrentWorkspaceScreen() {
        int curScreen;
        if (mWorkspace == null){
            curScreen = -1;
        } else {
            curScreen = mWorkspace.getCurrentScreenIndex();
        }
        return curScreen;
    }

    @Override
    public void reloadWidgetPreview() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void startBinding() {
        mHotSeats.startBinding();
    }

    @Override
    public void startLoading() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(" ");
        writer.println("Debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            writer.println("  " + sDumpLogs.get(i));
        }
    }
}
