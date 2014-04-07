package cn.minking.launcher.gadget;
/**
 * 作者：      minking
 * 文件名称:    ConfigableGadget.java
 * 创建时间：    2014-04-02
 * 描述：      
 * 更新内容
 * ====================================================================================
 * 2014-04-02： 桌面小控件配置
 * ====================================================================================
 */
import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.FileUtils;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public abstract class ConfigableGadget extends FrameLayout
    implements View.OnClickListener, Gadget {
    public static class BackupManager {
        private final int mGadgetId;

        public String getBackupDir(Context context) {
            return context.getDir((new StringBuilder()).append(getTypeName()).append("_bak").toString(), 1).getAbsolutePath();
        }

        public String getBackupName(long id) {
            return (new StringBuilder()).append(getBackupNamePrefix()).append(id).toString();
        }

        public String getBackupNamePrefix() {
            Object aobj[] = new Object[2];
            aobj[0] = getTypeName();
            aobj[1] = getSizeDescript();
            return String.format("%s_%s_", aobj);
        }

        public String getBackupPath(Context context, long id) {
            Object aobj[] = new Object[2];
            aobj[0] = getBackupDir(context);
            aobj[1] = getBackupName(id);
            return String.format("%s/%s", aobj);
        }

        public String getEntryName() {
            Object aobj[] = new Object[2];
            aobj[0] = getTypeName();
            aobj[1] = getSizeDescript();
            return String.format("%s_%s", aobj);
        }

        public String getPathInTheme() {
            Object aobj[] = new Object[2];
            aobj[0] = "/data/system/theme";
            aobj[1] = getEntryName();
            return String.format("%s/%s", aobj);
        }

        public String getSizeDescript() {
            String size;
            switch (mGadgetId) {
            default:
                Object aobj[] = new Object[1];
                aobj[0] = Integer.valueOf(mGadgetId);
                throw new UnsupportedOperationException(String.format("Unknown gadget id %d", aobj));

            case GadgetFactory.ID_GADGET_CLOCK_1X2: // '\004'
                size = "1x2";
                break;

            case GadgetFactory.ID_GADGET_CLOCK_2X2: // '\005'
            case GadgetFactory.ID_GADGET_PHOTO_2X2: // '\007'
                size = "2x2";
                break;

            case GadgetFactory.ID_GADGET_CLOCK_2X4: // '\006'
            case GadgetFactory.ID_GADGET_PHOTO_2X4: // '\b'
                size = "2x4";
                break;

            case GadgetFactory.ID_GADGET_PHOTO_4X4: // '\t'
                size = "4x4";
                break;
            }
            return size;
        }

        public String getSystemGadgetTheme() {
            String typeName = getTypeName();
            Object aobj[] = new Object[3];
            aobj[0] = typeName;
            aobj[1] = getSizeDescript();
            aobj[2] = typeName;
            return String.format("/system/media/theme/.data/content/%s_%s/%s.mrc", aobj);
        }

        public String getTypeName() {
            String typeName;
            switch (mGadgetId) {
            default:
                Object aobj[] = new Object[1];
                aobj[0] = Integer.valueOf(mGadgetId);
                throw new UnsupportedOperationException(String.format("Unknown gadget id %d", aobj));

            case GadgetFactory.ID_GADGET_CLOCK_1X2: // '\004'
            case GadgetFactory.ID_GADGET_CLOCK_2X2: // '\005'
            case GadgetFactory.ID_GADGET_CLOCK_2X4: // '\006'
                typeName = "clock";
                break;

            case GadgetFactory.ID_GADGET_PHOTO_2X2: // '\007'
            case GadgetFactory.ID_GADGET_PHOTO_2X4: // '\b'
            case GadgetFactory.ID_GADGET_PHOTO_4X4: // '\t'
                typeName = "photoframe";
                break;
            }
            return typeName;
        }

        public boolean prepareBackup(Context context, long id) {
            boolean flag = true;
            String backupPath = getBackupPath(context, id);
            File file = new File(backupPath);
            if (!file.isFile()) {
                File parentFile = file.getParentFile();
                if (!parentFile.isDirectory()) {
                    parentFile.mkdirs();
                }
                if (!Utils.copyFile(backupPath, getPathInTheme())) {
                    Utils.extract(backupPath, getSystemGadgetTheme(), getSystemGadgetTheme());
                }
                if (!file.exists()) {
                    flag = false;
                } else {
                    FileUtils.setPermissions(backupPath, FileUtils.S_IRUSR | FileUtils.S_IWUSR | FileUtils.S_IROTH, -1, -1);
                }
            }
            return flag;
        }

        public BackupManager(int gadgetId) {
            mGadgetId = gadgetId;
        }
    }


    private static final String TAG = "MKHome.ConfigableGadget";
    protected BackupManager mBackupManager;

    public ConfigableGadget(Context context) {
        this(context, null);
    }

    public ConfigableGadget(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public ConfigableGadget(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
    }

    public static void deleteConfig(Context context, String s) {
        SharedPreferences.Editor editor = 
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(s);
        editor.commit();
    }

    public void deleteConfig() {
        deleteConfig(mContext, getPrefKey());
    }

    protected abstract View getEditView();

    public long getItemId() {
        long id;
        if (!(getTag() instanceof GadgetInfo)) {
            id = -1L;
        } else {
            id = ((GadgetInfo)getTag()).id;
        }
        return id;
    }

    protected String getPrefKey() {
        return mBackupManager.getBackupName(getItemId());
    }

    public boolean isRestrictClick() {
        return false;
    }

    public String loadConfig() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(getPrefKey(), null);
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    public void onCreate() {
        setOnClickListener(this);
        mBackupManager = new BackupManager(((GadgetInfo)getTag()).mGadgetId);
    }

    @Override
    public void onDeleted() {
        Log.d(TAG, (new StringBuilder()).append("remove gadget ").append(getItemId()).toString());
        (new File(mBackupManager.getBackupPath(mContext, getItemId()))).delete();
        deleteConfig();
    }

    public void onEditDisable() {
        View view = getEditView();
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public void onEditNormal() {
        View view = getEditView();
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            view.bringToFront();
            view.setSelected(false);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        return true;
    }

    @Override
    public void onResume() {
    }
    
    @Override
    public void onStart() {
    }
    
    @Override
    public void onStop() {
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        View view = getEditView();
        if (view != null && view.getVisibility() == 0 && !isRestrictClick())
            switch (MotionEvent.ACTION_MASK & ev.getAction())
            {
            case MotionEvent.ACTION_DOWN: // '\0'
                view.setSelected(true);
                break;

            case MotionEvent.ACTION_UP: // '\001'
            case MotionEvent.ACTION_CANCEL: // '\003'
                view.setSelected(false);
                break;
            }
        return super.onTouchEvent(ev);
    }

    public boolean saveConfig(String config) {
        boolean flag;
        if (getItemId() == -1L) {
            flag = false;
        } else {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            editor.putString(getPrefKey(), config);
            flag = editor.commit();
        }
        return flag;
    }

}