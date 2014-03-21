package cn.minking.launcher;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.R.bool;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ScreenUtils {
    
    // 桌面单屏信息
    static final class ScreenInfo{
        long screenId;
        int screenOrder;

        /**
         * 描述： 屏幕的ID及排序
         * @param id
         * @param order
         */
        public ScreenInfo(long id, int order){
            screenId = id;
            screenOrder = order;
        }
    }
    
    // 占据屏幕单项信息
    private static final class CellInfo {
        int cellX;
        int cellY;
        long screenId;
        int screenOrder;
        int spanX;
        int spanY;
    }
    
    // 屏幕数据库查询筛选数据
    private static interface ScreensQuery {
        public static final String COLUMNS[] = new String[]{"_id","screenOrder"};
    }
    
    // APP位置查询筛选数据
    private static interface AppPlaceQuery {

        public static final String COLUMNS[] 
                = new String[]{
                    "screenOrder",
                    "cellX",
                    "cellY",
                    "spanX",
                    "spanY"};
    }

    // 屏幕中被占用位置查询筛选数据
    private static interface OccupidQuery {
        public static final String COLUMNS[] 
                = new String[]{
                    "container",
                    "cellX",
                    "cellY",
                    "spanX",
                    "spanY",
                    "itemType",
                    "screen"};
    }
    
    final private static String TAG = "MKHome.ScreenUtils";
    final private static Boolean LOGD = true;
    private static String sInstalledComponentsArg = null;
    
    
    private static void addToHomeScreen(
            Context context, ResolveInfo resolveinfo, 
            SQLiteDatabase db, ArrayList<ScreenInfo> screenInfos, long id){
        if (id >= 0L) {
            Log.d(TAG, (new StringBuilder()).append("Updating home screen item ").append(id).toString());
            if (verifyItemPosition(db, id)) {
                updateFavorite(db, id, buildValuesForUpdate(context, resolveinfo));
                return;
            }
        }
        CellInfo cellinfo = findEmptyCell(context, db, screenInfos, 1, 1);
        if (cellinfo != null){
            db.insert("favorites", null, buildValuesForInsert(context, resolveinfo, cellinfo));
        }
        
    }
    
    private static ContentValues buildValuesForInsert(
        Context context, ResolveInfo resolveinfo, CellInfo cellinfo){
        ContentValues contentvalues = buildValuesForUpdate(context, resolveinfo);
        contentvalues.put("itemType", Integer.valueOf(LauncherSettings.Favorites.ITEM_TYPE_APPLICATION));
        contentvalues.put("container", Integer.valueOf(LauncherSettings.Favorites.CONTAINER_DESKTOP));
        contentvalues.put("iconPackage", resolveinfo.activityInfo.applicationInfo.packageName);
        contentvalues.put("iconType", Integer.valueOf(LauncherSettings.Favorites.ICON_TYPE_RESOURCE));
        contentvalues.put("screen", Long.valueOf(cellinfo.screenId));
        contentvalues.put("cellX", Integer.valueOf(cellinfo.cellX));
        contentvalues.put("cellY", Integer.valueOf(cellinfo.cellY));
        contentvalues.put("spanX", Integer.valueOf(cellinfo.spanX));
        contentvalues.put("spanY", Integer.valueOf(cellinfo.spanY));
        if (LOGD) {
            Object aobj[] = new Object[4];
            aobj[0] = contentvalues.get("title");
            aobj[1] = Long.valueOf(cellinfo.screenId);
            aobj[2] = Integer.valueOf(cellinfo.cellX);
            aobj[3] = Integer.valueOf(cellinfo.cellY);
            Log.d(TAG, String.format("Adding new app %s to screen %d, pos (%d, %d)", aobj));    
        }
        
        return contentvalues;
    }
    
    private static ContentValues buildValuesForUpdate(Context context, ResolveInfo resolveinfo){
        ContentValues contentvalues = new ContentValues();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(resolveinfo.activityInfo.applicationInfo.packageName,
                resolveinfo.activityInfo.name));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        contentvalues.put("intent", intent.toUri(0));
        contentvalues.put("title", resolveinfo.loadLabel(context.getPackageManager()).toString());
        contentvalues.put("itemFlags", Integer.valueOf(0));
        contentvalues.put("itemType", Integer.valueOf(LauncherSettings.Favorites.ITEM_TYPE_APPLICATION));
        return contentvalues;
    }
    
    private static void deleteFavorite(SQLiteDatabase db, long id){
        String as[] = new String[]{String.valueOf(id)};
        db.delete("favorites", "_id=?", as);
    }
    
    /**
     * 描述： 如果排序的屏幕数不足，则创建屏幕给予放ITEM
     * @param context
     * @param sqlitedatabase
     * @param screenInfos
     * @param screenOrder
     */
    private static void ensureEnoughScreensForCell(
            Context context, SQLiteDatabase sqlitedatabase, ArrayList<ScreenInfo> screenInfos, int screenOrder){
        int sOrder = Math.min(screenOrder + 1, 30);
        
        for (int k = screenInfos.size(); k < sOrder; k++){
            boolean flag;
            if (k != sOrder - 1){
                flag = false;
            } else { 
                flag = true;
            }
            screenInfos.add(insertScreen(context, sqlitedatabase, k, flag));
        }
    }
    
    static void ensureEnoughScreensForItem(
            Context context, SQLiteDatabase db, ArrayList<ScreenInfo> screenInfos, long id){
        String as[] = new String[]{String.valueOf(id)};
        ensureEnoughScreensForItem(context, db, screenInfos, "favorites._id=?", as);
    }
    
    static void ensureEnoughScreensForItem(
            Context context, SQLiteDatabase db, 
            ArrayList<ScreenInfo> screenInfos, String s, String as[]){
        Cursor cursor = db.query("favorites JOIN screens ON (screen=screens._id) ", AppPlaceQuery.COLUMNS, s, as, null, null, null);
        if (cursor == null) return;
        try {
            if (cursor.moveToNext()){
                int id = cursor.getInt(0);
                ensureEnoughScreensForCell(context, db, screenInfos, id);
            }
            cursor.close(); 
        } catch (Exception e) {
            cursor.close();
        }
    }
    
    static boolean fillEmptyCell(
            Context context, SQLiteDatabase sqlitedatabase, 
            ArrayList<ScreenInfo> arraylist, ContentValues contentvalues){
        CellInfo cellinfo = findEmptyCell(context, sqlitedatabase, 
                arraylist, contentvalues.getAsInteger("spanX").intValue(), 
                contentvalues.getAsInteger("spanY").intValue());
        boolean flag;
        if (cellinfo != null) {
            contentvalues.put("screen", Long.valueOf(cellinfo.screenId));
            contentvalues.put("cellX", Integer.valueOf(cellinfo.cellX));
            contentvalues.put("cellY", Integer.valueOf(cellinfo.cellY));
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }
    
    private static void fillOccupied(int occupied[][], int cx, int cy, int sx, int sy){
        for (int x = cx; x < cx + sx; x++) {
            for (int y = cy; y < cy + sy; y ++){
                occupied[x][y] = 1;
            }
        }
    }
    
    static Intent getLaunchableIntent(){
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return intent;
    }
    
    static List<ResolveInfo> findActivitiesForPackage(Context context, String packageName){
        Intent intent = getLaunchableIntent();
        intent.setPackage(packageName);
        return context.getPackageManager().queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
    }
    
    private static CellInfo findEmptyCell(Context context, SQLiteDatabase db, int sx, int sy){
        CellInfo cellinfo = null;
        
        CellInfo cellinfoT = new CellInfo();
        cellinfoT.screenOrder = 0;
        cellinfoT.cellX = 0;
        cellinfoT.cellY = 0;
        cellinfoT.spanX = sx;
        cellinfoT.spanY = sy;
        cellinfoT = findEmptyCell(context, db, cellinfoT, 
                "container=-100 AND screenOrder=(SELECT MAX(screenOrder) "
                + "FROM favorites JOIN screens ON (screen=screens._id)  "
                + "WHERE container=-100)", null, true);
        if (cellinfoT.screenOrder >= 30){
            for (int i = 29; i > 0 ; i--) {
                cellinfoT.screenOrder = i;
                String as[] = new String[]{String.valueOf(i)};
                cellinfoT = findEmptyCell(context, db, cellinfoT, 
                        "container=-100 AND screenOrder=?", as, false);
                if (cellinfoT.screenOrder == i){
                    break;
                }
            }
            cellinfo = cellinfoT;
        } else {
            cellinfo = cellinfoT;
        }
        
        return cellinfo;
    }
    
    private static CellInfo findEmptyCell(
            Context context, SQLiteDatabase db, 
            CellInfo cellinfo, String s, String as[], boolean flag){
        Cursor cursor = db.query(
                "favorites JOIN screens ON (screen=screens._id) ", 
                AppPlaceQuery.COLUMNS, s, as, null, null, "cellY ASC, cellX ASC");
        
        int at[] = new int[]{ResConfig.getCellCountX(), ResConfig.getCellCountY()};
        
        int[][] occupied = (int[][])Array.newInstance(Integer.TYPE, at);
        
        try {
            while (cursor != null && cursor.moveToNext()){
                int screenOrder = cursor.getInt(0);
                if (screenOrder != cellinfo.screenOrder){
                    cellinfo.screenOrder = screenOrder;
                } 
                fillOccupied(occupied, cursor.getInt(1), cursor.getInt(2), 
                        cursor.getInt(3), cursor.getInt(4));
            }
            cursor.close();
        } catch (Exception e) {
            if (cursor != null) cursor.close();
        }
        
        if (cursor != null) {
            cursor.close();
        }
        
        if (!findEmptyCell(occupied, cellinfo, flag)) {
            cellinfo.screenOrder = cellinfo.screenOrder + 1;
            cellinfo.cellX = 0;
            cellinfo.cellY = 0;
        }
        
        return cellinfo;
    }
    
    static CellInfo findEmptyCell(
            Context context, SQLiteDatabase db, 
            ArrayList<ScreenInfo> screenInfos, int sx, int sy){
        CellInfo cellinfo = null;
        
        if (sx <= ResConfig.getCellCountX() && sy <= ResConfig.getCellCountY()) {
            cellinfo = findEmptyCell(context, db, sx, sy);
            if (cellinfo != null && cellinfo.screenOrder < 30) {
                ensureEnoughScreensForCell(context, db, screenInfos, cellinfo.screenOrder);
                cellinfo.screenId = (screenInfos.get(cellinfo.screenOrder)).screenId;
            } else {
                Log.e(TAG, "Too many apps installed, not adding to home screen");
                cellinfo = null;
            }
        } else {
            cellinfo = null;
        }
        
        return cellinfo;
    }
    
    private static boolean findEmptyCell(int occupied[][], CellInfo cellinfo, boolean flag){
        boolean bFind = false;
        
        for (int x = ResConfig.getCellCountX() - 1; x >= 0; x--){
            for (int y = ResConfig.getCellCountY() - 1; y >= 0; y--){
                if (x + cellinfo.spanX > ResConfig.getCellCountX() 
                        || y + cellinfo.spanY > ResConfig.getCellCountY()){
                    break;
                }
                if (!testOccupied(occupied, x, y, cellinfo.spanX, cellinfo.spanY)){
                    bFind = true;
                    cellinfo.cellX = x;
                    cellinfo.cellY = y;
                    break;
                }
            }
            if (bFind) {
                break;
            }
        }
        
        return bFind;
    }
    
    /**
     * 功能： 根据给出的package名称得到返回的ID列表
     * @param context
     * @param db
     * @param packageName
     * @return
     */
    private static ArrayList<Long> getPackageItemIds(Context context, 
            SQLiteDatabase db, String packageName){
        Cursor cursor;
        String as[] = new String[]{"_id"};
        String sel[] = new String[]{packageName};
        ArrayList<Long> idList = new ArrayList<Long>();
        
        cursor = db.query("favorites", as, "iconPackage=? AND (itemType=0 OR itemFlags&1 != 0)",
                sel, null, null, null);
        
        if (cursor == null) return null;
        
        try {
            if (cursor.moveToNext()) {
                idList.add(Long.valueOf(cursor.getLong(0)));        
            }   
            cursor.close();
        } catch (Exception e) {
            cursor.close();
        }
        
        return idList;
    }
    
    /**
     * 功能： 往数据库中添加新屏幕
     * @param context
     * @param db
     * @param order
     * @param flag
     * @return
     */
    private static ScreenInfo insertScreen(Context context, 
            SQLiteDatabase db, int order, boolean flag){
        ContentValues contentvalues = new ContentValues();
        contentvalues.put("screenOrder", Integer.valueOf(order));
        long id = db.insert("screens", null, contentvalues);
        if (LOGD) {
            Log.d(TAG, (new StringBuilder()).append("Added screen id ").
                    append(id).append(" order ").append(order).toString()); 
        }
        if (flag){
            notifyChange(context, LauncherSettings.Screens.CONTENT_URI);
        }
        return new ScreenInfo(id, order);
    }
    
    /**
     * 描述： 从数据库中读取屏幕信息
     * @param db
     * @return
     */
    static ArrayList<ScreenInfo> loadScreens(SQLiteDatabase db){
        Cursor cursor = db.query("screens", 
                ScreensQuery.COLUMNS, null, null, null, null, "screenOrder ASC");
        
        if (cursor == null) return null;
        
        // 根据得到的屏幕数量创建screen list
        ArrayList<ScreenInfo> screenList = new ArrayList<ScreenInfo>(cursor.getCount());
        try {
            while (cursor.moveToNext()) {
                screenList.add(new ScreenInfo(cursor.getLong(0), cursor.getInt(1)));
            }   
            cursor.close();
        } catch (Exception e) {
            cursor.close();
        }
        return screenList;
    }
    
    private static void notifyChange(Context context, Uri uri){
        context.getContentResolver().notifyChange(uri, null);
    }
    
    static void removePackage(Context context, SQLiteDatabase db, String packageName){
        ArrayList<Long> idList = getPackageItemIds(context, db, packageName);
        int i = 0;
        
        while (i < idList.size()){
            deleteFavorite(db, (idList.get(i)).longValue());
            i++;
        }
    }
    
    private static boolean testOccupied(int occupied[][], int cx, int cy, int sx, int xy){
        boolean bOccupied = false;
        
        for (int x = cx; x < cx + sx; x++) {
            for (int y = cy; y < cy + xy; y++) {
                if (occupied[x][y] != 0) {
                    bOccupied = true;
                    break;
                }
            }
            if (bOccupied) {
                break;
            }
        }
        
        return bOccupied;
    }
    
    private static void updateFavorite(SQLiteDatabase db, long id, ContentValues values){
        String as[] = new String[]{String.valueOf(id)};
        db.update("favorites", values, "_id=?", as);
    }
    
    static void updateHomeScreen(Context context, SQLiteDatabase db,
            ArrayList<ScreenInfo> screenInfos, String packageName, boolean flag){
        List<ResolveInfo> list = findActivitiesForPackage(context, packageName);
        ArrayList<Long> idList = getPackageItemIds(context, db, packageName);
        
        if (LOGD) {
            Log.d(TAG, (new StringBuilder()).
                    append("Updating home screen for package ").
                    append(packageName).append(" with ").
                    append(idList.toString()).toString());  
        }
        
        if (list != null && list.size() > 0) {
            int j = 0;
            Iterator<ResolveInfo> iterator = list.iterator();
            while (iterator.hasNext()){
                ResolveInfo resolveInfo = iterator.next();
                long id = -1L;
                if (j < idList.size()) {
                    id = ((Long)idList.get(j)).longValue();
                }
                addToHomeScreen(context, resolveInfo, db, screenInfos, id);
                j++;
            }
            
            if (!flag) {
                int i = j;
                while (i < idList.size()){
                    if (LOGD) {
                        Log.d(TAG, (new StringBuilder()).
                                append("Removing useless home screen item ").
                                append(idList.get(i)).toString());  
                    }
                    deleteFavorite(db, ((Long)idList.get(i)).longValue());
                    i++;
                }
                
                if (list == null || list.isEmpty()){
                    String as[] = new String[]{packageName};
                    db.delete("favorites", "iconPackage=?", as);
                }
            }
        } else {
            if (LOGD) {
                Log.d(TAG, (new StringBuilder()).
                        append("No activities found for package ").append(packageName).toString());
            }
        }
    }
    
    /**
     * 功能： 将读取的APP类名以静态字符变量存储在sInstalledComponentsArg
     * @param context
     */
    static void updateInstalledComponentsArg(Context context){
        List<ResolveInfo> list = context.getPackageManager().
                queryIntentActivities(getLaunchableIntent(), PackageManager.PERMISSION_GRANTED);
        
        if (!list.isEmpty()) {
            StringBuilder stringbuilder = new StringBuilder().append('(');
            Iterator<ResolveInfo> iterator = list.iterator();
            while (iterator.hasNext()){
                ResolveInfo resolveinfo = iterator.next();
                if (!TextUtils.isEmpty(resolveinfo.activityInfo.packageName) 
                        && !TextUtils.isEmpty(resolveinfo.activityInfo.name)){
                    stringbuilder.append(
                            (new StringBuilder()).append("'").
                            append(resolveinfo.activityInfo.packageName).
                            append("'").append(",").toString());
                }
            }
            stringbuilder.setCharAt(stringbuilder.length() - 1, ')');
            sInstalledComponentsArg = stringbuilder.toString();
        }
    }
    
    /**
     * 功能： 从数据库中查询ID为id的位置信息
     */
    static boolean verifyItemPosition(SQLiteDatabase db, long id){
        boolean flag = false;
        
        int container = -1;
        int cellX = -1;
        int cellY = -1;
        int spanX = -1;
        int spanY = -1;
        int itemType = -1;
        int screen = -1;
        
        String as[] = OccupidQuery.COLUMNS;
        String sel[] = new String[]{String.valueOf(id)};
        Cursor cursor = db.query("favorites", as, "_id=?", sel, null, null, null);
        
        if (cursor == null) return flag;
        
        if (cursor.getCount() == 1 && cursor.moveToFirst()){
            if (!cursor.isNull(0)){
                container = cursor.getInt(0);
            }
            
            if (!cursor.isNull(1)){
                cellX = cursor.getInt(1);
            }
            
            if (!cursor.isNull(2)){
                cellY = cursor.getInt(2);
            }
            
            if (!cursor.isNull(3)){
                spanX = cursor.getInt(3);
            }
            
            if (!cursor.isNull(4)){
                spanY = cursor.getInt(4);
            }
            
            if (!cursor.isNull(5)){
                itemType = cursor.getInt(5);
            }
            
            if (!cursor.isNull(6)){
                screen = cursor.getInt(6);
            }
            
            if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
                String as1[] = OccupidQuery.COLUMNS;
                String sel1[] = new String[]{String.valueOf(LauncherSettings.Favorites.CONTAINER_HOTSEAT)};
                
                Cursor c1 = db.query("favorites", as1, "container=?", sel1, null, null, null);
                int iGetCount = c1.getCount();
                int iHotseatMaxCount = ResConfig.getHotseatCount();
                if (iGetCount < iHotseatMaxCount) {
                    flag = true;
                    c1.close();
                } else {
                    deleteFavorite(db, id);
                    flag = false;
                    c1.close();
                }
            }
            
            if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (screen == -1 || cellX == -1 || cellY == -1) {
                    flag = false;
                } else {
                    int count;
                    String sqlString = (new StringBuilder()).append("SELECT _id FROM favorites WHERE container=-100 AND screen=").
                            append(screen).append(" AND (").append("cellX").append("-").
                            append(cellX + spanX).append(")*(").append("cellX").append("+").
                            append("spanX").append("-").append(cellX).append(")< 0 AND (").
                            append("cellY").append("-").append(cellY + spanY).append(")*(").
                            append("cellY").append("+").append("spanY").append("-").
                            append(cellY).append(")< 0 AND ").append("_id").append("!=").
                            append(id).toString();
                    if (sInstalledComponentsArg != null){
                        sqlString = (new StringBuilder()).append(sqlString).append(" AND ((itemType<=1 AND iconPackage IN ").
                                append(sInstalledComponentsArg).append(") OR ").append("itemType").
                                append(">").append(1).append(")").toString();
                    }
                    Cursor c2 = db.rawQuery(sqlString, null);
                    if (c2 != null) {
                        count = c2.getCount();
                        if (count <= 0) {
                            Cursor c3 = db.rawQuery((new StringBuilder()).append("SELECT _id FROM screens WHERE _id=").
                                    append(screen).append(";").toString(), null);
                            if (c3 != null) {
                                if (c3.getCount() <= 0){
                                    deleteFavorite(db, id);
                                    flag = false;
                                } else {
                                    flag = true;
                                }
                                c3.close();
                            }
                            
                            if (flag) {
                                String occupid[] = OccupidQuery.COLUMNS;
                                String selOcc[] = new String[]{String.valueOf(id)};
                                
                                Cursor c4 = db.query("favorites", occupid, "_id=?", selOcc, null, null, null);
                                if (c4 != null) {
                                    if (cursor.getCount() == 1) {
                                        flag = true;
                                    } else {
                                        flag = false;
                                    }
                                    c4.close();
                                }
                            }
                        } else {
                            deleteFavorite(db, id);
                            flag = false;
                        }
                        c2.close(); 
                    }
                }
            }
        }
        cursor.close();
        return flag;
    }
    
    
    
}