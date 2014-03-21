package cn.minking.launcher;

import java.util.ArrayList;
import java.util.Iterator;

import cn.minking.launcher.AllAppsList.RemoveInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

/**
 * 作者：      minking
 * 文件名称:    FolderInfo.java
 * 创建时间：    2014-02-28
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: 文件夹类
 * ====================================================================================
 */
public class FolderInfo extends ItemInfo {
    ArrayList<ShortcutInfo> contents;
    FolderIcon icon;
    private ShortcutsAdapter mAdapter;
    boolean opened;
    public CharSequence title;

    public FolderInfo() {
        icon = null;
        contents = new ArrayList<ShortcutInfo>();
        mAdapter = null;
        itemType = 2;
    }

    public void add(ShortcutInfo shortcutinfo) {
        contents.add(shortcutinfo);
    }

    public ItemInfo clone() {
        FolderInfo folderinfo = (FolderInfo)super.clone();
        folderinfo.contents = new ArrayList<ShortcutInfo>();
        return folderinfo;
    }
    
    int count() {
        return contents.size();
    }

    public ShortcutsAdapter getAdapter(Context context) {
        if (mAdapter == null) {
            mAdapter = new ShortcutsAdapter(context, this);
        }
        return mAdapter;
    }

    public void load(Cursor cursor) {
        super.load(cursor);
        title = cursor.getString(2);
    }

    void notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        if (icon != null) {
            icon.loadItemIcons();
        }
    }

    public void onAddToDatabase(ContentValues contentvalues) {
        super.onAddToDatabase(contentvalues);
        contentvalues.put("title", title.toString());
    }

    public void remove(ShortcutInfo shortcutinfo) {
        contents.remove(shortcutinfo);
    }

    public void removeItems(ArrayList<RemoveInfo> arraylist, Launcher launcher) {
        ArrayList<ShortcutInfo> rArrayList = new ArrayList<ShortcutInfo>(1);;
        boolean flag = false;
        
        for (int j = 0; j < contents.size(); j++) {
            ShortcutInfo shortcutInfo = (ShortcutInfo)contents.get(j);;
            String packageName = shortcutInfo.getPackageName();
            Intent intent = shortcutInfo.intent;
            if (packageName != null 
                    && (Intent.ACTION_MAIN.equals(intent.getAction()) 
                            || (shortcutInfo.isPresetApp() 
                                && Intent.ACTION_VIEW.equals(intent.getAction())))){
                Iterator<RemoveInfo> rIterator = arraylist.iterator();
                if (rIterator.hasNext()) {
                    if (rIterator.next().packageName.equals(packageName)) {
                        rArrayList.add(shortcutInfo);
                        if (shortcutInfo.itemType != 0){
                            LauncherModel.deleteItemFromDatabase(launcher, shortcutInfo);
                        }
                        flag = true;
                    }   
                }
            }
        }
        contents.removeAll(rArrayList);
        if (flag){
            notifyDataSetChanged();
        }
    }

    public void setTitle(CharSequence charsequence, Context context) {
        title = charsequence;
        if (icon != null) {
            icon.setTitle(charsequence);
        }
        if (id != -1L) {
            LauncherModel.updateFolderTitleInDatabase(context, this);
        }
    }
}
