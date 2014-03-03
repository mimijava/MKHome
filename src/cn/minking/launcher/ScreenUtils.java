package cn.minking.launcher;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class ScreenUtils {
    
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
}