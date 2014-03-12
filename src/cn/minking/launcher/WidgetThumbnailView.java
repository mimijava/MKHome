package cn.minking.launcher;

import cn.minking.launcher.DropTarget.DragObject;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;

public class WidgetThumbnailView extends ThumbnailView
    implements OnLongClickListener, DragSource{
    
    
    private final int mCoordinatesTemp[];
    private DragController mDragController;
    private final int mWidgetCellMeasureHeight;
    private final int mWidgetCellMeasureWidth;
    private Context mContext;
    
    
    public WidgetThumbnailView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public WidgetThumbnailView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        mCoordinatesTemp = new int[2];
        initWidgetThumbnailView();
        setOnLongClickListener(this);
        mContext = context;
        mWidgetCellMeasureWidth = context.getResources().getDimensionPixelSize(R.dimen.workspace_widget_cell_measure_width);
        mWidgetCellMeasureHeight = context.getResources().getDimensionPixelSize(R.dimen.workspace_widget_cell_measure_height);
    }

    private void initWidgetThumbnailView() {
        setScreenGridSize(1, 4);
        setArrowIndicatorMarginRect(new Rect(5, 0, 5, 0));
        setClipToPadding(false);
        setAnimationCacheEnabled(false);
    }

    protected ThumbnailScreen createScreen(Context context, int i, int j, int k, int l) {
        return new ThumbnailScreen(mContext, mRowCountPerScreen, mColumnCountPerScreen, mThumbnailWidth, mThumbnailHeight, false);
    }
    
    @Override
    public void onDropCompleted(View view, DragObject dragobject, boolean flag) {
        mDragController.setTouchTranslator(null);
    }
    @Override
    public boolean onLongClick(View v) {
        // TODO Auto-generated method stub
        return false;
    }
    
    public void setDragController(DragController dragcontroller) {
        mDragController = dragcontroller;
    }
}