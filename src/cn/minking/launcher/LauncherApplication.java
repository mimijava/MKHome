package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    LauncherApplication.java
 * 创建时间：    2013-XX-XX
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 
 * ====================================================================================
 */
import java.lang.ref.WeakReference;

import android.app.Application;
import android.content.*;

public class LauncherApplication extends Application {
    private static float sScreenDensity = 0.0F;
    
    /// M: 处理图标显示的缓冲
    private IconCache mIconCache = null;
    private boolean mJustRestoreFinished = false;
    private Launcher mLauncher = null;
    WeakReference<LauncherProvider> mLauncherProvider = null;
    private LauncherModel mModel = null;
    
    public LauncherApplication() {
    }
    
    Launcher getLauncher(){
        return mLauncher;
    }
    
    LauncherModel getModel() {
        return mModel;
    }
    
    LauncherProvider getLauncherProvider(){
        return (LauncherProvider)mLauncherProvider.get();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(mModel);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sScreenDensity = getResources().getDisplayMetrics().density;
        mIconCache = new IconCache(this);
        mModel = new LauncherModel(this, mIconCache);
        IntentFilter intentfilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentfilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentfilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentfilter.addDataScheme("package");
        registerReceiver(mModel, intentfilter);
        intentfilter = new IntentFilter();
        intentfilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentfilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        registerReceiver(mModel, intentfilter);
        intentfilter = new IntentFilter();
        intentfilter.addAction("android.intent.action.ACCESS_CONTROL_CHANGED");
        registerReceiver(mModel, intentfilter);
    }
    
    public LauncherModel setLauncher(Launcher launcher) {
        mLauncher = launcher;
        mModel.initialize(launcher);
        return mModel;
    }
    
    public void setLauncherProvider(LauncherProvider launcherProvider) {
        mLauncherProvider = new WeakReference<LauncherProvider>(launcherProvider);
    }
    
    public IconCache getIconCache(){
        return mIconCache;
    }
    
    public static Launcher getLauncher(Context context) {
        Object object = context.getApplicationContext();
        if (!(object instanceof LauncherApplication)) {
            object = null;
        }else {
            object = ((LauncherApplication)object).getLauncher();
        }
        return ((Launcher)(object));
    }
    
    public static float getScreenDensity(){
        return sScreenDensity;
    }
    
    public static void startActivity(Context context, Intent intent){
        Launcher launcher = getLauncher(context);
        if (launcher != null)
            launcher.startActivityEx(intent, null);
    }

    public static void startActivityForResult(Context context, Intent intent, int i){
        Launcher launcher = getLauncher(context);
        if (launcher != null)
            launcher.startActivityForResult(intent, i);
    }

    protected void attachBaseContext(Context context){
        super.attachBaseContext(context);
        
        // ResConfig 初始化
        ResConfig.Init(context);
    }

    public void setJustRestoreFinished() {
        mJustRestoreFinished = true;
    }
    
    public boolean isJustRestoreFinished() {
        boolean bFlag = false;
        if (mJustRestoreFinished) {
            mJustRestoreFinished  = false;
            bFlag = true;
        }
        return bFlag;
    }

}
