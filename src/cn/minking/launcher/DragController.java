package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    DragController.java
 * 创建时间：    2014
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140303: 添加两个基本操作
 * ====================================================================================
 */
import java.util.ArrayList;
import java.util.Iterator;

import android.R.integer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public class DragController {
    /**
     * 描述： 屏幕滚动处理线程
     * @author minking
     *
     */
    private class ScrollRunnable
        implements Runnable {
        private int mDirection;
    
        public void run() {
            if (mDragScroller != null && mDragging) {
                if (mDirection == DIRECTION_LEFT) {
                    mDragScroller.scrollDragingLeft();
                } else {
                    mDragScroller.scrollDragingRight();
                }
                mDistanceSinceScroll = 0;
                mHandler.postDelayed(mScrollRunnable, 800L);
            }
        }
    
        void setDirection(int i) {
            mDirection = i;
        }
    
        ScrollRunnable() {
        }
    }
    
    static interface TouchTranslator {
        public abstract void translatePosition(Rect rect);
        public abstract void translateTouch(float af[]);
    }
    
    static interface DragListener {
        public abstract void onDragEnd();
        public abstract void onDragStart(DragSource dragsource, ItemInfo iteminfo, int dragAction);
    }

    /********** 常量  **********/
    // 滑动方向
    final public static int DIRECTION_LEFT = 0;
    final public static int DIRECTION_RIGHT = 1;
    
    final public static int DRAG_ACTION_COPY = 1;
    final public static int DRAG_ACTION_MOVE = 0;
    final private static long VIBRATE_DURATION = 35L;
    
    /// M: 存储拖动目标
    private ArrayList<DropTarget> mDropTargets;
    private RectF mDeleteRegion;
    private TouchTranslator mTouchTranslater;
    
    private Context mContext;
    private final int mCoordinatesTemp[] = new int[2];
    private DisplayMetrics mDisplayMetrics;
    private int mDistanceSinceScroll;
    private DropTarget.DragObject mDragObject;
    private DragScroller mDragScroller;
    private int mDragViewAlpha;
    private boolean mDragging;
    private Handler mHandler;
    private InputMethodManager mInputMethodManager;
    private DropTarget mLastDropTarget;
    private int mLastTouch[];
    private Launcher mLauncher;
    private ArrayList<DragListener> mListeners;
    private int mMotionDownX;
    private int mMotionDownY;
    private View mMoveTarget;
    private final HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
    private Rect mRectTemp;
    private ScrollRunnable mScrollRunnable;
    private int mScrollState;
    private View mScrollView;
    private int mScrollZone;
    private int mSecondaryPointerId;
    private Rect mTempRect;
    private Canvas mTmpCanvas;
    private float mTransloateXY[];
    private final Vibrator mVibrator;
    private IBinder mWindowToken;
    
    public DragController(Context context){
        mRectTemp = new Rect();
        mDisplayMetrics = new DisplayMetrics();
        mDropTargets = new ArrayList<DropTarget>();
        mListeners = new ArrayList<DragListener>();
        mTransloateXY = new float[2];
        mTempRect = new Rect();
        mScrollState = 0;
        mSecondaryPointerId = -1;
        mScrollRunnable = new ScrollRunnable();
        mDistanceSinceScroll = 0;
        mLastTouch = new int[2];
        mTmpCanvas = new Canvas();
        mContext = context;
        mLauncher = (Launcher)context;
        mHandler = new Handler();
        mVibrator = (Vibrator)context.getSystemService("vibrator");
        mScrollZone = context.getResources().getDimensionPixelSize(R.dimen.scroll_zone);
        mDragViewAlpha = context.getResources().getInteger(R.integer.config_dragViewAlpha);
        recordScreenSize();
    }
    
    /**
     * 功能： 创建拖动视图的描边框
     * @param bitmap
     * @param i
     * @return
     */
    private Bitmap createDragOutline(Bitmap bitmap, int i) {
        int color = mLauncher.getResources().getColor(R.color.dragging_outline);
        Bitmap viewRect = Bitmap.createBitmap(bitmap);
        Canvas canvas = mTmpCanvas;
        canvas.setBitmap(viewRect);
        mOutlineHelper.applyMediumExpensiveOutlineWithBlur(viewRect, canvas, color, color);
        canvas.setBitmap(null);
        return viewRect;
    }

    private Bitmap createViewBitmap(View view) {
        view.clearFocus();
        view.setPressed(false);
        boolean flag = view.willNotCacheDrawing();
        view.setWillNotCacheDrawing(false);
        int i = view.getDrawingCacheBackgroundColor();
        if (i != 0) {
            view.setDrawingCacheBackgroundColor(0);
        }
        
        boolean flag1;
        if (!(view instanceof ItemIcon)) {
            flag1 = false;
        } else {
            if (((ItemIcon)view).isCompact()) {
                flag1 = false;
            } else {
                flag1 = true;
            }
        }
        if (flag1) {
            ((ItemIcon)view).setCompactViewMode(true);
        }
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap);
            if (flag1) {
                ((ItemIcon)view).setCompactViewMode(false);
            }
            if (flag) {
                view.destroyDrawingCache();
                view.setWillNotCacheDrawing(flag);
            }
            if (i != 0) {
                view.setDrawingCacheBackgroundColor(i);
            }
        } else {
            Log.e("Launcher.DragController", (new StringBuilder()).
                    append("failed getViewBitmap(").
                    append(view).append(")").toString(), new RuntimeException());
            bitmap = null;
        }
        return bitmap;
    }

    private void drop(float f, float f1) {
        boolean flag1 = true;
        int ai[] = mCoordinatesTemp;
        Object obj = findDropTarget((int)f, (int)f1, ai);
        mDragObject.x = ai[0];
        mDragObject.y = ai[1];
        boolean flag = false;
        if (obj != null) {
            mDragObject.dragComplete = flag1;
            ((DropTarget) (obj)).onDragExit(mDragObject);
            if (((DropTarget) (obj)).acceptDrop(mDragObject)) {
                flag = ((DropTarget) (obj)).onDrop(mDragObject);
            }
        }
        DropTarget.DragObject dragobject = mDragObject;
        if (flag) {
            flag1 = false;
        }
        dragobject.cancelled = flag1;
        mDragObject.dragSource.onDropCompleted((View)obj, mDragObject, flag);
    }

    private void endDrag() {
        if (mDragging){ 
            mDragging = false;
            for (DragListener listener : mListeners) {
                listener.onDragEnd();
            }
            if (mDragObject.dragView != null) {
                mDragObject.dragView.remove();
                mDragObject.dragView = null;
            }
            mDragObject = null;
        }
        
        if (9 == mLauncher.getEditingState()){
            mLauncher.setEditingState(7);
        }
        return;
    }

    private DropTarget findDropTarget(int i, int j, int ai[]) {
        DropTarget droptarget = null;
        Rect rect = mRectTemp;
        ArrayList<DropTarget> arraylist = mDropTargets;
        for (int k = 0; k < arraylist.size() - 1; k++) {
            droptarget = (DropTarget)arraylist.get(k);
            if (droptarget.isDropEnabled() && ((View)droptarget).isShown()) {
                droptarget.getHitRect(rect);
                droptarget.getLocationOnScreen(ai);
                rect.offset(ai[0] - droptarget.getLeft(), ai[1] - droptarget.getTop());
                if (rect.contains(i, j)) {
                    break;
                }
            }   
        }
        ai[0] = i - ai[0];
        ai[1] = j - ai[1];
        return droptarget;
    }

    private void handleMoveEvent(int i, int j, MotionEvent motionevent) {
        mDragObject.dragView.move(i, j);
        int ai[] = mCoordinatesTemp;
        DropTarget droptarget = findDropTarget(i, j, ai);
        mDragObject.x = ai[0];
        mDragObject.y = ai[1];
        if (droptarget == null) {
            if (mLastDropTarget != null){
                mLastDropTarget.onDragExit(mDragObject);
            }
        } else {
            DropTarget droptarget1 = droptarget.getDropTargetDelegate(mDragObject);
            if (droptarget1 != null) {
                droptarget = droptarget1;
            }
            if (mLastDropTarget != droptarget)  {
                if (mLastDropTarget != null) {
                    mLastDropTarget.onDragExit(mDragObject);
                }
                droptarget.onDragEnter(mDragObject);
            }
            droptarget.onDragOver(mDragObject);
        }
        mLastDropTarget = droptarget;
        boolean flag = false;
        if (mDeleteRegion != null) {
            flag = mDeleteRegion.contains(i, j);
        }
        int k = ViewConfiguration.get(mLauncher).getScaledWindowTouchSlop();
        mDistanceSinceScroll = (int)((double)mDistanceSinceScroll + Math.sqrt(Math.pow(mLastTouch[0] - i, 2D) + Math.pow(mLastTouch[1] - j, 2D)));
        mLastTouch[0] = i;
        mLastTouch[1] = j;
        if (flag || i >= mScrollZone) {
            if (flag || i <= mScrollView.getWidth() - mScrollZone) {
                if (mScrollState != 1) {
                    if (motionevent != null && mSecondaryPointerId > 0) {
                        if (motionevent.findPointerIndex(mSecondaryPointerId) <= 0) {
                            mSecondaryPointerId = -1;
                        }
                        else {
                            if (Math.abs((float)i - motionevent.getX(motionevent.findPointerIndex(mSecondaryPointerId))) > 1F) {
                                cancelScroll();
                                mDragScroller.onSecondaryPointerMove(motionevent, mSecondaryPointerId);
                            }
                        }
                    }
                } else {
                    mScrollState = 0;
                    cancelScroll();
                    mDragScroller.onExitScrollArea();
                }
            } else {
                if (mScrollState == 0 && mDistanceSinceScroll > k) {
                    mScrollState = 1;
                    if (mDragScroller.onEnterScrollArea(i, j, 1)) {
                        mScrollRunnable.setDirection(DIRECTION_RIGHT);
                        cancelScroll();
                        mHandler.postDelayed(mScrollRunnable, 1000L);
                    }
                }
            }
        } else {
            if (mScrollState == 0 && mDistanceSinceScroll > k) {
                mScrollState = 1;
                if (mDragScroller.onEnterScrollArea(i, j, 0)) {
                    mScrollRunnable.setDirection(DIRECTION_LEFT);
                    cancelScroll();
                    mHandler.postDelayed(mScrollRunnable, 1000L);
                }
            }
        }
    }

    private void recordScreenSize() {
        ((WindowManager)mContext.getSystemService("window")).getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    private Bitmap renderDrawableToBitmap(Drawable drawable) {
        Rect rect = drawable.copyBounds();
        Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), android.graphics.Bitmap.Config.ARGB_8888);
        if (bitmap != null) {
            Canvas canvas = mTmpCanvas;
            canvas.setBitmap(bitmap);
            drawable.draw(canvas);
            canvas.setBitmap(null);
        } else {
            bitmap = null;
        }
        return bitmap;
    }

    public void addDragListener(DragListener draglistener) {
        mListeners.add(draglistener);
    }

    public void cancelDrag(){
        cancelScroll();
        if (mDragging) {
            if (mLastDropTarget != null) {
                mLastDropTarget.onDragExit(mDragObject);
            }
            mDragObject.cancelled = true;
            mDragObject.dragComplete = true;
            mDragObject.dragSource.onDropCompleted(null, mDragObject, false);
        }
        endDrag();
    }
    
    public void cancelScroll() {
        mHandler.removeCallbacks(mScrollRunnable);
    }

    public boolean dispatchKeyEvent(KeyEvent keyevent) {
        return mDragging;
    }

    public boolean dispatchUnhandledMove(View view, int i) {
        boolean flag;
        if (mMoveTarget == null || !mMoveTarget.dispatchUnhandledMove(view, i)){
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }


    private static int clamp(int rawX, int base, int metrics) {
        if (rawX >= base){
            if (rawX < metrics) {
                base = rawX;
            } else {
                base = metrics - 1;
            }
        }
        return base;
    }
    
    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        int x = clamp((int)motionevent.getRawX(), 0, mDisplayMetrics.widthPixels);
        int y = clamp((int)motionevent.getRawY(), 0, mDisplayMetrics.heightPixels);
        switch (motionevent.getAction()) {
        case MotionEvent.ACTION_MOVE: // '\002'
        default:
            break;

        case MotionEvent.ACTION_DOWN: // '\0'
            mMotionDownX = x;
            mMotionDownY = y;
            mLastDropTarget = null;
            break;

        case MotionEvent.ACTION_UP: // '\001'
            if (mDragging){
                drop(x, y);
            }
            endDrag();
            break;

        case MotionEvent.ACTION_CANCEL: // '\003'
            cancelDrag();
            break;
        }
        return mDragging;
    }

    /**
     * 功能： 处理触摸事件
     * @param motionevent
     * @return
     */
    public boolean onTouchEvent(MotionEvent motionevent) {
        boolean flag = false;
        View view = mScrollView;
        if (mDragging) {
            int x = clamp((int)motionevent.getRawX(), 0, mDisplayMetrics.widthPixels);
            int y = clamp((int)motionevent.getRawY(), 0, mDisplayMetrics.heightPixels);
            switch (motionevent.getAction() & 0xff) {
            case MotionEvent.ACTION_OUTSIDE: // '\004'
            default:
                break;

            case MotionEvent.ACTION_DOWN: // '\0'
                mMotionDownX = ((flag) ? 1 : 0);
                mMotionDownY = y;
                if (x >= mScrollZone && x <= view.getWidth() - mScrollZone) {
                    mScrollState = 0;
                } else {
                    mScrollState = 1;
                    cancelScroll();
                    mHandler.postDelayed(mScrollRunnable, 1000L);
                }
                break;

            case MotionEvent.ACTION_UP: // '\001'
                handleMoveEvent(x, y, motionevent);
                cancelScroll();
                if (mDragging){
                    drop(x, y);
                }
                endDrag();
                break;

            case MotionEvent.ACTION_MOVE: // '\002'
                handleMoveEvent(x, y, motionevent);
                break;

            case MotionEvent.ACTION_CANCEL: // '\003'
                cancelDrag();
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // '\005'
                mSecondaryPointerId = motionevent.getPointerId((0xff00 & motionevent.getAction()) >> 8);
                mDragScroller.onSecondaryPointerDown(motionevent, mSecondaryPointerId);
                break;

            case MotionEvent.ACTION_POINTER_UP: // '\006'
                if (mSecondaryPointerId >= 0) {
                    mDragScroller.onSecondaryPointerUp(motionevent, mSecondaryPointerId);
                    mSecondaryPointerId = -1;
                }
                break;
            }
            flag = true;
        }
        return flag;
    }

    public void setDragScoller(DragScroller dragscroller) {
        mDragScroller = dragscroller;
    }

    void setMoveTarget(View view) {
        mMoveTarget = view;
    }

    public void setScrollView(View view) {
        mScrollView = view;
    }

    public void setWindowToken(IBinder ibinder) {
        mWindowToken = ibinder;
    }

    void setDeleteRegion(RectF rectf){
        mDeleteRegion = rectf;
    }
    
    /**
     * 功能： 添加拖动目标
     * @param droptarget
     */
    public void addDropTarget(DropTarget droptarget){
        mDropTargets.add(droptarget);
    }
    
    /**
     * 功能： 删除拖动目标
     */
    public void removeDropTarget(DropTarget droptarget){
        mDropTargets.remove(droptarget);
    }
    
    public void setTouchTranslator(TouchTranslator touchtranslator) {
        mTouchTranslater = touchtranslator;
    }
    
    /**
     * 功能： 开始拖动 
     */
    public void startDrag(Bitmap viewBitmap, Bitmap outlineBitmap, 
            int cx, int cy, DragSource dragsource, ItemInfo itemInfo, int dragAction, 
            Point dragOffset, Rect dragRegion){
        if (!mLauncher.isInEditing()){
            mLauncher.setEditingState(9);
        }
        
        // 如果有输入框，则关闭
        if (mInputMethodManager == null) {
            mInputMethodManager = (InputMethodManager)mLauncher.getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        mInputMethodManager.hideSoftInputFromWindow(mWindowToken, 0);
        
        for (DragListener listener : mListeners) {
            listener.onDragStart(dragsource, itemInfo, dragAction);
        }
        
        float motionDownX = mMotionDownX;
        float motionDownY = mMotionDownY;
        if (mTouchTranslater != null) {
            mTransloateXY[0] = mMotionDownX;
            mTransloateXY[1] = mMotionDownY;
            mTouchTranslater.translateTouch(mTransloateXY);
            motionDownX = mTransloateXY[0];
            motionDownY = mTransloateXY[1];
        }
        
        int registrationX = (int)motionDownX - cx;
        int registrationY = (int)motionDownY - cy;
        
        final int dragRegionLeft = dragRegion == null ? 0 : dragRegion.left;
        final int dragRegionTop = dragRegion == null ? 0 : dragRegion.top;
        
        mDragging = true;
        
        mDragObject = new DropTarget.DragObject();
        mDragObject.dragComplete = false;
        mDragObject.xOffset = (int)(motionDownX - (float)(cx + dragRegionLeft));
        mDragObject.yOffset = (int)(motionDownY - (float)(cy + dragRegionTop));
        mDragObject.dragSource = dragsource;
        mDragObject.dragInfo = itemInfo;
        mDragObject.outline = outlineBitmap;
        
        mVibrator.vibrate(VIBRATE_DURATION);
        
        DropTarget.DragObject dragobject = mDragObject;
        DragView dragView = new DragView(mLauncher, viewBitmap, registrationX, 
                registrationY, 0, 0, viewBitmap.getWidth(), viewBitmap.getHeight());
        dragobject.dragView = dragView;
        dragView.setAlpha((float)mDragViewAlpha / 255F);
        if (dragOffset != null) {
            dragView.setDragVisualizeOffset(new Point(dragOffset));
        }
        if (dragRegion != null) {
            dragView.setDragRegion(new Rect(dragRegion));
        }
        dragView.show(mMotionDownX, mMotionDownY);
        handleMoveEvent(mMotionDownX, mMotionDownY, null);
    }
    
    public void startDrag(Drawable drawable, int i, int j,
            DragSource dragsource, ItemInfo iteminfo, int k, Rect rect){
        Bitmap bitmap = renderDrawableToBitmap(drawable);
        if (bitmap != null) {
            Bitmap outlineBitmap = createDragOutline(bitmap, HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS);
            if (outlineBitmap != null){
                startDrag(bitmap, outlineBitmap, i, j, dragsource, iteminfo, k, null, rect);
            }
        }
    }
    
    public void startDrag(View view, DragSource dragsource, ItemInfo iteminfo, int action){
        startDrag(view, true, dragsource, iteminfo, action, null);
    }
    
    public void startDrag(View view, boolean bOutline, DragSource dragsource, ItemInfo iteminfo, int action, Rect rect){
        if (!mDragging) {
            // 获取对象的位图
            Bitmap viewBitmap = createViewBitmap(view);
            if (viewBitmap != null) {
                // 是否需要显示描边框
                Bitmap outlineBitmap = null;
                if (bOutline) {
                    outlineBitmap = createDragOutline(viewBitmap, HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS);
                }
                if (!bOutline || outlineBitmap != null) {
                    int ai[] = mCoordinatesTemp;
                    mLauncher.getDragLayer().getLocationInDragLayer(view, ai);
                    startDrag(viewBitmap, outlineBitmap, ai[0], ai[1], dragsource, iteminfo, action, null, rect);
                    viewBitmap.recycle();
                    if (action == DRAG_ACTION_MOVE) {
                        view.setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}
