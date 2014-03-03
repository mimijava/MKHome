package cn.minking.launcher;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.text.TextPaint;
import android.util.DisplayMetrics;

public final class Utilities{
    private static final String TAG = "MKHome.Utilities";
    
    private static final Paint sBlurPaint = new Paint();
    private static final Canvas sCanvas = new Canvas();
    static int sColorIndex = 0;
    static int sColors[] = { 0xffff0000, 0xff00ff00, 0xff0000ff };;
    static float sDensity;
    static int sDensityDpi;
    private static final Paint sDisabledPaint = new Paint();
    private static final Paint sGlowColorFocusedPaint = new Paint();
    private static final Paint sGlowColorPressedPaint = new Paint();
    private static final Rect sOldBounds = new Rect();
    private static Resources sSystemResource;
    
    static class BubbleText{
        private final int mBitmapHeight;
        private final int mBitmapWidth;
        private final RectF mBubbleRect = new RectF();
        private final int mDensity;
        private final int mFirstLineY;
        private final int mLeading = (int)(0.5F + 0F);
        private final int mLineHeight;
        private final TextPaint mTextPaint;
        private final float mTextWidth;

        BubbleText(Context context){
            Resources resources = context.getResources();
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            float desity = displayMetrics.density;
            mDensity = displayMetrics.densityDpi;
            
            float f2 = 2F * desity;
            float f1 = 2F * desity;
            float ttWidth = resources.getDimension(R.dimen.title_texture_width);
            
            RectF rectf = mBubbleRect;
            rectf.left = 0F;
            rectf.top = 0F;
            rectf.right = (int)ttWidth;
            mTextWidth = ttWidth - f2 - f1;
            mTextPaint = new TextPaint();
            mTextPaint.setTypeface(Typeface.DEFAULT);
            mTextPaint.setTextSize(13F * desity);
            mTextPaint.setColor(-1);
            mTextPaint.setAntiAlias(true);
            float ascent = -mTextPaint.ascent();
            float decent = mTextPaint.descent();
            mFirstLineY = (int)(0.5F + (0F + ascent));
            mLineHeight = (int)(0.5F + (decent + (0F + ascent)));
            mBitmapWidth = (int)(0.5F + mBubbleRect.width());
            mBitmapHeight = Utilities.roundToPow2((int)(0.5F + (0F + (float)(2 * mLineHeight))));
            mBubbleRect.offsetTo(((float)mBitmapWidth - mBubbleRect.width()) / 2F, 0F);
        }
    }
    
    static int roundToPow2(int n) {
        int orig = n;
        n >>= 1;
        int mask = 0x8000000;
        while (mask != 0 && (n & mask) == 0) {
            mask >>= 1;
        }
        while (mask != 0) {
            n |= mask;
            mask >>= 1;
        }
        n += 1;
        if (n != orig) {
            n <<= 1;
        }
        return n;
    }
    
    static int generateRandomId() {
        return new Random(System.currentTimeMillis()).nextInt(1 << 24);
    }
    
    static{
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
        sSystemResource = Resources.getSystem();
        sDensityDpi = sSystemResource.getDisplayMetrics().densityDpi;
        sDensity = sSystemResource.getDisplayMetrics().density;
    }
    
    private static void initStatics(Context context){
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        final float density = metrics.density;
        
        sBlurPaint.setMaskFilter(new BlurMaskFilter(5F * density, BlurMaskFilter.Blur.NORMAL));
        sGlowColorPressedPaint.setColor(0xffffc300);
        //sGlowColorPressedPaint.setMaskFilter(TableMaskFilter.CreateClipTable(0, 30));
        sGlowColorFocusedPaint.setColor(0xffff8e00);
        //sGlowColorFocusedPaint.setMaskFilter(TableMaskFilter.CreateClipTable(0, 30));
        
        ColorMatrix colormatrix = new ColorMatrix();
        colormatrix.setSaturation(0.2F);
        sDisabledPaint.setColorFilter(new ColorMatrixColorFilter(colormatrix));
        sDisabledPaint.setAlpha(0x88);
    }
    
    public static int getDipPixelSize(int dip){
        return (int)(0.5F + (float)dip * sDensity);
    }
    
    static Bitmap createIconBitmap(Drawable drawable, Context context) {
            if (!(drawable instanceof BitmapDrawable) || ResConfig.getIconWidth() != drawable.getIntrinsicWidth() 
                    || ResConfig.getIconHeight() != drawable.getIntrinsicHeight()){
                synchronized (sCanvas) { // we share the statics :-(
                    if (ResConfig.getIconWidth() == -1) {
                        initStatics(context);
                    }
                    
                    int width = ResConfig.getIconWidth();
                    int height = ResConfig.getIconHeight();
                    if ((drawable instanceof PaintDrawable)){
                        PaintDrawable paintdrawable = (PaintDrawable)drawable;
                        paintdrawable.setIntrinsicWidth(width);
                        paintdrawable.setIntrinsicHeight(height);
                    } else if (drawable instanceof BitmapDrawable) {
                        // Ensure the bitmap has a density.
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                        Bitmap bitmap = bitmapDrawable.getBitmap();
                        if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                            bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                        }
                    }
                    
                    int sourceWidth = drawable.getIntrinsicWidth();
                    int sourceHeight = drawable.getIntrinsicHeight();
                    if (sourceWidth > 0 && sourceHeight > 0) {
                        // There are intrinsic sizes.
                        if (width < sourceWidth || height < sourceHeight) {
                            // It's too big, scale it down.
                            final float ratio = (float) sourceWidth / sourceHeight;
                            if (sourceWidth > sourceHeight) {
                                height = (int) (width / ratio);
                            } else if (sourceHeight > sourceWidth) {
                                width = (int) (height * ratio);
                            }
                        } else if (sourceWidth < width && sourceHeight < height) {
                            // Don't scale up the icon
                            width = sourceWidth;
                            height = sourceHeight;
                        }
                    }
                    
                    // no intrinsic size --> use default size
                    int textureWidth = ResConfig.getIconWidth();
                    int textureHeight = ResConfig.getIconHeight();
                    
                    final Bitmap bitmap = Bitmap.createBitmap(ResConfig.getIconWidth(), ResConfig.getIconHeight(),
                            Bitmap.Config.ARGB_8888);
                    final Canvas canvas = sCanvas;
                    canvas.setBitmap(bitmap);
                    
                    final int left = (textureWidth-width) / 2;
                    final int top = (textureHeight-height) / 2;
                    
                    @SuppressWarnings("all") // suppress dead code warning
                    final boolean debug = false;
                    if (debug) {
                        // draw a big box for the icon for debugging
                        canvas.drawColor(sColors[sColorIndex]);
                        if (++sColorIndex >= sColors.length) sColorIndex = 0;
                        Paint debugPaint = new Paint();
                        debugPaint.setColor(0xffcccc00);
                        canvas.drawRect(left, top, left+width, top+height, debugPaint);
                    }

                    sOldBounds.set(drawable.getBounds());
                    drawable.setBounds(left, top, left+width, top+height);
                    drawable.setFilterBitmap(true);
                    drawable.draw(canvas);
                    drawable.setBounds(sOldBounds);
                    canvas.setBitmap(null);
                    return bitmap;
                }
            }else {
                return ((BitmapDrawable)drawable).getBitmap();
            }
    }
    
    static Bitmap resampleIconBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (ResConfig.getIconWidth() == -1) {
                initStatics(context);
            }

            if (bitmap.getWidth() == ResConfig.getIconWidth() 
                    && bitmap.getHeight() == ResConfig.getIconHeight()) {
                return bitmap;
            } else {
                final Resources resources = context.getResources();
                return createIconBitmap(new BitmapDrawable(resources, bitmap), context);
            }
        }
    }
}