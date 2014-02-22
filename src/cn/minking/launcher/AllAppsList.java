package cn.minking.launcher;

import java.util.ArrayList;

public class AllAppsList {
    class RemoveInfo{
        public final String packageName;
        public final boolean replacing;
        
        public RemoveInfo(String name, boolean b_replace) {
            packageName = name;
            replacing = b_replace;
        }
    }
    
    private static String sSelectionArgs2[] = new String[2];
    private ArrayList<ShortcutInfo> added;
    private ArrayList<RemoveInfo> removed;
    
    public AllAppsList() {
        added = new ArrayList<ShortcutInfo>(3);
        removed = new ArrayList<RemoveInfo>();
    }
}
