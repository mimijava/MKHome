package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ItemIcon.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 图标文件创建
 * ====================================================================================
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class ItemIcon extends FrameLayout{
    
    public static float DEFAULT_PRESET_APP_ALPHA = 0.5F;
    protected ImageView mIcon;
    protected FrameLayout mIconContainer;
    protected int mIconSideMargin;
    protected ImageView mIconTile;
    private boolean mIsCompact;
    private boolean mIsEnableTranslateAnimation;
    private TextView mMessage;
    private String mMessageBackground;
    private int mMessageHeight;
    private byte mMessageIconTile[];
    private int mMessageWidth;
    private ImageView mShadow;
    protected TextView mTitle;
    
    public ItemIcon(Context context, AttributeSet attributeset){
        super(context, attributeset);
        int type = View.LAYER_TYPE_NONE;
        mIsCompact = false;
        mIsEnableTranslateAnimation = false;
        Resources resources = getResources();
        if (resources.getBoolean(R.bool.config_use_software_icon)){
            type = View.LAYER_TYPE_SOFTWARE;
        }
        setLayerType(type, null);
        mIconSideMargin = resources.getDimensionPixelSize(R.dimen.icon_side_margin);
        mMessageWidth = resources.getDimensionPixelSize(R.dimen.icon_message_width);
        mMessageHeight = resources.getDimensionPixelSize(R.dimen.icon_message_height);
    }
    
    public void setCompactViewMode(boolean flag){
        mIsCompact = flag;
        if (flag) {
            mTitle.setVisibility(View.GONE);
        }else {
            mTitle.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onFinishInflate() {
        mIconContainer = (FrameLayout)findViewById(R.id.icon_container);
        mIconTile = (ImageView)findViewById(R.id.icon_tile);
        mIcon = (ImageView)findViewById(R.id.icon_icon);
        mMessage = (TextView)findViewById(R.id.icon_msg);
        mTitle = (TextView)findViewById(R.id.icon_title);
        mShadow = (ImageView)findViewById(R.id.icon_shadow_layer);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (!mIsCompact 
                && PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean("pref_key_icon_shadow", true)){
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            Drawable drawable = mIcon.getDrawable();
            if ((drawable instanceof BitmapDrawable) 
                    && mShadow.getVisibility() == 8 && width > 0 && height > 0){
                Bitmap bitmap = createShadowBackground(width, height, ((BitmapDrawable)drawable).getBitmap());
                if (bitmap != null){
                    mShadow.setImageBitmap(bitmap);
                    mShadow.setVisibility(View.VISIBLE);
                }
            }
        } else {
            mShadow.setVisibility(View.GONE);
        }
    }
    
    private Bitmap createShadowBackground(int width, int height, Bitmap bitmap){
        Bitmap bitmap1 = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        if (bitmap1 != null){
            Canvas canvas = new Canvas(bitmap1);
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            float radius = getContext().getResources().getDimension(R.dimen.icon_shadow_size);
            paint.setMaskFilter(new BlurMaskFilter(radius, android.graphics.BlurMaskFilter.Blur.INNER));
            Bitmap bitmap2 = bitmap.extractAlpha(paint, null);
            paint = new Paint();
            paint.setColor(0);
            paint.setShadowLayer(radius, 1F, radius, getContext().getResources().getColor(R.color.icon_shadow));
            canvas.drawBitmap(bitmap2, mIconContainer.getLeft(), mIconContainer.getTop(), paint);
            bitmap2.recycle();
        }
        return bitmap1;
    }
    

}