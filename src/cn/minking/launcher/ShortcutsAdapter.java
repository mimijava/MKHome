package cn.minking.launcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.WeakHashMap;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ShortcutsAdapter extends ArrayAdapter<ShortcutInfo>{
    public static class PositionComparator
        implements Comparator<ShortcutInfo>{
        
        public int compare(ShortcutInfo i, ShortcutInfo j){
            int ok;
            if (i.cellX >= j.cellX) {
                ok = 1;
            } else {
                ok = -1;
            }
            return ok;
        }
    }
    
    private static PositionComparator PC = new PositionComparator();
    private WeakHashMap<ShortcutInfo, ShortcutIcon> mIconCache;
    private FolderInfo mInfo;
    private final Launcher mLauncher;
    private Object mPositionMap[];

    public ShortcutsAdapter(Context context, FolderInfo folderinfo) {
        super(context, 0, folderinfo.contents);
        mIconCache = new WeakHashMap<ShortcutInfo, ShortcutIcon>();
        mInfo = folderinfo;
        mLauncher = ((LauncherApplication)context.getApplicationContext()).getLauncher();
        buildSortingMap();
    }

    private void buildSortingMap() {
        TreeMap<ShortcutInfo, Integer> treemap = new TreeMap<ShortcutInfo, Integer>(PC);
        int i = 0;
        Iterator<ShortcutInfo> iterator = mInfo.contents.iterator();
        while(iterator.hasNext()) {
            ShortcutInfo shortcutinfo = (ShortcutInfo)iterator.next();
            treemap.put(shortcutinfo, Integer.valueOf(i));
            i++;
        }
        mPositionMap = treemap.values().toArray();
    }

    public ShortcutInfo getItem(int i) {
        return (ShortcutInfo)super.getItem(((Integer)mPositionMap[i]).intValue());
    }

    public View getView(int i, View view, ViewGroup viewgroup) {
        ShortcutInfo shortcutinfo = getItem(i);
        ShortcutIcon shortcuticon = (ShortcutIcon)mIconCache.get(shortcutinfo);
        if (shortcuticon != null) {
            shortcuticon.updateInfo(mLauncher, shortcutinfo);
        } else {
            shortcuticon = ShortcutIcon.fromXml(R.layout.application_folder, mLauncher, viewgroup, shortcutinfo);
            mIconCache.put(shortcutinfo, shortcuticon);
        }
        return shortcuticon;
    }

    @Override
    public void notifyDataSetChanged() {
        buildSortingMap();
        super.notifyDataSetChanged();
    }

    public void reorderItemByInsert(ShortcutInfo fInfo, ShortcutInfo sInfo) {
        if (fInfo == sInfo) return;

        int i = 0;
        int j = 0;
        int k = -1;

        while (i < getCount()) {
            ShortcutInfo info = getItem(i);
            if (info != fInfo) {
                if (info == sInfo)  {
                    if (k != -1) {
                        int k1 = j + 1;
                        sInfo.cellX = j;
                        j = k1 + 1;
                        fInfo.cellX = k1;
                        break;
                    }
                    int i1 = j + 1;
                    fInfo.cellX = j;
                    j = i1;
                }
                int j1 = j + 1;
                info.cellX = j;
                j = j1;
            } else {
                k = i;
            }
            i++;
        }
        notifyDataSetChanged();
    }

    public void saveContentPosition() {
        ArrayList<ContentProviderOperation> arraylist = new ArrayList<ContentProviderOperation>();
        Iterator<ShortcutInfo> iterator = mInfo.contents.iterator();
        while(iterator.hasNext()) {
            ShortcutInfo shortcutInfo = iterator.next();
            arraylist.add(LauncherModel.getMoveItemOperation(shortcutInfo, 
                    mInfo.id, -1L, shortcutInfo.cellX, 0));
        }
        if (!arraylist.isEmpty()){
            LauncherModel.applyBatch(mLauncher, "cn.minking.launcher.settings", arraylist);
        }

    }

}