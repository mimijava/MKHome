package cn.minking.launcher.upsidescene;

import java.util.Iterator;

import cn.minking.launcher.gadget.Gadget;
import cn.minking.launcher.upsidescene.SceneData.Sprite;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FreeLayout extends ViewGroup {
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public int left;
        public int top;

        public LayoutParams() {
            super(-2, -2);
        }
    }

    private static Rect mTmpRect = new Rect();
    private SceneScreen mSceneScreen;
    private SceneData.Screen mScreenData;
    private Context mContext;

    public FreeLayout(Context context) {
        this(context, null);
    }

    public FreeLayout(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public FreeLayout(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        mContext = context;
    }

    private boolean isChildVisibleNow(View view) {
        view.getHitRect(mTmpRect);
        return mTmpRect.intersect(getLeft() + getScrollX(), getTop(), getRight() + getScrollX(), getBottom());
    }

    private void measureChild(View view) {
        int j = View.MeasureSpec.makeMeasureSpec(0, 0);
        int i = View.MeasureSpec.makeMeasureSpec(0, 0);
        if (view.getLayoutParams().width > 0) {
            j = View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().width, MeasureSpec.EXACTLY);
            i = View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().height, MeasureSpec.EXACTLY);
        }
        view.measure(j, i);
    }

    public SceneData.Screen getScreenData() {
        return mScreenData;
    }

    public void notifyGadgets(int i) {
        /*
        for (int j = 0; j < getChildCount() - 1; j++) {
            View v = getChildAt(j);
            if (v instanceof SpriteView) {
                SceneData.SpriteCell sCell = v.getSpriteData();
                if (sCell instanceof SceneData.SpriteCell) {
                    if (sCell.getContentType() == 3 || sCell.getContentType() == 4) {
                        Gadget gadget = (Gadget)v.getContentView();
                        switch (i)
                        {
                        case 9: // '\t'
                        default:
                            break;

                        case 1: // '\001'
                            gadget.onStart();
                            break;

                        case 2: // '\002'
                            gadget.onStop();
                            break;

                        case 3: // '\003'
                            gadget.onPause();
                            break;

                        case 4: // '\004'
                            if (isChildVisibleNow(v)){
                                gadget.onResume();
                            }
                            break;

                        case 5: // '\005'
                            gadget.onCreate();
                            break;

                        case 6: // '\006'
                            gadget.onDestroy();
                            break;

                        case 7: // '\007'
                            gadget.onEditDisable();
                            break;

                        case 8: // '\b'
                            gadget.onEditNormal();
                            break;

                        case 10: // '\n'
                            if (!isChildVisibleNow(v)){
                                gadget.onPause();
                            } else {
                                gadget.onResume();
                            }
                            break;
                        }
                    }
                }
            }
        }*/
    }

    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        for (int i1 = 0; i1 < getChildCount(); i1++) {
            View view = getChildAt(i1);
            if (view.getVisibility() != View.GONE) {
                LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                int k1 = layoutparams.left;
                int l1 = layoutparams.top;
                view.layout(k1, l1, k1 + view.getMeasuredWidth(), l1 + view.getMeasuredHeight());
            }
        }
        
    }

    protected void onMeasure(int i, int j) {
        int k = android.view.View.MeasureSpec.getMode(i);
        int l = android.view.View.MeasureSpec.getSize(i);
        int j1 = android.view.View.MeasureSpec.getMode(j);
        int i1 = android.view.View.MeasureSpec.getSize(j);
        if (k == 0 || j1 == 0) {
            l = mScreenData.getWidth();
            i1 = mScreenData.getHeight();
        }
        setMeasuredDimension(l, i1);
        for (int k1 = 0; k1 < getChildCount(); k1++) {
            measureChild(getChildAt(k1));
        }
    }

    public void setSceneScreen(SceneScreen scenescreen) {
        mSceneScreen = scenescreen;
    }

    public void setScreenData(SceneData.Screen screen) {
        /*
        mScreenData = screen;
        getLayoutParams().width = screen.getWidth();
        removeAllViews();
        Iterator<Sprite> iterator = mScreenData.getSprites().iterator();
        while (iterator.hasNext()){
            SceneData.Sprite sprite = (SceneData.Sprite)iterator.next();
            SpriteView spriteview = new SpriteView(mContext);
            spriteview.setSceneScreen(mSceneScreen);
            spriteview.setSpriteData(sprite);
            LayoutParams layoutparams = new LayoutParams();
            layoutparams.top = sprite.getTop();
            layoutparams.left = sprite.getLeft();
            layoutparams.width = sprite.getRight() - sprite.getLeft();
            layoutparams.height = sprite.getBottom() - sprite.getTop();
            addView(spriteview, layoutparams);
        }
        */
    }
}