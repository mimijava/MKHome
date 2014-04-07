package cn.minking.launcher.gadget;
/**
 * 作者：      minking
 * 文件名称:    ClockGadgetDelegate.java
 * 创建时间：    2014-04-02
 * 描述：      
 * 更新内容
 * ====================================================================================
 * 2014-04-02： 桌面时钟小控件
 * ====================================================================================
 */
import java.io.File;

import org.w3c.dom.Element;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import cn.minking.launcher.LauncherApplication;
import cn.minking.launcher.R;
import cn.minking.launcher.screenelement.elements.ButtonScreenElement;

public class ClockGadgetDelegate extends ConfigableGadget
    implements ButtonScreenElement.ButtonActionListener {
    private static final String TAG = "MKHome.ClockGadgetDelegate";
    private static final float DENSITY_SCALE;
    private final Context mActivity;
    final Clock mClock;
    private String mClockType;
    private View mEditView;
    private View mErrorDisplay;
    private final int mRequestCode;
    private boolean mRestrictClick;
    public int mStatus;
    private Gadget mActualGadget;
    
    static {
        DENSITY_SCALE = (float)Resources.getSystem().getDisplayMetrics().densityDpi / 240F;
    }
    
    /**
     * 描述： 桌面时钟控件
     * @param context
     * @param requestCode
     */
    public ClockGadgetDelegate(Context context, int requestCode) {
        super(context);
        mStatus = 0;
        mActivity = context;
        mClock = new Clock(context);
        mRequestCode = requestCode;
    }

    private void adjustByAttributes(Element element, View view) {
        int x = getIntFromElement(element, "clock_x", 0);
        int y = getIntFromElement(element, "clock_y", 0);
        FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams)view.getLayoutParams();
        layoutparams.leftMargin = (int)(DENSITY_SCALE * (float)x);
        layoutparams.topMargin = (int)(DENSITY_SCALE * (float)y);
        view.setLayoutParams(layoutparams);
    }

    private int getIntFromElement(Element element, String name, int i) {
        int elementId = i;
        String attrs = element.getAttribute(name);
        if (attrs != "" && attrs != null) {
            elementId = Integer.valueOf(attrs).intValue();
        }
        return elementId;
    }

    private void setupViews() {
        inflate(mContext, R.layout.gadget_error_display, this);
        mErrorDisplay = findViewById(R.id.error_display);
        ((ImageView)mErrorDisplay.findViewById(R.id.gadget_icon)).setImageResource(R.drawable.gadget_clock_error);
        mErrorDisplay.setVisibility(View.GONE);
        ImageView imageview = new ImageView(mContext);
        FrameLayout.LayoutParams layoutparams = 
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutparams.gravity = Gravity.TOP | Gravity.RIGHT;
        imageview.setLayoutParams(layoutparams);
        imageview.setImageResource(R.drawable.gadget_edit_tag);
        imageview.setVisibility(View.GONE);
        mEditView = imageview;
        mEditView.setOnClickListener(this);
        addView(mEditView);
    }

    public static void updateBackup(Context context) {
        int gadgetId[] = new int[] {4, 5, 6};

        for (int i = 0; i < gadgetId.length; i++) {
            ConfigableGadget.BackupManager backupmanager = new ConfigableGadget.BackupManager(gadgetId[i]);
            String s = (new StringBuilder()).append("clock_changed_time_").
                    append(backupmanager.getSizeDescript()).toString();
            File file = new File(backupmanager.getBackupDir(context));
            if (file.isDirectory()) {
                long l = Settings.System.getLong(context.getContentResolver(), s, 0L);
                String s1 = backupmanager.getBackupNamePrefix();
                File afile[] = file.listFiles();
                
                for (int j = 0; j < afile.length; j++) {
                    File file1 = afile[j];
                    String s2 = file1.getName();
                    if (file1.lastModified() < l && s2.startsWith(s1)) {
                        s2 = s2.substring(s1.length());
                        try {
                            long l1 = Long.valueOf(s2).longValue();
                            ConfigableGadget.deleteConfig(context, backupmanager.getBackupName(l1));
                            Object aobj[] = new Object[2];
                            aobj[0] = Long.valueOf(l1);
                            aobj[1] = file1.getAbsolutePath();
                            Log.d(TAG, String.format("delete gadget config id=%d, path=%s", aobj));
                        } catch (NumberFormatException _ex) { }
                        file1.delete();
                    }
                }
            }
        }
    }

    public View getEditView() {
        return mEditView;
    }

    public boolean isRestrictClick() {
        return mRestrictClick;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public boolean onButtonDoubleClick(String s) {
        return false;
    }

    @Override
    public boolean onButtonDown(String s) {
        return false;
    }

    @Override
    public boolean onButtonLongClick(String s) {
        return false;
    }

    @Override
    public boolean onButtonUp(String s) {
        if (mEditView.getVisibility() != View.VISIBLE) {
            Intent intent = new Intent("android.intent.action.SET_ALARM");
            LauncherApplication.startActivity(getContext(), intent);
        } else {
            Intent intent1 = new Intent("android.intent.action.PICK_GADGET");
            intent1.putExtra("REQUEST_GADGET_NAME", "clock");
            intent1.putExtra("REQUEST_GADGET_SIZE", mBackupManager.getSizeDescript());
            intent1.putExtra("REQUEST_CURRENT_USING_PATH", loadConfig());
            intent1.putExtra("REQUEST_TRACK_ID", String.valueOf(getItemId()));
            LauncherApplication.startActivityForResult(getContext(), intent1, mRequestCode);
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        if ("flip".equals(mClockType) || !mRestrictClick || view == mEditView) {
            onButtonUp(null);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mStatus = 1 | mStatus;
        setupViews();
        mClock.init();
        if (mActualGadget != null) {
            mActualGadget.onCreate();
        } else {
            updateActualGadget();
        }
        (new IntentFilter(Intent.ACTION_MEDIA_MOUNTED)).addDataScheme("file");
    }

    @Override
    public void onDeleted() {
        if (mActualGadget != null) {
            mActualGadget.onDeleted();
        }
        super.onDeleted();
    }

    @Override
    public void onDestroy() {
        mStatus = -2 & mStatus;
        mClock.pause();
        if (mActualGadget != null) {
            mActualGadget.onDestroy();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        boolean flag;
        if (!mRestrictClick) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    @Override
    public void onPause() {
        mStatus = -5 & mStatus;
        mClock.pause();
        if (mActualGadget != null) {
            mActualGadget.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatus = 4 | mStatus;
        if (mActualGadget != null) {
            mActualGadget.onResume();
            mClock.resume();
        }
    }
    
    @Override
    public void onStart() {
        mStatus = 2 | mStatus;
        if (mActualGadget != null) {
            mActualGadget.onStart();
        }
    }

    @Override
    public void onStop() {
        mStatus = -3 & mStatus;
        mClock.pause();
        if (mActualGadget != null) {
            mActualGadget.onStop();
        }
    }

    public boolean saveConfig(String s) {
        boolean flag;
        if (!Utils.extract(mBackupManager.getBackupPath(mActivity, getItemId()), s, mBackupManager.getSystemGadgetTheme())) {
            flag = false;
        } else {
            flag = super.saveConfig(s);
        }
        return flag;
    }

    void updateActualGadget() {
        if (!mBackupManager.prepareBackup(mActivity, getItemId())) {
            Log.d(TAG, "prepare back up failed");
        }
        String backupPath = mBackupManager.getBackupPath(mActivity, getItemId());
        Element element = Utils.parseManifestInZip(backupPath);

        View view = null;
        
        if (element == null) {
            // 如果读取ZIP资源失败则显示错误提示
            mErrorDisplay.setVisibility(View.VISIBLE);
        } else {
            mClockType = element.getAttribute("type");
             
            // 创建时钟GADGET
            if (!"flip".equals(mClockType)) {
                view = new AwesomeClock(mContext);
            } else {
                view = inflate(mContext, R.layout.gadget_flipclock, null);
            }
            mErrorDisplay.setVisibility(View.GONE);
            
            // 初始化配置
            if (view instanceof Clock.ClockStyle) {
                ((Clock.ClockStyle)view).initConfig(backupPath);
            }
        }
        
        if (mActualGadget != null) {
            if ((4 & mStatus) != 0) {
                mActualGadget.onPause();
            }
            if ((2 & mStatus) != 0) {
                mActualGadget.onStop();
            }
            if ((1 & mStatus) != 0) {
                mActualGadget.onDestroy();
            }
            removeView((View)mActualGadget); 
        }
        if (!(view instanceof Gadget)) {
            mClock.setClockStyle(null);
            mActualGadget = null;
        } else {
            addView(view);
            view.setTag(getTag());
            adjustByAttributes(element, view);
            mEditView.bringToFront();
            Gadget gadget = (Gadget)view;
            if ((1 & mStatus) != 0) {
                gadget.onCreate();
            }
            if ((2 & mStatus) != 0) {
                gadget.onStart();
            }
            if ((4 & mStatus) != 0) {
                gadget.onResume();
            }
            if (gadget instanceof Clock.ClockStyle) {
                mClock.setClockStyle((Clock.ClockStyle)gadget);
            }
            mActualGadget = ((Gadget)gadget);
        }
        if ("awesome".equals(mClockType)) {
            mRestrictClick = ((AwesomeClock)view).setClockButtonListener(this);
        }
    }

    @Override
    public void updateConfig(Bundle bundle) {
        saveConfig(bundle.getString("RESPONSE_PICKED_RESOURCE"));
        updateActualGadget();
    }
}