package cn.minking.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ThumbnailViewAdapter extends BaseAdapter{
    private static final Drawable DEFAULT_THUMBNAIL_BACKGROUND = new ColorDrawable(R.color.thumb_color);
    protected Context mContext;
    private ViewGroup mSourceGroup;

    public ThumbnailViewAdapter(Context context) {
        mContext = context;
    }

    public int getCount() {
        return mSourceGroup.getChildCount();
    }

    public android.view.animation.Animation.AnimationListener getEnterAnimationListener() {
        return null;
    }

    public android.view.animation.Animation.AnimationListener getExitAnimationListener() {
        return null;
    }

    public int getFocusedItemPosition() {
        return 0;
    }

    public View getItem(int i) {
        return mSourceGroup.getChildAt(i);
    }

    public long getItemId(int i) {
        View view = getItem(i);
        long l;
        if (view != null){
            l = view.getId();
        } else {
            l = -1L;
        }
        return l;
    }

    @SuppressWarnings("deprecation")
    public View getView(int i, View view, ViewGroup viewgroup) {
        View v = getItem(i);
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), 
                android.graphics.Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bitmap));
        ImageView imageView = new ImageView(mContext);
        imageView.setImageBitmap(bitmap);
        imageView.setBackgroundDrawable(DEFAULT_THUMBNAIL_BACKGROUND);
        imageView.setTag(Integer.valueOf(i));
        return ((View)imageView);
    }

    public void onEndDragging() {
    }

    public void onStartDragging(int i) {
    }

    public void onThumbnailClick(int i) {
    }

    public void onThumbnailPositionChanged(int ai[]){
    }
}