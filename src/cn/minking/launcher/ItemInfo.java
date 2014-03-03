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

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;

public class ItemInfo implements Cloneable {
    public int cellX;
    public int cellY;
    public long container;
    public long id;
    public boolean isGesture;
    public boolean isRetained;
    public int itemFlags;
    public int itemType;
    public int launchCount;
    public long screenId;
    public int spanX;
    public int spanY;

    public ItemInfo(){
        id = -1L;
        container = -1L;
        screenId = -1L;
        cellX = -1;
        cellY = -1;
        spanX = 1;
        spanY = 1;
        launchCount = 0;
        isGesture = false;
        isRetained = false;
    }
    
    public static byte[] flattenBitmap(Bitmap bitmap){
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bytearrayoutputstream);
        return bytearrayoutputstream.toByteArray();
    }

    public static void writeBitmap(ContentValues contentvalues, Bitmap bitmap){
        if (bitmap != null)
            contentvalues.put("icon", flattenBitmap(bitmap));
    }

    @Override
    public ItemInfo clone(){
        ItemInfo iteminfo;
        try
        {
            iteminfo = (ItemInfo)super.clone();
        }
        catch (CloneNotSupportedException _ex)
        {
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
        launchCount = 1 + launchCount;
    }

    public String toString(){
        return (new StringBuilder()).append("Item(id=")
                .append(id).append(" type=").append(itemType).append(")").toString();
    }

    public void unbind(){
    }
}
