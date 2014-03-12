package cn.minking.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

public class ThumbnailScreen extends ViewGroup{
    private Animation.AnimationListener mAnimationListener;
    protected int mChildHeight;
    protected int mChildWidth;
    protected int mColumnCount;
    private View mFoucsedThumbnail;
    private int mLastestMoveIndex;
    private long mLastestMoveTime;
    protected int mMaxChildrenCount;
    private boolean mMovingAnimationStarted;
    protected boolean mOrderThumbnailInRowFirst;
    protected int mRowCount;
    protected int mScreenMarginLeft;
    protected int mScreenMarginTop;
    
    public ThumbnailScreen(Context context, int i, int j, int k, int l, boolean flag){
        super(context);
        mLastestMoveIndex = -1;
        mMovingAnimationStarted = false;
        mRowCount = Math.max(1, i);
        mColumnCount = Math.max(1, j);
        mChildHeight = Math.max(1, l);
        mChildWidth = Math.max(1, k);
        mMaxChildrenCount = mRowCount * mColumnCount;
        mOrderThumbnailInRowFirst = flag;
    }
    
    private int convertToColumnIndex(int i) {
        int j;
        if (!mOrderThumbnailInRowFirst){
            j = i / mRowCount;
        } else {
            j = i % mColumnCount;
        }
        return j + (i / mMaxChildrenCount) * mColumnCount;
    }
    
    private int convertToRawIndex(int i, int j) {
        int k;
        if (!mOrderThumbnailInRowFirst) {
            k = i + j * mRowCount;
        } else {
            k = j + i * mColumnCount;
        }
        return k;
    }

    private int convertToRowIndex(int i) {
        int j;
        if (!mOrderThumbnailInRowFirst) {
            j = i % mRowCount;
        } else {
            j = i / mColumnCount;
        }
        return j % mRowCount;
    }

    private int getPositionIndex(int i, int j) {
        int k = -1;
        int l = i - mScreenMarginLeft;
        int j1 = j - mScreenMarginTop;
        int k1 = j1 / mChildHeight;
        int i1 = l / mChildWidth;
        if (l >= 0 && j1 >= 0 && i1 < mColumnCount && k1 < mRowCount) {
            if (7 * Math.abs(l % mChildWidth - mChildWidth / 2) <= 3 * mChildWidth 
                    && 7 * Math.abs(j1 % mChildHeight - mChildHeight / 2) <= 3 * mChildHeight){
                k = convertToRawIndex(k1, i1);
            }
        } else {
            k = convertToRawIndex(Math.max(0, Math.min(k1, -1 + mRowCount)), 
                    Math.max(0, Math.min(i1, -1 + mColumnCount)));
        }
        return k;
    }

    public void addView(View view, int i, android.view.ViewGroup.LayoutParams layoutparams) {
        if (getChildCount() < mMaxChildrenCount) {
            super.addView(view, i, layoutparams);
            return;
        } else {
            Object aobj[] = new Object[1];
            aobj[0] = Integer.valueOf(mMaxChildrenCount);
            throw new IllegalArgumentException(String.format("ScreenViewItem only support %d children.", aobj));
        }
    }

    public int getThumbnailIndex(int i, int j) {
        Rect rect = new Rect();
        int k = getChildCount() - 1;
        
        while (k >= 0) {
            getChildAt(k).getHitRect(rect);
            if (rect.contains(i, j))
                break;
            k--;
        }
        
        return k;
    }

    public boolean isMovingAnimationStarted() {
        return mMovingAnimationStarted;
    }

    protected void layoutChildByIndex(int i) {
        int j = convertToRowIndex(i);
        int k = convertToColumnIndex(i);
        getChildAt(i).layout(mScreenMarginLeft + 
                k * mChildWidth, mScreenMarginTop + 
                j * mChildHeight, mScreenMarginLeft + 
                mChildWidth * (k + 1), mScreenMarginTop + 
                mChildHeight * (j + 1));
    }

    public void moveThumbnail(int i, int j, int k){
        boolean flag = true;
        if (j == k) return;
        int l = getChildCount() - 1;
        while (l >= 0) {
            getChildAt(l).clearAnimation();
            l--;
        }
    }
    
    public int moveThumbnailInto(boolean flag, ThumbnailScreen thumbnailscreen, int i) {
        int j = 0;
        View view = thumbnailscreen.getChildAt(i);
        thumbnailscreen.removeView(view);
        if (!flag) {
            View v = getChildAt(getChildCount() - 1);
            removeViewInLayout(v);
            thumbnailscreen.addView(v, 0);
            addViewInLayout(view, -1, generateDefaultLayoutParams(), true);
            j = -1 + getChildCount();
        } else {
            View view1 = getChildAt(0);
            removeViewInLayout(view1);
            thumbnailscreen.addView(view1);
            addViewInLayout(view, 0, generateDefaultLayoutParams(), true);
        }
        return j;
    }
    
    public int moveThumbnailTo(int i, int j, int k, int l) {
        int k1 = Math.min(getPositionIndex(k, l), -1 + getChildCount());
        if (k1 < 0) {
            k1 = j;
        }
        if (k1 != j) {
            if (mLastestMoveIndex >= 0 && mLastestMoveIndex == k1) {
                if (System.currentTimeMillis() - mLastestMoveTime < 100L)
                    k1 = j;
            } else {
                mLastestMoveTime = System.currentTimeMillis();
                mLastestMoveIndex = k1;
                k1 = j;
            }
        }
        moveThumbnail(i, j, k1);
        int i1 = k - mChildWidth / 2;
        int j1 = l - mChildHeight / 2;
        View view = getChildAt(k1);
        view.layout(i1, j1, i1 + view.getWidth(), j1 + view.getHeight());
        invalidate();
        return k1;
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        
    }

    @Override
    protected void onAnimationEnd() {
        // TODO Auto-generated method stub
        super.onAnimationEnd();
    }

    @Override
    protected void onAnimationStart() {
        // TODO Auto-generated method stub
        super.onAnimationStart();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 
                MeasureSpec.getSize(heightMeasureSpec));
        measureChildren(MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY), 
                MeasureSpec.makeMeasureSpec(mChildHeight, MeasureSpec.EXACTLY));
    }
    
    public void resetThumbnailLayout(int i) {
        layoutChildByIndex(i);
    }
    
    public void startMovingAnimation(int i, int j, int k){
        
    }
    
    public void startSwitchingAnimation(boolean flag, int i, int j, Animation.AnimationListener animationlistener){
        
    }
}