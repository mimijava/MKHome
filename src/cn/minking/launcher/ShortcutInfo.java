package cn.minking.launcher;

import java.net.URISyntaxException;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class ShortcutInfo extends ItemInfo {
    final private static String TAG = "MKHome.ShortcutInfo"; 
    public Intent intent;
    public CharSequence title;
    public boolean usingFallbackIcon;
    public boolean onExternalStorage;
    public int mIconType;
    private Bitmap mIcon;
    private String mIconPackage;
    ShortcutIconResource iconResource;
    
    public ShortcutInfo(){
        itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        mIconType = LauncherSettings.Favorites.ICON_TYPE_RESOURCE;
    }
    
    public ShortcutInfo(Context context, ResolveInfo resolveinfo){
        ComponentName componentname = new ComponentName(resolveinfo.activityInfo.applicationInfo.packageName,
                resolveinfo.activityInfo.name);
        container = -1L;
        setActivity(componentname, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        title = resolveinfo.activityInfo.loadLabel(context.getPackageManager());
    }
    
    static Bitmap combineIcon(Bitmap destBitmap, Bitmap sourceBitmap, Drawable drawable){
        if (destBitmap != null)
            destBitmap.eraseColor(Color.TRANSPARENT); //将图片设置为透明
        else
            destBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), sourceBitmap.getConfig());
        Canvas canvas = new Canvas(destBitmap);
        canvas.drawBitmap(sourceBitmap, 0F, 0F, null);
        if (drawable.getBounds() == null || drawable.getBounds().isEmpty()){
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        canvas.translate((sourceBitmap.getWidth() - drawable.getBounds().width()) / 2, 
                (sourceBitmap.getHeight() - drawable.getBounds().height()) / 2);
        drawable.draw(canvas);
        return destBitmap;
    }
    
    public boolean deletePresetArchive(){
        boolean flag = false;
        if (!isPresetApp()){
            flag = false;
        } else {
            // 使用SHELL指令删除图标
            //flag = Shell.remove(intent.getData().getPath());
        }
        return flag;
    }
    
    public void ensureToggleIcon(Context context){
        if (mIconType == 3){
//            mIcon = combineIcon(mIcon, 
//                  ((BitmapDrawable)context.getResources().getDrawable(R.drawable.toggle_bg)).getBitmap(), 
//                  ToggleManager.getImageDrawable(getToggleId()));
        }
    }
    
    final void setActivity(ComponentName componentname, int flag){
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(componentname);
        intent.setFlags(flag);
        itemType = 0;
    }
    
    public void setIcon(Bitmap bitmap) {
        mIcon = bitmap;
    }
    
    public Bitmap getIcon(IconCache iconcache){
        if (mIcon == null){
            mIcon = iconcache.getIcon(intent, itemType);
        }
        return mIcon;
    }
    
    @Override
    public void load(Cursor cursor) {
        super.load(cursor);
        if (title == null){
            title = cursor.getString(LauncherModel.colToInt(ItemQuery.COL.TITLE));
        }
        try {
            intent = Intent.parseUri(cursor.getString(LauncherModel.colToInt(ItemQuery.COL.INTENT)), 0);
        } catch (URISyntaxException urisyntaxexception) {
            urisyntaxexception.printStackTrace();
        }
        mIconPackage = cursor.getString(LauncherModel.colToInt(ItemQuery.COL.ICONPACKAGE));
    }
    
    @Override
    public void onAddToDatabase(ContentValues contentvalues) {
        super.onAddToDatabase(contentvalues);
        String packageName;
        if (title == null) {
            packageName = null;
        }else {
            packageName = title.toString();
        }
        contentvalues.put("title", packageName);
        
        String intentString;
        if (intent == null) {
            intentString = null;
        }else {
            intentString = intent.toUri(0);
        }
        contentvalues.put("intent", intentString);
        contentvalues.put("iconType", Integer.valueOf(mIconType));
        if (mIconType == LauncherSettings.Favorites.ICON_TYPE_RESOURCE) {
            if (onExternalStorage && !usingFallbackIcon) {
                writeBitmap(contentvalues, mIcon);
            }
            if (iconResource != null) {
                contentvalues.put("iconPackage", iconResource.packageName);
                contentvalues.put("iconResource", iconResource.resourceName);
            }
        } else {
            writeBitmap(contentvalues, mIcon);
        }
        if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            if (intent == null || intent.getComponent() == null){
                Log.e(TAG, "Application shortcut's intent or component is null");
            } else {
                contentvalues.put("iconPackage", intent.getComponent().getPackageName());
            }
        }
    }

    @Override
    public void unbind() {
        super.unbind();
    }

    public String toString(){
        return (new StringBuilder()).append("ShortcutInfo(title=").append(title).append(")").toString();
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
