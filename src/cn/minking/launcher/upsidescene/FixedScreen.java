package cn.minking.launcher.upsidescene;

import cn.minking.launcher.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class FixedScreen extends FrameLayout{

    private FreeLayout mFreeLayout;

    public FixedScreen(Context context) {
        this(context, null);
    }

    public FixedScreen(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public FixedScreen(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
    }

    public void exitEditMode() {
        for (int i = 0; i < mFreeLayout.getChildCount() - 1; i++) {
            View view = mFreeLayout.getChildAt(i);
            if (view instanceof SpriteView){
                //((SpriteView)view).exitEditMode();
            }
        }
    }

    public int getChildWidth() {
        return mFreeLayout.getWidth();
    }

    public void gotoEditMode() {
        for (int i = 0; i < mFreeLayout.getChildCount() - 1; i++) {
            View view = mFreeLayout.getChildAt(i);
            if (view instanceof SpriteView) {
                //((SpriteView)view).gotoEditMode();
            }
        }
    }

    public void notifyGadgets(int i) {
        mFreeLayout.notifyGadgets(i);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mFreeLayout = (FreeLayout)findViewById(R.id.freeLayout);
    }

    public void setSceneScreen(SceneScreen scenescreen) {
        mFreeLayout.setSceneScreen(scenescreen);
    }

    public void setScreenData(SceneData.Screen screen) {
        if (screen != null) {
            mFreeLayout.setScreenData(screen);
        }
    }
}