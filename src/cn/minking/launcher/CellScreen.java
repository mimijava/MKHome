package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    CellScreen.java
 * 创建时间：    2014-02-28
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: 显示页
 * ====================================================================================
 */
import java.lang.ref.SoftReference;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class CellScreen extends FrameLayout
    implements android.view.View.OnClickListener, DragController.TouchTranslator{
    private Animation.AnimationListener mAnimComplateListener;
    private ImageView mBackground;
    private FrameLayout mBackgroundContainer;
    private Animation mBgCenterEnter;
    private Animation mBgCenterExit;
    private Animation mBgLeftEnter;
    private Animation mBgLeftExit;
    private Animation mBgRightEnter;
    private Animation mBgRightExit;
    private Animation mCellCenterEnter;
    private Animation mCellCenterExit;
    private CellLayout mCellLayout;
    private Animation mCellLeftExit;
    private Animation mCellOthersEnter;
    private Animation mCellRightExit;
    private ImageView mDeleteButton;
    private SoftReference<Object> mEditingPreview;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private boolean mInEditing;
    private boolean mIsEditingNewScreenMode;
    private Paint mMyCachePaint;
    private ImageView mNewButton;
    private float mTranslateXY[];
    private SoftReference<Object> mWorkspacePreview;
    
    public CellScreen(Context context, AttributeSet attributeset){
        super(context, attributeset);
        mInEditing = false;
        mIsEditingNewScreenMode = false;
        mMyCachePaint = null;
        mTranslateXY = new float[2];
        mAnimComplateListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                ViewParent viewparent = getParent();
                if (viewparent instanceof Workspace)
                    if (!mInEditing){
                        ((Workspace)viewparent).onEditModeExitComplete();
                    } else {
                        ((Workspace)viewparent).onEditModeEnterComplete();
                    }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                
            }
        };
        mWorkspacePreview = null;
        mEditingPreview = null;
        if (!Launcher.isHardwareAccelerated()) {
            mMyCachePaint = new Paint();
            mMyCachePaint.setFilterBitmap(true);
        }
    }
    
    private float translateTouchX(float f) {
        return 1.235178F * f - (0.2351779F * (float)getMeasuredWidth()) / 2F;
    }

    private float translateTouchY(float f) {
        return 1.235178F * f - (0.2351779F * (float)getMeasuredHeight()) / 3F;
    }
    
    
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mInEditing) {
            mTranslateXY[0] = ev.getX();
            mTranslateXY[1] = ev.getY();
            translateTouch(mTranslateXY);
            ev.addBatch(ev.getEventTime(), mTranslateXY[0], mTranslateXY[1], 
                    ev.getPressure(), ev.getSize(), ev.getMetaState());
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 描述： 返回屏幕的布局层
     * @return
     */
    public CellLayout getCellLayout(){
        return mCellLayout;
    }

    public Object getTag(int i) {
        Object obj = null;
        if (i != R.id.celllayout_thumbnail_for_workspace_preview) {
            if (i != R.id.celllayout_thumbnail_for_workspace_editing_preview){
                obj = super.getTag(i);
            } else {
                if (mEditingPreview != null){
                    obj = mEditingPreview.get();
                }
            }
        } else {
            if (mWorkspacePreview != null){
                obj = mWorkspacePreview.get();
            }
        }
        return obj;
    }
    
    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        updateVision();
        translatePosition(dirty);
        return super.invalidateChildInParent(location, dirty);
    }

    @Override
    protected void onAttachedToWindow() {
        updateLayerType();
        super.onAttachedToWindow();
    }

    public boolean isEditingNewScreenMode() {
        return mIsEditingNewScreenMode;
    }

    public void onClick(View view) {
        ViewParent viewparent = getParent();
        if (viewparent instanceof Workspace){
            if (view != mDeleteButton) {
                if (view == mNewButton){
                    ((Workspace)viewparent).insertNewScreen(-1);
                }
            } else {
                ((Workspace)viewparent).deleteScreen(mCellLayout.getScreenId());
            }
        }
    }

    public void onDragEnter(DropTarget.DragObject dragobject) {
        mCellLayout.onDragEnter(dragobject);
    }

    public void onDragExit(DropTarget.DragObject dragobject) {
        if (isEditingNewScreenMode())
            mNewButton.setSelected(false);
        mCellLayout.onDragExit(dragobject);
    }

    public void onDragOver(DropTarget.DragObject dragobject) {
        if (!isEditingNewScreenMode()) {
            translateTouch(dragobject);
            mCellLayout.onDragOver(dragobject);
        } else {
            mNewButton.setSelected(true);
        }
    }

    public boolean onDrop(DropTarget.DragObject dragobject, View view) {
        translateTouch(dragobject);
        return mCellLayout.onDrop(dragobject, view);
    }

    public void onEditingAnimationEnterEnd() {
        if (!Launcher.isHardwareAccelerated()) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, mMyCachePaint);
            setDrawingCacheEnabled(true);
            mBackgroundContainer.setDrawingCacheEnabled(false);
            mCellLayout.setDrawingCacheEnabled(false);
        }
    }

    public void onEditingAnimationEnterStart() {
        if (!Launcher.isHardwareAccelerated()) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setDrawingCacheEnabled(false);
            mBackgroundContainer.setDrawingCacheEnabled(true);
            mCellLayout.setDrawingCacheEnabled(true);
            mBackgroundContainer.getDrawingCache(true);
            mCellLayout.getDrawingCache(true);
        } else {
            setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    public void onEditingAnimationExitEnd() {
        mCellLayout.clearAnimation();
        if (Launcher.isHardwareAccelerated()) {
            updateLayerType();
        } else {
            setDrawingCacheEnabled(true);
            mBackgroundContainer.setDrawingCacheEnabled(false);
            mCellLayout.setDrawingCacheEnabled(false);
        }
    }

    public void onEditingAnimationExitStart() {
        if (!Launcher.isHardwareAccelerated()) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setDrawingCacheEnabled(false);
            mBackgroundContainer.setDrawingCacheEnabled(true);
            mCellLayout.setDrawingCacheEnabled(true);
            mBackgroundContainer.getDrawingCache(true);
            mCellLayout.getDrawingCache(true);
        }
    }
    
    @Override
    protected void onFinishInflate() {
        mCellLayout = (CellLayout)findViewById(R.id.cell_layout);
        mBackgroundContainer = (FrameLayout)findViewById(R.id.background_container);
        mBackground = (ImageView)findViewById(R.id.background);
        mDeleteButton = (ImageView)findViewById(R.id.delete_btn);
        mDeleteButton.setOnClickListener(this);
        mNewButton = (ImageView)findViewById(R.id.new_btn);
        mNewButton.setOnClickListener(this);
        mCellCenterEnter = AnimationUtils.loadAnimation(getContext(), R.anim.cell_editing_center_enter);
        mCellCenterExit = AnimationUtils.loadAnimation(getContext(), R.anim.cell_editing_center_exit);
        mCellCenterExit.setAnimationListener(mAnimComplateListener);
        mCellLeftExit = AnimationUtils.loadAnimation(getContext(), R.anim.cell_editing_left_exit);
        mCellRightExit = AnimationUtils.loadAnimation(getContext(), R.anim.cell_editing_right_exit);
        mCellOthersEnter = AnimationUtils.loadAnimation(getContext(), R.anim.cell_editing_others_enter);
        mBgCenterEnter = AnimationUtils.loadAnimation(getContext(), R.anim.cell_bg_editing_center_enter);
        mBgCenterEnter.setAnimationListener(mAnimComplateListener);
        mBgCenterExit = AnimationUtils.loadAnimation(getContext(), R.anim.cell_bg_editing_center_exit);
        mBgLeftEnter = AnimationUtils.loadAnimation(getContext(), R.anim.cell_bg_editing_left_enter);
        mBgLeftExit = AnimationUtils.loadAnimation(getContext(), R.anim.cell_bg_editing_left_exit);
        mBgRightEnter = AnimationUtils.loadAnimation(getContext(), R.anim.cell_bg_editing_right_enter);
        mBgRightExit = AnimationUtils.loadAnimation(getContext(), R.anim.cell_bg_editing_right_exit);
        mFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        mFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        CellLayout celllayout = mCellLayout;
        boolean flag;
        if (!mInEditing || !isEditingNewScreenMode() && mCellLayout.getChildCount() != 0){
            flag = false;
        } else {
            flag = true;
        }
        celllayout.setDisableTouch(flag);
        return false;
    }
    
    public void onQuickEditingModeChanged(boolean flag) {
        if (Launcher.isHardwareAccelerated()){
            if (!flag){
                updateLayerType();
            } else {
                setLayerType(0, null);
            }
        }
    }
    
    public void setEditMode(boolean flag, int i){
        mInEditing = flag;
        mCellLayout.setEditMode(flag);
        updateLayout();
        FrameLayout framelayout = mBackgroundContainer;
        if (!flag){
            framelayout.setVisibility(View.INVISIBLE);
            onEditingAnimationExitStart();
        } else {
            framelayout.setVisibility(View.VISIBLE);
            onEditingAnimationEnterStart();
        }
        switch (i) {
        case -1: 
            if (!flag) {
                mCellLayout.startAnimation(mCellLeftExit);
                mBackgroundContainer.startAnimation(mBgLeftExit);
            } else {
                mCellLayout.startAnimation(mCellOthersEnter);
                mBackgroundContainer.startAnimation(mBgLeftEnter);
            }
            break;

        case 0: // '\0'
            Animation animation;
            if (!flag){
                animation = mCellCenterExit;
            } else {
                animation = mCellCenterEnter;
            }
            mCellLayout.startAnimation(animation);
            if (!flag) {
                animation = mBgCenterExit;
            } else {
                animation = mBgCenterEnter;
            }
            mBackgroundContainer.startAnimation(animation);
            break;

        case 1: // '\001'
            if (!flag) {
                mCellLayout.startAnimation(mCellRightExit);
                mBackgroundContainer.startAnimation(mBgRightExit);
            } else {
                mCellLayout.startAnimation(mCellOthersEnter);
                mBackgroundContainer.startAnimation(mBgRightEnter);
            }
            break;
        default:
            if (!flag) {
                mCellLayout.clearAnimation();
            } else {
                mCellLayout.startAnimation(mCellOthersEnter);
            }
            break;
        }
    }
    
    public void setEditingNewScreenMode() {
        mBackground.setImageResource(R.drawable.editing_new_screen);
        mNewButton.setVisibility(View.VISIBLE);
        mCellLayout.setScreenId(-1L);
        mIsEditingNewScreenMode = true;
    }

    public void setOnLongClickListener(View.OnLongClickListener onlongclicklistener) {
        mCellLayout.setOnLongClickListener(onlongclicklistener);
    }

    @Override
    public void setTag(int i, Object obj) {
        SoftReference<Object> softreference = null;
        if (i != R.id.celllayout_thumbnail_for_workspace_preview)
        {
            if (i != R.id.celllayout_thumbnail_for_workspace_editing_preview) {
                super.setTag(i, obj);
            } else {
                if (obj != null){
                    softreference = new SoftReference<Object>(obj);
                }
                mEditingPreview = softreference;
            }
        } else {
            if (obj != null) {
                softreference = new SoftReference<Object>(obj);
            }
            mWorkspacePreview = softreference;
        }
    }

    public void translatePosition(Rect rect) {
        if (mInEditing) {
            rect.offset((int)(0.1904F * (float)getMeasuredWidth()) >> 1, 
                    (int)(0.1904F * (float)getMeasuredHeight()) >> 2);
        }
    }

    public void translateTouch(DropTarget.DragObject dragobject) {
        if (mInEditing) {
            dragobject.x = (int)translateTouchX(dragobject.x);
            dragobject.y = (int)translateTouchY(dragobject.y);
        }
    }

    public void translateTouch(float af[]) {
        if (mInEditing) {
            af[0] = translateTouchX(af[0]);
            af[1] = translateTouchY(af[1]);
        }
    }

    public void updateLayerType() {
        setLayerType(((Workspace)getParent()).getCellScreenLayerTypeAndUpdateSurface(), null);
    }

    public void updateLayout() {
        if (mInEditing){
            if (isEditingNewScreenMode() || mCellLayout.getChildCount() != 0) {
                if (mDeleteButton.getVisibility() == View.VISIBLE) {
                    mDeleteButton.setVisibility(View.INVISIBLE);
                    mDeleteButton.startAnimation(mFadeOut);
                }
            } else {
                mDeleteButton.setVisibility(View.VISIBLE);
                mDeleteButton.startAnimation(mFadeIn);
            }
        }
        mCellLayout.clearCellBackground();
    }

    public void updateVision() {
        setTag(R.id.celllayout_thumbnail_for_workspace_preview_dirty, Boolean.valueOf(true));
        setTag(R.id.celllayout_thumbnail_for_workspace_editing_preview_dirty, Boolean.valueOf(true));
    }
}