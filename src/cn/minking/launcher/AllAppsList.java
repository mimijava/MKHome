package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    AllAppsList.java
 * 创建时间：    2014-03-03
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 2014030301: APP LIST
 * ====================================================================================
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AllAppsList {
    private static interface PositionQuery{
        public static final String COLUMNS[] 
                = new String[]{
                    "screen",
                    "cellX",
                    "cellY",
                    "container",
                    "_id"
                    };
    }

    
    class RemoveInfo{
        public final String packageName;
        public final boolean replacing;
        
        public RemoveInfo(String name, boolean b_replace) {
            packageName = name;
            replacing = b_replace;
        }
    }
    
    
    private final static String TAG = "MKHome.AllAppsList";
    private final static Boolean LOGD = true;
    private static String sSelectionArgs[] = new String[2];
    public ArrayList<ShortcutInfo> added;
    public ArrayList<RemoveInfo> removed;
    
    public AllAppsList() {
        added = new ArrayList<ShortcutInfo>(3);
        removed = new ArrayList<RemoveInfo>();
    }
    
    private void loadShortcuts(Context context, Intent intent, String packageName){
        Uri uri = LauncherSettings.Favorites.CONTENT_URI;
        String as[] = ItemQuery.COLUMNS;
        String as1[] = new String[]{intent.toUri(0).toString(), packageName};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, as, "intent=? AND iconPackage=? AND itemType=1", as1, null);;
        
        if (cursor == null) return;
        
        final int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
        final int iconTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
        final int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
        final int iconPackageIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
        final int iconResourceIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
        
        try {
            if (cursor.moveToNext()) {
                ShortcutInfo shortcutinfo = ((LauncherApplication)context).getModel().
                        getShortcutInfo(intent, cursor, context, 
                                iconTypeIndex, iconPackageIndex, iconResourceIndex, iconIndex, titleIndex);
                shortcutinfo.load(cursor);
                shortcutinfo.intent = intent;
                add(shortcutinfo);
            }
            cursor.close();
        } catch (Exception e) {
            cursor.close();
        }
    }
    
    private void addApp(Context context, ResolveInfo resolveinfo){
        ShortcutInfo shortcutInfo = new ShortcutInfo(context, resolveinfo);
        loadPosition(context, resolveinfo.activityInfo.packageName, shortcutInfo);
        add(shortcutInfo);
        loadShortcuts(context, shortcutInfo.intent, resolveinfo.activityInfo.packageName);
    }
    
    private void loadPosition(Context context, String packageName, ShortcutInfo shortcutInfo){
        ContentResolver contentResolver = context.getContentResolver();
        sSelectionArgs[0] = shortcutInfo.intent.toUri(0).toString();
        sSelectionArgs[1] = packageName;
        
        Cursor cursor = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI,
                PositionQuery.COLUMNS, "intent=? AND iconPackage=? AND itemType=0", sSelectionArgs, null);
        
        if (cursor == null) {
            shortcutInfo.screenId = -1L;
            Log.e(TAG, (new StringBuilder()).append("Can't load postion for app ").append(shortcutInfo.title).toString());
            return;
        }
        
        try {
            if (cursor.moveToNext()) {
                // 如果已经存储在数据库中，则直接读取屏幕、位置、大小、ID、容器
                shortcutInfo.screenId = cursor.getInt(0);
                shortcutInfo.cellX = cursor.getInt(1);
                shortcutInfo.cellY = cursor.getInt(2);
                shortcutInfo.container = cursor.getLong(3);
                shortcutInfo.id = cursor.getInt(4);
                shortcutInfo.spanY = 1;
                shortcutInfo.spanX = 1;
                
                Object aobj[] = new Object[5];
                aobj[0] = shortcutInfo.title;
                aobj[1] = Integer.valueOf(shortcutInfo.cellX);
                aobj[2] = Integer.valueOf(shortcutInfo.cellY);
                aobj[3] = Long.valueOf(shortcutInfo.screenId);
                aobj[4] = Long.valueOf(shortcutInfo.container);
                
                if (LOGD) Log.d(TAG, String.format("Loaded application %s at (%d, %d) of screen %d under container %d", aobj));
            } else {
                // 否则分配坐标及位置
                shortcutInfo.screenId = -1L;
                if (LOGD) Log.e(TAG, (new StringBuilder()).append("Can't load postion for app ").append(shortcutInfo.title).toString());
            }
        } catch (Exception e) {
            if (LOGD) Log.e(TAG, (new StringBuilder()).append("Can't load postion for app ").append(shortcutInfo.title).toString());
            if (cursor != null) cursor.close();
        }
        cursor.close();
    }
    
    public void add(ShortcutInfo shortcutInfo){
        added.add(shortcutInfo);
    }
    
    public void addPackage(Context context, String packageName){
        List<ResolveInfo> packList = ScreenUtils.findActivitiesForPackage(context, packageName);
        if ((packList == null) || (packList.size() <= 0)) {
            return;
        }
        Iterator<ResolveInfo> iterator = packList.iterator();
        while(iterator.hasNext()){
            addApp(context, iterator.next());
        }
        
    }
    
    public void clear() {
        added.clear();
        removed.clear();
    }
    
    public void removePackage(String packageName) {
        removePackage(packageName, false);
    }

    public void removePackage(String packageName, boolean flag) {
        removed.add(new RemoveInfo(packageName, flag));
    }
    
    public void updatePackage(Context context, String packageName){
        removed.add(new RemoveInfo(packageName, true));
        addPackage(context, packageName);
    }
}
