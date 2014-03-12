package cn.minking.launcher;

import android.appwidget.AppWidgetProviderInfo;

class LauncherAppWidgetProviderInfo extends ItemInfo{
    AppWidgetProviderInfo providerInfo;

    LauncherAppWidgetProviderInfo(AppWidgetProviderInfo appwidgetproviderinfo) {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
        providerInfo = appwidgetproviderinfo;
    }

    public LauncherAppWidgetProviderInfo clone() {
        return (LauncherAppWidgetProviderInfo)super.clone();
    }

}