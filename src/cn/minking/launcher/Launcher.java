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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import cn.minking.launcher.AllAppsList.RemoveInfo;
import cn.minking.launcher.gadget.Gadget;
import cn.minking.launcher.gadget.GadgetInfo;
import cn.minking.launcher.upsidescene.SceneData;
import cn.minking.launcher.upsidescene.SceneScreen;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
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
    
    // 桌面加载延时弹出框
    private ProgressDialog mLoadingProgressDialog;
    
    /// M: WORKSPACE是否处于装载
    private boolean mWorkspaceLoading;
    
    /// M: 用于强制重载WORKSPACE
    private boolean mIsLoadingWorkspace;
    
    private boolean mOnResumeExpectedForActivityResult;
    
    private static boolean mIsHardwareAccelerated = false;

    private boolean mRestoring = false;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
    
    private int mEditingState;
        
    private boolean mSceneAnimating;
    
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
    
    private FolderCling mFolderCling;
    private SceneScreen mSceneScreen;
    private View mPositionSnap;
    private SceneData mUpsideScene;
    
    // 桌面背景
    private Background mDragLayerBackground;
    private Point mTmpPoint = new Point();
    
    // 桌面缩略图
    private WorkspaceThumbnailView mWorkspacePreview;
    
    private WidgetThumbnailView mWidgetThumbnailView;
    
    private GuidePopupWindow mEditingGuideWindow;
    
    private ErrorBar mErrorBar;
    private DeleteZone mDeleteZone;
    
    /// M: 静态变量标识本地信息是否变更
    private static boolean sLocaleChanged = false;
    
    /******* 数据 ********/
    private LauncherModel mModel;
    private LauncherAppWidgetHost mAppWidgetHost;
    private static HashMap<Long, FolderInfo> mFolders = new HashMap<Long, FolderInfo>();
    private IconCache mIconCache;
    private ItemInfo mLastAddInfo;
    private ApplicationsMessage mApplicationsMessage;
    
    // 桌面内容
    private ArrayList<ItemInfo> mDesktopItems = new ArrayList<ItemInfo>();
    public ArrayList<Gadget> mGadgets;

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
        mPositionSnap = null;
        mEditingState = 7;
        mLoadingProgressDialog = null;
        mOnResumeExpectedForActivityResult = false;
    }
    
    private boolean acceptFilter() {
        boolean flag;
        if (((InputMethodManager)getSystemService("input_method")).isFullscreenMode()){
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    /**
     * 功能： SIM卡的本地信息是否改变
     * @return
     */
    private boolean checkForLocaleChange() {
        boolean flag = true;
        LocaleConfiguration localeconfiguration = new LocaleConfiguration();
        readConfiguration(this, localeconfiguration);
        Configuration configuration = getResources().getConfiguration();
        String loc_loc = localeconfiguration.locale;
        String con_loc = configuration.locale.toString();
        int loc_mcc = localeconfiguration.mcc;
        int loc_mnc = localeconfiguration.mnc;
        int con_mcc = configuration.mcc;
        int con_mnc = configuration.mnc;
        boolean bChange;
        if (con_loc.equals(loc_loc) 
            && loc_mnc == loc_mcc 
            && con_mnc == con_mcc){
            bChange = false;
        } else {
            bChange = true;
        }
        
        // 如果改变则把最新的信息写入到配置文件中
        if (!bChange) {
            flag = false;
        } else {
            localeconfiguration.locale = con_loc;
            localeconfiguration.mcc = con_mcc;
            localeconfiguration.mnc = con_mnc;
            writeConfiguration(this, localeconfiguration);
        }
        return flag;
    }
    
    /**
     * 描述： 从文件中读取SIM卡的本地信息
     * @param context
     * @param localeconfiguration
     */
    private static void readConfiguration(Context context, LocaleConfiguration localeconfiguration) {
        DataInputStream datainputstream = null;
        try {
            datainputstream = new DataInputStream(context.openFileInput("launcher.preferences"));
            localeconfiguration.locale = datainputstream.readUTF();
            localeconfiguration.mcc = datainputstream.readInt();
            localeconfiguration.mnc = datainputstream.readInt();
            
            datainputstream.close();
        } catch (IOException e) {
            if (datainputstream != null) {
                try {
                    datainputstream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        }
    }
    
    /**
     * 描述： 将SIM卡的信息写入文件
     * @param context
     * @param localeconfiguration
     */
    private static void writeConfiguration(Context context, LocaleConfiguration localeconfiguration) {
        DataOutputStream dataoutputstream = null;
        try {
            dataoutputstream = new DataOutputStream(context.openFileOutput("launcher.preferences", 0));
            dataoutputstream.writeUTF(localeconfiguration.locale);
            dataoutputstream.writeInt(localeconfiguration.mcc);
            dataoutputstream.writeInt(localeconfiguration.mnc);
            dataoutputstream.flush();
            
            dataoutputstream.close();
        } catch (IOException e) {
            if (dataoutputstream != null) {
                try {
                    dataoutputstream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        Window localWindow = getWindow();
        localWindow.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        // 是否全屏显示，不带状态栏
        //localWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
    
    /**
     * 描述： 配置桌面用到的各类动画效果
     */
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
        
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWorkspace.onResume();
        closeFolder();
        mDragLayer.clearAllResizeFrames();
        if (!mOnResumeExpectedForActivityResult && isInEditing()){
            setEditingState(7);
        }
        mOnResumeExpectedForActivityResult = false;
        // 显示桌面背景
        mDragLayer.updateWallpaper();
        
        sPausedFromUserAction = false;
        scrollToDefault();
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
    public boolean onLongClick(View view) {
        boolean flag;
        if (!isWorkspaceLocked()) {
            if (!(view instanceof CellLayout)){
                view = (View)view.getParent();
            }
            CellLayout.CellInfo cellinfo = (CellLayout.CellInfo)view.getTag();
            if (cellinfo != null) {
                if (mWorkspace.allowLongPress()){
                    if (cellinfo.cell != null) {
                        if (!(cellinfo.cell instanceof Folder)) {
                            mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, 
                                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                            mWorkspace.startDrag(cellinfo);
                        }
                    } else {
                        int editState;
                        if (!isInEditing()) {
                            editState = 8;
                        } else {
                            editState = 7;
                        }
                        setEditingState(editState);
                        mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, 
                                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    }
                }
                flag = true;
            } else {
                flag = true;
            }
        } else {
            flag = false;
        }
        return flag;
    }

    @Override
    public void onClick(View view) {
//        Object object = view.getTag();
//        if ((object instanceof FolderInfo)) {
//          openFolder((FolderInfo)object, view);
//      } else {
//          if (!isInEditing()) {
//              ShortcutInfo shortcutinfo = (ShortcutInfo)object;
//              if (!shortcutinfo.isPresetApp()) {
//                  if (shortcutinfo.mIconType != 3/*LauncherSettings.Favorites.ICON_TYPE_RESOURCE*/) {
//                      Intent intent = new Intent(shortcutinfo.intent);
//                      int loc[] = new int[2];
//                      view.getLocationOnScreen(loc);
//                      intent.setSourceBounds(new Rect(loc[0], loc[1], 
//                              loc[0] + view.getWidth(), loc[1] + view.getHeight()));
//                      startActivity(intent, object);
//                      if (2 == shortcutinfo.mIconType) {
//                          shortcutinfo.loadContactInfo(this);
//                          ((ShortcutIcon)view).updateInfo(this, shortcutinfo);
//                      } 
//                  } 
//              } 
//          }
//      }
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
        
        mFolderCling = (FolderCling)findViewById(R.id.folder_cling);
        mFolderCling.setLauncher(this);
        mFolderCling.setDragController(dragController);
        
        setupAnimations();
        mPositionSnap = mDragLayer.findViewById(R.id.default_position);
    }

    private void showStatusBar(boolean flag) {
        
    }
    
    private void showEditPanel(boolean flag, boolean flag1) {
        boolean showSBar;
        boolean showDeletezone;
        if (flag) {
            showSBar = false;
        } else {
            showSBar = true;
        }
        showStatusBar(showSBar);
        DeleteZone deletezone = mDeleteZone;
        if (!flag){
            deletezone.setVisibility(View.INVISIBLE);
        } else {
            deletezone.setVisibility(View.VISIBLE);
        }
        
        Animation animation;
        if (!flag){
            animation = mDeleteZoneEditingExit;
        } else {
            animation = mDeleteZoneEditingEnter;
        }
        deletezone.startAnimation(animation);
        if (!flag1) {
            if (!flag) {
                mDragLayerBackground.setExitEditingMode();
            } else {
                mDragLayerBackground.setEnterEditingMode();
            }
            HotSeats hotSeats = mHotSeats;
            Animation animHotSeats;
            if (!flag) {
                animHotSeats = mHotseatEditingEnter;
            } else {
                animHotSeats = mHotseatEditingExit;
            }
            hotSeats.startAnimation(animHotSeats);
            
            if (!flag){
                hotSeats.setVisibility(View.VISIBLE);
            } else {
                hotSeats.setVisibility(View.INVISIBLE);
            }
            
            if (!flag){
                mWidgetThumbnailView.startAnimation(mWidgetEditingExit);
            } else{
                mWidgetThumbnailView.startAnimation(mWidgetEditingEnter);
            }
            
            if (flag){
                mWidgetThumbnailView.setVisibility(View.VISIBLE);
            }
                
            
            if (!flag || mEditingGuideWindow == null) {
                if (mEditingGuideWindow != null) {
                    mEditingGuideWindow.dismiss();
                    mEditingGuideWindow = null;
                }
            } else {
                mEditingGuideWindow.show(mWidgetThumbnailView, 0, 0, true);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putBoolean("pref_key_guide_tips_editing_mode", false);
                editor.commit();
            }
        }
    }
    
    public IconCache getIconCache(){
        return mIconCache;
    }
    
    public Padding getPaddingForWidget(ComponentName componentname) {
        Padding padding = new Padding();
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo aInfo = packageManager.getApplicationInfo(componentname.getPackageName(), 0);
            if (aInfo.targetSdkVersion >= 14) {
                Resources resources = getResources();
                padding.left = resources.getDimensionPixelSize(R.dimen.widget_left_padding);
                padding.right = resources.getDimensionPixelSize(R.dimen.widget_right_padding);
                padding.top = resources.getDimensionPixelSize(R.dimen.widget_top_padding);
                padding.bottom = resources.getDimensionPixelSize(R.dimen.widget_bottom_padding);
            }   
        } catch (NameNotFoundException e) { }
        
        return padding;
    }
    
    int addAppWidget(LauncherAppWidgetProviderInfo launcherappwidgetproviderinfo){
        return 0;
    }
    
    public void bindAppMessage(ShortcutIcon shortcuticon, ComponentName componentname){
        mApplicationsMessage.addApplication(shortcuticon, componentname);
    }
    
    DragController.TouchTranslator getTouchTranslator() {
        CellScreen cellscreen;
        if (mEditingState != 8) {
            cellscreen = null;
        } else { 
            cellscreen = getWorkspace().getCurrentCellScreen();
        }
        return cellscreen;
    }
    
    public SceneData getUpsideScene() {
        return mUpsideScene;
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
    
    public int getEditingState() {
        return mEditingState;
    }
    
    private void completeAddShortcut(Intent intent) {
        int cx = 0;
        int cy = 0;
        if (mLastAddInfo instanceof ShortcutProviderInfo) {
            ShortcutProviderInfo sInfo = (ShortcutProviderInfo)mLastAddInfo;
            cx = sInfo.cellX;
            cy = sInfo.cellY;
        }
        mLastAddInfo = null;
        CellLayout.CellInfo cellinfo = findSingleSlot(cx, cy, true);
        if (cellinfo != null) {
            CellLayout cellLayout = mWorkspace.getCurrentCellLayout();
            ShortcutInfo shortcutinfo = mModel.addShortcut(this, intent, cellinfo);
            if (shortcutinfo != null) {
                ItemIcon itemIcon = createItemIcon(((ViewGroup)cellLayout), shortcutinfo);
                mWorkspace.addInCurrentScreen(((View)itemIcon), 
                        cellinfo.cellX, cellinfo.cellY, 1, 1, isWorkspaceLocked());
            }
        }
    }
    
    /**
     * 功能： 给ITEM找位置
     * @param cx
     * @param cy
     * @param flag
     * @return
     */
    private CellLayout.CellInfo findSingleSlot(int cx, int cy, boolean flag) {
        return findSlot(cx, cy, 1, 1, flag);
    }
    
    private CellLayout.CellInfo findSlot(int cx, int cy, int sx, int sy, boolean flag) {
        return findSlot(-1L, cx, cy, sx, sy, flag);
    }
    
    private CellLayout.CellInfo findSlot(long screen, 
            int cx, int cy, int sx, int sy, boolean flag) {
        CellLayout cellLayout;
        CellLayout.CellInfo cInfo;
        if (screen != -1L) {
            cellLayout = mWorkspace.getCellLayout(mWorkspace.getScreenIndexById(screen));
        } else {
            cellLayout = mWorkspace.getCurrentCellLayout();
        }
        if (cellLayout != null) {
            int ai[] = cellLayout.findNearestVacantAreaByCellPos(cx, cy, sx, sy, false);
            if (ai != null) {
                cInfo = new CellLayout.CellInfo();
                cInfo.cellX = ai[0];
                cInfo.cellY = ai[1];
                cInfo.spanX = sx;
                cInfo.spanY = sy;
                cInfo.screenId = mWorkspace.getCurrentScreenId();
            } else {
                if (flag) {
                    showError(R.string.out_of_space);
                }
                cInfo = null;
            }
        } else {
            cInfo = null;
        }
        return cInfo;
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
    
    public FolderIcon createNewFolder(long screen, int cx, int cy) {
        FolderInfo folderinfo = new FolderInfo();
        folderinfo.title = getText(R.string.folder_name);
        LauncherModel.addItemToDatabase(this, folderinfo, 
                LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, cx, cy);
        mFolders.put(Long.valueOf(folderinfo.id), folderinfo);
        return (FolderIcon)createItemIcon(mWorkspace.getCurrentCellLayout(), folderinfo);
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

    /**
     * 功能： 添加桌面快捷方式
     * @param shortcutinfo
     * @param flag
     */
    void addShortcut(ShortcutInfo shortcutInfo, boolean flag){
        if (getParentFolderIcon(shortcutInfo) == null){
            mWorkspace.addInScreen(createItemIcon(mWorkspace.getCurrentCellLayout(), shortcutInfo), 
                    shortcutInfo.screenId, shortcutInfo.cellX, shortcutInfo.cellY, 1, 1, flag);
        } else {
            FolderInfo folderinfo = getParentFolderInfo(shortcutInfo);
            if (folderinfo == null || !(folderinfo instanceof FolderInfo)){
                Log.e("Launcher", (new StringBuilder()).append("Can't find user folder of id ").append(shortcutInfo.container).toString());
            } else {
                folderinfo.add(shortcutInfo);
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
    
    public boolean isSceneAnimating() {
        return mSceneAnimating;
    }
    
    public boolean isSceneShowing() {
        boolean flag;
        if (mSceneScreen == null || !mSceneScreen.isShowing()) {
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
    
    private void showSceneScreenCore() {/*
        if (mSceneScreen == null) {
            mSceneScreen = (SceneScreen)LayoutInflater.from(this).inflate(R.layout.upside_scene_screen, null);
            mSceneScreen.setLauncher(this);
            mDragLayer.addView(mSceneScreen, mDragLayer.indexOfChild(mFolderCling), new FrameLayout.LayoutParams(-1, -1));
            mSceneScreen.setSceneData(getUpsideScene());
        }
        mSceneScreen.onShowAnimationStart();
        mSceneScreen.setTranslationY(-mScreen.getHeight());
        mSceneScreen.post(new Runnable() {
            @Override
            public void run() {
                Animator oAnimator = ObjectAnimator.ofFloat(mSceneScreen, "translationY", 0F);
                oAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        mScreen.setVisibility(4);
                        mSceneScreen.onShowAnimationEnd();
                        mSceneAnimating = false;
                        mSceneScreen.notifyGadgets(4);
                        showUpsideEnterOrExitTipIfNeed(false);
                    }
                });
                oAnimator.start();
                goOutOldLayer();
            }
        });
        */
    }
    
    public void showSceneScreen() {
        mSceneAnimating = true;
        if (mSceneScreen != null) {
            showSceneScreenCore();
        } else {
            showSceneScreenLoading();
        }
    }

    public void showSceneScreenLoading() {
        /*
        mSceneScreenLoading = (ViewGroup)getLayoutInflater().inflate(R.layout.upside_loading, mDragLayer, false);
        mDragLayer.addView(mSceneScreenLoading, mDragLayer.indexOfChild(mSceneScreen));
        mSceneScreenLoading.setTranslationY(-mScreen.getHeight());
        ViewGroup viewgroup = mSceneScreenLoading;
        float af[] = new float[1];
        af[0] = 0F;
        ObjectAnimator objectanimator = ObjectAnimator.ofFloat(viewgroup, "translationY", af);
        objectanimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                showSceneScreenCore();
            }
        });
        objectanimator.start();
        goOutOldLayer();
        */
    }
    
    public void removeGadget(ItemInfo iteminfo) {
        if (iteminfo.itemType == 5) {
            Gadget gadget1 = null;
            Iterator iterator = mGadgets.iterator();
            while (iterator.hasNext()){
                Gadget gadget = (Gadget)iterator.next();
                if (!((View)gadget).getTag().equals(iteminfo)){
                    continue;
                }
                gadget1 = gadget;
            }
            if (gadget1 != null) {
                mGadgets.remove(gadget1);
                gadget1.onDestroy();
                gadget1.onDeleted();
            }
        }
    }
    
    public void scrollToDefault() {
        mPositionSnap.setFocusableInTouchMode(true);
        mPositionSnap.requestFocus();
        mPositionSnap.setFocusableInTouchMode(false);
    }
    
    public void hideSceneScreen() {
        mSceneAnimating = true;
        mScreen.setVisibility(View.VISIBLE);
        mSceneScreen.onHideAnimationStart();
        
        float af1[] = new float[]{-mSceneScreen.getHeight()};
        Animator oAnimator = ObjectAnimator.ofFloat(mSceneScreen, "translationY", af1);
        oAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mSceneScreen.onHideAnimationEnd();
                mSceneAnimating = false;
                notifyGadgetStateChanged(4);
            }

        });
        oAnimator.start();
        ObjectAnimator.ofFloat(mScreen, "translationY", 0F).start();
    }
    /**
     * 描述： 是否有文件处于打开状态
     * @return
     */
    public boolean isFolderShowing() {
        return mFolderCling.isOpened();
    }
    
    public boolean isInEditing(){
        boolean flag;
        if (mEditingState == 7){
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    private void unbindDesktopItems() {
        Iterator<ItemInfo> iterator = mDesktopItems.iterator();
        while(iterator.hasNext()){
            iterator.next().unbind();
        }
    }
    
    public boolean isPreviewShowing() {
        return mWorkspacePreview.isShowing();
    }
    
    @Override
    public void bindAppWidget(LauncherAppWidgetInfo launcherappwidgetinfo) {
        // TODO Auto-generated method stub
        
    }

    /**
     * 功能： 将读取的APP添加至屏幕中，绑定Cell screen及Layout
     * 
     */
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
    public void bindAppsRemoved(ArrayList<RemoveInfo> arraylist) {
        mDragController.cancelDrag();
        mWorkspace.removeItems(arraylist);
        mHotSeats.removeItems(arraylist);
        
        RemoveInfo removeinfo;
        for (Iterator<RemoveInfo> iterator = arraylist.iterator(); 
                iterator.hasNext(); mApplicationsMessage.removeApplication(removeinfo.packageName)){
            removeinfo = iterator.next();
        }
    }

    @Override
    public void bindFolders(HashMap<Long, FolderInfo> hashmap) {
        mFolders.clear();
        mFolders.putAll(hashmap);
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
        if (mLoadingProgressDialog != null) {
            mLoadingProgressDialog.dismiss();
            mLoadingProgressDialog = null;
        }
    }
    
    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }
    
    /**
     * 描述： 得到当前打开的文件夹
     * @return
     */
    public View getCurrentOpenedFolder() {
        FolderCling foldercling;
        if (!isFolderShowing()) {
            foldercling = null;
        } else {
            foldercling = mFolderCling;
        }
        return foldercling;
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

    /**
     * 功能： 开机装载条
     */
    @Override
    public void startLoading() {
        mLoadingProgressDialog = ProgressDialog.show(this, "", getText(R.string.loading), true);
        mLoadingProgressDialog.setCancelable(false);
    }
    
    /**
     * 描述： 打开文件夹
     * @param folderinfo
     * @param view
     */
    public void openFolder(FolderInfo folderinfo, View view) {
        mFolderCling.bind(folderinfo);
        mFolderCling.open();
        mDimAnim.cancel();
        float af[] = new float[]{1F, 0.3F};
        mDimAnim.setFloatValues(af);
        mDimAnim.start();
    }
    
    boolean closeFolder() {
        return closeFolder(true);
    }

    /**
     * 描述： 关闭文件夹及过程动画
     * @param bClose
     * @return
     */
    boolean closeFolder(boolean bClose) {
        boolean flag;
        if (!mFolderCling.isOpened()) {
            flag = false;
        } else {
            mFolderCling.close(bClose);
            mDimAnim.cancel();
            float ai[] = new float[]{0.3F, 1F};
            mDimAnim.setFloatValues(ai);
            mDimAnim.start();
            flag = true;
        }
        return flag;
    }
    
    public void removeAppWidget(LauncherAppWidgetInfo launcherappwidgetinfo) {
        mDesktopItems.remove(launcherappwidgetinfo);
        launcherappwidgetinfo.hostView = null;
    }
    
    void removeFolder(FolderInfo folderinfo) {
        mFolders.remove(Long.valueOf(folderinfo.id));
    }
    
    void removeFolder(FolderIcon foldericon) {
        ((ViewGroup)foldericon.getParent()).removeView(foldericon);
        FolderInfo folderinfo = (FolderInfo)foldericon.getTag();
        LauncherModel.deleteUserFolderContentsFromDatabase(this, folderinfo);
        removeFolder(folderinfo);
    }
    
    void preRemoveItem(View view) {
        ViewGroup viewgroup = (ViewGroup)view.getParent();
        if (viewgroup instanceof CellLayout) {
            ((CellLayout)viewgroup).preRemoveView(view);
        }
    }
    
    public void updateFolderMessage(FolderInfo folderinfo) {
        mApplicationsMessage.updateFolderMessage(folderinfo);
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
    
    private void notifyGadgetStateChanged(int i) {
        
    }
    
    public void setEditingState(int state)
    {
        boolean flag = true;
        if (state != mEditingState 
            && !mWorkspace.inEditingModeAnimating() 
            && (mEditingState != 7)) {
            switch (state) {
            default:
                break;

            case 7: // '\007'
                boolean flag1;
                if (9 != mEditingState){
                    flag1 = false;
                } else {
                    flag1 = true;
                }
                showEditPanel(false, flag1);
                notifyGadgetStateChanged(7);
                Workspace workspace = mWorkspace;
                if (mEditingState != 9){ 
                    flag = false;
                }
                workspace.setEditMode(state, flag);
                break;

            case 8: // '\b'
                showEditPanel(flag, false);
                notifyGadgetStateChanged(8);
                mWorkspace.setEditMode(state, false);
                mDragLayer.clearAllResizeFrames();
                break;

            case 9: // '\t'
                showEditPanel(flag, flag);
                mWorkspace.setEditMode(state, flag);
                break;
            }
            mEditingState = state;
            ErrorBar errorbar = mErrorBar;
            int j;
            if (state == 7){
                j = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            } else {
                j = 0;
            }
            errorbar.setMargins(0, j, 0, 0);
        }
    }
    
    public void showError(int i) {
        mErrorBar.showError(i);
        mDeleteZone.onShowError();
    }
}
