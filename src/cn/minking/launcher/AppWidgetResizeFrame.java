package cn.minking.launcher;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class AppWidgetResizeFrame extends FrameLayout{
    final int BACKGROUND_PADDING = 24;
    final float DIMMED_HANDLE_ALPHA = 0F;
    final float RESIZE_THRESHOLD = 0.66F;
    final int SNAP_DURATION = 150;
    private int mBackgroundPadding;
    private int mBaselineHeight;
    private int mBaselineWidth;
    private int mBaselineX;
    private int mBaselineY;
    private boolean mBottomBorderActive;
    private ImageView mBottomHandle;
    private CellLayout mCellLayout;
    private int mDeltaX;
    private int mDeltaY;
    private DragLayer mDragLayer;
    private int mExpandability[];
    private ItemInfo mItemInfo;
    private Launcher mLauncher;
    private boolean mLeftBorderActive;
    private ImageView mLeftHandle;
    private int mMinHSpan;
    private int mMinVSpan;
    private int mResizeMode;
    private boolean mRightBorderActive;
    private ImageView mRightHandle;
    private int mRunningHInc;
    private int mRunningVInc;
    private int mTmpPos[];
    private boolean mTopBorderActive;
    private ImageView mTopHandle;
    private int mTouchTargetWidth;
    private int mWidgetPaddingBottom;
    private int mWidgetPaddingLeft;
    private int mWidgetPaddingRight;
    private int mWidgetPaddingTop;
    private LauncherAppWidgetHostView mWidgetView;
    private Workspace mWorkspace;
    
    public AppWidgetResizeFrame(Context context, ItemInfo iteminfo, 
            LauncherAppWidgetHostView launcherappwidgethostview, 
            CellLayout celllayout, DragLayer draglayer) {
        super(context);
        
        mExpandability = new int[4];
        mTmpPos = new int[2];
        mLauncher = (Launcher)context;
        mItemInfo = iteminfo;
        mCellLayout = celllayout;
        mWidgetView = launcherappwidgethostview;
        mResizeMode = launcherappwidgethostview.getAppWidgetInfo().resizeMode;
        mDragLayer = draglayer;
        mWorkspace = (Workspace)draglayer.findViewById(R.id.workspace);
        AppWidgetProviderInfo aInfo = launcherappwidgethostview.getAppWidgetInfo();
        mMinHSpan = ResConfig.getWidgetSpanX(aInfo.minResizeWidth);
        mMinVSpan = ResConfig.getWidgetSpanX(aInfo.minResizeHeight);
        setBackgroundResource(R.drawable.widget_resize_frame_holo);
        setPadding(0, 0, 0, 0);
        mLeftHandle = new ImageView(context);
        mLeftHandle.setImageResource(R.drawable.widget_resize_handle_left);
        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(-2, -2, 19);
        addView(mLeftHandle, lParams);
        mRightHandle = new ImageView(context);
        mRightHandle.setImageResource(R.drawable.widget_resize_handle_right);
        FrameLayout.LayoutParams rParams = new FrameLayout.LayoutParams(-2, -2, 21);
        addView(mRightHandle, rParams);
        mTopHandle = new ImageView(context);
        mTopHandle.setImageResource(R.drawable.widget_resize_handle_top);
        FrameLayout.LayoutParams tParams = new FrameLayout.LayoutParams(-2, -2, 49);
        addView(mTopHandle, tParams);
        mBottomHandle = new ImageView(context);
        mBottomHandle.setImageResource(R.drawable.widget_resize_handle_bottom);
        FrameLayout.LayoutParams bParams = new FrameLayout.LayoutParams(-2, -2, 81);
        addView(mBottomHandle, bParams);
        Launcher.Padding padding = mLauncher.getPaddingForWidget(launcherappwidgethostview.getAppWidgetInfo().provider);
        mWidgetPaddingLeft = padding.left;
        mWidgetPaddingTop = padding.top;
        mWidgetPaddingRight = padding.right;
        mWidgetPaddingBottom = padding.bottom;
        if (mResizeMode != 1) {
            if (mResizeMode == 2) {
                mLeftHandle.setVisibility(View.GONE);
                mRightHandle.setVisibility(View.GONE);
            }
        } else {
            mTopHandle.setVisibility(View.GONE);
            mBottomHandle.setVisibility(View.GONE);
        }
        mBackgroundPadding = (int)Math.ceil(24F * mLauncher.getResources().getDisplayMetrics().density);
        mTouchTargetWidth = 2 * mBackgroundPadding;
    }
    
    private void resizeWidgetIfNeeded() {
        int cWidth = mCellLayout.getCellWidth() + mCellLayout.getWidthGap();
        int cHeight = mCellLayout.getCellHeight() + mCellLayout.getHeightGap();
        float f = (1F * (float)mDeltaX) / (float)cWidth - (float)mRunningHInc;
        float f1 = (1F * (float)mDeltaY) / (float)cHeight - (float)mRunningVInc;
        int k = 0;
        int l = 0;
        int i = 0;
        int j = 0;
        
        if (Math.abs(f) > 0.66F) {
            k = Math.round(f);
        }
        if (Math.abs(f1) > 0.66F) {
            l = Math.round(f1);
        }
        if (k != 0 || l != 0) {
            mCellLayout.updateCellOccupiedMarks(mWidgetView, true);
            CellLayout.LayoutParams layoutparams = (CellLayout.LayoutParams)mWidgetView.getLayoutParams();
            if (mRightBorderActive) {
                k = Math.min(mExpandability[2], k);
                k = Math.max(-(layoutparams.cellHSpan - mMinHSpan), k);
                mRunningHInc = k + mRunningHInc;
            } else {
                j = Math.max(-mExpandability[0], k);
                j = Math.min(layoutparams.cellHSpan - mMinHSpan, j);
                k *= -1;
                k = Math.min(mExpandability[0], k);
                k = Math.max(-(layoutparams.cellHSpan - mMinHSpan), k);
                mRunningHInc = mRunningHInc - k;
            }
            if (mBottomBorderActive) {
                l = Math.min(mExpandability[3], l);
                l = Math.max(-(layoutparams.cellVSpan - mMinVSpan), l);
                mRunningVInc = l + mRunningVInc;
            } else {
                i = Math.max(-mExpandability[1], l);
                i = Math.min(layoutparams.cellVSpan - mMinVSpan, i);
                l *= -1;
                l = Math.min(mExpandability[1], l);
                l = Math.max(-(layoutparams.cellVSpan - mMinVSpan), l);
                mRunningVInc = mRunningVInc - l;
            }
            if (mLeftBorderActive || mRightBorderActive) {
                layoutparams.cellHSpan = k + layoutparams.cellHSpan;
                layoutparams.cellX = j + layoutparams.cellX;
            }
            if (mTopBorderActive || mBottomBorderActive) {
                layoutparams.cellVSpan = l + layoutparams.cellVSpan;
                layoutparams.cellY = i + layoutparams.cellY;
            }
            mCellLayout.getExpandabilityArrayForView(mWidgetView, mExpandability);
            mCellLayout.updateCellOccupiedMarks(mWidgetView, false);
            mWidgetView.requestLayout();
        }
    }

    public boolean beginResizeIfPointInRegion(int i, int j) {
        float f = 1F;
        boolean flag1;
        if ((1 & mResizeMode) == 0){
            flag1 = false;
        } else {
            flag1 = true;
        }
        
        boolean flag;
        if ((2 & mResizeMode) == 0) {
            flag = false;
        } else {
            flag = true;
        }
        
        boolean flag2;
        if (i >= mTouchTargetWidth || !flag1) {
            flag2 = false;
        } else {
            flag2 = true;
        }
        mLeftBorderActive = flag2;
        
        if (i <= getWidth() - mTouchTargetWidth || !flag1) { 
            flag1 = false;
        } else {
            flag1 = true;
        }
        mRightBorderActive = flag1;
        
        if (j >= mTouchTargetWidth || !flag) {
            flag1 = false;
        } else {
            flag1 = true;
        }
        mTopBorderActive = flag1;
        
        if (j <= getHeight() - mTouchTargetWidth || !flag){
            flag = false;
        } else {
            flag = true;
        }
        mBottomBorderActive = flag;
        
        if (!mLeftBorderActive && !mRightBorderActive && !mTopBorderActive && !mBottomBorderActive){
            flag = false;
        } else {
            flag = true;
        }
        
        mBaselineWidth = getMeasuredWidth();
        mBaselineHeight = getMeasuredHeight();
        mBaselineX = getLeft();
        mBaselineY = getTop();
        mRunningHInc = 0;
        mRunningVInc = 0;
        
        if (flag)  {
            ImageView imageview1 = mLeftHandle;
            float f1;
            if (!mLeftBorderActive){
                f1 = 0F;
            } else {
                f1 = f;
            }
            imageview1.setAlpha(f1);
            
            ImageView imageview = mRightHandle;
            float f2;
            if (!mRightBorderActive){
                f2 = 0F;
            } else {
                f2 = f;
            }
            imageview.setAlpha(f2);
            imageview = mTopHandle;
            
            if (!mTopBorderActive){
                f2 = 0F;
            } else {
                f2 = f;
            }
            imageview.setAlpha(f2);
            imageview = mBottomHandle;
            
            if (!mBottomBorderActive){
                f = 0F;
            }
            imageview.setAlpha(f);
        }
        mCellLayout.getExpandabilityArrayForView(mWidgetView, mExpandability);
        return flag;
    }

    public void commitResizeForDelta(int i, int j) {
        visualizeResizeForDelta(i, j);
        CellLayout.LayoutParams layoutparams = (CellLayout.LayoutParams)mWidgetView.getLayoutParams();
        LauncherModel.resizeItemInDatabase(getContext(), mItemInfo, layoutparams.cellX, layoutparams.cellY, layoutparams.cellHSpan, layoutparams.cellVSpan);
        mWidgetView.requestLayout();
        post(new Runnable() {
            @Override
            public void run() {
                snapToWidget(false);
            }
        });
    }

    public void snapToWidget(boolean flag) {
        DragLayer.LayoutParams layoutParams = (DragLayer.LayoutParams)getLayoutParams();
        mCellLayout.getLocationInWindow(mTmpPos);
        int cx = ((mCellLayout.getLeft() + mCellLayout.getPaddingLeft()) - mDragLayer.getScrollX()) + mTmpPos[0];
        int cy = ((mCellLayout.getTop() + mCellLayout.getPaddingTop()) - mDragLayer.getScrollY()) + mTmpPos[1];
        int widgetWidth = (mWidgetView.getWidth() + 2 * mBackgroundPadding) - mWidgetPaddingLeft - mWidgetPaddingRight;
        int widgetHeight = (mWidgetView.getHeight() + 2 * mBackgroundPadding) - mWidgetPaddingTop - mWidgetPaddingBottom;
        cx = cx + (mWidgetView.getLeft() - mBackgroundPadding) + mWidgetPaddingLeft;
        cy = cy + (mWidgetView.getTop() - mBackgroundPadding) + mWidgetPaddingTop;
        if (cy < 0) {
            widgetHeight -= -cy;
            cy = 0;
        }
        if (cy + widgetHeight > mDragLayer.getHeight()){
            widgetHeight -= (cy + widgetHeight) - mDragLayer.getHeight();
        }
        if (flag) {
            int af[] = new int[2];
            af[0] = layoutParams.width;
            af[1] = widgetWidth;
            PropertyValuesHolder wHolder = PropertyValuesHolder.ofInt("width", af);
            af[0] = layoutParams.height;
            af[1] = widgetHeight;
            PropertyValuesHolder hHolder = PropertyValuesHolder.ofInt("height", af);
            af[0] = layoutParams.x;
            af[1] = cx;
            PropertyValuesHolder xHolder = PropertyValuesHolder.ofInt("x", af);
            af[0] = layoutParams.y;
            af[1] = cy;
            PropertyValuesHolder yHolder = PropertyValuesHolder.ofInt("y", af);
            
            PropertyValuesHolder[] vHolders = new PropertyValuesHolder[4];
            vHolders[0] = wHolder;
            vHolders[1] = hHolder;
            vHolders[2] = xHolder;
            vHolders[3] = yHolder;
            ObjectAnimator oAnimator = ObjectAnimator.ofPropertyValuesHolder(layoutParams, vHolders);
            
            ObjectAnimator lAnimator = ObjectAnimator.ofFloat(mLeftHandle, "alpha", 1F);
            ObjectAnimator rAnimator = ObjectAnimator.ofFloat(mRightHandle, "alpha", 1F);
            ObjectAnimator tAnimator = ObjectAnimator.ofFloat(mTopHandle, "alpha", 1F);
            ObjectAnimator bAnimator = ObjectAnimator.ofFloat(mBottomHandle, "alpha", 1F);
            ValueAnimator.AnimatorUpdateListener vAnimator = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueanimator) {
                    requestLayout();
                }

            };
            
            oAnimator.addUpdateListener(vAnimator);
            AnimatorSet animatorSet = new AnimatorSet();
            if (mResizeMode != 2) {
                if (mResizeMode != 1) {
                    Animator aanimator[] = new Animator[5];
                    aanimator[0] = oAnimator;
                    aanimator[1] = lAnimator;
                    aanimator[2] = rAnimator;
                    aanimator[3] = tAnimator;
                    aanimator[4] = bAnimator;
                    animatorSet.playTogether(aanimator);
                } else {
                    Animator aanimator[] = new Animator[3];
                    aanimator[0] = oAnimator;
                    aanimator[1] = lAnimator;
                    aanimator[2] = rAnimator;
                    animatorSet.playTogether(aanimator);
                }
            } else  {
                Animator aanimator[] = new Animator[3];
                aanimator[0] = oAnimator;
                aanimator[1] = tAnimator;
                aanimator[2] = bAnimator;
                animatorSet.playTogether(aanimator);
            }
            animatorSet.setDuration(150L);
            animatorSet.start();
        } else {
            layoutParams.width = widgetWidth;
            layoutParams.height = widgetHeight;
            layoutParams.x = cx;
            layoutParams.y = cy;
            mLeftHandle.setAlpha(1F);
            mRightHandle.setAlpha(1F);
            mTopHandle.setAlpha(1F);
            mBottomHandle.setAlpha(1F);
            requestLayout();
        }
    }

    public void updateDeltas(int i, int j) {
        if (mRightBorderActive) {
            mDeltaX = Math.min(mDragLayer.getWidth() - (mBaselineX + mBaselineWidth), i);
            mDeltaX = Math.max(-mBaselineWidth + 2 * mTouchTargetWidth, mDeltaX);
        } else {
            mDeltaX = Math.max(-mBaselineX, i);
            mDeltaX = Math.min(mBaselineWidth - 2 * mTouchTargetWidth, mDeltaX);
        }
        if (mBottomBorderActive) {
                mDeltaY = Math.min(mDragLayer.getHeight() - (mBaselineY + mBaselineHeight), j);
                mDeltaY = Math.max(-mBaselineHeight + 2 * mTouchTargetWidth, mDeltaY);
        } else {
            mDeltaY = Math.max(-mBaselineY, j);
            mDeltaY = Math.min(mBaselineHeight - 2 * mTouchTargetWidth, mDeltaY);
        }
    }

    public void visualizeResizeForDelta(int i, int j) {
        updateDeltas(i, j);
        DragLayer.LayoutParams layoutparams = (DragLayer.LayoutParams)getLayoutParams();
        if (mRightBorderActive) {
            layoutparams.width = mBaselineWidth + mDeltaX;
        } else {
            layoutparams.x = mBaselineX + mDeltaX;
            layoutparams.width = mBaselineWidth - mDeltaX;
        }
        if (mBottomBorderActive) {
            layoutparams.height = mBaselineHeight + mDeltaY;
        } else {
            layoutparams.y = mBaselineY + mDeltaY;
            layoutparams.height = mBaselineHeight - mDeltaY;
        }
        resizeWidgetIfNeeded();
        requestLayout();
    }
}