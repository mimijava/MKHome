package cn.minking.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ErrorBar extends TextView{
    private Runnable mCloseErrorBar;
    private Animation mFadeIn;
    private Animation mFadeOut;
    
    public ErrorBar(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        mCloseErrorBar = new Runnable() {
            @Override
            public void run()
            {
                hideError();
            }
        };
        mFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.shrink_to_top);
        mFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.shrink_to_bottom);
    }

    private void showErrorOrWarning(int i, boolean flag) {
        setText(i);
        setVisibility(View.VISIBLE);
        startAnimation(mFadeIn);
        removeCallbacks(mCloseErrorBar);
        postDelayed(mCloseErrorBar, getContext().getResources().getInteger(R.integer.error_notification_duration));
    }

    void hideError() {
        setVisibility(View.INVISIBLE);
        startAnimation(mFadeOut);
    }

    public void setMargins(int i, int j, int k, int l)    {
        FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams)getLayoutParams();
        layoutparams.setMargins(i, j, k, l);
        setLayoutParams(layoutparams);
    }

    void showError(int i) {
        showErrorOrWarning(i, false);
    }
}