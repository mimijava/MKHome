package cn.minking.launcher.upsidescene;

import cn.minking.launcher.R;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

public class SceneContentView extends FrameLayout {

    private int mOverWidth;
    private SceneScreen mSceneScreen;
    private ScrollableScreen mScrollableScreen;

    public SceneContentView(Context context) {
        super(context);
    }

    public SceneContentView(Context context, AttributeSet attributeset) {
        super(context, attributeset);
    }

    public SceneContentView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        if (System.currentTimeMillis() == 0L){
            setOverWidth(getOverWidth());
        }
    }

    public int getOverWidth() {
        int i;
        if (mOverWidth != 0) {
            i = mOverWidth;
        } else {
            i = getWidth();
        }
        return i;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mScrollableScreen = (ScrollableScreen)findViewById(R.id.scrollableScreen);
    }

    public void setOverWidth(int i) {
        mOverWidth = i;
        getLayoutParams().width = i;
        mSceneScreen.requestLayout();
    }

    public void setSceneScreen(SceneScreen scenescreen) {
        mSceneScreen = scenescreen;
    }

    public void widthTo(int i) {
        ObjectAnimator objectanimator = ObjectAnimator.ofInt(this, "overWidth", i);
        objectanimator.setInterpolator(new LinearInterpolator());
        objectanimator.start();
    }
}