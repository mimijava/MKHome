package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    Workspace.java
 * 创建时间：    2013
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: WORKSPACE文件
 * ====================================================================================
 */
import java.util.ArrayList;
import java.util.HashMap;

import cn.minking.launcher.AllAppsList.RemoveInfo;
import cn.minking.launcher.gadget.Gadget;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class Workspace extends DragableScreenView 
    implements SensorEventListener, DragSource, DropTarget{

    private class WorkspaceThumbnailViewAdapter extends ThumbnailViewAdapter {

        private final View.OnClickListener DELETE_SCREEN_HANDLER = 
                new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (((ThumbnailScreen)mThumbnailView.getCurrentScreen()).isMovingAnimationStarted()){
                    long l = ((Long)v.getTag()).longValue();
                    int j = (mScreenIdMap.get(l)).intValue();
                    if (mPositionMapping == null) {
                        
                    }
                    int i = mPositionMapping.length - 1;
                    while (i >= 0){
                        if (j == mPositionMapping[i]){
                            break;
                        }
                        i--;
                    }
                    j = i;
                    deleteScreen(l);
                    notifyDataSetChanged();
                    mThumbnailView.startDeletedAnimation(j);
                }
            }
        };
        
        private final Animation.AnimationListener ENTER_PREVIEW_ANIMATION_LISTENER =
                new Animation.AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                setTouchState(null, 6);
                setIndicatorBarVisibility(View.GONE);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.INVISIBLE);
                setTouchState(null, 0);
            }
        };
        
        private final Animation.AnimationListener EXIT_PREVIEW_ANIMATION_LISTENER = 
                new Animation.AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                setTouchState(null, 7);
                setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                mThumbnailView.setVisibility(View.INVISIBLE);
                setTouchState(null, 0);
                setIndicatorBarVisibility(0);
            }
        };
        
        private final View.OnClickListener HOME_MARK_CLICK_HANDLER = 
                new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (zDefaultScreenButton != v) {
                    long l = ((Long)v.getTag()).longValue();
                    mDefaultScreenId = l;
                    android.content.SharedPreferences.Editor editor = 
                            PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                    editor.putLong("pref_default_screen", l);
                    editor.commit();
                    if (zDefaultScreenButton != null){
                        zDefaultScreenButton.setImageResource(R.drawable.home_button_sethome_off);
                    }
                    zDefaultScreenButton = (ImageView)v;
                    zDefaultScreenButton.setImageResource(R.drawable.home_button_sethome_on);
                }
            }
        };
        
        private final View.OnClickListener NEW_SCREEN_HANDLER = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                insertNewScreen(getScreenCount());
                notifyDataSetChanged();
            }
        };
        
        private final View.OnClickListener THUMBNAIL_CLICK_HANDLER = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                exitPreview(((Long)v.getTag()).longValue());
            }
        };
        
        private Canvas mThumbnailCanvas;
        private ImageView zDefaultScreenButton;
        private ImageView zNewScreenView;
        
        private Bitmap createThumbnail(View view, ImageView imageview, Bitmap bitmap) {
            int width = imageview.getBackground().getIntrinsicWidth() 
                    - imageview.getPaddingLeft() - imageview.getPaddingRight();
            int height = imageview.getBackground().getIntrinsicHeight() 
                    - imageview.getPaddingTop() - imageview.getPaddingBottom();
            float f = (float)width / (float)view.getWidth();
            if (bitmap != null) {
                bitmap.eraseColor(0);
            } else {
                bitmap = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
            }
            mThumbnailCanvas.save();
            mThumbnailCanvas.setBitmap(bitmap);
            mThumbnailCanvas.scale(f, f);
            view.draw(mThumbnailCanvas);
            mThumbnailCanvas.restore();
            return bitmap;
        }
        
        private void exitPreview(long l) {
            setTouchState(null, 7);
            setCurrentScreenById(l);
            mLauncher.showPreview(false);
        }

        public void onEndDragging() {
            zNewScreenView.setImageResource(R.drawable.thumbnail_new_screen);
        }

        public void onStartDragging(int i) {
            performHapticFeedback(0, 1);
            if (i == getScreenCount())
                zNewScreenView.setImageResource(R.drawable.thumbnail_new_screen_p);
        }

        public void onThumbnailClick(int i) {
            exitPreview(getCellLayout(i).getScreenId());
        }

        public void onThumbnailPositionChanged(int ai[]) {
            int j = ai.length;
            if (mPositionMapping == null || mPositionMapping.length != j){
                mPositionMapping = new int[j];
            }
            for (int i = 0; i < j; i++) {
                mPositionMapping[i] = ai[i];    
            }
            if (ai[j - 1] != j - 1){
                NEW_SCREEN_HANDLER.onClick(null);
            }
        }


        public WorkspaceThumbnailViewAdapter(Context context) {
            super(context);
            mThumbnailCanvas = new Canvas();
        }
        
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return getScreenCount() + 1;
        }

        @Override
        public View getItem(int i) {
            // TODO Auto-generated method stub
            return getScreen(i);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (position != getScreenCount()) {
                CellScreen cellscreen = getCellScreen(position);
                CellLayout cellLayout = cellscreen.getCellLayout();
                if (convertView != null) {
                    view = (ImageView)convertView.findViewById(R.id.thumbnail);
                } else {
                    convertView = mInflater.inflate(R.layout.workspace_preview_item, null);
                    view = (ImageView)convertView.findViewById(R.id.thumbnail);
                    view.setTag(Long.valueOf(cellLayout.getScreenId()));
                    view.setOnClickListener(THUMBNAIL_CLICK_HANDLER);
                    ImageView imageview = (ImageView)convertView.findViewById(R.id.home_mark);
                    imageview.setTag(Long.valueOf(cellLayout.getScreenId()));
                    imageview.setOnClickListener(HOME_MARK_CLICK_HANDLER);
                    if (cellLayout.getChildCount() == 0) {
                        ImageView imageviewDelete = (ImageView)convertView.findViewById(R.id.delete);
                        imageviewDelete.setTag(Long.valueOf(cellLayout.getScreenId()));
                        imageviewDelete.setVisibility(View.VISIBLE);
                        imageviewDelete.setImageResource(R.drawable.delete_screen_btn);
                        imageviewDelete.setOnClickListener(DELETE_SCREEN_HANDLER);
                    }
                }
                Drawable drawable;
                Bitmap bitmap;
                if (position != getCurrentScreenIndex()) {
                    drawable = getResources().getDrawable(R.drawable.thumbnail_bg);
                } else {
                    drawable = getResources().getDrawable(R.drawable.thumbnail_bg_current);
                }
                view.setBackgroundDrawable(drawable);
                bitmap = (Bitmap)cellscreen.getTag(R.id.celllayout_thumbnail_for_workspace_preview);
                if (bitmap == null || ((Boolean)cellscreen.getTag(R.id.celllayout_thumbnail_for_workspace_preview_dirty)).booleanValue()){
                    bitmap = createThumbnail(cellscreen, (ImageView)view, bitmap);
                    cellscreen.setTag(R.id.celllayout_thumbnail_for_workspace_preview, bitmap);
                    cellscreen.setTag(R.id.celllayout_thumbnail_for_workspace_preview_dirty, Boolean.valueOf(false));
                }
                ((ImageView)view).setImageBitmap(bitmap);
                view = (ImageView)convertView.findViewById(R.id.home_mark);
                if (position != getDefaultScreenIndex()) {
                    ((ImageView)view).setImageResource(R.drawable.home_button_sethome_off);
                } else {
                    zDefaultScreenButton = (ImageView)view;
                    ((ImageView)view).setImageResource(R.drawable.home_button_sethome_on);
                }
                view = convertView;
            } else {
                if (zNewScreenView == null) {
                    zNewScreenView = new ImageView(mContext);
                    zNewScreenView.setImageResource(R.drawable.thumbnail_new_screen);
                    zNewScreenView.setOnClickListener(NEW_SCREEN_HANDLER);
                }
                view = zNewScreenView;
            }
            return view;
        
        }
        
        public Animation.AnimationListener getEnterAnimationListener() {
            return ENTER_PREVIEW_ANIMATION_LISTENER;
        }

        public Animation.AnimationListener getExitAnimationListener() {
            return EXIT_PREVIEW_ANIMATION_LISTENER;
        }
        
        public int getFocusedItemPosition() {
            return getCurrentScreenIndex();
        }
    }
    
    private final static String TAG = "MKHome.Workspace";
    private final static Boolean LOGD = true;
    private Camera mCamera;
    private long mDefaultScreenId;
    private DragController mDragController;
    private CellLayout.CellInfo mDragInfo;
    private float mDragViewVisualCenter[];
    private boolean mEditingModeAnimating;
    private CellScreen mEditingNewScreenLeft;
    private CellScreen mEditingNewScreenRight;
    private boolean mEditingScreenChanging;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private boolean mInDraggingMode;
    private int mInEditingMode;
    private final LayoutInflater mInflater;
    private float mInitThreePinchSize;
    private long mLastDragScreenID;
    private long mLastShakeTime;
    private float mLastShakeX;
    private int mLastTouchPointerCount;
    private Launcher mLauncher;
    private long mNewScreenId;
    private int mOldTransitionType;
    private int mPositionMapping[];
    private int mPreviousScreen;
    private ContentResolver mResolver;
    private LongSparseArray<Integer> mScreenIdMap;
    private ArrayList<Long> mScreenIds;
    private int mShakeCounter;
    private boolean mShowEditingIndicator;
    private boolean mSkipDrawingChild;
    private Animation mSlideBarEditingEnter;
    private Animation mSlideBarEditingExit;
    private SurfaceView mSurfaceViewForFpsAccelerate;
    private int mTempCell[];
    private WorkspaceThumbnailView mThumbnailView;
    private ThumbnailViewAdapter mThumbnailViewAdapter;
    private final WallpaperManager mWallpaperManager;
    private Context mContext;
    
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPreviousScreen = -1;
        mLastDragScreenID = -1L;
        mTempCell = new int[2];
        mDragViewVisualCenter = new float[2];
        mSkipDrawingChild = true;
        mInEditingMode = 7;
        mShowEditingIndicator = false;
        mInDraggingMode = false;
        mEditingModeAnimating = false;
        mEditingScreenChanging = false;
        mNewScreenId = -1L;
        mLastTouchPointerCount = 0;
        mInitThreePinchSize = 0F;
        mScreenIds = new ArrayList<Long>();
        mScreenIdMap = new LongSparseArray<Integer>();
        mCamera = new Camera();
        mLastShakeTime = -1L;
        mLastShakeX = 0F;
        mShakeCounter = -1;
        mContext = context;
        mResolver = context.getContentResolver();
        mWallpaperManager = WallpaperManager.getInstance(context);
        mInflater = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        Resources localResources = getResources();
        String str = localResources.getString(R.string.home_indicator);
        FrameLayout.LayoutParams localLayoutParams 
            = new FrameLayout.LayoutParams(0, localResources.getDimensionPixelSize(R.dimen.slide_bar_height));
        if (!str.equals("bottom_point")) {
            if (!str.equals("top_point")) {
                if (str.equals("slider")) {
                    localLayoutParams.width = -1;
                    localLayoutParams.gravity = Gravity.BOTTOM;
                    setSlideBarPosition(localLayoutParams);
                    mShowEditingIndicator = false;
                }
            }else {
                localLayoutParams.width = -2;
                localLayoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                localLayoutParams.topMargin = localResources.getDimensionPixelSize(R.dimen.status_bar_height);
                setSeekBarPosition(localLayoutParams);
                mShowEditingIndicator = false;
            }
        }else {
            localLayoutParams.width = -2;
            localLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            setSeekPointResource(R.drawable.workspace_seekpoint);
            setSeekBarPosition(localLayoutParams);
            mShowEditingIndicator = true;
        }
        setAnimationCacheEnabled(false);
        setMaximumSnapVelocity(6000);
        setClipChildren(false);
    }

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    private void delayedShowPreview(final boolean show) {
        if (!mLauncher.isWorkspaceLocked() && show != mThumbnailView.isShowing()) {
            if (!show) {
                if (mPositionMapping != null){
                    reorderScreens();
                }
            } else {
                mPositionMapping = null;
                mThumbnailViewAdapter.notifyDataSetChanged();
                mThumbnailView.setVisibility(View.VISIBLE);
                mPreviousScreen = mCurrentScreen;
            }
            mThumbnailView.show(show);
        }
    }

    private int getDefaultScreenIndex() {
        return Math.max(0, Math.min(getScreenIndexById(mDefaultScreenId), getScreenCount() - 1));
    }

    private float[] getDragViewVisualCenter(int i, int j, int k, int l, 
            DragView dragview, float af[]) {
        if (af == null){
            af = new float[2];
        }
        int i1 = i + getResources().getDimensionPixelSize(R.dimen.dragViewOffsetX);
        int j1 = j + getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);
        i1 -= k;
        j1 -= l;
        af[0] = i1 + dragview.getDragRegion().width() / 2;
        af[1] = j1 + dragview.getDragRegion().height() / 2;
        return af;
    }
    
    private float getThreePinchSize(MotionEvent motionevent) {
        return (float)(Math.pow(motionevent.getX(0) - motionevent.getX(1), 2D) 
                + Math.pow(motionevent.getY(0) - motionevent.getY(1), 2D) 
                + Math.pow(motionevent.getX(1) - motionevent.getX(2), 2D) 
                + Math.pow(motionevent.getY(1) - motionevent.getY(2), 2D) 
                + Math.pow(motionevent.getX(2) - motionevent.getX(0), 2D) 
                + Math.pow(motionevent.getY(2) - motionevent.getY(0), 2D));
    }
    
    private boolean isDropAllow(DropTarget.DragObject dragobject) {
        return mScroller.isFinished();
    }
    
    private boolean isInNormalEditingMode() {
        boolean flag;
        if (mInEditingMode != 8){
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    private boolean isInQuickEditingMode() {
        boolean flag;
        if (mInEditingMode != 9){
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    private void registAccelerometer() {
        SensorManager sensormanager = (SensorManager)getContext().getSystemService("sensor");
        if (sensormanager != null) {
            sensormanager.registerListener(this, sensormanager.getDefaultSensor(1), 2);
        }
    }
    
    private void setEditModeIfNeeded() {
        if (isInNormalEditingMode()){
            setEditMode(mInEditingMode, false);
        }
    }
    
    public void setEditMode(int i, boolean flag) {
        boolean flag4 = true;
        boolean flag2 = isInNormalEditingMode();
        mInEditingMode = i;
        boolean flag1 = isInNormalEditingMode();
        boolean flag3;
        if (flag2 || !flag1) {
            flag3 = false;
        } else {
            flag3 = flag4;
        }
        if (!flag2 || !flag1) {
            flag2 = false;
        } else {
            flag2 = flag4;
        }
        boolean bEditingModeAnim;
        if (flag || flag2) {
            bEditingModeAnim = false;
        } else {
            bEditingModeAnim = flag4;
        }
        mEditingModeAnimating = bEditingModeAnim;
        
        if (!flag) {
            setupEditingScreen(flag1, flag3);
            if (!flag2) {
                if (flag1) {
                    flag4 = false;
                }
                mSkipDrawingChild = flag4;
                if (!mShowEditingIndicator || mScreenSeekBar == null) {
                    
                    if (!flag1) {
                        setIndicatorBarVisibility(View.VISIBLE);
                    } else {
                        setIndicatorBarVisibility(View.INVISIBLE);
                    }
                    
                    if (mSlideBar != null) {
                        ScreenView.SlideBar slidebar = mSlideBar;
                        Animation animation1;
                        if (!flag1) {
                            animation1 = mFadeIn;
                        } else {
                            animation1 = mFadeOut;
                        }
                        slidebar.startAnimation(animation1);
                    }
                    if (mScreenSeekBar != null) {
                        ScreenView.SeekBarIndicator seekbarindicator1 = mScreenSeekBar;
                        Animation animation;
                        if (!flag1) {
                            animation = mFadeIn;
                        } else {
                            animation = mFadeOut;
                        }
                        seekbarindicator1.startAnimation(animation);
                    }
                } else {
                    ScreenView.SeekBarIndicator seekbarindicator = mScreenSeekBar;
                    Animation animation2;
                    if (!flag1) {
                        animation2 = mSlideBarEditingExit;
                    } else {
                        animation2 = mSlideBarEditingEnter;
                    }
                    seekbarindicator.startAnimation(animation2);
                }
                if (!flag3) {
                    unRegistAccelerometer();
                } else {
                    mOldTransitionType = getScreenTransitionType();
                    setOvershootTension(0F);
                    setScreenSnapDuration(180);
                    setScreenTransitionType(9);
                    registAccelerometer();
                }
            }
        }
        for (int j = 0; j < getScreenCount(); j++) {
            CellScreen cellscreen = getCellScreen(j);
            if (cellscreen != null)
                if (!flag) {
                    int k;
                    if (flag2) {
                        k = 0x80000000;
                    } else {
                        k = j - getCurrentScreenIndex();
                    }
                    cellscreen.setEditMode(flag1, k);
                } else {
                    cellscreen.onQuickEditingModeChanged(isInQuickEditingMode());
                }
        }
    }
    
    private void setupEditingScreen(boolean flag, boolean flag1) {
        mEditingScreenChanging = true;
        if (!flag) {
            removeScreen(0);
            setCurrentScreen(-1 + getCurrentScreenIndex());
            removeScreen(-1 + getScreenCount());
        } else {
            addView(mEditingNewScreenLeft, 0);
            addView(mEditingNewScreenRight, getScreenCount());
            if (flag1) {
                setCurrentScreen(1 + getCurrentScreenIndex());
            }
        }
        mEditingScreenChanging = false;
    }

    private void updateWallpaperOffset() {
        if (getScreenCount() > 0) {
            if (getTouchState() != 7) {
                int i = getScreen(getScreenCount() - 1).getRight();
                int j = getWidth();
                int offset;
                if (!isInNormalEditingMode()) {
                    offset = 1;
                } else {
                    offset = 3;
                }
                updateWallpaperOffset(i - offset * j);
            } else {
                updateWallpaperOffsetDuringSwitchingPreview();
            }
        }
    }

    private void updateWallpaperOffset(int i) {
        if (getWindowToken() != null) {
            float f;
            if (getScreenCount() != 1) {
                f = 1F / (float)(-1 + getScreenCount());
            } else {
                f = 0F;
            }
            float f1;
            if (getScreenCount() != 1) {
                f1 = mScrollX;
                int j;
                if (!isInNormalEditingMode()) {
                    j = 0;
                } else {
                    j = getWidth();
                }
                f1 = Math.max(0F, Math.min((float)(f1 - j) / (float)i, 1F));
            } else {
                f1 = 0F;
            }
            mLauncher.updateWallpaperOffset(f, 0F, f1, 0F);
        }
    }

    public void addFocusables(ArrayList<View> arraylist, int i, int j) {
        if (mLauncher.getCurrentOpenedFolder() == null && getScreenCount() > 0) {
            getScreen(mCurrentScreen).addFocusables(arraylist, i);
            View view = null;
            if (i != 17) {
                if (i == 66) {
                    view = getScreen(1 + mCurrentScreen);
                }
            } else {
                view = getScreen(mCurrentScreen - 1);
            }
            if (view != null){
                view.addFocusables(arraylist, i);
            }
        }
    }

    /**
     * 功能： 将VIEW添加到桌面对应ID的屏幕中
     * @param view
     * @param screen    ： 屏幕ID
     * @param cx        ： 处于屏幕的横向坐标
     * @param cy        ：处于屏幕的纵向坐标
     * @param sx        ：横向所占空间大小
     * @param sy        ： 纵向所占空间大小
     * @param flag
     */
    void addInCurrentScreen(View view, int cx, int cy, int sx, int sy, boolean flag) {
        addInScreen(view, getScreenIdByIndex(mCurrentScreen), cx, cy, sx, sy, flag);
    }

    public void addInScreen(View view, long screen, int cx, int cy, int sx, int sy) {
        addInScreen(view, screen, cx, cy, sx, sy, false);
    }
    
    public void addInScreen(View view, long screen, int cx, int cy, int sx, int sy, 
            boolean flag) {
        // 根据screen id得到屏幕在桌面的索引值
        int index = getScreenIndexById(screen);
        int iFlag = 0;
        if (index < 0) {
            loadScreens(false);
            index = getScreenIndexById(screen);
            if (index < 0) {
                // 索引值必须大于等于0，否则无效退出
                Log.e(TAG, "The screen must be >= 0; skipping child");
                return;
            }
        }
        
        CellLayout celllayout = getCellLayout(index);
        mLauncher.closeFolder();
        CellLayout.LayoutParams lParams = (CellLayout.LayoutParams)view.getLayoutParams();
        if (lParams != null) {
            lParams.cellX = cx;
            lParams.cellY = cy;
            lParams.cellHSpan = sx;
            lParams.cellVSpan = sy;
        } else {
            lParams = new CellLayout.LayoutParams(cx, cy, sx, sy);
        }
        if (!flag) {
            iFlag = -1;
        }
        
        // 布局添加
        celllayout.addView(view, iFlag, lParams);
        if (mThumbnailView.isShowing()){
            post(new Runnable() {
                @Override
                public void run() {
                    mThumbnailViewAdapter.notifyDataSetChanged();
                }
            });
        }
        
        // 屏幕更新
        getCellScreen(index).updateLayout();
    }
    
    @Override
    public void computeScroll() {
        super.computeScroll();
        updateWallpaperOffset();
    }
    
    /**
     * 功能： 拖动图标后，如果两图标叠加则创建文件夹
     * @param dragShortcutInfo
     * @param orgShortcutInfo
     * @return
     */
    public boolean createUserFolderWithDragOverlap(ShortcutInfo dragShortcutInfo, ShortcutInfo orgShortcutInfo) {
        CellLayout cellLayout = getCellLayout(getScreenIndexById(orgShortcutInfo.screenId));
        if (cellLayout != null) {
            FolderIcon foldericon = null;
            if (cellLayout.getChildVisualPosByTag(orgShortcutInfo, mTempCell)){
                // 在org位置创建文件夹
                foldericon = mLauncher.createNewFolder(orgShortcutInfo.screenId, mTempCell[0], mTempCell[1]);
            }
            boolean flag;
            if (foldericon != null) {
                orgShortcutInfo.cellX = mTempCell[0];
                orgShortcutInfo.cellY = mTempCell[1];
                cellLayout.removeChild(orgShortcutInfo);
                cellLayout.clearBackupLayout();
                addInScreen(foldericon, orgShortcutInfo.screenId, mTempCell[0], mTempCell[1], 1, 1);
                FolderInfo folderInfo = (FolderInfo)foldericon.getTag();
                LauncherModel.addOrMoveItemInDatabase(mLauncher, orgShortcutInfo, folderInfo.id, -1L, 0, 0);
                folderInfo.add(orgShortcutInfo);
                LauncherModel.addOrMoveItemInDatabase(mLauncher, dragShortcutInfo, folderInfo.id, -1L, 1, 0);
                folderInfo.add(dragShortcutInfo);
                folderInfo.notifyDataSetChanged();
                foldericon.onDragExit(null);
                flag = true;
            } else {
                flag = false;
            }
            return flag;
        }
        if (LOGD) {
            StringBuilder stringbuilder = new StringBuilder();
            stringbuilder.append("overItem.screenId=").append(orgShortcutInfo.screenId);
            stringbuilder.append(",currScreenId=").append(getCurrentScreenId());
            stringbuilder.append(",mScreenIdMap=");
            for (int i = 0; i < mScreenIdMap.size(); i++) {
                long l = mScreenIdMap.keyAt(i);
                stringbuilder.append(l).append(":");
                stringbuilder.append(mScreenIdMap.get(l));
            }
            Log.d(TAG, stringbuilder.toString());
        }
        return false;
    }
    
    public void deleteScreen(long l) {
        int screenCount;
        if (!isInNormalEditingMode()) {
            screenCount = getScreenCount();
        } else {
            screenCount = getScreenCount() - 2;
        }
        if (screenCount != 1) {
            if (mDefaultScreenId == l) {
                mDefaultScreenId = 0L;
                android.content.SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                editor.putLong("pref_default_screen", 0L);
                editor.commit();
            }
            if (!isInNormalEditingMode()){
                if (getCurrentScreenIndex() >= -1 + getScreenCount())
                    setCurrentScreen(-1 + getCurrentScreenIndex());
            } else {
                int j = getScreenIndexById(l);
                if (j != -1) {
                    if (j >= getScreenCount() / 2) {
                        int _tmp = j - 1;
                    } else {
                        int _tmp1 = j + 1;
                    }
                    setCurrentScreen(j);
                }
            }
            mResolver.delete(LauncherSettings.Screens.CONTENT_URI, (new StringBuilder()).append("_id=").append(l).toString(), null);
            reorderScreens();
        }
    }
    
    /**
     * 模式： 进入编辑模式
     */
    public void onEditModeEnterComplete() {
        mEditingModeAnimating = false;
        for (int i = 0; i < getScreenCount(); i++) {
            CellScreen cellscreen = getCellScreen(i);
            if (cellscreen != null){
                cellscreen.onEditingAnimationEnterEnd();
            }
        }
    }

    /**
     * 模式： 退出编辑模式
     */
    public void onEditModeExitComplete() {
        mEditingModeAnimating = false;
        setScreenTransitionType(mOldTransitionType);
        for (int i = 0; i < getScreenCount(); i++) {
            CellScreen cellscreen = getCellScreen(i);
            if (cellscreen != null) {
                cellscreen.onEditingAnimationExitEnd();
            }
        }
    }
    
    @Override
    protected void onFinishInflate() {
        mFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        mFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        mSlideBarEditingEnter = AnimationUtils.loadAnimation(getContext(), R.anim.slidebar_editing_enter);
        mSlideBarEditingExit = AnimationUtils.loadAnimation(getContext(), R.anim.slidebar_editing_exit);
        loadScreens(true);
        mEditingNewScreenLeft = (CellScreen)mInflater.inflate(R.layout.cell_screen, this, false);
        mEditingNewScreenLeft.setEditingNewScreenMode();
        mEditingNewScreenRight = (CellScreen)mInflater.inflate(R.layout.cell_screen, this, false);
        mEditingNewScreenRight.setEditingNewScreenMode();
    }

    /**
     * 功能： 读取存储的屏幕信息
     * @param flag
     */
    public void loadScreens(boolean flag){
        long screenId = 0L;
        if (!flag) {
            Log.d(TAG, (new StringBuilder()).append("Screens before reload ").
                    append(mScreenIds).toString());
            screenId = getCurrentScreenId();
            if (screenId == -1L) {
                screenId = mNewScreenId;
                mNewScreenId = -1L;
            }
            mScreenIds.clear();
            mScreenIdMap.clear();
        }
        
        // 从数据库Screens表中读取屏幕信息，将读取的ID放入mScreenIdMap及mScreenIds数组中
        Uri uri = LauncherSettings.Screens.CONTENT_URI;
        String as[] = new String[]{"_id"};
        Cursor cursor = mResolver.query(uri, as, null, null, "screenOrder ASC");
        if (cursor == null) return;
        try {
            while (cursor.moveToNext()){
                long id = cursor.getLong(0);
                mScreenIdMap.put(id, Integer.valueOf(mScreenIds.size()));
                mScreenIds.add(Long.valueOf(id));
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        if(LOGD) {
            Log.d(TAG, (new StringBuilder()).append("Screens loaded ").
                    append(mScreenIds).toString());
        }
        
        // 将读取的屏幕信息添加到布局中
        HashMap<Long, CellScreen> screenHashMap= new HashMap<Long, CellScreen>();
        for (int i = 0; i < getScreenCount(); i++) {
            CellScreen cellscreen = (CellScreen)getScreen(i);
            if (cellscreen != null) {
                cellscreen.getCellLayout().clearAnimation();
                screenHashMap.put(Long.valueOf(cellscreen.getCellLayout().getScreenId()), cellscreen);
            }
        }
        
        removeScreensInLayout(0, getScreenCount());
        
        for (int i = 0; i < mScreenIds.size(); i++) {
            long id = ((Long)mScreenIds.get(i)).longValue();
            CellScreen cellScreen = (CellScreen)screenHashMap.get(Long.valueOf(id));
            if (cellScreen == null) {
                cellScreen = (CellScreen)mInflater.inflate(R.layout.cell_screen, this, false);
                CellLayout celllayout = cellScreen.getCellLayout();
                celllayout.setScreenId(id);
                celllayout.setContainerId(LauncherSettings.Favorites.CONTAINER_DESKTOP);
                celllayout.setOnLongClickListener(mLongClickListener);
            }
            addView(cellScreen, 0);
        }
        
        if (!flag) {
            setEditModeIfNeeded();
            if (((Integer)mScreenIdMap.get(screenId, Integer.valueOf(-1))).intValue() != -1) {
                setCurrentScreenById(screenId);
                getCurrentScreen().requestFocus();
            }
        } else {
            mDefaultScreenId = PreferenceManager.getDefaultSharedPreferences(mContext).
                    getLong("pref_default_screen", 3L);
            setCurrentScreen(getDefaultScreenIndex());
        }
    }
    
    void setThumbnailView(WorkspaceThumbnailView workspacethumbnailview) {
        Drawable drawable = getResources().getDrawable(R.drawable.thumbnail_bg);
        mThumbnailView = workspacethumbnailview;
        mThumbnailView.setThumbnailMeasureSpec(
                MeasureSpec.makeMeasureSpec(drawable.getIntrinsicWidth(), MeasureSpec.EXACTLY), 
                MeasureSpec.makeMeasureSpec(drawable.getIntrinsicHeight(), MeasureSpec.EXACTLY));
        mThumbnailViewAdapter = new WorkspaceThumbnailViewAdapter(mContext);
        mThumbnailView.setAdapter(mThumbnailViewAdapter);
        mThumbnailView.setAnimationDuration(mContext.getResources().getInteger(R.integer.config_animDuration));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDragController.setWindowToken(getWindowToken());
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int point = ev.getPointerCount();
        if (!mEditingModeAnimating && !mThumbnailView.isShowing() 
                && getTouchState() == TOUCH_STATE_REST && 3 == point){
            if (mLastTouchPointerCount == point) {
                if (!mLauncher.isInEditing() && 0.7F * mInitThreePinchSize > getThreePinchSize(ev)) {
                    finishCurrentGesture();
                    mLauncher.showPreview(true);
                }
            } else {
                mInitThreePinchSize = getThreePinchSize(ev);
            }
        }
        mLastTouchPointerCount = point;
        boolean flag;
        if (ev.getAction() != MotionEvent.ACTION_DOWN || !mLauncher.isWorkspaceLocked()){
            flag = super.dispatchTouchEvent(ev);
        } else {
            flag = false;
        }
        return flag;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean flag = false;
        if (!mLauncher.isWorkspaceLocked() && !mLauncher.isFolderShowing()) {
            switch (MotionEvent.ACTION_MASK & ev.getAction()) {
            case MotionEvent.ACTION_MOVE: // '\002'
            default:
                break;

            case MotionEvent.ACTION_UP: // '\001'
            case MotionEvent.ACTION_CANCEL: // '\003'
                if (getTouchState() == TOUCH_STATE_REST
                    && !getCellLayout(mCurrentScreen).lastDownOnOccupiedCell()) {
                    // 按的位置为壁纸位置，没有按到明确的项目目标
                    onWallpaperTap(ev);
                }
                break;
            }
            flag = super.onInterceptTouchEvent(ev);
        }
        return flag;
    }
    
    protected void onWallpaperTap(MotionEvent ev) {
        final int[] position = mTempCell;
        getLocationOnScreen(position);

        int pointerIndex = ev.getActionIndex();
        position[0] += (int) ev.getX(pointerIndex);
        position[1] += (int) ev.getY(pointerIndex);

        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                ev.getAction() == MotionEvent.ACTION_UP
                        ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP,
                position[0], position[1], 0, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mFirstLayout){
            updateWallpaperOffset(View.MeasureSpec.getSize(widthMeasureSpec) * (getScreenCount() - 1));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onPinchOut(ScaleGestureDetector detector) {
        if (mLauncher.getEditingState() == 8) {
            finishCurrentGesture();
            mLauncher.setEditingState(7);
        }
        super.onPinchOut(detector);
    }

    @Override
    public void onPinchIn(ScaleGestureDetector detector) {
        if (!mLauncher.isInEditing()) {
            finishCurrentGesture();
            mLauncher.setEditingState(8);
        }
        super.onPinchIn(detector);
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isInNormalEditingMode()) {
            float f = event.values[0];
            if (mShakeCounter != -1) {
                long l = System.currentTimeMillis();
                long l1 = l - mLastShakeTime;
                if (Math.abs(f - mLastShakeX) <= 5F) {
                    if (l1 > 600L) {
                        mShakeCounter = -1;
                        mLastShakeTime = -1L;
                    }
                } else {
                    if (mLastShakeTime != -1L) {
                        if (l1 <= 300L || l1 >= 600L) {
                            if (l1 > 900L) {
                                mShakeCounter = -1;
                                mLastShakeTime = -1L;
                            }
                        } else {
                            mShakeCounter = 1 + mShakeCounter;
                            mLastShakeTime = l;
                            if (mShakeCounter == 3) {
                                getCurrentCellLayout().alignIconsToTop();
                                mShakeCounter = -1;
                                mLastShakeTime = -1L;
                            }
                        }
                    } else {
                        mShakeCounter = 1 + mShakeCounter;
                        mLastShakeTime = l;
                    }
                }
                mLastShakeX = f;
            } else {
                mLastShakeX = f;
                mShakeCounter = 0;
            }
            return;
        } else {
            throw new AssertionError();
        }
    }

    private void unRegistAccelerometer() {
        SensorManager sensormanager = (SensorManager)getContext().getSystemService("sensor");
        if (sensormanager != null){
            sensormanager.unregisterListener(this);
        }
    }

    public void onDestory() {
        unRegistAccelerometer();
    }
    
    
    @Override
    public void onResume() {
        for (int i = 0; i < getScreenCount(); i++) {
            //getScreen(i).getDisplayList();
        }
    }

    @Override
    public void onSecondaryPointerDown(MotionEvent motionEvent, int point_id) {
        if (!mLauncher.isFolderShowing()){
            super.onSecondaryPointerDown(motionEvent, point_id);
        }
    }

    @Override
    public void onSecondaryPointerMove(MotionEvent motionEvent, int point_id) {
        if (!mLauncher.isFolderShowing()){
            super.onSecondaryPointerMove(motionEvent, point_id);
        }
    }

    @Override
    public void onSecondaryPointerUp(MotionEvent motionEvent, int point_id) {
        if (!mLauncher.isFolderShowing()){
            super.onSecondaryPointerUp(motionEvent, point_id);
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {
        boolean flag = false;
        if (getScreenCount() != 0) {
            View view = mLauncher.getCurrentOpenedFolder();
            int j = 0;
            if (view == null) {
                if (mNextScreen == -1) {
                    j = mCurrentScreen;
                } else {
                    j = mNextScreen;
                }
                flag = getScreen(j).requestFocus(direction, previouslyFocusedRect);
            } else {
                flag = view.requestFocus(direction, previouslyFocusedRect);
            }
        }
        return flag;
    }

    @Override
    protected void setCurrentScreenInner(int cur_screen) {
        // TODO Auto-generated method stub
        super.setCurrentScreenInner(cur_screen);
    }

    @Override
    public void snapToScreen(int screen) {
        int k = Math.max(0, Math.min(screen, getScreenCount() - 1));
        mNextScreen = k;
        View view = getFocusedChild();
        if (view != null && k != mCurrentScreen && view == getScreen(mCurrentScreen)){
            view.clearFocus();
        }
        super.snapToScreen(screen);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int i = -1;
        long l = getDrawingTime();
        if (mSkipDrawingChild && !mEditingModeAnimating) {
            int j = getTouchState();
            boolean flag;
            if (mNextScreen != i || j != 0 && j != 6 && j != 7) {
                flag = false;
            } else {
                flag = true;
            }
            if (!flag) {
                float f = (float)mScrollX / (float)getWidth();
                if (f >= 0F){
                    i = (int)f;
                }
                int k = i + 1;
                if (i >= 0 && i < getScreenCount()) {
                    drawChild(canvas, getScreen(i), l);
                }
                if (f != (float)i && k >= 0 && k < getScreenCount()) {
                    drawChild(canvas, getScreen(k), l);
                }
            } else {
                drawChild(canvas, getScreen(mCurrentScreen), l);
            }
            i = getChildCount();
            for (int i1 = getScreenCount(); i1 < i; i1++) {
                View view = getChildAt(i1);
                if (view.getVisibility() == 0) {
                    drawChild(canvas, view, l);
                }
            }
        } else {
            super.dispatchDraw(canvas);
        }
    }
    
    @Override
    protected void finishCurrentGesture() {
        // TODO Auto-generated method stub
        super.finishCurrentGesture();
    }

    @Override
    public void focusableViewAvailable(View v) {
        View view2 = getScreen(mCurrentScreen);
        for (View view1 = v; view1 != view2; view1 = (View)view1.getParent()){
            if (view1 == this || !(view1.getParent() instanceof View)){
                return;
            }
        }
        super.focusableViewAvailable(v);
    }

    public CellLayout getCellLayout(int index) {
        CellScreen cellScreen = getCellScreen(index);
        CellLayout cellLayout;
        if (cellScreen == null) {
            cellLayout = null;
        } else {
            cellLayout = cellScreen.getCellLayout();
        }
        return cellLayout;
    }

    public CellScreen getCellScreen(int index) {
        CellScreen cellscreen;
        if (!(getScreen(index) instanceof CellScreen)) {
            cellscreen = null;
        } else {
            cellscreen = (CellScreen)getScreen(index);
        }
        return cellscreen;
    }
    
    public int getCellScreenLayerTypeAndUpdateSurface() {
        int mode = View.LAYER_TYPE_HARDWARE;
        switch (getScreenTransitionType()) {
        case 3: // '\003'
        default:
            if (mSurfaceViewForFpsAccelerate != null && mSurfaceViewForFpsAccelerate.getParent() != null){
                mLauncher.getDragLayer().removeView(mSurfaceViewForFpsAccelerate);
            }
            break;

        case 2: // '\002'
        case 4: // '\004'
        case 5: // '\005'
        case 6: // '\006'
        case 7: // '\007'
        case 8: // '\b'
            if (mSurfaceViewForFpsAccelerate == null){
                mSurfaceViewForFpsAccelerate = new SurfaceView(mContext);
            }
            if (mSurfaceViewForFpsAccelerate.getParent() == null){
                mLauncher.getDragLayer().addView(mSurfaceViewForFpsAccelerate, 1, 1);
            }
            break;
        }
        return mode;
    }

    protected boolean getChildStaticTransformationByScreen(View view, Transformation transformation, float f) {
        Matrix matrix = transformation.getMatrix();
        float f3 = view.getMeasuredWidth();
        float f4 = view.getMeasuredHeight();
        float f2 = f4 / 2F;
        float f1 = 0.131F * f3;
        boolean flag = false;
        f4 *= 0.5395F;
        if (Math.abs(f) <= 1.5F) {
            mCamera.save();
            if (f > 0F){
                mCamera.translate(f3, 0F, 0F);
            }
            mCamera.rotateY(10F * f);
            if (f > 0F) {
                mCamera.translate(-f3, 0F, 0F);
            }
            mCamera.getMatrix(matrix);
            mCamera.restore();
            matrix.preTranslate(0F, -f2);
            matrix.postTranslate(f1 * f + f1 / 2F, f4);
            matrix.postScale(0.88F, 0.88F, 0.5F, 0F);
            flag = true;
        } 
        return flag;
    }
    
    public void setDragController(DragController dragcontroller) {
        mDragController = dragcontroller;
    }
    
    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }
    
    public void onStart() {
        int i = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(mLauncher).getString("pref_key_transformation_type", String.valueOf(1))).intValue();
        if (!isInNormalEditingMode()) {
            setScreenTransitionType(i);
        }else {
            mOldTransitionType = i;
        }
    }
    
    /**
     * 功能： 返回当前屏幕的ID
     * @return
     */
    public long getCurrentScreenId() {
        return getCurrentCellLayout().getScreenId();
    }
    
    /**
     * 功能： 根据Index返回当前屏幕
     * @return
     */
    long getScreenIdByIndex(int i) {
        long l = -1L;
        if (!isInNormalEditingMode())
        {
            if (i < mScreenIds.size())
                l = ((Long)mScreenIds.get(i)).longValue();
        } else
        if (i <= mScreenIds.size() && i != 0)
            l = ((Long)mScreenIds.get(i - 1)).longValue();
        return l;
    }

    /**
     * 功能： 返回当前屏幕
     * @return
     */
    public CellScreen getCurrentCellScreen(){
        return (CellScreen)getCurrentScreen();
    }
    
    /**
     * 描述： 返回当前屏幕的布局层
     * @return
     */
    public CellLayout getCurrentCellLayout(){
        return getCurrentCellScreen().getCellLayout();
    }
    
    void reorderScreens(){
        
    }
    
    public void showPreview(final boolean show) {
        post(new Runnable() {
            @Override
            public void run() {
                delayedShowPreview(show);
            }
        });
    }
    
    public void insertNewScreen(int i){
        ContentValues contentvalues = new ContentValues();
        contentvalues.put("screenOrder", Integer.valueOf(0));
        Long screen = Long.valueOf(mResolver.insert(LauncherSettings.Screens.CONTENT_URI, contentvalues).getLastPathSegment());
        if (i == -1) {
            int j = getCurrentScreenIndex();
            int k;
            if (!isInNormalEditingMode()){
                k = 0;
            } else {
                k = 1;
            }
            i = Math.max(0, j - k);
        }
        mNewScreenId = screen.longValue();
        mScreenIds.add(i, screen);
        reorderScreens();
    }
    
    /**
     * 描述： 不处于编辑动画的显示过程中
     * @return
     */
    public boolean inEditingModeAnimating() {
        return mEditingModeAnimating;
    }

    boolean isDefaultScreenShowing() {
        boolean flag = false;
        if (!isScrolling() && mCurrentScreen == getDefaultScreenIndex()) {
            flag = true;
        }
        return flag;
    }

    boolean isScrolling() {
        boolean flag;
        if (mScroller.isFinished()) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }

    public boolean isTouchStateNotInScroll() {
        boolean flag;
        if (getTouchState() != 0 && getTouchState() != 4) {
            flag = false;
        } else {
            flag = true;
        }
        return flag;
    }
    
    void moveToDefaultScreen(boolean flag) {
        int i = getDefaultScreenIndex();
        if (!flag) {
            setCurrentScreen(i);
        } else {
            snapToScreen(i);
        }
        getScreen(i).requestFocus();
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragobject) {
        boolean flag;
        if (isDropAllow(dragobject)) {
            flag = true;
        } else {
            mLauncher.showError(R.string.failed_to_drop);
            flag = false;
        }
        return flag;
    }
    
    @Override
    public DropTarget getDropTargetDelegate(DragObject dragobject) {
        return null;
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public void onDragEnter(DragObject dragobject) {
        mInDraggingMode = true;
    }

    @Override
    public void onDragExit(DragObject dragobject) {
        if (mInDraggingMode){
            mInDraggingMode = false;
        }
        mLastDragScreenID = INVALID_SCREEN;
        getCurrentCellScreen().onDragExit(dragobject);
    }

    @Override
    public void onDragOver(DragObject dragobject) {
        if (isDropAllow(dragobject)) {
            CellScreen cellscreen = getCurrentCellScreen();
            CellLayout celllayout = cellscreen.getCellLayout();
            if (mLastDragScreenID != cellscreen.getCellLayout().getScreenId()) {
                if (mLastDragScreenID != INVALID_SCREEN){
                    getCellScreen(getScreenIndexById(mLastDragScreenID)).onDragExit(dragobject);
                }
                cellscreen.onDragEnter(dragobject);
                mLastDragScreenID = celllayout.getScreenId();
            }
            getCurrentCellScreen().onDragOver(dragobject);
        }
    }

    @Override
    public boolean onDrop(DragObject dragobject) {
        getCurrentCellScreen().onDragExit(dragobject);
        mDragViewVisualCenter = getDragViewVisualCenter(dragobject.x, dragobject.y,
                dragobject.xOffset, dragobject.yOffset, dragobject.dragView, mDragViewVisualCenter);
        final CellScreen cellScreen = getCurrentCellScreen();
        if (cellScreen.isEditingNewScreenMode()) {
            insertNewScreen(-1);
        }
        boolean flag;
        if (dragobject.dragSource == this) {
            if (mDragInfo == null) {
                flag = true;
            } else {
                View view = mDragInfo.cell;
                flag = cellScreen.onDrop(dragobject, view);
                if (flag) {
                    if (dragobject.dragInfo.screenId != mDragInfo.screenId) {
                        CellScreen cellscreen = getCellScreen(getScreenIndexById(mDragInfo.screenId));
                        cellscreen.getCellLayout().removeView(view);
                        cellscreen.updateLayout();
                        if (dragobject.dragInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                            cellScreen.getCellLayout().addView(view);
                            cellScreen.updateLayout();
                            if (view instanceof Gadget) {
                                ((Gadget)view).onResume();
                            }
                        }
                    }
                    if (!isInNormalEditingMode() && (view instanceof LauncherAppWidgetHostView)) {
                        final CellLayout cellLayout = cellScreen.getCellLayout();
                        final ItemInfo info = dragobject.dragInfo;
                        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView)view;
                        if (hostView.getAppWidgetInfo().resizeMode != 0) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    mLauncher.getDragLayer().addResizeFrame(info, hostView, cellLayout);
                                }

                            });
                        }
                    }
                } else {
                    mLauncher.showError(R.string.failed_to_drop);
                }
                if (!dragobject.dragView.hasDrawn() 
                        || dragobject.dragInfo.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    mLauncher.getDragLayer().animateViewIntoPosition(dragobject.dragView, view, 300, null);
                }
            }
        } else {
            flag = onDropExternal(cellScreen.getCellLayout(), dragobject);
        }
        return flag;
    }

    private boolean onDropExternal(CellLayout celllayout, DropTarget.DragObject dragobject) {
        return false;
    }
    
    @Override
    public void onDropCompleted(View view, DragObject dragobject, boolean flag) {
        if (!flag) {
            if (mDragInfo != null) {
                getCellLayout(getScreenIndexById(mDragInfo.screenId)).onDropAborted(mDragInfo.cell);
            }
            if (view == this && getCurrentScreenId() != mDragInfo.screenId) {
                mLauncher.showError(R.string.failed_to_drop);
            }
        } else {
            if (view != this && mDragInfo != null) {
                getCellLayout(getScreenIndexById(mDragInfo.screenId)).removeView(mDragInfo.cell);
                getCurrentCellScreen().updateLayout();
            }
        }
        if (dragobject.cancelled && mDragInfo.cell != null){
            if (!dragobject.dragView.hasDrawn()){
                mDragInfo.cell.setVisibility(View.VISIBLE);
            } else {
                mLauncher.getDragLayer().animateViewIntoPosition(dragobject.dragView, mDragInfo.cell, 300, null);
            }
        }
        mDragController.setTouchTranslator(null);
        mDragInfo = null;
    }

    /**
     * 功能： 从screen id map中根据id 得到此屏幕的Index
     * @param id
     * @return
     */
    public int getScreenIndexById(long id) {
        int index = (mScreenIdMap.get(id, Integer.valueOf(-1))).intValue();
        if (index != -1) {
            if (isInNormalEditingMode()){
                index++;
            }
        } else {
            index = -1;
        }
        return index;
    }
    
    public void setCurrentScreenById(long id) {
        setCurrentScreen(Math.max(0, getScreenIndexById(id)));
    }
    
    public void removeItems(ArrayList<RemoveInfo> arraylist){
        
    }
    
    /**
     * 功能： 长按进入拖动项目
     * @param cellinfo
     */
    void startDrag(CellLayout.CellInfo cellinfo) {
        View view = cellinfo.cell;
        // 没有文件夹打开
        if (view.isInTouchMode() && !mLauncher.isFolderShowing()) {
            mDragInfo = cellinfo;
            view.clearFocus();
            view.setPressed(false);
            CellLayout celllayout = getCurrentCellLayout();
            mDragInfo.screenId = celllayout.getScreenId();
            celllayout.onDragChild(view);
            mDragController.setTouchTranslator(mLauncher.getTouchTranslator());
            mDragController.startDrag(view, this, (ItemInfo)view.getTag(), DragController.DRAG_ACTION_MOVE);
            invalidate();
        }
    }
    
    public void updateWallpaperOffsetDuringSwitchingPreview() {
        IBinder ibinder = getWindowToken();
        Animation animation = getScreen(mCurrentScreen).getAnimation();
        if (ibinder != null && animation != null && animation.getStartTime() != -1L)
        {
            float f1 = Math.max(0F, Math.min((float)(SystemClock.uptimeMillis() - animation.getStartTime()) / (float)animation.getDuration(), 1F));
            float f;
            if (getScreenCount() != 1){
                f = 1F / (float)(getScreenCount() - 1);
            } else {
                f = 0F;
            }
            if (getScreenCount() != 1) {
                f1 = f * ((float)mPreviousScreen * (1F - f1) + f1 * (float)mCurrentScreen);
            } else {
                f1 = 0F;
            }
            mLauncher.updateWallpaperOffset(f, 0F, Math.max(0F, Math.min(f1, 1F)), 0F);
        }
    }
}
