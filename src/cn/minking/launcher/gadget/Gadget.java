package cn.minking.launcher.gadget;

import android.os.Bundle;

public interface Gadget{

    public abstract void onAdded();

    public abstract void onCreate();

    public abstract void onDeleted();

    public abstract void onDestroy();

    public abstract void onEditDisable();

    public abstract void onEditNormal();

    public abstract void onPause();

    public abstract void onResume();

    public abstract void onStart();

    public abstract void onStop();

    public abstract void updateConfig(Bundle bundle);
}
