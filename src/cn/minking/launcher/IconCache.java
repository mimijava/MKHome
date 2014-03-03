package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    IconCache.java
 * 创建时间：    2014-03-03
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 2014030301 ： 图标显示缓存类
 * ====================================================================================
 */
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;

public class IconCache {
    
    private static final String TAG = "MKHome.IconCache";
    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;
    
    private static class CacheEntry{
        public Bitmap icon;
        public String title;
    }
    
    private final Utilities.BubbleText mBubble;
    private final HashMap<ComponentName, CacheEntry> mCache = 
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private final LauncherApplication mContext;
    private Bitmap mDefaultIcon;
    private final PackageManager mPackageManager;

    private Bitmap makeDefaultIcon(){
        return Utilities.createIconBitmap(mPackageManager.getDefaultActivityIcon(), mContext);
    }
    
    private CacheEntry cacheLocked(ComponentName componentname, ResolveInfo resolveinfo, boolean flag){
        CacheEntry cacheentry = (CacheEntry)mCache.get(componentname);
        if (cacheentry == null){
            cacheentry = new CacheEntry();
            mCache.put(componentname, cacheentry);

            cacheentry.title = resolveinfo.loadLabel(mPackageManager).toString();
            if (cacheentry.title == null){
                cacheentry.title = resolveinfo.activityInfo.name;
            }
            
            cacheentry.icon = Utilities.createIconBitmap(resolveinfo.activityInfo.
                    loadIcon(mPackageManager), mContext);
        }
        return cacheentry;
    }
    
    public void flush(){
        synchronized (mCache) {
            mCache.clear();
        }
        return;
    }

    public Bitmap getDefaultIcon(){
        return mDefaultIcon;
    }
    
    public void updateDefaultIcon(){
        mDefaultIcon = makeDefaultIcon();
    }
    
    public IconCache(LauncherApplication launcherApplication) {
        mContext = launcherApplication;
        mPackageManager = launcherApplication.getPackageManager();
        mBubble = new Utilities.BubbleText(launcherApplication);
        mDefaultIcon = makeDefaultIcon();
    }
    
    public Bitmap getIcon(ComponentName componentname, ResolveInfo resolveinfo){
        synchronized (mCache) {
            Bitmap bitmap;
            if (resolveinfo == null || componentname == null)
                bitmap = null;
            else
                bitmap = cacheLocked(componentname, resolveinfo, false).icon;
            return bitmap;
        }
    }
    
    public Bitmap getIcon(Intent intent, int i){
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, false);
            return entry.icon;
        }
    }
    
    public void remove(String s){
        
    }
}
