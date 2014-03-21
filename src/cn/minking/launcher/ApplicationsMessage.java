package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ApplicationsMessage.java
 * 创建时间：    2014-03-12
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 2014031201: 应用消息
 * ====================================================================================
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


public class ApplicationsMessage{
    class MessageReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if ("android.intent.action.APPLICATION_MESSAGE_UPDATE".equals(action)){
                    updateMessage(ComponentName.unflattenFromString(
                            intent.getStringExtra("android.intent.extra.update_application_component_name")), 
                            intent.getStringExtra("android.intent.extra.update_application_message_text"), 
                            intent.getStringExtra("android.intent.extra.update_application_message_text_background"), 
                            intent.getByteArrayExtra("android.intent.extra.update_application_message_icon_tile"));
                }   
            } catch (NullPointerException e) {
                Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", e);
            }
        }
    }

    public static interface IconMessage {
        public abstract byte[] getMessageIconTile();
        public abstract String getMessageText();
        public abstract String getMessageTextBackground();
        public abstract boolean isEmptyMessage();
        public abstract void setMessage(String s, String s1, byte abyte0[]);
    }


    final private static String TAG = "MKHome.ApplicationsMessage";
    private Launcher mLauncher;
    private final HashMap<ComponentName, ShortcutIcon> mLoadedApps = 
            new HashMap<ComponentName, ShortcutIcon>();
    private MessageReceiver mMessageReceiver;

    public ApplicationsMessage(Launcher launcher) {
        mLauncher = launcher;
    }

    private void initialize() {
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.intent.action.APPLICATION_MESSAGE_UPDATE");
        mMessageReceiver = new MessageReceiver();
        mLauncher.registerReceiver(mMessageReceiver, intentfilter);
    }

    private void updateMessage(ComponentName componentname, String s, String s1, byte abyte0[]) {
        if (mLoadedApps.containsKey(componentname)) {
            IconMessage iconMessage = (IconMessage)mLoadedApps.get(componentname);
            iconMessage.setMessage(s, s1, abyte0);
            ShortcutInfo shortcutInfo = (ShortcutInfo)((ShortcutIcon)iconMessage).getTag();
            updateFolderMessage(mLauncher.getParentFolderInfo(shortcutInfo));
        }
    }

    public void addApplication(ShortcutIcon shortcuticon, ComponentName componentname) {
        if (componentname != null) {
            if (mLoadedApps.containsKey(componentname)) {
                IconMessage iconmessage = (IconMessage)mLoadedApps.get(componentname);
                shortcuticon.setMessage(iconmessage.getMessageText(), 
                        iconmessage.getMessageTextBackground(), iconmessage.getMessageIconTile());
                mLoadedApps.remove(componentname);
            }
            mLoadedApps.put(componentname, shortcuticon);
        }
    }

    public void destory() {
        mLauncher.unregisterReceiver(mMessageReceiver);
        mMessageReceiver = null;
        mLoadedApps.clear();
    }

    public void onLaunchApplication(ComponentName componentname) {
    }

    public void removeApplication(CharSequence charsequence) {
    }

    public void requestUpdateMessages() {
        if (mMessageReceiver == null){
            initialize();
        }
        Intent intent = new Intent("android.intent.action.APPLICATION_MESSAGE_QUERY");
        mLauncher.sendBroadcast(intent);
    }

    public void updateFolderMessage(FolderInfo folderinfo) {
        if (folderinfo == null) return;
        FolderIcon foldericon = mLauncher.getFolderIcon(folderinfo);;
        if (foldericon == null) return;
        int i = 0;
        
        Iterator<ShortcutIcon> iterator = mLoadedApps.values().iterator();

        while (iterator.hasNext()) {
            IconMessage iconmessage = (IconMessage)iterator.next();
            if (((ShortcutInfo)((ShortcutIcon)iconmessage).getTag()).container == 
                    folderinfo.id && !iconmessage.isEmptyMessage()){
                i++;
            }
        }
        if (i != 0){
            foldericon.setMessage(String.valueOf(i));
        } else {
            foldericon.setMessage(null);
        }
        return;
    }
}