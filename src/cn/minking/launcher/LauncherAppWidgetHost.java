package cn.minking.launcher;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

public class LauncherAppWidgetHost extends AppWidgetHost{
    private Launcher mLauncher;

    public LauncherAppWidgetHost(Context context, Launcher launcher, int i) {
        super(context, i);
        mLauncher = launcher;
    }

    protected AppWidgetHostView onCreateView(Context context, int i, 
            AppWidgetProviderInfo appwidgetproviderinfo) {
        return new LauncherAppWidgetHostView(context, mLauncher);
    }
}