package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    LauncherModel.java
 * 创建时间：    2013
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: Launcher加载类
 * ====================================================================================
 */
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cn.minking.launcher.AllAppsList.RemoveInfo;
import cn.minking.launcher.gadget.GadgetInfo;
import cn.minking.launcher.upsidescene.SceneData;
import android.R.anim;
import android.R.integer;
import android.app.ActionBar.Tab;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class LauncherModel extends BroadcastReceiver {
    
    /********* 常量 *********/
    // 打印LOG的TAG标识
    static final String TAG = "MKHome.Model";
    static final boolean LOGD = true;
    
    // Specific runnable types that are run on the main thread deferred handler, this allows us to
    // clear all queued binding runnables when the Launcher activity is destroyed.
    private static final int MAIN_THREAD_NORMAL_RUNNABLE = 0;
    private static final int MAIN_THREAD_BINDING_RUNNABLE = 1;
 // batch size for the workspace icons
    private static final int ITEMS_CHUNK = 6;
    
    /********* 状态 *********/
    // 
    private boolean mWorkspaceLoaded = false;
    private boolean mAllAppsLoaded = false;
    
    /********* 数据 *********/
    private static HashSet<ItemInfo> sDelayedUpdateBuffer = null;
    private AllAppsList mAllAppsList = null;
    private WeakReference<Callbacks> mCallbacks = null;

    // sBgAppWidgets is all LauncherAppWidgetInfo created by LauncherModel. Passed to bindAppWidget()
    static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets = 
            new ArrayList<LauncherAppWidgetInfo>();
    
    
    private final ArrayList<LauncherAppWidgetInfo> mAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
    private final ArrayList<GadgetInfo> mGadgets = new ArrayList<GadgetInfo>();
    private final HashMap<Long, FolderInfo> mFolders = new HashMap<Long, FolderInfo>();
    private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();
    
    // 存储ID所对应的ComponentName
    private final HashMap<ComponentName, Long> mLoadedApps = new HashMap<ComponentName, Long>();
    private final HashSet<String> mLoadedPackages = new HashSet<String>();
    private final HashSet<String> mLoadedPresetPackages = new HashSet<String>();
    private final HashSet<String> mLoadedUris = new HashSet<String>();
        
    /// M: 处理图标显示的缓存，在LauncherApplication中分配
    private IconCache mIconCache = null;
    
    private final LauncherApplication mApp;
    
    /********* 锁 *********/
    private final Object mAllAppsListLock = new Object();
    private final Object mLock = new Object();
    
    // 在引用任何静态bg数据结构之前，这个锁必须锁死。与其他锁不同，这个锁会被长时间持有，我们不希望在第一次装载配置信息的工作线程之外任何bg静态数据结构被引用。
    private static final Object sBgLock = new Object();

    /********* 行为 *********/
    // launcher loader 的装载线程
    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());
    private LoaderTask mLoaderTask = null;

    private DeferredHandler mHandler;
    /********* 其他 *********/
    
    
    private class LoaderTask implements Runnable{
        private final ContentResolver mContentResolver;
        private Context mContext;
        // 用来存储手机安装的APP Component
        private HashSet<ComponentName> mInstalledComponents;
        private boolean mIsJustRestoreFinished = false;
        private boolean mIsLaunching = false;
        private boolean mIsLoadingAndBindingWorkspace;
        private boolean mLoadAndBindStepFinished = false;
        private PackageManager mManager = null;
        private boolean mStopped;

        /**
         * 功能： 桌面加载线程的run函数
         */
        @Override
        public void run() {
            // 对终端用户的使用体验： 如果Launcher运行起来，在前端运行了APP，则首先装载所有的APP。否则，先装载WORKSPACE
            final Callbacks callbacks = mCallbacks.get();
            final boolean loadWorkspaceFirst = callbacks != null ? (!callbacks.isAllAppsVisible()) : true;
            
            keep_running:{
                // 在第一次启动的时候提升装载系统的优先级，防止桌面上什么东西都没有，看起来比较酷
                synchronized (mLock) {
                    android.os.Process.setThreadPriority(mIsLaunching ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }
                
                mHandler.post(new Runnable() {
                    public void run(){
                        if (!mStopped && callbacks != null)
                            callbacks.startLoading();
                    }
                });
                Log.d(TAG, "step 1: loading workspace");
                
                synchronized (mLock){
                    loadAndBindWorkspace();
                }
                
                if (mStopped) {
                    break keep_running;
                }
                
                // 如果时间较久，那么将装载线程的优先级降下来，先处理UI的线程
                synchronized (mLock) {
                    if (mIsLaunching) {
                        Log.d(TAG, "Setting thread priority to BACKGROUND");
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                
                waitForIdle();
                
                Log.d(TAG, "step 2: loading missing icons");
                synchronized (mLock){
                    loadAndBindMissingIcons();
                }
                
                mHandler.post(new Runnable() {
                    public void run(){
                            if (!mStopped && callbacks != null){
                                callbacks.finishLoading();
                            }
                        }
                    });
                
                Log.d("Launcher.Model", "step 3: loading upside scene");
                SceneData sceneData = new SceneData();
                if (sceneData.loadData(mContext)) {
                    mHandler.post(new DataCarriedRunnable(callbacks) {
                        public void run(){
                            if (!mStopped){
                                Log.d(TAG, "Finally adding missing icons");
                                callbacks.bindUpsideScene((SceneData)mData);
                            }
                        }
                    });
                }
                
                synchronized (mLock) {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }
            }
            
            // 清除mContext的引用，防止callback的runnable运行完成后还一直持有
            mContext = null;

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                if (LOGD) {
                    Log.d(TAG, "Reset load task running flag <<<<, this = " + this);
                }
            }

        }
        
        /**
         * 功能： 加载数据库中没有读取的图标
         */
        private void loadAndBindMissingIcons(){
            if (mStopped) return;
            
            Iterator<ComponentName> iterator = mInstalledComponents.iterator();
            if (mInstalledComponents.size() == 0) {
                Log.e(TAG, "No main activity found, the system is so clean");
                return;
            }
            
            if (mCallbacks == null) return;
            
            final Callbacks oldCallbacks = mCallbacks.get();
            
            if (oldCallbacks == null) {
                Log.e(TAG, "No callback to call back");
                return;
            }
            
            HashSet<String> hashSet = new HashSet<String>();
            
            while (iterator.hasNext() && !mStopped){
                ComponentName componentname = (ComponentName)iterator.next();
                
                if (hashSet.contains(componentname.getPackageName()) || mLoadedApps.containsKey(componentname)) {
                    continue;
                }
                try {
                    LauncherSettings.updateHomeScreen(mContext, componentname.getPackageName());
                    synchronized (mAllAppsListLock) {
                        mAllAppsList.updatePackage(mContext, componentname.getPackageName());
                    }
                    hashSet.add(componentname.getPackageName());
                } catch (Exception e) {
                    Log.d(TAG, "database didnot ready,ignore this package.");
                }
            }
            
            if (!mStopped && (mAllAppsList.removed.size() > 0)) {
                final ArrayList<RemoveInfo> removeList = new ArrayList<RemoveInfo>(mAllAppsList.removed);
                mAllAppsList.removed.clear();
                mHandler.post(new DataCarriedRunnable(oldCallbacks) {
                    public void run(){
                            Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                            if (!mStopped && callbacks != null){
                                Log.d(TAG, "Finally removing useless icons");
                                callbacks.bindAppsRemoved(removeList);
                            }
                        }
                    });
                onRemoveItems(mAllAppsList.removed);
            }
            
            if (!mStopped && (mAllAppsList.added.size() > 0)) {
                final ArrayList<ShortcutInfo> addList = new ArrayList<ShortcutInfo>(mAllAppsList.added);
                mAllAppsList.added.clear();
                mHandler.post(new DataCarriedRunnable(oldCallbacks) {
                    public void run(){
                            Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                            if (!mStopped && callbacks != null){
                                Log.d(TAG, "Finally adding missing icons");
                                callbacks.bindAppsAdded(addList);
                            }
                        }
                    });
                onLoadShortcuts(mAllAppsList.removed);
            }
            
            mHandler.post(new Runnable() {
                public void run(){
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (!mStopped && callbacks != null){
                        callbacks.finishBindingMissingItems();
                    }
                }
            });
            
        }
        
        public boolean isLaunching() {
            return mIsLaunching;
        }
        
        private void waitForIdle() {
            synchronized (LoaderTask.this) {
                long workspaceWaitTime = SystemClock.uptimeMillis();
                if (LOGD) {
                    Log.d(TAG, "waitForIdle start, workspaceWaitTime : " + workspaceWaitTime + "ms, Thread priority :"
                            + Thread.currentThread().getPriority() + ", this = " + this);
                }
                
                // 桌面加载后执行
                mHandler.postIdle(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (LoaderTask.this) {
                            mLoadAndBindStepFinished = true;
                            if (LOGD) {
                                Log.d(TAG, "done with previous binding step");
                            }
                            LoaderTask.this.notify();
                        }
                    }
                });
                
                while(!mStopped && !mLoadAndBindStepFinished){
                    try{
                        this.wait();
                    } catch (InterruptedException _ex) {
                        
                    }
                }
                if (LOGD) {
                    Log.d(TAG, (new StringBuilder()).append("waited ").
                            append(SystemClock.uptimeMillis() - workspaceWaitTime).
                            append("ms for previous step to finish binding").toString());
                }

            }
        }
        
        /**
         * 功能：  BIND WORKSPACE
         */
        private void bindWorkspace() {
            final long t = LOGD ? SystemClock.uptimeMillis() : 0;
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            mHandler.cancel();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                    
                }
            });
            
            final HashMap<Long, FolderInfo> folderHashMap = new HashMap<Long, FolderInfo>(mFolders);
            if (!folderHashMap.isEmpty()) {
                mHandler.post(new DataCarriedRunnable(folderHashMap) {
                    @Override
                    public void run(){
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null){
                            callbacks.bindFolders((HashMap<Long, FolderInfo>)mData);
                        }
                    }
                }); 
            }
            
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Going to start binding widgets soon.");
                }
            });
            
         // Bind the workspace items
            final ArrayList<ItemInfo> itemInfos = new ArrayList<ItemInfo>(mItems);
            
            int N = itemInfos.size();
            for (int i = 0; i < N; i += ITEMS_CHUNK) {
                final int start = i;
                final int chunkSize = (i+ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N-i);
                mHandler.post(new DataCarriedRunnable(itemInfos) {
                    @Override
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null){
                            callbacks.bindItems((ArrayList<ItemInfo>)mData, start, start + chunkSize);
                        }
                    }
                });
            } 
            
            int curScreen = oldCallbacks.getCurrentWorkspaceScreen();
            final ArrayList<LauncherAppWidgetInfo> appWidgetInfos = new ArrayList<LauncherAppWidgetInfo>(mAppWidgets);
            int appWidgetSize = appWidgetInfos.size();
            if ((curScreen != -1) && (appWidgetSize > 0)) {
                final LauncherAppWidgetInfo firstAppWidgetInfo = appWidgetInfos.get(0);
                if (firstAppWidgetInfo.screenId == 0){
                    mHandler.post(new DataCarriedRunnable(appWidgetInfos) {
                        @Override
                        public void run() {
                            Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                            if (callbacks != null){
                                callbacks.bindAppWidget((LauncherAppWidgetInfo)mData);
                            }
                        }
                    });
                }
            } else {
                final ArrayList<GadgetInfo> gadgetInfos = new ArrayList<GadgetInfo>(mGadgets);
                int gadgetSize = gadgetInfos.size();
                int i = 0;
                while (i < gadgetSize){
                    GadgetInfo gadgetInfo = gadgetInfos.get(i);
                    if (gadgetInfo.screenId != (long)curScreen){
                        mHandler.post(new DataCarriedRunnable(gadgetInfo) {
                            @Override
                            public void run() {
                                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                                if (callbacks != null){
                                    callbacks.bindGadget((GadgetInfo)mData);
                                }
                            }
    
    
                        });
                    }
                    i++;
                }
                
                if (i >= gadgetSize){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                            if (callbacks != null) {
                                callbacks.finishBindingSavedItems();
                            }
                        }
                    });
                    
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, (new StringBuilder()).append("bind workspace in ").
                                    append(SystemClock.uptimeMillis() - t).append("ms").toString());
                        }
                    });
                }
            }
        }
        
        /**
         * 功能： LOAD AND BIND WORKSPACE
         */
        private void loadAndBindWorkspace(){
            if (!mWorkspaceLoaded) {
                if (LOGD) Log.d(TAG, (new StringBuilder()).append("loadAndBindWorkspace loaded=").append(mWorkspaceLoaded).toString());
                loadWorkspace();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        mWorkspaceLoaded = false;
                        return;
                    }
                    mWorkspaceLoaded = true;
                }
            }
            
            // 绑定 workspace
            bindWorkspace();
        }
        
        /**
         * 功能： 加载 WORKSPACE
         */
        private void loadWorkspace() {
            final long t = LOGD ? SystemClock.uptimeMillis() : 0;
            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();
            
            synchronized (sBgLock) {
                // MKHOME 桌面项清零重置
                mItems.clear();
                mAppWidgets.clear();
                mFolders.clear();
                mGadgets.clear();
                mLoadedApps.clear();
                mLoadedUris.clear();
                mLoadedPackages.clear();
                mLoadedPresetPackages.clear();
                mInstalledComponents.clear();
                
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                
                // APP的总数
                int appCounts = 0;
                
                // 此处得到所有APP的包名和入口类名称，resolveInfo指向手机中所有的Package
                // 开机时重新读取现有的APP名称，然后再从数据库中读取存储的信息，防止重新开机后APP的信息变化
                Iterator<ResolveInfo> resolveInfo = mManager.queryIntentActivities(intent, 0).iterator();
                while (resolveInfo.hasNext()) {
                    ResolveInfo rInfo = resolveInfo.next();
                    String packageName = rInfo.activityInfo.packageName;
                    String name = rInfo.activityInfo.name;
                    if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty((CharSequence)(name))) {
                        
                        // 将得到的包和类名称存放到哈希表中存储
                        mInstalledComponents.add(new ComponentName(packageName, name));
                        appCounts++;
                        // if (LOGD) Log.d(TAG, "MK : pack: " + packageName + "name: " + name);
                    }
                }
                
                if (LOGD) Log.d(TAG, "MK : appCounts = " + appCounts);
                
                // 将已安装的APP的BUNDLE数据保存起来
                // 如： {"com.android.contacts","com.android.mms", ...... }
                if (!mInstalledComponents.isEmpty()) {
                    updateInstalledComponentsArg(mContext);
                }
                
                // ITEM 的 ID 列表
                ArrayList<Long> idList = new ArrayList<Long>();
                // ITEM 列表
                ArrayList<ItemInfo> itemList = new ArrayList<ItemInfo>();
                
                // Query筛选项
                String columns[] = ItemQuery.COLUMNS;
                String containerSelDestop[] = new String[]{String.valueOf(LauncherSettings.Favorites.CONTAINER_DESKTOP)};
                
                Uri joinUri = LauncherSettings.Favorites.getJoinContentUri(" JOIN screens ON favorites.screen=screens._id");
                
                // 数据库Uri
                Uri uri = LauncherSettings.Favorites.CONTENT_URI;
                
                // 1. 首先读取桌面容器中的ITEM
                loadItems(contentResolver.query(joinUri, columns, "container=?", containerSelDestop, 
                        "screens.screenOrder ASC, cellY ASC, cellX ASC, itemType ASC"), idList, itemList);
                
                // 选择筛选项， 此两项详细请看URI的query方法
                String containerSelHotseat[] = new String[]{String.valueOf(LauncherSettings.Favorites.CONTAINER_HOTSEAT)};
                
                // 2. 再读取HOTSEAT容器中的ITEM
                loadItems(contentResolver.query(uri, columns, 
                        "container=?", containerSelHotseat, null), idList, itemList);
                
                // Provider的代理
                ContentProviderClient contentProviderClient = 
                        mContentResolver.acquireContentProviderClient(LauncherSettings.Favorites.CONTENT_URI);
                
                Log.d(TAG, "MK : idList size " + idList.size() + " itemList size " + itemList.size());
                
                if (!idList.isEmpty()) {
                    for (Iterator<Long> iterator = idList.iterator(); iterator.hasNext();) {
                        long id = (iterator.next()).longValue();
                        Log.d(TAG, "MK : " + (new StringBuilder()).append("Removed id = ").append(id).toString());
                        try {
                            int result = contentProviderClient.delete(LauncherSettings.Favorites.getContentUri(id), null, null);
                            Log.w(TAG, "MK : Remove id =" + id + " result = " + result);
                        } catch (RemoteException _ex) {
                            Log.w(TAG, "MK : " + (new StringBuilder()).append("Could not remove id = ").append(id).toString());
                        } catch (SQLException _ex) {
                            Log.w(TAG, "MK : " + (new StringBuilder()).append("Could not remove id(database readonly) = ").append(id).toString());
                        }
                    }
                }
                
                if (!itemList.isEmpty()) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("screen", Integer.valueOf(-1));
                    
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_id").append(" IN(");
                    for (Iterator<ItemInfo> iterator = itemList.iterator(); 
                            iterator.hasNext();){
                        stringBuilder.append(((ItemInfo)iterator.next()).id).append(',');
                    }
                    stringBuilder.setCharAt(stringBuilder.length() - 1, ')');
                    
                    try {
                        int result = contentProviderClient.update(LauncherSettings.Favorites.CONTENT_URI, contentValues, stringBuilder.toString(), null);
                        Log.w(TAG, "MK : Update" + stringBuilder.toString() + " result = " + result);
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                    
                    Iterator<ItemInfo> itemIterator = itemList.iterator();
                    while (itemIterator.hasNext()) {
                        ItemInfo itemInfo = itemIterator.next();
                        itemInfo.screenId = -1L;
                        itemInfo.onAddToDatabase(contentValues);
                        try {
                            contentProviderClient.update(LauncherSettings.Favorites.getContentUri(itemInfo.id),
                                    contentValues, null, null);
                            itemInfo.loadPosition(contentValues);
                        } catch (RemoteException remoteException) {
                            remoteException.printStackTrace();
                        }
                        contentValues.clear();
                    }
                }
                
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, (new StringBuilder()).append("load workspace in ").
                                append(SystemClock.uptimeMillis() - t).append("ms").toString());    
                    }
                });
            }
        }
        
        private boolean isInvalidPosition(ItemInfo iteminfo){
            boolean flag;
            if ((iteminfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                    && ((iteminfo.cellX >= 0 && ((iteminfo.cellX + iteminfo.spanX) < ResConfig.getCellCountX()))
                        && (iteminfo.cellY >= 0 && ((iteminfo.cellY + iteminfo.spanY) < ResConfig.getCellCountY()))))
                        || (iteminfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT 
                            && (iteminfo.cellX >= 0 && iteminfo.cellX < ResConfig.getHotseatCount()))) {
                flag = false;
            }else {
                flag = true;
            }
            return flag;
        }
        
        private boolean ensureItemUniquePostiton(Context context, Long id, int i){
            Boolean flag = false;
            try {
                Bundle bundle = context.getContentResolver().
                        acquireContentProviderClient(LauncherSettings.Favorites.CONTENT_URI).
                        call("ensureItemUniquePosition", Long.toString(id.longValue()), null);
                if (bundle == null) {
                    flag = false;
                }else {
                    flag = bundle.getBoolean("resultBoolean");
                }
            } catch (RemoteException remoteException) {
                
            }
            return flag;
        }
        
        /**
         * 功能： 从数据库中读取类型为SHORTCUT和APPLICATION的项目
         */
        private void loadShortcut(Cursor cursor, int itemType, 
                ArrayList<Long> idList, ArrayList<ItemInfo> itemList){
            if (cursor == null) return;
            
            Intent intent;
            ShortcutInfo shortcutInfo;
            String intentDescription;
            
            // 找到各项在数据表中的Index值
            final int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
            final int iconTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
            final int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
            final int iconPackageIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
            final int iconResourceIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
            final int containerIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
            final int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
            final int appWidgetIdIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
            final int screenIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
            final int cellXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
            final int cellYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
            final int spanXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
            final int spanYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
            
            // 获取ID， 如果读取的ID， 且ID必须大于0
            long id = cursor.getLong(idIndex);
            if (id > 0) { 
                try {
                    // 获取Intent信息
                    intentDescription = cursor.getString(intentIndex);
                    intent = Intent.parseUri(intentDescription, 0);
                    ComponentName componentName = intent.getComponent();
                    
                    if(itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION){
                        // 如果读取的包入口不在Installed的HASH表中则返回
                        if (mInstalledComponents.contains(componentName) 
                                || LauncherSettings.isRetainedComponent(componentName)){
                            //  类型APP
                            try{
                                // CN所对应的 secondary group-ids 不为0， 即此APP为用户组APP，LINUX特性
                                int[] a= mContext.getPackageManager().getPackageGids(componentName.getPackageName());
                                if (a.length != 0){
                                    idList.add(Long.valueOf(id));
                                    if (LOGD) Log.w(TAG, (new StringBuilder()).
                                            append("Remove:").append(componentName).
                                            append(",package updated but current class does not exist.").toString());
                                }
                            } catch (NameNotFoundException ex) { 
                                Log.w(TAG, "package not finded" + componentName);
                            }
                            
                            if (mIsJustRestoreFinished){
                                if (LOGD) Log.e(TAG, (new StringBuilder()).append("Restored:").
                                        append(componentName).append(",doesn't exist anymore,removing it.").toString());
                                idList.add(Long.valueOf(id));
                            }
                        }
                    }

                    // 确保每个ITEM拥有唯一的POS
                    if (!ensureItemUniquePostiton(mContext, Long.valueOf(id), 0)){
                        return;
                    }
                    
                    if(itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION){
                        shortcutInfo = getShortcutInfo(mManager, intent, mContext, cursor, 
                                iconIndex, titleIndex);
                        if (shortcutInfo != null && mLoadedApps.containsKey(intent.getComponent())){
                            shortcutInfo = null;
                        } else {
                            mLoadedApps.put(intent.getComponent(), Long.valueOf(id));
                        }
                    } else {
                        shortcutInfo = getShortcutInfo(intent, cursor, mContext, 
                                iconTypeIndex, iconPackageIndex, iconResourceIndex, iconIndex, titleIndex);
                    }
                    
                    if (shortcutInfo != null) {
                        shortcutInfo.intent = intent;
                        // 如果对应的快捷方式信息不为空，那么读取详细的信息
                        shortcutInfo.load(cursor);
                        
                        // 如果为无效的位置，将读取的信息放入itemList，后续更新信息
                        if (isInvalidPosition(shortcutInfo)) {
                            itemList.add(shortcutInfo);
                        }
                        
                        // 更新
                        updateSavedIcon(mContext, shortcutInfo, cursor, 15);
                        if (shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                                || shortcutInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            mItems.add(shortcutInfo);
                        }else {
                            LauncherModel.findOrMakeUserFolder(mFolders, shortcutInfo.container).add(shortcutInfo);
                        }
                        onLoadShortcut(shortcutInfo);
                    } else {
                        // 如果装载失败或者ID所对应的APP在mLoadedApps中重复，那么删除掉这个ID对应的项
                        id = cursor.getLong(idIndex);
                        Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                        mContentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                    id), null, null);
                    }
                    
                    // 如果读取的信息为空，则将此ID添加到删除项
                    if (shortcutInfo == null) {
                        Log.e(TAG, (new StringBuilder()).append("Error loading shortcut ").append(id).append(", removing it").toString());
                        idList.add(Long.valueOf(id));
                    }
                } catch (URISyntaxException e) {
                    
                }
            }
        }
        
        private void loadFolder(Cursor cursor, ArrayList<ItemInfo> itemList){
            
        }
        
        private void loadAppWidget(Cursor cursor, ArrayList<Long> idList, ArrayList<ItemInfo> itemList){
            
        }
        
        private void loadGadget(Cursor cursor, ArrayList<Long> idList, ArrayList<ItemInfo> itemList){
            
        }
        
        /**
         * 功能：从存储的数据库中 装载项目
         * @param cursor
         * @param idList
         * @param itemList
         */
        private void loadItems(Cursor cursor, ArrayList<Long> idList, ArrayList<ItemInfo> itemList){
            int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
            
            while (!mStopped && cursor.moveToNext()) {
                try {
                    // 从数据库中读到项目的类型
                    int itemType = cursor.getInt(itemTypeIndex);        
                    switch (itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        {
                            loadShortcut(cursor, itemType, idList, itemList);
                        }
                        break;
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        {
                            loadFolder(cursor, itemList);
                        }
                        break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        {
                            loadAppWidget(cursor, idList, itemList);
                        }
                        break;
                        case LauncherSettings.Favorites.ITEM_TYPE_GADGET:
                        {
                            loadGadget(cursor, idList, itemList);
                        }
                        break;
                                
                        default:
                            break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "MK : Desktop items loading interrupted:", e);
                    continue;
                }
            }
            cursor.close();
        }
        
        /**
         * 功能： 调用到LauncherProvider的call 函数，返回Bundle数据
         * @param context
         */
        private void updateInstalledComponentsArg(Context context){
            try {
                context.getContentResolver().
                        acquireContentProviderClient(LauncherSettings.Favorites.CONTENT_URI).
                        call("updateInstalledComponentsArg", null, null);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                notify();
            }
        }
        
        /**
         * 功能： 获取回调函数接口
         * @param oldCallbacks
         * @return
         */
        Callbacks tryGetCallbacks(Callbacks oldCallbacks){
            synchronized (mLock) {
                if (mStopped) {
                    Log.i(TAG, "tryGetCallbacks returned null by stop flag.");
                    return null;
                }

                if (mCallbacks == null) {
                    return null;
                }

                final Callbacks callbacks = mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                }

                return callbacks;
            }
        }
        
        public LoaderTask(Context context, boolean is_launching, boolean is_restore_finished) {
            mInstalledComponents = new HashSet<ComponentName>();
            mContext = context;
            mIsLaunching = is_launching;
            mContentResolver = context.getContentResolver();
            mManager = context.getPackageManager();
            mIsJustRestoreFinished = is_restore_finished;
        }
    }
    
    private abstract class DataCarriedRunnable implements Runnable {
        protected Object mData;
        public DataCarriedRunnable(Object obj){
            mData = obj;
        }
    }
    
    public static interface Callbacks{

        public abstract void bindAppWidget(LauncherAppWidgetInfo launcherappwidgetinfo);

        public abstract void bindAppsAdded(ArrayList<ShortcutInfo> arraylist);

        public abstract void bindAppsRemoved(ArrayList<RemoveInfo> arraylist);

        public abstract void bindFolders(HashMap<Long, FolderInfo> hashmap);

        public abstract void bindGadget(GadgetInfo gadgetinfo);

        public abstract void bindItems(ArrayList<ItemInfo> arraylist, int i, int j);

        public abstract void bindUpsideScene(SceneData scenedata);

        public abstract void finishBindingMissingItems();

        public abstract void finishBindingSavedItems();

        public abstract void finishLoading();
        
        public boolean isAllAppsVisible();

        public abstract int getCurrentWorkspaceScreen();

        public abstract void reloadWidgetPreview();

        public abstract void startBinding();

        public abstract void startLoading();
    }
    
    
    public LauncherModel(LauncherApplication launcherApplication, IconCache iconCache) {
        mHandler = new DeferredHandler();
        mAllAppsList = new AllAppsList();
        mApp = launcherApplication;
        mIconCache = iconCache;
    }
    
    
    static void addItemToDatabase(Context context, ItemInfo iteminfo, final long container, 
            final int screen, final int cellX, final int cellY){
        addItemToDatabase(context, iteminfo, container, screen, cellX, cellY, false);
    }
    
    /**
     * 功能：  将读取的Item添加到数据库中
     * @param context
     * @param iteminfo
     * @param container ： 属于哪个容器（WORKSPACE OR HOTSEAT）
     * @param screen: 属于哪个SCREEN
     * @param cellX： SCREEN中的 X 位置
     * @param cellY： SCREEN中的 Y 位置
     * @param reload: 是否需要重新加载
     */
    static void addItemToDatabase(Context context, final ItemInfo item, final long container, 
            final int screen, final int cellX, final int cellY, final boolean reload){
        if (LOGD) {
            Log.d(TAG, "addItemToDatabase item = " + item + ", container = " + container + ", screen = " + screen
                    + ", cellX " + cellX + ", cellY = " + cellY + ", notify = " + reload);
        }
        
        item.container = container;
        item.screenId = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        
        // 生成一个新的ID给这个ITEM
        item.id = app.getLauncherProvider().generateNewId();
        
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        
        // 添加至数据库中
        item.onAddToDatabase(values);
        
        values.put(LauncherSettings.Favorites._ID, item.id);
        
        Runnable r = new Runnable() {
            public void run() {
                String transaction = "DbDebug    Add item (" + item.id + ") to db, id: "
                        + item.id + " (" + container + ", " + screen + ", " + cellX + ", "
                        + cellY + ")";
                // Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);
                Uri uri = cr.insert(LauncherSettings.Favorites.CONTENT_URI, values);
                if (uri == null) {
                    Log.d(TAG, (new StringBuilder()).append("Error when insert item to database with spanX:")
                            .append(item.spanX).append(" spanY:").append(item.spanY).append(",ignore it.").toString());
                    return;
                }
                Cursor cursor = cr.query(LauncherSettings.Favorites.getContentUri(item.id), ItemQuery.COLUMNS, null, null, null);
                if (cursor == null) {
                    return;
                }
                if (cursor.moveToNext()){
                    item.load(cursor);
                }
                cursor.close();
            }
        };
        runOnWorkerThread(r);
    }
    
    static void moveItemInDatabase(Context context, ItemInfo iteminfo, 
            long container, int screen, int cellX, int cellY) {
        ContentValues contentvalues = new ContentValues();
        contentvalues.put("container", Long.valueOf(container));
        contentvalues.put("cellX", Integer.valueOf(cellX));
        contentvalues.put("cellY", Integer.valueOf(cellY));
        contentvalues.put("spanX", Integer.valueOf(iteminfo.spanX));
        contentvalues.put("spanY", Integer.valueOf(iteminfo.spanY));
        contentvalues.put("screen", Long.valueOf(screen));
        iteminfo.cellX = cellX;
        iteminfo.cellY = cellY;
        iteminfo.container = container;
        iteminfo.screenId = screen;
        updateItemInDatabase(context, iteminfo.id, contentvalues);
    }
    
    static void addOrMoveItemInDatabase(Context context, ItemInfo iteminfo, 
            final long container, final int screen, final int cellX, final int cellY) {
        if (iteminfo.container != -1L){
            moveItemInDatabase(context, iteminfo, container, screen, cellX, cellY);
        } else {
            addItemToDatabase(context, iteminfo, container, screen, cellX, cellY);
        }
    }
    
    static void applyBatch(final Context context, 
            final String authority, 
            final ArrayList<ContentProviderOperation> operations) {
        runOnWorkerThread(new Runnable() {
            public void run() {
                try {
                    context.getContentResolver().applyBatch(authority, operations);
                    return;
                } catch (RemoteException _ex) {
                    throw new RuntimeException("applyBatch failed with RemoteException.");
                } catch (OperationApplicationException _ex) {
                    throw new RuntimeException("applyBatch failed with OperationApplicationException.");
                }
            }
        });
    }
    
    /**
     * 功能：  
     */
    private void onLoadShortcuts(ArrayList arraylist){
        
    }
    
    /**
     * 功能：  
     */
    private void onRemoveItems(ArrayList arraylist){
        
    }
    
    /**
     * 功能：  将线程放入到主线程上立即执行
     */
    private void runOnMainThread(Runnable r) {
        runOnMainThread(r, 0);
    }
    
    /**
     * 功能：  
     */
    private void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }
    
    /** Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler. */
    private static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }
    
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }
    
    public void startLoader(Context context, boolean isLaunching) {
        synchronized (mLock) {
            // 如果线程没有任何作用则不进入调用节省时间及资源
            if (mCallbacks != null && mCallbacks.get() != null) {
                // 如果已经有一个线程在运行则通知停止已经在运行的线程
                isLaunching = isLaunching || stopLoaderLocked();
                
                mLoaderTask = new LoaderTask(mApp, isLaunching, 
                        ((LauncherApplication)context.getApplicationContext()).isJustRestoreFinished());
                
                sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                sWorker.post(mLoaderTask);
            }
        }
    }
    
    private void onLoadShortcut(ShortcutInfo shortcutinfo){
        synchronized (mLock) {
            if (shortcutinfo.intent != null) {
                mLoadedUris.add(makeUniquelyIntentKey(shortcutinfo.intent));
                String packageName = shortcutinfo.getPackageName();
                if (packageName != null)
                {
                    mLoadedPackages.add(packageName);
                    if (shortcutinfo.isPresetApp())
                        mLoadedPresetPackages.add(packageName);
                }
            }
        }
    }
    
    private String makeUniquelyIntentKey(Intent intent){
        String string = "";
        
        if (intent != null) {
            Intent intentTmp = new Intent(intent);
            if (intentTmp.getComponent() != null
                    && intentTmp.getComponent().getPackageName().equals(intentTmp.getPackage())) {
                intentTmp.setPackage(null);
            }
            intentTmp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intentTmp.getCategories() != null) {
                intentTmp.getCategories().clear();
            }
            intentTmp.setAction(null);
            string = intentTmp.toUri(0);
        }
        
        return string;
    }
    
    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock) {
            // 首先停止现有的加载器，mAllAppsLoaded及mWorkspaceLoaded设置为false
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }
    
    private static FolderInfo findOrMakeUserFolder(HashMap<Long, FolderInfo> hashmap, long container){
        FolderInfo folderInfo = hashmap.get(Long.valueOf(container));
        if ((folderInfo == null) && (folderInfo instanceof FolderInfo)) {
            folderInfo = new FolderInfo();
            hashmap.put(Long.valueOf(container), folderInfo);
        }
        return folderInfo;
    }
    
    public static boolean updateItemFlagsInDatabaseDelayed(Context context, ItemInfo iteminfo){
        if (sDelayedUpdateBuffer == null){
            sDelayedUpdateBuffer = new HashSet<ItemInfo>();
        }
        sDelayedUpdateBuffer.add(iteminfo);
        return true;
    }
    
    static boolean flashDelayedUpdateItemFlags(final Context context){
        boolean flag;
        if (sDelayedUpdateBuffer != null && !sDelayedUpdateBuffer.isEmpty()) {
            runOnWorkerThread(new Runnable() {
                @Override
                public void run() {
                    HashSet hashset = LauncherModel.sDelayedUpdateBuffer;
                    ItemInfo iteminfo;
                    ContentValues contentvalues;
                    for (Iterator iterator = LauncherModel.sDelayedUpdateBuffer.iterator(); iterator.hasNext(); LauncherModel.updateItemInDatabase(context, iteminfo.id, contentvalues))
                    {
                        iteminfo = (ItemInfo)iterator.next();
                        contentvalues = new ContentValues();
                        contentvalues.put("launchCount", Integer.valueOf(iteminfo.launchCount));
                        contentvalues.put("itemFlags", Integer.valueOf(iteminfo.itemFlags));
                    }

                    LauncherModel.sDelayedUpdateBuffer.clear();
                }

            
            });
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }
    
    static ContentProviderOperation getMoveItemOperation(ItemInfo iteminfo, 
            long container, long screen, int cx, int cy) {
        ContentValues contentvalues = new ContentValues();
        contentvalues.put("container", Long.valueOf(container));
        contentvalues.put("cellX", Integer.valueOf(cx));
        contentvalues.put("cellY", Integer.valueOf(cy));
        contentvalues.put("spanX", Integer.valueOf(iteminfo.spanX));
        contentvalues.put("spanY", Integer.valueOf(iteminfo.spanY));
        contentvalues.put("screen", Long.valueOf(screen));
        return ContentProviderOperation.newUpdate(LauncherSettings.Favorites.getContentUri(iteminfo.id)).withValues(contentvalues).build();
    }
    
    private boolean stopLoaderLocked() {
        boolean isLaunching = false;
        LoaderTask oldTask = mLoaderTask;
        if (oldTask != null) {
            if (oldTask.isLaunching()) {
                isLaunching = true;
            }
            oldTask.stopLocked();
        }
        return isLaunching;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

    }
    
    public Bitmap getFallbackIcon(){
        return Bitmap.createBitmap(mIconCache.getDefaultIcon());
    }

    public Bitmap getIconFromCursor(Cursor cursor, int iconIndex){
        if (LOGD) {
            Log.d(TAG, "getIconFromCursor app="
                    + cursor.getString(cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE)));
        }
        byte[] data = cursor.getBlob(iconIndex);
        if (data == null) {
            return null;
        }
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }
    
    public ShortcutInfo getShortcutInfo(PackageManager packageManager, Intent intent, 
            Context context, Cursor cursor, int iconIndex, int iTitle){
        Bitmap bitmap = null;
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        ComponentName componentName = intent.getComponent();
        
        if (componentName != null) {
            ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                bitmap = mIconCache.getIcon(componentName, resolveInfo);
            }
            if (bitmap == null && cursor != null) {
                bitmap = getIconFromCursor(cursor, iconIndex);
            }
            if (bitmap == null) {
                bitmap = getFallbackIcon();
                shortcutInfo.usingFallbackIcon = true;
            }
            shortcutInfo.setIcon(bitmap);
            if (resolveInfo != null) {
                shortcutInfo.title = resolveInfo.activityInfo.loadLabel(packageManager);
            }
            if (shortcutInfo.title == null && cursor != null) {
                shortcutInfo.title = cursor.getString(iTitle);
            }
            if (shortcutInfo.title == null) {
                shortcutInfo.title = componentName.getClassName();
            }
        } else {
            shortcutInfo = null;
        }
        shortcutInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        return shortcutInfo;
    }
    
    public ShortcutInfo getShortcutInfo(Intent intent, Cursor cursor, Context context, 
            int iIconType, int iIconPackage, int iIconResource, int iIcon, int iTitle){
        Bitmap bitmap = null;
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        shortcutInfo.itemFlags = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ITEMFLAGS));
        shortcutInfo.title = cursor.getString(iTitle);
        shortcutInfo.mIconType = cursor.getInt(iIconType);
        switch(shortcutInfo.mIconType){
        case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
            String packageName = cursor.getString(iIconPackage);
            String resourceName = cursor.getString(iIconResource);
            PackageManager packageManager = context.getPackageManager();
            if (context.getPackageName().equals(packageName))
                shortcutInfo.isRetained = true;
            
            try {
                Resources resources = packageManager.getResourcesForApplication(packageName);
                if (resources != null) {
                    final int id = resources.getIdentifier(resourceName, null, null);
                    bitmap = Utilities.createIconBitmap(
                            resources.getDrawable(id), context);
                }
            } catch (Exception e) {
                // drop this.  we have other places to look for icons
            }
            // the db
            if (bitmap == null) {
                bitmap = getIconFromCursor(cursor, iIcon);
            }
            // the fallback icon
            if (bitmap == null) {
                bitmap = getFallbackIcon();
                shortcutInfo.usingFallbackIcon = true;
            }
            break;
        case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
            bitmap = getIconFromCursor(cursor, iIcon);
            if (bitmap == null) {
                bitmap = getFallbackIcon();
                shortcutInfo.usingFallbackIcon = true;
            }
            break;
        case 2:
            //shortcutInfo.intent = intent;
            //shortcutInfo.loadContactInfo(context);
            break;
        case 3:
            //shortcutInfo.intent = intent;
            //shortcutInfo.loadToggleInfo(context);
            break;
        default:
            bitmap = getFallbackIcon();
            shortcutInfo.usingFallbackIcon = true;
            break;
        }
        if (LauncherSettings.Favorites.ITEM_TYPE_FOLDER != shortcutInfo.mIconType 
                && LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER != shortcutInfo.mIconType){
            shortcutInfo.setIcon(bitmap);
            shortcutInfo.wrapIconWithBorder(context);
        }
        
        return shortcutInfo;
    }
    
    public void stopLoader(){
        synchronized (mLock) {
            if (mLoaderTask != null){
                mLoaderTask.stopLocked();
            }
        }
    }
    
    public void stopLoaderAsync() {
        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                stopLoader();
            }
        });
    }
    
    /**
     * 功能： 更新保存的图标信息
     * @param context
     * @param shortcutInfo
     * @param cursor
     * @param iconIndex
     */
    public void updateSavedIcon(Context context, ShortcutInfo shortcutInfo, Cursor cursor, int iconIndex){
        boolean needSave = true;
        if (shortcutInfo.mIconType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                || shortcutInfo.usingFallbackIcon) {
            return;
        }
        // 从数据库中读取图标信息
        byte[] data = cursor.getBlob(iconIndex);
        
        try {
            if (data != null) {
                Bitmap saved = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap loaded = shortcutInfo.getIcon(mIconCache);
                needSave = !saved.sameAs(loaded);
            } else {
                needSave = true;
            }
        } catch (Exception e) {
            needSave = true;
        }
        if (needSave) {
            Log.d(TAG, "going to save icon bitmap for info=" + shortcutInfo);
            // This is slower than is ideal, but this only happens once
            // or when the app is updated with a new icon.
            updateItemInDatabase(context, shortcutInfo);
        }
    }
    
    /**
     * Update an item to the database in a specified container.
     */
    public static void updateItemInDatabase(final Context context, long id, final ContentValues contentvalues) {
        final Uri uri = LauncherSettings.Favorites.getContentUri(id);
        final ContentResolver cr = context.getContentResolver();
        
        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (cr.update(uri, contentvalues, null, null) >= 0) {
                    return;
                }else {
                    throw new RuntimeException("update Item in database failed.");
                }
            }
        });
    }

    /**
     * 功能： 在数据库中更新项目
     * @param context
     * @param iteminfo
     */
    static void updateItemInDatabase(Context context, ItemInfo iteminfo){
        final ContentValues values = new ContentValues();
        iteminfo.onAddToDatabase(values);
        
        if (LOGD) {
            Object aobj[] = new Object[4];
            aobj[0] = Integer.valueOf(iteminfo.cellX);
            aobj[1] = Integer.valueOf(iteminfo.cellY);
            aobj[2] = Long.valueOf(iteminfo.screenId);
            aobj[3] = Long.valueOf(iteminfo.container);
            Log.d(TAG, String.format("Update item in database (%d, %d) of screen %d under container %d", aobj));
        }
        
        updateItemInDatabase(context, iteminfo.id, values);
    }
    
    public AllAppsList getAllAppsList() {
        return mAllAppsList;
    }
    
    public static int colToInt(ItemQuery.COL col){
        int i = 0;
        switch (col) {
        case ID:
            i = 0;
            break;
        case TITLE:
            i = 1;
            break;
        case INTENT:
            i = 2;
            break;
        case CONTAINER:
            i = 3;
            break;
        case SCREEN:
            i = 4;
            break;
        case CELLX:
            i = 5;
            break;
        case CELLY:
            i = 6;
            break;
        case SPANX:
            i = 7;
            break;
        case SPANY:
            i = 8;
            break;
        case ITEMTYPE:
            i = 9;
            break;
        case APPWIDGETID:
            i = 10;
            break;
        case ISSHORTCUT:
            i = 11;
            break;
        case ICONTYPE:
            i = 12;
            break;
        case ICONPACKAGE:
            i = 13;
            break;
        case ICONRESOURCE:
            i = 14;
            break;
        case ICON:
            i = 15;
            break;
        case URI:
            i = 16;
            break;
        case DISPLAYMODE:
            i = 17;
            break;
        case LAUNCHERCOUNT:
            i = 18;
            break;
        case SORTMODE:
            i = 19;
            break;
        case ITEMFLAGS:
            i = 20;
            break;
        }
        return i;
    }
    
}
