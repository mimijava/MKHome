package cn.minking.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestoreFinishedReceiver extends BroadcastReceiver {
    final private static String TAG = "MKHome.RestoreFinishedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "{android.intent.action.RESTORE_FINISH} received.");
        ((LauncherApplication)(LauncherApplication)context.getApplicationContext()).setJustRestoreFinished();
    }

}
