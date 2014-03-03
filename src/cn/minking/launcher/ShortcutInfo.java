package cn.minking.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class ShortcutInfo extends ItemInfo {
    public Intent intent;
    public CharSequence title;
    public boolean usingFallbackIcon;
    public boolean onExternalStorage;
    public int mIconType;
    private Bitmap mIcon;
    private String mIconPackage;
    ShortcutIconResource iconResource;
    
    public ShortcutInfo(){
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }
    
    public ShortcutInfo(Context context, ResolveInfo resolveinfo){
        ComponentName componentname = new ComponentName(resolveinfo.activityInfo.applicationInfo.packageName,
                resolveinfo.activityInfo.name);
        container = -1L;
        setActivity(componentname, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        title = resolveinfo.activityInfo.loadLabel(context.getPackageManager());
    }
    
    final void setActivity(ComponentName componentname, int flag){
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(componentname);
        intent.setFlags(flag);
        itemType = 0;
    }
    
    public void setIcon(Bitmap b) {
        mIcon = b;
    }
    
    public Bitmap getIcon(IconCache iconcache){
        if (mIcon == null)
            mIcon = iconcache.getIcon(intent, itemType);
        return mIcon;
    }
    
    
    public String getPackageName(){
        String string;
        if (!isPresetApp()) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null)
                string = componentName.getPackageName();
            else
                string = null;
        } else {
            string = mIconPackage;
        }
        return string;
    }
    
    public void wrapIconWithBorder(Context context){
        if (mIcon != null){
            BitmapDrawable bitmapdrawable = new BitmapDrawable(context.getResources(), mIcon);
            //if (!isCustomizedIcon() && !isPresetApp())
                //bitmapdrawable = IconCustomizer.generateShortcutIconDrawable(bitmapdrawable);
            mIcon = Utilities.createIconBitmap(bitmapdrawable, context);
        }
    }
    
    public void loadContactInfo(Context context){
        
    }
}
