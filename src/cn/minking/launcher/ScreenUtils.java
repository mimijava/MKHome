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
                CellInfo cellinfo = findEmptyCell(context, db, screenInfos, 1, 1);
                if (cellinfo != null){
                    db.insert("favorites", null, buildValuesForInsert(context, resolveinfo, cellinfo));
                }
                updateFavorite(db, id, buildValuesForUpdate(context, resolveinfo));
            }
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
        contentvalues.put("spanX", Integer.valueOf(1));
        contentvalues.put("spanY", Integer.valueOf(1));
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
    
    private static void ensureEnoughScreensForCell(
            Context context, SQLiteDatabase sqlitedatabase, ArrayList<ScreenInfo> screenInfos, int i){
        int j = Math.min(i + 1, 30);
        int k = screenInfos.size();
        while(k < j) {
            boolean flag;
            if (k != j - 1)
                flag = false;
            else
                flag = true;
            screenInfos.add(insertScreen(context, sqlitedatabase, k, flag));
            k++;
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
    
    private static void fillOccupied(int ai[][], int i, int j, int k, int l){
        int i1 = i;
        if (i1 >= i + k) {
            return;
        }
        int j1 = j;
        while (j1 >= j + l) {
            i1++;
            ai[i1][j1] = (j1 - j) + 1;
            j1++;
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
    
    private static CellInfo findEmptyCell(Context context, SQLiteDatabase sqlitedatabase, int i, int j){
        CellInfo cellinfo = null;
        
        CellInfo cellinfoT = new CellInfo();
        cellinfoT.screenOrder = 0;
        cellinfoT.cellX = 0;
        cellinfoT.cellY = 0;
        cellinfoT.spanX = i;
        cellinfoT.spanY = j;
        cellinfoT = findEmptyCell(context, sqlitedatabase, cellinfoT, 
                "container=-100 AND screenOrder=(SELECT MAX(screenOrder) "
                + "FROM favorites JOIN screens ON (screen=screens._id)  "
                + "WHERE container=-100)", null, true);
        if (cellinfoT.screenOrder >= 30){
            String as[] = new String[1];
            int k = 29;
            while (k >= 0) {
                cellinfoT.screenOrder = k;
                as[0] = String.valueOf(k);
                cellinfoT = findEmptyCell(context, sqlitedatabase, cellinfoT, 
                        "container=-100 AND screenOrder=?", as, false);
                if (cellinfoT.screenOrder == k){
                    break;
                }
                k--;
            }
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
        
        int k = ResConfig.getCellCountX();
        int i = ResConfig.getCellCountY();
        int at[] = new int[]{k, i};
        
        int[][] occupied = (int[][])Array.newInstance(Integer.TYPE, at);
        
        try {
            if (cursor != null && cursor.moveToNext()){
                int id = cursor.getInt(0);
                if (id <= cellinfo.screenOrder){
                    int j = cellinfo.screenOrder;
                    if (id >= j){
                        fillOccupied(occupied, cursor.getInt(1), cursor.getInt(2), 
                                cursor.getInt(3), cursor.getInt(4));
                    } else {
                        
                    }
                } else {
                    cellinfo.screenOrder = id;
                }
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
            ArrayList<ScreenInfo> screenInfos, int i, int j){
        CellInfo cellinfo = null;
        
        if (i <= ResConfig.getCellCountX() && j <= ResConfig.getCellCountY()) {
            cellinfo = findEmptyCell(context, db, i, j);
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
        boolean bOccupied = false;
        int x = ResConfig.getCellCountX() - 1;
        int y = ResConfig.getCellCountY() - 1;
        
        
        if (x + cellinfo.spanX > ResConfig.getCellCountX() 
                || y + cellinfo.spanY > ResConfig.getCellCountY()){
            
        }
        
        if (testOccupied(occupied, x, y, cellinfo.spanX, cellinfo.spanY)){
            cellinfo.cellX = x;
            cellinfo.cellY = y;
            bOccupied = true;
            x--;
        }
        
        int k;
        
        if (!bOccupied || cellinfo.cellY != y || cellinfo.spanX <= 1 && cellinfo.spanY <= 1){
            return bOccupied;
        }else {
            y = occupied[x][y] - 1;
            k = cellinfo.cellY;
            x = cellinfo.cellY - 1;
        }
        
        if (x >= 0 && cellinfo.cellY - x <= y 
                && testOccupied(occupied, cellinfo.cellX, x, cellinfo.spanX, cellinfo.spanY)){
            k = x;
            x--;
        } else {
            cellinfo.cellY = k;
        }
        return true;
    }
    
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
    

    static ArrayList<ScreenInfo> loadScreens(SQLiteDatabase db){
        Cursor cursor = db.query("screens", 
                ScreensQuery.COLUMNS, null, null, null, null, "screenOrder ASC");
        
        if (cursor == null) return null;
        
        ArrayList<ScreenInfo> screenInfos = new ArrayList<ScreenInfo>(cursor.getCount());
        try {
            while (cursor.moveToNext()) {
                screenInfos.add(new ScreenInfo(cursor.getLong(0), cursor.getInt(1)));
            }   
            cursor.close();
        } catch (Exception e) {
            cursor.close();
        }
        return screenInfos;
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
        boolean flag = false;
        
        int x = cx;
        int y = cy;
        
        while (x >= cx + sx) {
            while (occupied[x][y] != 0) {
                if (y >= cy + xy) {
                    x++;
                }
                y++;
            }
        }
        flag = true;        
        
        return flag;
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
                        if (c3 == null) return false;
                        if (c3.getCount() == 0){
                            deleteFavorite(db, id);
                            flag = false;
                            c3.close();
                        }
                    } else {
                        deleteFavorite(db, id);
                    }
                    c2.close(); 
                } else {
                    return false;
                }
                if (screen == -1 || cellX == -1 || cellY == -1) {
                    flag = false;
                }
            }
        }
        cursor.close();
        return flag;
    }
    
    
    
}