package cn.minking.launcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import cn.minking.launcher.gadget.GadgetInfo;
import cn.minking.launcher.upsidescene.SceneData;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;

public class LauncherModel extends BroadcastReceiver {
	private class LoaderTask implements Runnable{
		private final ContentResolver mContentResolver;
		private Context mContext;
		private HashSet<ComponentName> mInstalledComponents;
		private boolean mIsJustRestoreFinished = false;
        private boolean mIsLaunching = false;
        private boolean mLoadAndBindStepFinished = false;
        private PackageManager mManager = null;
        private boolean mStopped;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
		public boolean isLaunching() {
            return mIsLaunching;
        }
		
		public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                this.notify();
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
	
	
	public static interface Callbacks{

        public abstract void bindAppWidget(LauncherAppWidgetInfo launcherappwidgetinfo);

        public abstract void bindAppsAdded(ArrayList arraylist);

        public abstract void bindAppsRemoved(ArrayList arraylist);

        public abstract void bindFolders(HashMap hashmap);

        public abstract void bindGadget(GadgetInfo gadgetinfo);

        public abstract void bindItems(ArrayList arraylist, int i, int j);

        public abstract void bindUpsideScene(SceneData scenedata);

        public abstract void finishBindingMissingItems();

        public abstract void finishBindingSavedItems();

        public abstract void finishLoading();

        public abstract int getCurrentWorkspaceScreen();

        public abstract void reloadWidgetPreview();

        public abstract void startBinding();

        public abstract void startLoading();
    }
	
	
	private static HashSet<String> sDelayedUpdateBuffer = null;
	private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
	private static final Handler sWorker = new Handler(sWorkerThread.getLooper());
	private AllAppsList mAllAppsList = null;
	private WeakReference<Callbacks> mCallbacks = null;
	private DeferredHandler mHandler = new DeferredHandler();
	private IconCache mIconCache = null;
	private LoaderTask mLoaderTask = null;
	private boolean mWorkspaceLoaded = false;
	private final Object mAllAppsListLock = new Object();
	private final Object mLock = new Object();
	private final ArrayList<LauncherAppWidgetInfo> mAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
	private final ArrayList<GadgetInfo> mGadgets = new ArrayList<GadgetInfo>();
	private final HashMap<Long, FolderInfo> mFolders = new HashMap<Long, FolderInfo>();
	private final ArrayList<Object> mItems = new ArrayList<Object>();
	private final HashMap<ComponentName, Long> mLoadedApps = new HashMap<ComponentName, Long>();
	private final HashSet<String> mLoadedPackages = new HashSet<String>();
	private final HashSet<String> mLoadedPresetPackages = new HashSet<String>();
	private final HashSet<String> mLoadedUris = new HashSet<String>();
	
	public LauncherModel(LauncherApplication launcherApplication, IconCache iconCache) {
	}
	
	public void initialize(Callbacks callbacks) {
		synchronized (mLock) {
			mCallbacks = new WeakReference<Callbacks>(callbacks);
		}
	}
	
	public void startLoader(Context context, boolean isLaunching) {
		synchronized (mLock) {
			if (mCallbacks != null && mCallbacks.get() != null) {
				isLaunching = isLaunching || stopLoaderLocked();
			}
		}
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

}
