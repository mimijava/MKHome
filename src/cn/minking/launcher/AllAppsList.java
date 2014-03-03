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
    
    public void clear() {
        added.clear();
        removed.clear();
    }
    
    private void loadShortcuts(Context context, Intent intent, String packageName){
        Cursor cursor;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = LauncherSettings.Favorites.CONTENT_URI;
        String columns[] = ItemQuery.COLUMNS;
        String selectargs[] = new String[]{intent.toUri(0).toString(), packageName};
        
        cursor = contentResolver.query(uri, columns, null, null, null);
        
        if (cursor == null) {
            return;
        }
        
        try {
            if (cursor.moveToNext()){
                ShortcutInfo shortcutinfo = ((LauncherApplication)context).getModel()
                        .getShortcutInfo(intent, cursor, context, 
                                LauncherModel.colToInt(ItemQuery.COL.ICONTYPE), LauncherModel.colToInt(ItemQuery.COL.ICONPACKAGE),
                                LauncherModel.colToInt(ItemQuery.COL.ICONRESOURCE), LauncherModel.colToInt(ItemQuery.COL.ICON),
                                LauncherModel.colToInt(ItemQuery.COL.TITLE));
                shortcutinfo.load(cursor);
                shortcutinfo.intent = intent;
                if (LOGD) {
                    Object aobj[] = new Object[columns.length];
                    
                    aobj[0] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ID)));
                    aobj[1] = String.valueOf(cursor.getString(LauncherModel.colToInt(ItemQuery.COL.TITLE)));
                    //aobj[2] = String.valueOf(cursor.getString(LauncherModel.colToInt(ItemQuery.COL.INTENT)));
                    aobj[3] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.CONTAINER)));
                    aobj[4] = Long.valueOf(cursor.getLong(LauncherModel.colToInt(ItemQuery.COL.SCREEN)));
                    aobj[5] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.CELLX)));
                    aobj[6] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.CELLY)));
                    aobj[7] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.SPANX)));
                    aobj[8] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.SPANY)));
                    aobj[9] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ITEMTYPE)));
                    aobj[10] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.APPWIDGETID)));
                    aobj[11] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ISSHORTCUT)));
                    aobj[12] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ICONTYPE)));
                    aobj[13] = String.valueOf(cursor.getString(LauncherModel.colToInt(ItemQuery.COL.ICONPACKAGE)));
                    aobj[14] = String.valueOf(cursor.getString(LauncherModel.colToInt(ItemQuery.COL.ICONRESOURCE)));
                    aobj[15] = Long.valueOf(cursor.getLong(LauncherModel.colToInt(ItemQuery.COL.SCREEN)));
                    aobj[16] = String.valueOf(cursor.getString(LauncherModel.colToInt(ItemQuery.COL.URI)));
                    aobj[17] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.DISPLAYMODE)));
                    aobj[18] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.LAUNCHERCOUNT)));
                    aobj[19] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.SORTMODE)));
                    aobj[20] = Integer.valueOf(cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ITEMFLAGS)));
                    
                    Log.w(TAG, String.format("loadShortcuts("
                            + "\n ID:%d, TITLE:%s, "
                            + "\n Intent: %s, con: %d, scr: %d, cx: %d, cy:%d, "
                            + "sx: %d, sy: %d, it: %d, ai: %d,"
                            + "\n  ic: %d, it: %d, Ip: %s, is: %s, %d,"
                            + "\n uri: %s, %d, %d, %d, %d) ", aobj));
                }
                add(shortcutinfo);
            }
            if (cursor != null){
                cursor.close();
            }   
        } catch (Exception e) {
            Log.e(TAG, "Can't load postion for app " + packageName);
            if (cursor != null) cursor.close();
        }
    }
    
    private void addApp(Context context, ResolveInfo resolveinfo){
        ShortcutInfo shortcutinfo = new ShortcutInfo(context, resolveinfo);
        loadPosition(context, resolveinfo.activityInfo.packageName, shortcutinfo);
        add(shortcutinfo);
        loadShortcuts(context, shortcutinfo.intent, resolveinfo.activityInfo.packageName);
    }
    
    private void loadPosition(Context context, String packageName, ShortcutInfo shortcutinfo){
        ContentResolver contentResolver = context.getContentResolver();
        sSelectionArgs[0] = shortcutinfo.intent.toUri(0).toString();
        sSelectionArgs[1] = packageName;
        
        Cursor cursor = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI,
                PositionQuery.COLUMNS, null, null, null);
                //PositionQuery.COLUMNS, "intent=? AND iconPackage=?", sSelectionArgs, null);
        
        if (cursor == null) {
            return;
        }
        
        try {
            if (cursor.moveToNext()) {
                shortcutinfo.screenId = cursor.getInt(0);
                shortcutinfo.cellX = cursor.getInt(1);
                shortcutinfo.cellY = cursor.getInt(2);
                shortcutinfo.container = cursor.getLong(3);
                shortcutinfo.id = cursor.getInt(4);
                shortcutinfo.spanY = 1;
                shortcutinfo.spanX = 1;
                
                Object aobj[] = new Object[5];
                aobj[0] = shortcutinfo.title;
                aobj[1] = Integer.valueOf(shortcutinfo.cellX);
                aobj[2] = Integer.valueOf(shortcutinfo.cellY);
                aobj[3] = Long.valueOf(shortcutinfo.screenId);
                aobj[4] = Long.valueOf(shortcutinfo.container);
                
                Log.d(TAG, String.format("Loaded application %s at (%d, %d) of screen %d under container %d", aobj));
            }
        } catch (Exception e) {
            Log.e(TAG, (new StringBuilder()).append("Can't load postion for app ").append(shortcutinfo.title).toString());
            if (cursor != null) cursor.close();
        }
        cursor.close();
        shortcutinfo.screenId = -1L;
    }
    
    public void add(ShortcutInfo shortcutinfo){
        added.add(shortcutinfo);
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
    
    public void updatePackage(Context context, String packageName){
        removed.add(new RemoveInfo(packageName, true));
        addPackage(context, packageName);
    }
}
