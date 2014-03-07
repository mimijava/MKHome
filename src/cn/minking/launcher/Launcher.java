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
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;

public class Launcher extends Activity implements OnClickListener,
        OnLongClickListener, LauncherModel.Callbacks{
    /******* 常量 ********/
    static final private String LTAG = "ZwLauncher";
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

    /// M: 跟踪用户是否离开Launcher的行为状态
    private static boolean sPausedFromUserAction = false;
    
    /******* 桌面内容  ********/
    private DragLayer mDragLayer;
    private DragController mDragController;
    private Workspace mWorkspace;
    
    // HOTSEAT区域
    private HotSeats mHotSeats;
    
    // 桌面背景
    private Background mDragLayerBackground;
    private Point mTmpPoint = new Point();
    
    /// M: 静态变量标识本地信息是否变更
    private static boolean sLocaleChanged = false;
    
    /******* 数据 ********/
    private LauncherModel mModel;

    private static HashMap mFolders = new HashMap();
    private IconCache mIconCache;

    // 桌面内容
    private ArrayList<ItemInfo> mDesktopItems = new ArrayList();

    /******* 其他 ********/
    
    // 桌面错误是打印消息
    static final ArrayList<String> sDumpLogs = new ArrayList<String>();
    
    
    public Launcher(){
        mWorkspaceLoading = true;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LTAG, "onCreate");
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
        //contentResolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true, mWidgetObserver);
        //contentResolver.registerContentObserver(LauncherSettings.Screens.CONTENT_URI, true, mScreenChangeObserver);
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
        // Disconnect any of the callbacks and drawables associated with ItemInfos on the workspace
        // to prevent leaking Launcher activities on orientation change.
        if (mModel != null) {
            mModel.unbindItemInfosAndClearQueuedBindRunnables();
        }
        mWorkspace.onDestory();
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
    
    @Override
    public void startActivity(Intent intent, Bundle options) {
        super.startActivity(intent);
    }

    private void setupViews() {
        // Drag 控制器
        DragController dragController = mDragController;
        
        // Drag 层
        mDragLayer = (DragLayer)findViewById(R.id.drag_layer);
        mDragLayerBackground = (Background)findViewById(R.id.drag_layer_background);
        mDragLayer.setDragController(dragController);
        mDragLayer.setLauncher(this);
        
        // HOTSEAT
        mHotSeats = (HotSeats)mDragLayer.findViewById(R.id.hot_seats);
        mHotSeats.setLauncher(this);
        mHotSeats.setDragController(dragController);
        
        mWorkspace = (Workspace)mDragLayer.findViewById(R.id.workspace);
        Workspace workspace = mWorkspace;
        workspace.setHapticFeedbackEnabled(false);
        workspace.setOnLongClickListener(this);
        workspace.setDragController(dragController);
        workspace.setLauncher(this);
    }

    public IconCache getIconCache(){
        return mIconCache;
    }
    
    public void bindAppMessage(ShortcutIcon shortcuticon, ComponentName componentname){
        //mApplicationsMessage.addApplication(shortcuticon, componentname);
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
                //mApplicationsMessage.updateFolderMessage(folderinfo);
            }
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
    public void bindItems(ArrayList<ItemInfo> arraylist, int i, int j) {
        if (mWorkspace == null) return;
        int k = i;
        while (k < j) {
            ItemInfo iteminfo = arraylist.get(k);
            
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
            k++;
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
        mWorkspaceLoading = false;
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
