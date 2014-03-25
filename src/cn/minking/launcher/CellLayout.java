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
            if (mLastDragPos.stayType != 2) {
                if (mLastDragPos.stayType != 0) {
                    if (mLastDragPos.stayType != 4) {
                        if (lastDragObject.dragInfo.spanX == 1 && lastDragObject.dragInfo.spanY == 1) {
                            makeEmptyCellAt(cellToGapIndex(mLastDragPos.cellXY[0], mLastDragPos.cellXY[1], mLastDragPos.stayType));
                        } else {
                            pointToCell(lastDragObject.x - lastDragObject.xOffset, lastDragObject.y - lastDragObject.yOffset, mCellXY);
                            makeEmptyCellAt(mCellXY[0], mCellXY[1], lastDragObject.dragInfo.spanX, lastDragObject.dragInfo.spanY);
                        }
                    } else {
                        rollbackLayout();
                    }
                } else{
                    if (mOccupiedCellBak[mLastDragPos.cellXY[0]][mLastDragPos.cellXY[1]] == null){
                        rollbackLayout();
                    }
                }
            } else {
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
         
        for (int i = 0; i < mHCells; i++) {
            for (int j = 0; j < mVCells; j++) {
                if (ocBak[i][j] != oc[i][j])            {
                    ocBak[i][j] = oc[i][j];
                    flag = true;
                }   
            }
        }
        
        return flag;
    }
    
    private int cellToGapIndex(int cx, int cy, int type) {
        int index = cx + cy * (mHCells + 1);
        int gap;
        if (type != 3) {
            gap = 0;
        } else {
            gap = 1;
        }
        return gap + index;
    }

    private void gapToCellIndexes(int gap, int cellXY[]) {
        int i1 = -1;
        int cx = gap % (mHCells + 1);
        int cy = gap / (mHCells + 1);
        int index;
        if (cx != 0) {
            index = (cx + cy * mHCells) - 1;
        } else {
            index = -1;
        }
        cellXY[0] = index;
        if (cx != mHCells) {
            i1 = cx + cy * mHCells;
        }
        cellXY[1] = i1;
    }
    
    private void positionIndexToCell(int i, int cellXY[]) {
        cellXY[0] = i % mHCells;
        cellXY[1] = i / mHCells;
    }
    
    private void makeEmptyCellAt(int index) {
        int tmpCellLR[] = mTmpCellLR;
        
        gapToCellIndexes(index, tmpCellLR);
        int tcLRx = tmpCellLR[0];
        int tcLRy = tmpCellLR[1];
        
        while (tcLRy != -1 && tcLRy < mTotalCells){
            positionIndexToCell(tcLRy, mCellXY);
            if (mOccupiedCell[mCellXY[0]][mCellXY[1]] == null)
                break;
            tcLRy++;
        }
        if (tcLRy == mTotalCells){
            tcLRy = -1;
        }
        
        while(tcLRx != -1 && tcLRx > 0){
            positionIndexToCell(tcLRx, mCellXY);
            if (mOccupiedCell[mCellXY[0]][mCellXY[1]] == null)
                break;
            tcLRx--;
        }
        if (tcLRx < 0){
            tcLRx = -1;
        }
        
        if (tcLRx == -1 || tcLRy == -1) {
            if (tcLRy == -1 && tcLRx == -1) {
                tcLRx = tmpCellLR[0];
            } else {
                tcLRx = tmpCellLR[1];
            }
        } else {
            if (tcLRy - tcLRx != 2) {
                if (tcLRy - tmpCellLR[1] > tmpCellLR[0] - tcLRx){
                    tcLRx = tmpCellLR[0];
                } else {
                    tcLRx = tmpCellLR[1];
                }
            } else {
                if (tcLRy == tmpCellLR[1]){
                    tcLRx = tmpCellLR[0];
                } else {
                    tcLRx = tmpCellLR[1];
                }
            }
        }
        
        
        int move;
        if (tcLRx != tmpCellLR[0]){
            move = 1;
        } else {
            move = -1;
        }
        while (tcLRx < mTotalCells){
                
            positionIndexToCell(tcLRx, mCellXY);
            tcLRx += move;
            View view = mOccupiedCell[mCellXY[0]][mCellXY[1]];
            if (view != null) {
                LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                if (layoutparams.cellHSpan != 1 || layoutparams.cellVSpan != 1)
                    continue;
            } else {
                break;
            }
            mOccupiedCell[mCellXY[0]][mCellXY[1]] = view;
            if (view != null) {
                mOccupiedCell[mCellXY[0]][mCellXY[1]] = view;
                LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                layoutparams.cellX = mCellXY[0];
                layoutparams.cellY = mCellXY[1];
            }
        }
        requestLayout();
    }

    private void makeEmptyCellAt(int i, int j, int k, int l) {
        
    }
    
    
    private void onRemoveViews(int i, int j) {
        if (i < 0 || j < 0) return;
        
        int t = j;
        while (t > 0) {
            int k = t - 1;
            View view = getChildAt(i + k);
            if (!(view instanceof Folder) && view != mCellBackground) {
                LayoutParams lParams = (LayoutParams)view.getLayoutParams();
                if (((ItemInfo)view.getTag()).screenId == getScreenId())
                    updateCellOccupiedMarks(lParams, view, true);
            }
            t = k;
        }
    }

    private void relayoutByOccupiedCells(){
        long t = SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < mHCells; i++) {
            for (int j = 0; j < mVCells; j++) {
                View v = mOccupiedCell[i][j];
                if (v != null) {
                    LayoutParams layoutparams = (LayoutParams)v.getLayoutParams();
                    if (layoutparams.accessTag < t) {
                        ItemInfo itemInfo = (ItemInfo)v.getTag();
                        itemInfo.cellX = i;
                        itemInfo.cellY = j;
                        layoutparams.cellX = i;
                        layoutparams.cellY = j;
                        layoutparams.accessTag = t;
                    }
                }
            }
        }
        
        requestLayout();
        return;
    }
    
    
    private void rollbackLayout() {
        if (mLayoutBackupValid && copyOccupiedCells(mOccupiedCellBak, mOccupiedCell))
            relayoutByOccupiedCells();
    }

    private void saveCurrentLayout() {
        long t = SystemClock.currentThreadTimeMillis();;
        ArrayList<ContentProviderOperation> arraylist = new ArrayList<ContentProviderOperation>();
        mEmptyCellNumber = 0;
        
        for (int i = 0; i < mHCells; i++) {
            for (int j = 0; j < mVCells; j++) {
                View view = mOccupiedCell[i][j];
                if (view != null) {
                    LayoutParams layoutparams = (LayoutParams)view.getLayoutParams();
                    if (layoutparams.accessTag < t) {
                        layoutparams.accessTag = t;
                        if (view != mOccupiedCellBak[i][j]) {
                            ItemInfo iteminfo = (ItemInfo)view.getTag();
                            iteminfo.cellX = i;
                            iteminfo.cellY = j;
                            arraylist.add(LauncherModel.getMoveItemOperation(
                                    iteminfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, 
                                    getScreenId(), i, j));
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
    
    void cellToPoint(int i, int j, int ai[]) {
        ai[0] = mPaddingLeft + i * (mCellWidth + mWidthGap);
        ai[1] = mPaddingTop + j * (mCellHeight + mHeightGap);
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
    
    int[] findNearestVacantArea(int i, int j, int k, int l, boolean flag) {
        if (!flag && k * l > mEmptyCellNumber) {
            return null;
        }
        
        double d = 1.7976931348623157E+308D;
        int ai[] = mCellXY;
        int pos[] = new int[2];
        
        for (int j1 = mHCells - k; j1 > 0; j1--){
            for (int i1 = mVCells - l; i1 > 0; i1--)
            {
                cellToPoint(j1, i1, ai);
                double d1 = Math.pow(ai[0] - i, 2D) + Math.pow(ai[1] - j, 2D);
                if (d1 < d && (flag || !isCellOccupied(j1, i1, k, l))) {
                    d = d1;
                    pos[0] = j1;
                    pos[1] = i1;
                }
            }
        }
        
        if (d >= 1.7976931348623157E+308D){
            pos = null;
        }
        
        return pos;

    }

    int[] findNearestVacantAreaByCellPos(int i, int j, int k, int l, boolean flag)
    {
        cellToPoint(i, j, mCellXY);
        return findNearestVacantArea(mCellXY[0], mCellXY[1], k, l, flag);
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
    
    boolean isCellOccupied(int i, int j, int k, int l) {
        boolean flag;
        int i1; 
        for (i1 = 0; i1 < k; i1++) {
            for (int j1 = 0; j1 < l; j1++) {
                if (mOccupiedCell[i + i1][j + j1] != null)
                    break;
            }   
        }
        if (i1 >= k) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
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

    void onDragOver(DropTarget.DragObject dragobject) {
        int j = dragobject.dragInfo.spanX;
        int i = dragobject.dragInfo.spanY;
        int ai[] = findNearestVacantArea(dragobject.x - dragobject.xOffset, dragobject.y - dragobject.yOffset, j, i, false);
        if (ai != null) {
            removeView(mCellBackground);
            if (mLastCoveringView == null) {
                LayoutParams layoutparams = (LayoutParams)mCellBackground.getLayoutParams();
                layoutparams.cellX = ai[0];
                layoutparams.cellY = ai[1];
                layoutparams.cellHSpan = j;
                layoutparams.cellVSpan = i;
                addView(mCellBackground, 0, layoutparams);
                if (dragobject.dragSource != mLauncher.getWorkspace()) {
                    dragobject.dragInfo.cellX = ai[0];
                    dragobject.dragInfo.cellY = ai[1];
                }
            }
        }
        if (!(dragobject.dragInfo instanceof LauncherAppWidgetProviderInfo)) {
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
                    mTmpDragPos.stayType = 0;
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
            if (!mLastDragPos.equal(mTmpDragPos)) {
                mLastDragPos.set(mTmpDragPos);
                getHandler().removeCallbacks(mStayConfirm);
                mStayConfirm.lastDragObject = dragobject;
                StayConfirm stayconfirm = mStayConfirm;
                long l;
                if (mLastDragPos.stayType != 2) {
                    l = 150L;
                } else {
                    if (!(view instanceof FolderIcon));
                    l = 100;
                }
                postDelayed(stayconfirm, l);
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
                            itemInfo.container = -100L;
                            LayoutParams layoutParams = (LayoutParams)view.getLayoutParams();
                            layoutParams.cellX = ai[0];
                            layoutParams.cellY = ai[1];
                            layoutParams.isDragging = false;
                            layoutParams.dropped = flag;
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
    
    void pointToCell(int i, int j, int cellXY[]) {
        cellXY[0] = (i - mPaddingLeft) / (mCellWidth + mWidthGap);
        cellXY[1] = (j - mPaddingTop) / (mCellHeight + mHeightGap);
        cellXY[0] = Math.max(0, Math.min(cellXY[0], mHCells - 1));
        cellXY[1] = Math.max(0, Math.min(cellXY[1], mVCells - 1));
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