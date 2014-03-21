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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class ItemIcon extends FrameLayout
    implements ApplicationsMessage.IconMessage{
    
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
    private Context mContext;
    
    public ItemIcon(Context context, AttributeSet attributeset){
        super(context, attributeset);
        int type = View.LAYER_TYPE_NONE;
        mContext = context;
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
            mTitle.setVisibility(View.VISIBLE);
        }else {
            mTitle.setVisibility(View.GONE);
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
    
    private Drawable getRemoteResourceDrawable(String packageName){
        Drawable drawable = null;
        if (packageName == null) {
            return null;
        }
        try {
            Resources resources = mContext.getPackageManager().getResourcesForApplication(getResourcePackage(packageName));
            drawable = resources.getDrawable(resources.getIdentifier(packageName, null, null));
        } catch (NameNotFoundException e) {
            return null;
        }
        
        return drawable;
    }
    
    private void setMessageIconTile(byte icon[]){
        if (mMessageIconTile != icon){
            if (icon == null){
                mIconTile.setImageBitmap(null);
                mIconTile.setVisibility(View.INVISIBLE);
            } else {
                Bitmap bitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
                mIconTile.setImageBitmap(bitmap);
                mIconTile.setVisibility(View.VISIBLE);
            }
        }
    }
    
    @SuppressWarnings("deprecation")
    private void setMessageTextBackground(String text){
        Drawable drawable = getRemoteResourceDrawable(text);
        if (drawable == null){
            mMessage.setBackgroundResource(R.drawable.icon_notification_bg);
        } else {
            mMessage.setBackgroundDrawable(drawable);
        }
    }
    
    public byte[] getMessageIconTile(){
        return mMessageIconTile;
    }
    
    public String getMessageText(){
        String message;
        if (isEmptyMessage()) {
            message = null;
        } else {
            message = mMessage.getText().toString();
        }
        return message;
    }

    public String getMessageTextBackground(){
        return mMessageBackground;
    }
    
    public boolean isCompact(){
        return mIsCompact;
    }
    
    public boolean isEmptyMessage(){
        boolean flag;
        if (mMessage.getText().length() != 0 
                || mMessageBackground != null) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    private String getResourcePackage(String packageName){
        return packageName.substring(0, packageName.indexOf(':'));
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mIsCompact && PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_key_icon_shadow", true)){
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            Drawable drawable = mIcon.getDrawable();
            if ((drawable instanceof BitmapDrawable) 
                    && mShadow.getVisibility() == View.GONE 
                    && width > 0 
                    && height > 0) {
                Bitmap bitmap = createShadowBackground(width, height, ((BitmapDrawable)drawable).getBitmap());
                if (drawable != null) {
                    mShadow.setImageBitmap(bitmap);
                    mShadow.setVisibility(View.VISIBLE);
                }
            }
        } else {
            mShadow.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = mIconContainer.getMeasuredWidth();
        setupIconMargin(mIconContainer);
        int radioWidth = (int)((float)width / 1.5F);
        FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams)mMessage.getLayoutParams();
        if (mMessage.getMeasuredWidth() > radioWidth) {
            layoutparams.width = radioWidth;
        }
        if (!isCompact()) {
            layoutparams.setMargins(0, 0, Math.max(0, (getMeasuredWidth() - width - mMessageWidth) / 2), 0);
        } else {
            layoutparams.setMargins(0, Math.max(0, (getMeasuredHeight() - width - mMessageHeight) / 2),
                    Math.max(0, (getMeasuredWidth() - width - mMessageWidth) / 2), 0);
        }
        mMessage.setLayoutParams(layoutparams);
    }
    
    public void setEnableTranslateAnimation(boolean flag){
        mIsEnableTranslateAnimation = flag;
    }
    
    public void setIcon(Bitmap bitmap){
        setIcon(((Drawable)(new BitmapDrawable(mContext.getResources(), bitmap))));
    }
    
    public void setIcon(Drawable drawable){
        mIcon.setImageDrawable(drawable);
    }
    
    public void setMessage(String message){
        setMessage(message, null, null);
    }
    
    public void setMessage(String text, String bgString, byte icon[]){
        if (!mMessage.getText().equals(bgString)){
            if (!TextUtils.isEmpty(text)){
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(text);
                setMessageTextBackground(bgString);
                mMessageBackground = bgString;
            } else {
                mMessage.setVisibility(View.INVISIBLE);
                mMessage.setText(null);
                setMessageTextBackground(null);
                mMessageBackground = null;
            }
        }
        setMessageIconTile(icon);
        mMessageIconTile = icon;
    }
    
    public void setTitle(CharSequence charsequence){
        mTitle.setText(charsequence);
        setContentDescription(charsequence);
    }
    
    protected void setupIconMargin(View view){
        FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams)view.getLayoutParams();
        if (!isCompact()) {
            layoutparams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            layoutparams.setMargins(mIconSideMargin, mMessageHeight / 2, mIconSideMargin, 0);
        } else {
            layoutparams.gravity = Gravity.CENTER;
            layoutparams.setMargins(0, 0, 0, 0);
        }
        view.setLayoutParams(layoutparams);
    }
}