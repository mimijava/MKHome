package cn.minking.launcher;

import android.content.ComponentName;

class ShortcutProviderInfo extends ItemInfo {

    ComponentName mComponentName;

    ShortcutProviderInfo(String pkg, String cls) {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        mComponentName = new ComponentName(pkg, cls);
        spanX = 1;
        spanY = 1;
    }
}
