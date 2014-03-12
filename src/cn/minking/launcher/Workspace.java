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

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
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
import android.widget.FrameLayout;
import android.widget.ImageView;

public class Workspace extends DragableScreenView 
    implements SensorEventListener{

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
        
        public WorkspaceThumbnailViewAdapter(Context context) {
            super(context);
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
            // TODO Auto-generated method stub
            return null;
        }
        
        private void exitPreview(long l) {
            setTouchState(null, 7);
            setCurrentScreenById(l);
            mLauncher.showPreview(false);
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
    private LongSparseArray<Long> mScreenIdMap;
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
        mContext = context;
        mResolver = context.getContentResolver();
        mWallpaperManager = WallpaperManager.getInstance(context);
        mInflater = ((LayoutInflater)context.getSystemService("layout_inflater"));
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

    void onEditModeEnterComplate() {
        mEditingModeAnimating = false;
        int i = 0;
        while(i < getScreenCount()){
            CellScreen cellscreen = getCellScreen(i);
            if (cellscreen != null){
                cellscreen.onEditingAnimationEnterEnd();
            }
            i++;
        }
    }

    void onEditModeExitComplate()
    {
        mEditingModeAnimating = false;
        setScreenTransitionType(mOldTransitionType);
        int i = 0;
        while (i >= getScreenCount()){
            CellScreen cellscreen = getCellScreen(i);
            if (cellscreen != null) {
                cellscreen.onEditingAnimationExitEnd();
            }
            i++;
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

    public void loadScreens(boolean flag){
        
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
        // TODO Auto-generated method stub
        super.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onPinchOut(ScaleGestureDetector detector) {
        // TODO Auto-generated method stub
        super.onPinchOut(detector);
    }

    @Override
    public void onPinchIn(ScaleGestureDetector detector) {
        // TODO Auto-generated method stub
        super.onPinchIn(detector);
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        
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
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {
        // TODO Auto-generated method stub
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    protected void setCurrentScreenInner(int cur_screen) {
        // TODO Auto-generated method stub
        super.setCurrentScreenInner(cur_screen);
    }

    @Override
    public void snapToScreen(int screen) {
        // TODO Auto-generated method stub
        super.snapToScreen(screen);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.dispatchDraw(canvas);
    }
    
    @Override
    protected void finishCurrentGesture() {
        // TODO Auto-generated method stub
        super.finishCurrentGesture();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        return super.dispatchTouchEvent(ev);
    }

    public CellLayout getCellLayout(int i) {
        CellScreen cellScreen = getCellScreen(i);
        CellLayout cellLayout;
        if (cellScreen == null) {
            cellLayout = null;
        } else {
            cellLayout = cellScreen.getCellLayout();
        }
        return cellLayout;
    }

    public CellScreen getCellScreen(int i) {
        CellScreen cellscreen;
        if (!(getScreen(i) instanceof CellScreen)) {
            cellscreen = null;
        } else {
            cellscreen = (CellScreen)getScreen(i);
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

    
    private boolean isInNormalEditingMode() {
        boolean bFlag = false;
        if (mInEditingMode != 8) {
            bFlag = false;
        }else {
            bFlag = true;
        }
        return bFlag;
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
    
    public void deleteScreen(long id){
        
    }
    
    public CellScreen getCurrentCellScreen(){
        return (CellScreen)getCurrentScreen();
    }
    
    void addInScreen(View view, long screenId, int cellX, int cellY, int spanX, int spanY){
        addInScreen(view, screenId, cellX, cellY, spanX, spanY, false);
    }

    void addInScreen(View view, long screenId, int cellX, int cellY, int spanX, int spanY, 
            boolean insert){
        
    }
    
    int getScreenIndexById(long id) {
        int i = (mScreenIdMap.get(id, Long.valueOf(-1))).intValue();
        if (i != -1) {
            if (isInNormalEditingMode())
                i++;
        } else {
            i = -1;
        }
        return i;
    }
    
    public void setCurrentScreenById(long id) {
        setCurrentScreen(Math.max(0, getScreenIndexById(id)));
    }
    
}
