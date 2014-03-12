package cn.minking.launcher;

import android.appwidget.AppWidgetHostView;
import android.content.ContentValues;

public class LauncherAppWidgetInfo extends ItemInfo {
    int appWidgetId;
    AppWidgetHostView hostView;
    String packageName;

    LauncherAppWidgetInfo(int id) {
        hostView = null;
        itemType = 4;
        appWidgetId = id;
    }

    LauncherAppWidgetInfo(int id, LauncherAppWidgetProviderInfo launcherappwidgetproviderinfo) {
        this(id);
        cellX = launcherappwidgetproviderinfo.cellX;
        cellY = launcherappwidgetproviderinfo.cellY;
        spanX = launcherappwidgetproviderinfo.spanX;
        spanY = launcherappwidgetproviderinfo.spanY;
    }

    public void onAddToDatabase(ContentValues contentvalues) {
        super.onAddToDatabase(contentvalues);
        contentvalues.put("appWidgetId", Integer.valueOf(appWidgetId));
    }

    public String toString() {
        return (new StringBuilder()).append("AppWidget(id=").append(Integer.toString(appWidgetId)).append(")").toString();
    }

    public void unbind() {
        super.unbind();
        hostView = null;
    }
}
