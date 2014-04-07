package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    CellLayout.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140320:    1. 单屏幕布局
 *              2. LayoutParams
 * ====================================================================================
 */
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

import cn.minking.launcher.gadget.GadgetInfo;
import android.app.WallpaperManager;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

public class CellLayout extends ViewGroup
    implements OnLongClickAgent.VersionTagGenerator{
    /**
     * 描述： 每个子布局的信息
     * @author minking
     *
     */
    static final class CellInfo
        implements ContextMenu.ContextMenuInfo {
        View cell;
        int cellX;
        int cellY;
        int container;
        long screenId;
        int spanX;
        int spanY;
    
        public String toString() {
            StringBuilder stringbuilder = (new StringBuilder()).append("Cell[view=");
            Object obj;
            if (cell != null) {
                obj = cell.getClass();
            } else {
                obj = "null";
            }
            return stringbuilder.append(obj).append(", x=").append(cellX).append(", y=").append(cellY).append("]").toString();
        }
    
        CellInfo() {
            screenId = -1L;
        }
    }
    
    /**
     * 描述： 每个子布局的信息
     * @author minking
     *
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        long accessTag;
        public int cellHSpan;
        public int cellVSpan;
        public int cellX;
        public int cellY;
        boolean dropped;
        public boolean isDragging;
        boolean regenerateId;
        int x;
        int y;

        /**
         * 功能： 配置子布局的宽高及X. Y坐标值
         * @param cWidth
         * @param cHeight
         * @param widthGap
         * @param heightGap
         * @param paddingLeft
         * @param paddingRight
         */
        public void setup(int cWidth, int cHeight, 
                int widthGap, int heightGap, int paddingLeft, int paddingRight) {
            int ch = cellHSpan;
            int cv = cellVSpan;
            int cx = cellX;
            int cy = cellY;
            width = (ch * cWidth + widthGap * (ch - 1)) - leftMargin - rightMargin;
            height = (cv * cHeight + heightGap * (cv - 1)) - topMargin - bottomMargin;
            x = paddingLeft + cx * (cWidth + widthGap) + leftMargin;
            y = paddingRight + cy * (cHeight + heightGap) + topMargin;
        }

        public LayoutParams(int cx, int cy, int ch, int cv) {
            super(-1, -1);
            cellX = cx;
            cellY = cy;
            cellHSpan = ch;
            cellVSpan = cv;
        }

        public LayoutParams(Context context, AttributeSet attributeset){
            super(context, attributeset);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutparams) {
            super(layoutparams);
            cellHSpan = 1;
            cellVSpan = 1;
        }
    }
    
    private class StayConfirm
        implements Runnable {
        private DropTarget.DragObject lastDragObject;
        
        @Override
        public void run() {
            DropTarget dropTarget;
            View view = mOccupiedCell[mLastDragPos.cellXY[0]][mLastDragPos.cellXY[1]];
            if (!(view instanceof DropTarget)){
                dropTarget = null;
            } else {
                dropTarget = (DropTarget)view;
            }
            if(LOGD){
                Log.d(TAG, "staytype : " + mLastDragPos.stayType);
            }
            if (mLastDragPos.stayType != 2) {
                if (mLastDragPos.stayType != STAY_TYPE_EMPTY) {
                    if (mLastDragPos.stayType != 4) {
                        if (lastDragObject.dragInfo.spanX == 1 && lastDragObject.dragInfo.spanY == 1) {
                            makeEmptyCellAt(cellToGapIndex(mLastDragPos.cellXY[0], mLastDragPos.cellXY[1], mLastDragPos.stayType));
                        } else {
                            pointToCell(lastDragObject.x - lastDragObject.xOffset, 
                                    lastDragObject.y - lastDragObject.yOffset, mCellXY);
                            makeEmptyCellAt(mCellXY[0], mCellXY[1], lastDragObject.dragInfo.spanX, 
                                    lastDragObject.dragInfo.spanY);
                        }
                    } else {
                        rollbackLayout();
                    }
                } else{
                    if(LOGD){
                        Log.d(TAG, "x y : " + mLastDragPos.cellXY[0] + " , "+ mLastDragPos.cellXY[1]);
                    }
                    if (mOccupiedCellBak[mLastDragPos.cellXY[0]][mLastDragPos.cellXY[1]] == null){
                        rollbackLayout();
                    }
                }
            } else {
                if(LOGD){
                    Log.d(TAG, "2 : " + mLastCoveringView);
                }
                if (dropTarget != null && dropTarget.isDropEnabled() 
                        && dropTarget.acceptDrop(lastDragObject) 
                        && dropTarget != mLastCoveringView) {
                    dropTarget.onDragEnter(lastDragObject);
                    mLastCoveringView = dropTarget;
                }
            }
            lastDragObject = null;
        }
        
        private StayConfirm() {
            lastDragObject = null;
        }
    
    }
    
    class DragPos {
        int cellXY[];
        int stayType;

        boolean equal(DragPos dragpos) {
            boolean flag = true;
            if (cellXY[0] != dragpos.cellXY[0] 
                || cellXY[1] != dragpos.cellXY[1] 
                || stayType != dragpos.stayType) {
                flag = false;
            }
            return flag;
        }

        void reset() {
            cellXY[0] = -1;
            cellXY[1] = -1;
            stayType = 4;
        }

        void set(DragPos dragpos) {
            cellXY[0] = dragpos.cellXY[0];
            cellXY[1] = dragpos.cellXY[1];
            stayType = dragpos.stayType;
        }

        public DragPos() {
            cellXY = new int[2];
            stayType = 4;
            reset();
        }
    }
    
    /********** 常量  **********/
    private final static boolean LOGD = true;
    private final static String TAG = "MKHome.CellLayout";
    
    private final static int STAY_TYPE_EMPTY = 0;
    
    static final boolean sAssertionsDisabled = false;
    private ImageView mCellBackground;
    private final int mCellWidth;
    private final int mCellHeight;
    private final CellInfo mCellInfo;
    
    /********** 状态  **********/
    private boolean mLastDownOnOccupiedCell;
    private boolean mLayoutBackupValid;
    
    /********** 内容  **********/
    private Launcher mLauncher;
    private Drawable mDefaultCellBackground;
    private DragPos mLastDragPos;
    private DragPos mTmpDragPos;
    private DropTarget mLastCoveringView;
    private final WallpaperManager mWallpaperManager;
    private final int mWidgetCellPaddingBottom;
    private final int mWidgetCellPaddingTop;
    
    private boolean mDisableTouch;
    private int mHCells;
    private int mVCells;
    private int mWidthGap;
    private int mHeightGap;
    
    /********** 数据  **********/
    private int mTotalCells;
    private int mEmptyCellNumber;
    private int mCellXY[];
    private int mTmpCellLR[];
    private View mOccupiedCell[][];
    private View mOccupiedCellBak[][];
    private final Rect mRect;
    private Rect mRectTmp;
    private final int mPaddingLeft;
    private final int mPaddingRight;
    private final int mPaddingTop;
    
    private StayConfirm mStayConfirm;
    private int mStayConfirmSize;
    
    /********** 行为  **********/
    private OnLongClickAgent mOnLongClickAgent;
    
    private Context mContext;
    
    
    /**
     * 描述： 构造函数
     * @param context
     */
    public CellLayout(Context context){
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attributeset){
        this(context, attributeset, 0);
    }

    public CellLayout(Context context, AttributeSet attributeset, int i){
        super(context, attributeset, i);
        mContext = context;
        mCellInfo = new CellInfo();
        mRect = new Rect();
        mRectTmp  = new Rect();
        mLastDragPos = new DragPos();
        mTmpDragPos = new DragPos();
        mCellXY = new int[2];
        mTmpCellLR = new int[2];

        // 横向及纵向的单元格个数
        mHCells = ResConfig.getCellCountX();
        mVCells = ResConfig.getCellCountY();
        
        // 总单元格个数及未被占用单元格个数 
        mTotalCells = mHCells * mVCells;
        mEmptyCellNumber = mTotalCells;
        
        mLastDownOnOccupiedCell = false;
        mLastCoveringView = null;
        mDisableTouch = false;
        mLayoutBackupValid = false;
        
        Resources resources = context.getResources();
        mCellWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        mCellHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        mWidgetCellPaddingTop = resources.getDimensionPixelSize(R.dimen.workspace_widget_padding_top);
        mWidgetCellPaddingBottom = resources.getDimensionPixelSize(R.dimen.workspace_widget_padding_bottom);
        mPaddingTop = resources.getDimensionPixelSize(R.dimen.workspace_padding_top);
        mPaddingLeft = resources.getDimensionPixelSize(R.dimen.workspace_padding_side);
        mPaddingRight = mPaddingLeft;

        mStayConfirm = new StayConfirm();
        mStayConfirmSize = (int)(0.5F + 0.1F * (float)mCellWidth);
        
        // 标识单元格视图的二维数组
        int ai[] = new int[]{mHCells, mVCells};
        int aiBak[] = new int[]{mHCells, mVCells};
        mOccupiedCell = (View[][])Array.newInstance(View.class, ai);
        mOccupiedCellBak = (View[][])Array.newInstance(View.class, aiBak);
        
        mWallpaperManager = WallpaperManager.getInstance(getContext());
        mLauncher = (Launcher)context;
        mOnLongClickAgent = new OnLongClickAgent(this, mLauncher, this);
        
        mCellBackground = new ImageView(context);
        mCellBackground.setLayoutParams(new LayoutParams(0, 0, 0, 0));
        mDefaultCellBackground = resources.getDrawable(R.drawable.cell_bg);
        
        setTag(mCellInfo);
    }

    /**
     * 功能： 备份还原视图数组
     */
    private void backupLayout() {
        copyOccupiedCells(mOccupiedCell, mOccupiedCellBak);
        mLayoutBackupValid = true;
    }
    
    /**
     * 功能： 拷贝视图数组
     * @param oc
     * @param ocBak
     * @return
     */
    private boolean copyOccupiedCells(View oc[][], View ocBak[][]) {
        boolean flag = false;
         
        for (int mx = 0; mx < mHCells; mx++) {
            for (int my = 0; my < mVCells; my++) {
                if (ocBak[mx][my] != oc[mx][my])            {
                    ocBak[mx][my] = oc[mx][my];
                    flag = true;
                }   
            }
        }
        
        return flag;
    }
    
    /**
     * 功能： 单元格坐标XY转换成Index
     * @param cx
     * @param cy
     * @param type
     * @return
     */
    private int cellToGapIndex(int cx, int cy, int type) {
        int index = cx + cy * mHCells;
        int gapStart;
        if (type != 3) {
            gapStart = 0;
        } else {
            gapStart = 1;
        }
        return gapStart + index;
    }

    /**
     * 功能： 拖拽至两图标之间的GAP，将GAP转换成Index
     * @param index
     * @param cellXY
     */
    private void gapToCellIndexes(int index, int cellXY[]) {
        int left = -1;
        int right = -1;
        int cx = index % mHCells;
        int cy = index / mHCells;
        
        // GAP不在最左边
        if (cx != 0) {
            left = (cx + cy * mHCells) - 1;
        }
        cellXY[0] = left;
        
        // GAP不在最右边
        if (cx != mHCells) {
            right = cx + cy * mHCells;
        }
        cellXY[1] = right;
    }
    
    private void positionIndexToCell(int index, int cellXY[]) {
        cellXY[0] = index % mHCells;
        cellXY[1] = index / mHCells;
    }
    
    /**
     * 功能： 将触摸的实际坐标值转换成对应的单元格
     * @param cx
     * @param cy
     * @param cellXY
     */
    void pointToCell(int cx, int cy, int cellXY[]) {
        cellXY[0] = (cx - mPaddingLeft) / (mCellWidth + mWidthGap);
        cellXY[1] = (cy - mPaddingTop) / (mCellHeight + mHeightGap);
        cellXY[0] = Math.max(0, Math.min(cellXY[0], mHCells - 1));
        cellXY[1] = Math.max(0, Math.min(cellXY[1], mVCells - 1));
    }
    
    /**
     * 功能： 拖动时移动其他图标来生成空位置
     * @param index
     */
    private void makeEmptyCellAt(int index) {
        // 根据当前位置得到GAP左右的index
        gapToCellIndexes(index, mTmpCellLR);
        int leftGap = mTmpCellLR[0];
        int rightGap = mTmpCellLR[1];
        
        if (LOGD) {
            Log.d(TAG, "makeEmptyCellAt : " + index + "leftGap : " + leftGap + "rigth : " + rightGap);
        }
        
        // 先从右边开始找空位置，如果找到则break，记住right位置，后续移动使用
        while (rightGap != -1 && rightGap < mTotalCells){
            positionIndexToCell(rightGap, mCellXY);
            if (LOGD) {
                Log.d(TAG, "rigth : " + rightGap + "mOccupiedCell[mCellXY[0]][mCellXY[1]]" + mOccupiedCell[mCellXY[0]][mCellXY[1]]);
            }
            if (mOccupiedCell[mCellXY[0]][mCellXY[1]] == null){
                break;
            }
            rightGap++;
        }
        // 如果右边没有位置则标识为-1
        if (rightGap == mTotalCells){
            rightGap = -1;
        }
        
        if (LOGD) {
            Log.d(TAG, "rightGap : " + rightGap);
        }
        
        // 再计算左边，如果找到则break
        while(leftGap != -1 && leftGap >= 0){
            positionIndexToCell(leftGap, mCellXY);
            if (LOGD) {
                Log.d(TAG, "leftGap : " + leftGap + "mOccupiedCell[mCellXY[0]][mCellXY[1]]" + mOccupiedCell[mCellXY[0]][mCellXY[1]]);
            }
            if (mOccupiedCell[mCellXY[0]][mCellXY[1]] == null) {
                break;
            }
            leftGap--;
        }
        // 没找到标识-1
        if (leftGap < 0){
            leftGap = -1;
        }
        
        if (LOGD) {
            Log.d(TAG, "leftGap : " + leftGap);
        }
        
        if (leftGap == -1 || rightGap == -1) {
            if (leftGap == -1) {
                if (rightGap == -1) {
                    // 如果左右两边都没找到可移动的位置，则返回
                    return;
                }
                // 左边没有空位置, 则设置右边的GAP，往左移动
                leftGap = mTmpCellLR[1];
            } else {
                leftGap = mTmpCellLR[0];
            }
        } else {
            if (rightGap - leftGap != 2) {
                if (rightGap - mTmpCellLR[1] > mTmpCellLR[0] - leftGap){
                    leftGap = mTmpCellLR[0];
                } else {
                    leftGap = mTmpCellLR[1];
                }
            } else {
                if (rightGap == mTmpCellLR[1]){
                    leftGap = mTmpCellLR[0];
                } else {
                    leftGap = mTmpCellLR[1];
                }
            }
        }
        
        if (LOGD) {
            Log.d(TAG, "leftGap 2 : " + leftGap);
        }
        // 设置移动方向
        int move;
        if (leftGap != mTmpCellLR[0]){
            move = 1;
        } else {
            move = -1;
        }
        Object obj = null;
        while (leftGap < mTotalCells){
            positionIndexToCell(leftGap, mCellXY);
            leftGap += move;
            View view = mOccupiedCell[mCellXY[0]][mCellXY[1]];
            if (view != null) {
                LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                if (layoutparams.cellHSpan != 1 || layoutparams.cellVSpan != 1) {
                    continue;
                }
            } 
            // 替换
            mOccupiedCell[mCellXY[0]][mCellXY[1]] = (View)obj;
            if (obj != null) {
                mOccupiedCell[mCellXY[0]][mCellXY[1]] = (View)obj;
                LayoutParams layoutparams = (LayoutParams)((View)obj).getLayoutParams();
                layoutparams.cellX = mCellXY[0];
                layoutparams.cellY = mCellXY[1];
            }
            if (view == null) {
                break;
            }
            obj = view;
        }
        requestLayout();
    }

    private void makeEmptyCellAt(int cx, int cy, int sx, int sy) {
        
    }
    
    /**
     * 功能： 移除View
     * @param start
     * @param count
     */
    private void onRemoveViews(int start, int count) {
        if (start < 0 || count < 0) return;
        
        for (int i = 0; i < count; i++) {
            View view = getChildAt(start + i);
            if (!(view instanceof Folder) && view != mCellBackground) {
                LayoutParams lParams = (LayoutParams)view.getLayoutParams();
                if (((ItemInfo)view.getTag()).screenId == getScreenId()){
                    updateCellOccupiedMarks(lParams, view, true);
                }
            }
        }
    }

    private void relayoutByOccupiedCells(){
        long t = SystemClock.currentThreadTimeMillis();
        for (int mx = 0; mx < mHCells; mx++) {
            for (int my = 0; my < mVCells; my++) {
                View v = mOccupiedCell[mx][my];
                if (v != null) {
                    LayoutParams layoutparams = (LayoutParams)v.getLayoutParams();
                    if (layoutparams.accessTag < t) {
                        ItemInfo itemInfo = (ItemInfo)v.getTag();
                        itemInfo.cellX = mx;
                        itemInfo.cellY = my;
                        layoutparams.cellX = mx;
                        layoutparams.cellY = my;
                        layoutparams.accessTag = t;
                    }
                }
            }
        }
        
        requestLayout();
        return;
    }
    
    
    private void rollbackLayout() {
        if (mLayoutBackupValid && copyOccupiedCells(mOccupiedCellBak, mOccupiedCell)){
            relayoutByOccupiedCells();
        }
    }

    private void saveCurrentLayout() {
        long t = SystemClock.currentThreadTimeMillis();;
        ArrayList<ContentProviderOperation> arraylist = new ArrayList<ContentProviderOperation>();
        mEmptyCellNumber = 0;
        
        for (int x = 0; x < mHCells; x++) {
            for (int y = 0; y < mVCells; y++) {
                View view = mOccupiedCell[x][y];
                if (view != null) {
                    LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                    if (layoutparams.accessTag < t) {
                        layoutparams.accessTag = t;
                        if (view != mOccupiedCellBak[x][y]) {
                            ItemInfo iteminfo = (ItemInfo)view.getTag();
                            iteminfo.cellX = x;
                            iteminfo.cellY = y;
                            arraylist.add(LauncherModel.getMoveItemOperation(
                                    iteminfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, 
                                    getScreenId(), x, y));
                        }
                    }
                }
                mEmptyCellNumber = mEmptyCellNumber + 1;
            }
        }

        if (!arraylist.isEmpty()){
            LauncherModel.applyBatch(mContext, "cn.minking.launcher.settings", arraylist);
        }
    }
    
    public void addView(View child, int index, LayoutParams params) {
        LayoutParams pLayoutParams = params;
        pLayoutParams.regenerateId = true;
        if (child instanceof ItemIcon){
            ((ItemIcon)child).setEnableTranslateAnimation(true);
        }
        if (!(child instanceof Folder)){
            updateCellOccupiedMarks(pLayoutParams, child, false);
        }
        super.addView(child, index, params);
    }

    public void alignIconsToTop() {
        if (getEmptyCellNumber() == 0) return;

        ArrayList<ContentProviderOperation> arraylist = new ArrayList<ContentProviderOperation>();
        int h = 0;
        int v = 0;
        for (int i = 0; i < mHCells; i++) {
            for (int j = 0; j < mVCells; j++) {
                View view = mOccupiedCell[i][j];
                if (!(view instanceof ItemIcon))
                    continue;
                do
                {
                    if ((mOccupiedCell[h][v] instanceof ItemIcon) || mOccupiedCell[h][v] == null)
                        break;
                    if (++h >= mHCells) {
                        h = 0;
                        v++;
                    }
                } while (true);
                if (i != h || j != v ) {
                    mOccupiedCell[h][v] = view;
                    mOccupiedCell[i][j] = null;
                    LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                    layoutparams.cellX = h;
                    layoutparams.cellY = v;
                    arraylist.add(LauncherModel.getMoveItemOperation((ItemInfo)view.getTag(), 
                            LauncherSettings.Favorites.CONTAINER_DESKTOP, getScreenId(), h, v));
                }
                if (++h >= mHCells) {
                    h = 0;
                    v++;
                }
            }

        }

        if (!arraylist.isEmpty()) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
            try {
                mContext.getContentResolver().applyBatch("cn.minking.launcher.settings", arraylist);
            }
            catch (RemoteException _ex) { }
            catch (OperationApplicationException _ex) { }
            catch (SQLiteException _ex) { }
            requestLayout();
        }
    }
        
    @Override
    public void buildDrawingCache(boolean autoScale) {
        super.buildDrawingCache(autoScale);
    }

    @Override
    public void buildDrawingCache() {
        super.buildDrawingCache();
    }

    @Override
    public void cancelLongPress() {
        mOnLongClickAgent.cancelCustomziedLongPress();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).cancelLongPress();
        }
    }
    
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LayoutParams;
    }
    
    void clearBackupLayout() {
        for (int i = 0; i < mHCells; i++) {
            for (int j = 0; j < mVCells; j++) {
                mOccupiedCellBak[i][j] = null;
            }
        }
        mLayoutBackupValid = false;
    }

    void clearCellBackground() {
        removeView(mCellBackground);
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void destroyDrawingCache() {
        super.destroyDrawingCache();
    }
    
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }
    
    int getCellHeight() {
        return mCellHeight;
    }

    int getCellWidth() {
        return mCellWidth;
    }
    
    public boolean getChildVisualPosByTag(Object obj, int pos[]) {
        boolean flag = true;
        for (int i = 0; i < mHCells; i++) {
            for (int j = 0; j < mVCells; j++) {
                View view = mOccupiedCell[i][j];
                if (view != null && view.getTag().equals(obj)){
                    flag = true;
                    pos[0] = i;
                    pos[1] = j;
                    break;
                }
            }
        }
        return flag;
    }

    public int getEmptyCellNumber() {
        return mEmptyCellNumber;
    }
    
    public void getExpandabilityArrayForView(View view, int pos[]){
        
    }
    
    int getHeightGap() {
        return mHeightGap;
    }

    long getScreenId() {
        return mCellInfo.screenId;
    }

    public Object getVersionTag() {
        return Integer.valueOf(getWindowAttachCount());
    }

    int getWidthGap() {
        return mWidthGap;
    }
    
    /**
     * 功能： 判定是否所需要的单元格被占用， 传入CX CY 位置及SX SY来计算大小
     * @param cx
     * @param cy
     * @param sx
     * @param sy
     * @return
     */
    public boolean isCellOccupied(int cx, int cy, int sx, int sy) {
        boolean bOccupied = false;

        for (int mx = 0; mx < sx; mx++) {
            for (int my = 0; my < sy; my++) {
                // 如果发现其中一个单元格被占用则立即退出
                if (mOccupiedCell[cx + mx][cy + my] != null){
                    bOccupied = true;
                    break;
                }
            }   
        }
        return bOccupied;
    }
    
    /**
     * 功能： 返回最后被占用的单元格
     * @return
     */
    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }
    
    /**
     * 功能： 将单元格对应的位置转换成实际的XY坐标值
     * @param cx
     * @param cy
     * @param cellxy
     */
    public void cellToPoint(int cx, int cy, int cellxy[]) {
        cellxy[0] = mPaddingLeft + cx * (mCellWidth + mWidthGap);
        cellxy[1] = mPaddingTop + cy * (mCellHeight + mHeightGap);
    }
    
    /**
     * 功能： 找到最近的空置区域
     * @param pixelX  x 向的实际坐标，非单元格x
     * @param pixelY  y 向坐标
     * @param sx cell info 所占横向单元格 
     * @param sy 纵向单元格
     * @param flag
     * @return
     */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int sx, int sy, boolean ignoreOccupied) {
        // 如果横*纵大于剩余的单元格则退出
        if (!ignoreOccupied && sx * sy > mEmptyCellNumber) {
            return null;
        }
        
        double bestDistance = Double.MAX_VALUE;
        int pos[] = new int[2];
        
        // 计算最近的距离及得到CX CY数组
        for (int cx = 0; cx <= mHCells - sx; cx++){
            for (int cy = 0; cy <= mVCells - sy; cy++) {
                cellToPoint(cx, cy, mCellXY);
                double distance = Math.pow(mCellXY[0] - pixelX, 2D) + Math.pow(mCellXY[1] - pixelY, 2D);
                if (distance < bestDistance && (ignoreOccupied || !isCellOccupied(cx, cy, sx, sy))) {
                    bestDistance = distance;
                    pos[0] = cx;
                    pos[1] = cy;
                }
            }
        }
        
        if (bestDistance == Double.MAX_VALUE){
            pos = null;
        }
        
        return pos;

    }

    public int[] findNearestVacantAreaByCellPos(int cx, int cy, int sx, int sy, boolean flag) {
        cellToPoint(cx, cy, mCellXY);
        return findNearestVacantArea(mCellXY[0], mCellXY[1], sx, sy, flag);
    }
    /**
     * 功能： 计算各项的位置
     * @param view
     */
    public void measureChild(View view) {
        LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
        if ((!(view.getTag() instanceof LauncherAppWidgetInfo) && !(view.getTag() instanceof GadgetInfo)) 
             || (layoutparams.cellHSpan == 1 && layoutparams.cellVSpan == 1)) {
            // 占一个单元格的布局
            layoutparams.setup(mCellWidth, mCellHeight, mWidthGap, mHeightGap, mPaddingLeft, mPaddingTop);
        } else {
            // 大于一个单元格的布局
            int i = Math.max((getMeasuredHeight() - mWidgetCellPaddingTop - mWidgetCellPaddingBottom) / mVCells, ResConfig.getWidgetCellMinHeight());
            layoutparams.setup(Math.max(getMeasuredWidth() / mHCells, ResConfig.getWidgetCellMinWidth()), i, 0, 0, 0, mWidgetCellPaddingTop);
        }
        
        // 给每个布局都重新分配ID
        if (layoutparams.regenerateId) {
            view.setId((0xff & getId()) << 16 | (0xff & layoutparams.cellX) << 8 | 0xff & layoutparams.cellY);
            layoutparams.regenerateId = false;
        }
        view.measure(MeasureSpec.makeMeasureSpec(layoutparams.width, MeasureSpec.EXACTLY), 
                MeasureSpec.makeMeasureSpec(layoutparams.height, MeasureSpec.EXACTLY));
    }
    
    void onDragChild(View view) {
        LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
        layoutparams.isDragging = true;
        updateCellOccupiedMarks(layoutparams, view, true);
    }

    void onDragEnter(DropTarget.DragObject dragobject) {
        mLastDragPos.reset();
        backupLayout();
        if (dragobject.outline == null) {
            mCellBackground.setImageDrawable(mDefaultCellBackground);
            mCellBackground.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
        } else {
            mCellBackground.setImageBitmap(dragobject.outline);
            mCellBackground.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        }
    }

    void onDragExit(DropTarget.DragObject dragobject) {
        getHandler().removeCallbacks(mStayConfirm);
        clearCellBackground();
        mCellBackground.setImageDrawable(null);
        if (!dragobject.dragComplete) {
            rollbackLayout();
            clearBackupLayout();
            if (mLastCoveringView != null) {
                mLastCoveringView.onDragExit(dragobject);
                mLastCoveringView = null;
            }
        }
    }

    /**
     * 功能： 拖动over事件处理
     * @param dragobject
     */
    public void onDragOver(DropTarget.DragObject dragobject) {
        // 得到sx sy
        int sx = dragobject.dragInfo.spanX;
        int sy = dragobject.dragInfo.spanY;
        
        int pos[] = findNearestVacantArea(dragobject.x - dragobject.xOffset, dragobject.y - dragobject.yOffset, sx, sy, false);
        
        if (pos != null) {
            removeView(mCellBackground);
            if (mLastCoveringView == null) {
                LayoutParams layoutparams = (LayoutParams)mCellBackground.getLayoutParams();
                layoutparams.cellX = pos[0];
                layoutparams.cellY = pos[1];
                layoutparams.cellHSpan = sx;
                layoutparams.cellVSpan = sy;
                addView(mCellBackground, 0, layoutparams);
                if (dragobject.dragSource != mLauncher.getWorkspace()) {
                    dragobject.dragInfo.cellX = pos[0];
                    dragobject.dragInfo.cellY = pos[1];
                }
            }
        }
        if (!(dragobject.dragInfo instanceof LauncherAppWidgetProviderInfo)) {
            // 根据实际X,Y 坐标， 转换单元格
            pointToCell(dragobject.x, dragobject.y, mTmpDragPos.cellXY);
            View view = mOccupiedCell[mTmpDragPos.cellXY[0]][mTmpDragPos.cellXY[1]];
            if (mLastCoveringView != null && view != mLastCoveringView) {
                mLastCoveringView.onDragExit(dragobject);
                mLastCoveringView = null;
            }
            if (view == null || !(view instanceof ItemIcon)) {
                if (view != null){
                    mTmpDragPos.stayType = 4;
                } else {
                    mTmpDragPos.stayType = STAY_TYPE_EMPTY;
                }
            } else {
                view.getHitRect(mRectTmp);
                if (!(dragobject.dragInfo instanceof FolderInfo)) {
                    if (dragobject.x >= mRectTmp.left + mStayConfirmSize) {
                        if (dragobject.x <= mRectTmp.right - mStayConfirmSize) {
                            mTmpDragPos.stayType = 2;
                        } else {
                            mTmpDragPos.stayType = 3;
                        }
                    } else {
                        mTmpDragPos.stayType = 1;
                    }
                } else {
                    DragPos dragpos = mTmpDragPos;
                    int k;
                    if (dragobject.x >= mRectTmp.centerX()) {
                        k = 3;
                    } else {
                        k = 1;
                    }
                    dragpos.stayType = k;
                }
            }
            
            // 设置最后的拖至位置
            if (!mLastDragPos.equal(mTmpDragPos)) {
                if(LOGD){
                    Log.d(TAG, "mTx , mTy :" + mTmpDragPos.cellXY[0] + " , "+ mTmpDragPos.cellXY[1]);
                }
                mLastDragPos.set(mTmpDragPos);
                getHandler().removeCallbacks(mStayConfirm);
                mStayConfirm.lastDragObject = dragobject;
                long duration;
                if (mLastDragPos.stayType != 2) {
                    duration = 150L;
                } else {
                    if (!(view instanceof FolderIcon));
                    duration = 100L;
                }
                postDelayed(mStayConfirm, duration);
            }
        }
    }

    boolean onDrop(DropTarget.DragObject dragobject, View view) {
        boolean flag = true;
        getHandler().removeCallbacks(mStayConfirm);
        if (mLastCoveringView == null) {
            int ai[] = findNearestVacantArea(dragobject.x - dragobject.xOffset, 
                    dragobject.y - dragobject.yOffset, 
                    dragobject.dragInfo.spanX, 
                    dragobject.dragInfo.spanY, false);
            if (ai != null) {
                if (view != null) {
                    if (dragobject.dragInfo.container != (long)mCellInfo.container 
                            || ai[0] != mCellInfo.cellX 
                            || ai[1] != mCellInfo.cellY 
                            || dragobject.dragInfo.screenId != mCellInfo.screenId) {
                        if (view != null) {
                            ItemInfo itemInfo = (ItemInfo)view.getTag();
                            itemInfo.screenId = getScreenId();
                            itemInfo.cellX = ai[0];
                            itemInfo.cellY = ai[1];
                            itemInfo.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                            LayoutParams layoutParams = (LayoutParams)view.getLayoutParams();
                            layoutParams.cellX = ai[0];
                            layoutParams.cellY = ai[1];
                            layoutParams.isDragging = false;
                            layoutParams.dropped = true;
                            if (view.getParent() != null) {
                                view.requestLayout();
                                updateCellOccupiedMarks(layoutParams, view, false);
                            } else {
                                addView(view, -1, layoutParams);
                            }
                        }
                        saveCurrentLayout();
                        clearBackupLayout();
                    } else {
                        rollbackLayout();
                        updateCellOccupiedMarks((LayoutParams)view.getLayoutParams(), view, false);
                    }
                } else {
                    dragobject.dragInfo.cellX = ai[0];
                    dragobject.dragInfo.cellY = ai[1];
                }
            } else {
                flag = false;
            }
        } else {
            flag = mLastCoveringView.onDrop(dragobject);
            mLastCoveringView.onDragExit(dragobject);
            mLastCoveringView = null;
            rollbackLayout();
        }
        return flag;
    }

    void onDropAborted(View view) {
        getHandler().removeCallbacks(mStayConfirm);
        if (view != null) {
            LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
            layoutparams.isDragging = false;
            rollbackLayout();
            updateCellOccupiedMarks(layoutparams, view, false);
        }
    }
    
    
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean flag = false;
        
        int action = ev.getAction();
        
        if (action != MotionEvent.ACTION_DOWN) {
            if (action == MotionEvent.ACTION_UP) {
                mCellInfo.cell = null;
                mCellInfo.cellX = -1;
                mCellInfo.cellY = -1;
                mCellInfo.spanX = 0;
                mCellInfo.spanY = 0;

                mLastDownOnOccupiedCell = false;
            }
            if (mOnLongClickAgent.onInterceptTouchEvent(ev)){
                flag = true;
            }
            return flag;
        }
        
        int x = (int)ev.getX() + mScrollX;
        int y = (int)ev.getY() + mScrollY;
        
        View view;
        for (int i = 0; i < getChildCount(); i++) {
            view = getChildAt(i);
            if (view.getVisibility() == View.VISIBLE || view.getAnimation() != null) {
                view.getHitRect(mRect);
                if (mRect.contains(x, y)){
                    LayoutParams layoutParams = (LayoutParams)view.getLayoutParams();
                    mCellInfo.cell = view;
                    mCellInfo.cellX = layoutParams.cellX;
                    mCellInfo.cellY = layoutParams.cellY;
                    mCellInfo.spanX = layoutParams.cellHSpan;
                    mCellInfo.spanY = layoutParams.cellVSpan;
                    mLastDownOnOccupiedCell = true;
                    
                    break;
                }
            }
        }
        
        if (mLastDownOnOccupiedCell && (mCellInfo.cell != null)) {
            ItemInfo itemInfo = (ItemInfo)mCellInfo.cell.getTag();
            if (itemInfo != null) {
                if (!mLauncher.isInEditing() 
                        || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER 
                        || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER 
                        || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET){
                    flag = false;
                } else {
                    flag = true;
                }
            } else {
                flag = false;
                return flag;
            }
        } else {
            pointToCell(x, y, mCellXY);
            mCellInfo.cell = null;
            mCellInfo.cellX = mCellXY[0];
            mCellInfo.cellY = mCellXY[1];
            mCellInfo.spanX = 1;
            mCellInfo.spanY = 1;
        }
        
        long duration;
        if (!mLastDownOnOccupiedCell) {
            duration = 800L;
        } else {
            duration = 200L;
        }
        mOnLongClickAgent.setEditingTimeout(duration);
        if (mOnLongClickAgent.onInterceptTouchEvent(ev)){
            flag = true;
        }
        return flag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean flag;
        if (!mDisableTouch) {
            mOnLongClickAgent.onTouchEvent(event);
            flag = true;
        } else {
            mOnLongClickAgent.cancelCustomziedLongPress();
            flag = false;
        }
        return flag;
    }
    
    /**
     * 功能： 计算布局
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSm = MeasureSpec.getMode(widthMeasureSpec);
        int wSs = MeasureSpec.getSize(widthMeasureSpec);
        int hSm = MeasureSpec.getMode(heightMeasureSpec);
        int hSs = MeasureSpec.getSize(heightMeasureSpec);
        
        if (wSm != 0 && hSm != 0) {
            setMeasuredDimension(wSs, hSs);
            int wGap = 0;
            if (mHCells > 1) {
                wGap = (wSs - mPaddingLeft - mPaddingRight - mCellWidth * mHCells) / (mHCells - 1);
            } 
            mWidthGap = wGap;
            
            int hGap = 0;
            if (mVCells > 1) {
                hGap = (hSs - mPaddingTop - mPaddingBottom - mCellHeight * mVCells) / (mVCells - 1);
            }
            mHeightGap = hGap;
            
            for (int i = 0; i < getChildCount(); i++) {
                measureChild(getChildAt(i));
            }
            
        } else {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
    }
    
    /**
     * 功能： 布局
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view.getVisibility() != View.GONE) {
                LayoutParams layoutParams = (LayoutParams)view.getLayoutParams();
                int x = layoutParams.x;
                int y = layoutParams.y;
                view.layout(x, y, x + layoutParams.width, y + layoutParams.height);
                
                if (layoutParams.dropped) {
                    layoutParams.dropped = false;
                    getLocationOnScreen(mCellXY);
                    mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop", 
                            x + mCellXY[0] + layoutParams.width / 2, 
                            y + mCellXY[1] + layoutParams.height / 2, 0, null);
                }
            }
        }
    }
    
    @Override
    public void removeAllViewsInLayout() {
        onRemoveViews(0, getChildCount());
        super.removeAllViewsInLayout();
    }

    public void removeChild(ItemInfo iteminfo) {
        View view = mOccupiedCell[iteminfo.cellX][iteminfo.cellY];
        if (sAssertionsDisabled || view.getTag().equals(iteminfo)) {
            removeView(view);
            return;
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public void removeView(View view) {
        onRemoveViews(indexOfChild(view), 1);
        super.removeView(view);
    }

    @Override
    public void removeViewAt(int i) {
        onRemoveViews(i, 1);
        super.removeViewAt(i);
    }

    @Override
    public void removeViewInLayout(View view) {
        onRemoveViews(indexOfChild(view), 1);
        super.removeViewInLayout(view);
    }

    @Override
    public void removeViews(int i, int j) {
        onRemoveViews(i, j);
        super.removeViews(i, j);
    }

    @Override
    public void removeViewsInLayout(int i, int j) {
        onRemoveViews(i, j);
        super.removeViewsInLayout(i, j);
    }

    @Override
    public void requestChildFocus(View view, View view1) {
        super.requestChildFocus(view, view1);
        if (view != null) {
            Rect rect = new Rect();
            view.getDrawingRect(rect);
            requestRectangleOnScreen(rect);
        }
    }

    void setContainerId(int i) {
        mCellInfo.container = i;
    }

    void setDisableTouch(boolean flag) {
        mDisableTouch = flag;
    }

    public void setEditMode(boolean flag) {
    }

    public void setOnLongClickListener(View.OnLongClickListener onlongclicklistener) {
        mOnLongClickAgent.setOnLongClickListener(onlongclicklistener);
    }

    void setScreenId(long id) {
        mCellInfo.screenId = id;
    }
    
    public void preRemoveView(View view) {
        if (mLastCoveringView == view)
            mLastCoveringView = null;
        updateCellOccupiedMarks((LayoutParams)view.getLayoutParams(), view, true);
        backupLayout();
    }
    
    void updateCellOccupiedMarks(View view, boolean flag) {
        updateCellOccupiedMarks((LayoutParams)view.getLayoutParams(), view, flag);
    }

    void updateCellOccupiedMarks(LayoutParams layoutparams, View view, boolean flag){
        if (view == mCellBackground) return;
        for (int cx = layoutparams.cellX; cx < (layoutparams.cellX + layoutparams.cellHSpan); cx++){
            for (int cy = layoutparams.cellY; cy < (layoutparams.cellY + layoutparams.cellVSpan); cy++) {
                if (!flag) {
                    if (mOccupiedCell[cx][cy] == null){
                        mEmptyCellNumber = mEmptyCellNumber - 1;
                    }
                    mOccupiedCell[cx][cy] = view;
                } else if (view == mOccupiedCell[cx][cy]) {
                    mEmptyCellNumber = mEmptyCellNumber + 1;
                    mOccupiedCell[cx][cy] = null;
                }
            }
        }
    }
}