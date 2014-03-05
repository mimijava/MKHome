package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ItemInfo.java
 * 创建时间：    2013
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 图标 文件创建
 * ====================================================================================
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;

public class ItemInfo implements Cloneable {
    static final String TAG = "MKHome.Launcher";
    /// M:  cX 图标X位置,左上角第一个为0,向左递增,0-N共 N 值根据屏幕分辨率及大小决定
    ///     cY 图标Y位置,左上角第一个为0,向下递增,0-N共 N 值根据屏幕分辨率及大小决定 

    public int cellX;
    public int cellY;
    
    /// M:  sX 在x方向上所占格数
    /// M:  sY 在x方向上所占格数
    public int spanX;
    public int spanY;

    static final long NO_ID = -1L;
    /// M: container标识处于WORKSPACE还是HOTSEAT，暂时分为这两种
    public long container;
    
    /// M: id 标识
    public long id;
    
    /// M： 图标所在的屏幕编号
    public long screenId;

    public boolean isGesture;
    public boolean isRetained;
    public int itemFlags;
    public int itemType;
    public int launchCount;
    

    public ItemInfo(){
        id = NO_ID;
        container = NO_ID;
        screenId = NO_ID;
        cellX = -1;
        cellY = -1;
        spanX = 1;
        spanY = 1;
        launchCount = 0;
        isGesture = false;
        isRetained = false;
    }
    
    public static byte[] flattenBitmap(Bitmap bitmap){
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write icon");
            return null;
        }
    }

    public static void writeBitmap(ContentValues values, Bitmap bitmap){
        if (bitmap != null){
            byte[] data = flattenBitmap(bitmap);
            values.put(LauncherSettings.Favorites.ICON, data);
        }
    }

    @Override
    public ItemInfo clone(){
        ItemInfo iteminfo;
        try {
            iteminfo = (ItemInfo)super.clone();
        } catch (CloneNotSupportedException _ex) {
            throw new AssertionError();
        }
        return iteminfo;
    }
    
    public void copyPosition(ItemInfo iteminfo){
        container = iteminfo.container;
        screenId = iteminfo.screenId;
        cellX = iteminfo.cellX;
        cellY = iteminfo.cellY;
    }

    public boolean isCustomizedIcon(){
        boolean flag;
        if (!isRetained && (2 & itemFlags) == 0)
            flag = false;
        else
            flag = true;
        return flag;
    }

    public boolean isPresetApp(){
        boolean flag;
        if ((1 & itemFlags) == 0)
            flag = false;
        else
            flag = true;
        return flag;
    }
    
    public void load(Cursor cursor){
        id = cursor.getLong(LauncherModel.colToInt(ItemQuery.COL.ID));
        
        if (!cursor.isNull(LauncherModel.colToInt(ItemQuery.COL.CELLX))){
            cellX = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.CELLX));
        }
        
        if (!cursor.isNull(LauncherModel.colToInt(ItemQuery.COL.CELLY))){
            cellY = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.CELLX));
        }
        
        spanX = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.SPANX));
        spanY = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.SPANY));
        
        if (!cursor.isNull(LauncherModel.colToInt(ItemQuery.COL.SCREEN))){
            screenId = cursor.getLong(LauncherModel.colToInt(ItemQuery.COL.SCREEN));
        }

        itemType = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ITEMTYPE));
        container = cursor.getLong(LauncherModel.colToInt(ItemQuery.COL.CONTAINER));
        launchCount = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.LAUNCHERCOUNT));
        itemFlags = cursor.getInt(LauncherModel.colToInt(ItemQuery.COL.ITEMFLAGS));
    }    
    
    public void loadPosition(ContentValues contentvalues){
        container = contentvalues.getAsLong("container").longValue();
        screenId = contentvalues.getAsLong("screen").longValue();
        cellX = contentvalues.getAsInteger("cellX").intValue();
        cellY = contentvalues.getAsInteger("cellY").intValue();
    }
    
    public void onAddToDatabase(ContentValues contentvalues){
        contentvalues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(itemType));
        if (!isGesture) {
            contentvalues.put(LauncherSettings.Favorites.CONTAINER, Long.valueOf(container));
            contentvalues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(screenId));
            contentvalues.put(LauncherSettings.Favorites.CELLX, Integer.valueOf(cellX));
            contentvalues.put(LauncherSettings.Favorites.CELLY, Integer.valueOf(cellY));
            contentvalues.put(LauncherSettings.Favorites.SPANX, Integer.valueOf(spanX));
            contentvalues.put(LauncherSettings.Favorites.SPANY, Integer.valueOf(spanY));
            contentvalues.put(LauncherSettings.Favorites.LAUNCHER_COUNT, Integer.valueOf(launchCount));
            contentvalues.put(LauncherSettings.Favorites.ITEM_FLAG, Integer.valueOf(itemFlags));
        }
    }
    
    public void onLaunch(){
        launchCount += 1;
    }

    public String toString(){
        return "Item(id=" + this.id + " type=" + this.itemType + " container=" + this.container
                + " screen=" + screenId + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX
                    + " spanY=" + spanY + ")";
    }

    public void unbind(){
    }
}
