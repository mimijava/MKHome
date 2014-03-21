package cn.minking.launcher;

import android.graphics.*;

public class HolographicOutlineHelper {

    public static final int MAX_OUTER_BLUR_RADIUS;
    public static final int MIN_OUTER_BLUR_RADIUS;
    private static final MaskFilter sCoarseClipTable = TableMaskFilter.CreateClipTable(0, 200);
    private static final BlurMaskFilter sExtraThickInnerBlurMaskFilter;
    private static final BlurMaskFilter sExtraThickOuterBlurMaskFilter;
    private static final BlurMaskFilter sMediumInnerBlurMaskFilter;
    private static final BlurMaskFilter sMediumOuterBlurMaskFilter;
    private static final BlurMaskFilter sThickInnerBlurMaskFilter;
    private static final BlurMaskFilter sThickOuterBlurMaskFilter;
    private static final BlurMaskFilter sThinOuterBlurMaskFilter;
    private final Paint mAlphaClipPaint = new Paint();
    private final Paint mBlurPaint = new Paint();
    private final Paint mErasePaint = new Paint();
    private final Paint mHolographicPaint = new Paint();
    private int mTempOffset[];

    HolographicOutlineHelper() {
        mTempOffset = new int[2];
        mHolographicPaint.setFilterBitmap(true);
        mHolographicPaint.setAntiAlias(true);
        mBlurPaint.setFilterBitmap(true);
        mBlurPaint.setAntiAlias(true);
        mErasePaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT));
        mErasePaint.setFilterBitmap(true);
        mErasePaint.setAntiAlias(true);
        TableMaskFilter tablemaskfilter = TableMaskFilter.CreateClipTable(180, 255);
        mAlphaClipPaint.setMaskFilter(tablemaskfilter);
    }

    void applyExpensiveOutlineWithBlur(Bitmap bitmap, Canvas canvas, int i, int j, int k) {
        applyExpensiveOutlineWithBlur(bitmap, canvas, i, j, mAlphaClipPaint, k);
    }

    void applyExpensiveOutlineWithBlur(Bitmap bitmap, Canvas canvas, int i, int j, Paint paint, int k) {
        if (paint == null){
            paint = mAlphaClipPaint;
        }
        int ai[] = mTempOffset;
        Bitmap bitmap1 = bitmap.extractAlpha(paint, ai);
        BlurMaskFilter blurmaskfilter;
        switch (k) {
        default:
            throw new RuntimeException("Invalid blur thickness");

        case 0: // '\0'
            blurmaskfilter = sThickOuterBlurMaskFilter;
            break;

        case 1: // '\001'
            blurmaskfilter = sMediumOuterBlurMaskFilter;
            break;

        case 2: // '\002'
            blurmaskfilter = sExtraThickOuterBlurMaskFilter;
            break;
        }
        mBlurPaint.setMaskFilter(blurmaskfilter);
        int ai2[] = new int[2];
        Bitmap bitmap3 = bitmap1.extractAlpha(mBlurPaint, ai2);
        if (k != 2) {
            mBlurPaint.setMaskFilter(sThinOuterBlurMaskFilter);
        } else {
            mBlurPaint.setMaskFilter(sMediumOuterBlurMaskFilter);
        }
        int ai1[] = new int[2];
        Bitmap bitmap2 = bitmap1.extractAlpha(mBlurPaint, ai1);
        canvas.setBitmap(bitmap1);
        canvas.drawColor(0xff000000, android.graphics.PorterDuff.Mode.SRC_OUT);
        BlurMaskFilter blurmaskfilter1;
        switch (k) {
        default:
            throw new RuntimeException("Invalid blur thickness");

        case 0: // '\0'
            blurmaskfilter1 = sThickInnerBlurMaskFilter;
            break;

        case 1: // '\001'
            blurmaskfilter1 = sMediumInnerBlurMaskFilter;
            break;

        case 2: // '\002'
            blurmaskfilter1 = sExtraThickInnerBlurMaskFilter;
            break;
        }
        mBlurPaint.setMaskFilter(blurmaskfilter1);
        int ai3[] = new int[2];
        Bitmap bitmap4 = bitmap1.extractAlpha(mBlurPaint, ai3);
        canvas.setBitmap(bitmap4);
        canvas.drawBitmap(bitmap1, -ai3[0], -ai3[1], mErasePaint);
        canvas.drawRect(0F, 0F, -ai3[0], bitmap4.getHeight(), mErasePaint);
        canvas.drawRect(0F, 0F, bitmap4.getWidth(), -ai3[1], mErasePaint);
        canvas.setBitmap(bitmap);
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
        mHolographicPaint.setColor(i);
        canvas.drawBitmap(bitmap4, ai3[0], ai3[1], mHolographicPaint);
        canvas.drawBitmap(bitmap3, ai2[0], ai2[1], mHolographicPaint);
        mHolographicPaint.setColor(j);
        canvas.drawBitmap(bitmap2, ai1[0], ai1[1], mHolographicPaint);
        canvas.setBitmap(null);
        bitmap2.recycle();
        bitmap3.recycle();
        bitmap4.recycle();
        bitmap1.recycle();
    }

    void applyMediumExpensiveOutlineWithBlur(Bitmap bitmap, Canvas canvas, int i, int j) {
        applyExpensiveOutlineWithBlur(bitmap, canvas, i, j, 1);
    }

    static  {
        float f = LauncherApplication.getScreenDensity();
        MIN_OUTER_BLUR_RADIUS = (int)(f * 1F);
        MAX_OUTER_BLUR_RADIUS = (int)(f * 12F);
        sExtraThickOuterBlurMaskFilter = new BlurMaskFilter(12F * f, android.graphics.BlurMaskFilter.Blur.OUTER);
        sThickOuterBlurMaskFilter = new BlurMaskFilter(f * 6F, android.graphics.BlurMaskFilter.Blur.OUTER);
        sMediumOuterBlurMaskFilter = new BlurMaskFilter(f * 2F, android.graphics.BlurMaskFilter.Blur.OUTER);
        sThinOuterBlurMaskFilter = new BlurMaskFilter(f * 1F, android.graphics.BlurMaskFilter.Blur.OUTER);
        sExtraThickInnerBlurMaskFilter = new BlurMaskFilter(f * 6F, android.graphics.BlurMaskFilter.Blur.NORMAL);
        sThickInnerBlurMaskFilter = new BlurMaskFilter(4F * f, android.graphics.BlurMaskFilter.Blur.NORMAL);
        sMediumInnerBlurMaskFilter = new BlurMaskFilter(f * 2F, android.graphics.BlurMaskFilter.Blur.NORMAL);
    }
}