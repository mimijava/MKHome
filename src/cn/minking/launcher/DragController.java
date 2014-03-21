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
        public abstract void onDragStart(DragSource dragsource, ItemInfo iteminfo, int i);
    }

    /********** 常量  **********/
    // 滑动方向
    public final static int DIRECTION_LEFT = 0;
    public final static int DIRECTION_RIGHT = 1;
    
    public static int DRAG_ACTION_COPY = 1;
    public static int DRAG_ACTION_MOVE = 0;
    
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
    
    private static int clamp(int i, int j, int k) {
        if (i >= j){
            if (i < k) {
                j = i;
            } else {
                j = k - 1;
            }
        }
        return j;
    }
    
    private Bitmap createDragOutline(Bitmap bitmap, int i) {
        int j = mLauncher.getResources().getColor(R.color.dragging_outline);
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap);
        Canvas canvas = mTmpCanvas;
        canvas.setBitmap(bitmap1);
        mOutlineHelper.applyMediumExpensiveOutlineWithBlur(bitmap1, canvas, j, j);
        canvas.setBitmap(null);
        return bitmap1;
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
            Iterator<DragListener> iterator = mListeners.iterator();
            while (iterator.hasNext()) {
                ((DragListener)iterator.next()).onDragEnd();
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

    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        int j = motionevent.getAction();
        int i = clamp((int)motionevent.getRawX(), 0, mDisplayMetrics.widthPixels);
        int k = clamp((int)motionevent.getRawY(), 0, mDisplayMetrics.heightPixels);
        switch (j) {
        case MotionEvent.ACTION_MOVE: // '\002'
        default:
            break;

        case MotionEvent.ACTION_DOWN: // '\0'
            mMotionDownX = i;
            mMotionDownY = k;
            mLastDropTarget = null;
            break;

        case MotionEvent.ACTION_UP: // '\001'
            if (mDragging)
                drop(i, k);
            endDrag();
            break;

        case MotionEvent.ACTION_CANCEL: // '\003'
            cancelDrag();
            break;
        }
        return mDragging;
    }

    public boolean onTouchEvent(MotionEvent motionevent) {
        boolean flag = false;
        View view = mScrollView;
        if (mDragging) {
            int i = motionevent.getAction();
            int k = clamp((int)motionevent.getRawX(), 0, mDisplayMetrics.widthPixels);
            int j = clamp((int)motionevent.getRawY(), 0, mDisplayMetrics.heightPixels);
            switch (i & 0xff) {
            case 4: // '\004'
            default:
                break;

            case MotionEvent.ACTION_DOWN: // '\0'
                mMotionDownX = ((flag) ? 1 : 0);
                mMotionDownY = j;
                if (k >= mScrollZone && k <= view.getWidth() - mScrollZone) {
                    mScrollState = 0;
                } else {
                    mScrollState = 1;
                    cancelScroll();
                    mHandler.postDelayed(mScrollRunnable, 1000L);
                }
                break;

            case MotionEvent.ACTION_UP: // '\001'
                handleMoveEvent(k, j, motionevent);
                cancelScroll();
                if (mDragging){
                    drop(k, j);
                }
                endDrag();
                break;

            case MotionEvent.ACTION_MOVE: // '\002'
                handleMoveEvent(k, j, motionevent);
                break;

            case MotionEvent.ACTION_CANCEL: // '\003'
                cancelDrag();
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // '\005'
                mSecondaryPointerId = motionevent.getPointerId((0xff00 & i) >> 8);
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
    
    public void startDrag(Bitmap bitmap, Bitmap bitmap1, 
            int cx, int cy, DragSource dragsource, ItemInfo iteminfo, int k, 
            Point point, Rect rect){
        if (!mLauncher.isInEditing()){
            mLauncher.setEditingState(9);
        }
        if (mInputMethodManager == null) {
            mInputMethodManager = (InputMethodManager)mLauncher.getSystemService("input_method");
        }
        mInputMethodManager.hideSoftInputFromWindow(mWindowToken, 0);
        
        Iterator<DragListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            ((DragListener)iterator.next()).onDragStart(dragsource, iteminfo, k);
        }
        
        float x = mMotionDownX;
        float y = mMotionDownY;
        if (mTouchTranslater != null) {
            mTransloateXY[0] = mMotionDownX;
            mTransloateXY[1] = mMotionDownY;
            mTouchTranslater.translateTouch(mTransloateXY);
            x = mTransloateXY[0];
            y = mTransloateXY[1];
        }
        
        int i1 = (int)x - cx;
        int j1 = (int)y - cy;
        int left;
        if (rect != null) {
            left = rect.left;
        } else {
            left = 0;
        }
        int top;
        if (rect != null){
            top = rect.top;
        } else {
            top = 0;
        }
        mDragging = true;
        mDragObject = new DropTarget.DragObject();
        mDragObject.dragComplete = false;
        mDragObject.xOffset = (int)(x - (float)(cx + left));
        mDragObject.yOffset = (int)(y - (float)(cy + top));
        mDragObject.dragSource = dragsource;
        mDragObject.dragInfo = iteminfo;
        mDragObject.outline = bitmap1;
        mVibrator.vibrate(35L);
        DropTarget.DragObject dragobject = mDragObject;
        DragView dragView = new DragView(mLauncher, bitmap, i1, j1, 0, 0, bitmap.getWidth(), bitmap.getHeight());
        dragobject.dragView = dragView;
        dragView.setAlpha((float)mDragViewAlpha / 255F);
        if (point != null) {
            dragView.setDragVisualizeOffset(new Point(point));
        }
        if (rect != null) {
            dragView.setDragRegion(new Rect(rect));
        }
        dragView.show(mMotionDownX, mMotionDownY);
        handleMoveEvent(mMotionDownX, mMotionDownY, null);
        
        return;
    }
    
    public void startDrag(Drawable drawable, int i, int j,
            DragSource dragsource, ItemInfo iteminfo, int k, Rect rect){
        Bitmap bitmap = renderDrawableToBitmap(drawable);
        if (bitmap != null) {
            Bitmap bitmap1 = createDragOutline(bitmap, HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS);
            if (bitmap1 != null){
                startDrag(bitmap, bitmap1, i, j, dragsource, iteminfo, k, null, rect);
            }
        }
    }
    
    public void startDrag(View view, DragSource dragsource, ItemInfo iteminfo, int i){
        startDrag(view, true, dragsource, iteminfo, i, null);
    }
    
    public void startDrag(View view, boolean flag, DragSource dragsource, ItemInfo iteminfo, int i, Rect rect){
        if (!mDragging) {
            Bitmap bitmap = createViewBitmap(view);
            if (bitmap != null) {
                Bitmap bitmap1;
                if (!flag) {
                    bitmap1 = null;
                } else {
                    bitmap1 = createDragOutline(bitmap, HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS);
                }
                if (!flag || bitmap1 != null) {
                    int ai[] = mCoordinatesTemp;
                    mLauncher.getDragLayer().getLocationInDragLayer(view, ai);
                    startDrag(bitmap, bitmap1, ai[0], ai[1], dragsource, iteminfo, i, null, rect);
                    bitmap.recycle();
                    if (i == DRAG_ACTION_MOVE) {
                        view.setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}
