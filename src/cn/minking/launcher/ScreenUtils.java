package cn.minking.launcher;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class ScreenUtils {
    
    private static String sInstalledComponentsArg = null;
    
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
    
    /**
     * 功能： 将读取的APP类名以静态字符变量存储在sInstalledComponentsArg
     * @param context
     */
    static void updateInstalledComponentsArg(Context context){
        List<ResolveInfo> list = context.getPackageManager().
                queryIntentActivities(getLaunchableIntent(), PackageManager.PERMISSION_GRANTED);
        
        if (!list.isEmpty()) {
            StringBuilder stringbuilder = new StringBuilder().append('(');
            Iterator<ResolveInfo> iterator = list.iterator();
            while (iterator.hasNext()){
                ResolveInfo resolveinfo = iterator.next();
                if (!TextUtils.isEmpty(resolveinfo.activityInfo.packageName) 
                        && !TextUtils.isEmpty(resolveinfo.activityInfo.name)){
                    stringbuilder.append(
                            (new StringBuilder()).append("'").
                            append(resolveinfo.activityInfo.packageName).
                            append("'").append(",").toString());
                }
            }
            stringbuilder.setCharAt(stringbuilder.length() - 1, ')');
            sInstalledComponentsArg = stringbuilder.toString();
        }
    }
    
    static boolean verifyItemPosition(SQLiteDatabase sqlitedatabase, long id){
        return true;
    }
}