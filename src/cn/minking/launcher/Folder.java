package cn.minking.launcher;

import java.util.Iterator;
import java.util.List;

import cn.minking.launcher.DropTarget.DragObject;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Folder extends LinearLayout
    implements View.OnClickListener, AdapterView.OnItemClickListener, 
    AdapterView.OnItemLongClickListener, DragSource {
    
    private AnimationSet mCloseAnimation;
    private boolean mClosing;
    private View mComfirmBtn;
    protected DropableGridView mContent;
    private String mDefaultFolderName;
    private String mDefaultUnnamedFolderName;
    protected DragController mDragController;
    protected ShortcutInfo mDragItem;
    private int mDragPos;
    private View mDragedView;
    private InputMethodManager mImm;
    protected FolderInfo mInfo;
    private boolean mIsEditing;
    private int mLastAtMostMeasureHeight;
    protected Launcher mLauncher;
    private Runnable mOnFinishClose;
    private AnimationSet mOpenAnimation;
    private EditText mRenameEdit;
    protected TextView mTitleText;
    private boolean requestOpenAnimation;
    private Context mContext;
    
    public Folder(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        mContext = context;
        mInfo = null;
        mOnFinishClose = null;
        mDragPos = -1;
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void finishShow() {
        if (mClosing) {
            setVisibility(View.INVISIBLE);
            clearAnimation();
            if (mOnFinishClose != null) {
                mOnFinishClose.run();
                mOnFinishClose = null;
            }
        }
    }
    
    private CharSequence getEditText(CharSequence charsequence) {
        if (mDefaultFolderName.equals(charsequence)) { 
            charsequence = mDefaultUnnamedFolderName;
        }
        return charsequence;
    }

    private void prepairAnimationSet(AnimationSet animationset) {
        List<Animation> list = animationset.getAnimations();
        Iterator<Animation> iterator = list.iterator();
        while(iterator.hasNext()){
            Animation animation = (Animation)iterator.next();
            if (!(animation instanceof ScaleAnimation)){
                continue;
            }
            list.remove(animation);
        } while (true);
    }
    
    private void showEditPanel(boolean bEdit, boolean bRename)
    {
        boolean flag2 = true;
        if (mIsEditing != bEdit) {
            if (!bEdit) {
                if (bRename){
                    mTitleText.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
                }
                mImm.hideSoftInputFromWindow(getWindowToken(), 0);
                mTitleText.setVisibility(View.VISIBLE);
                mRenameEdit.setVisibility(View.INVISIBLE);
                mComfirmBtn.setVisibility(View.INVISIBLE);
                mLauncher.scrollToDefault();
            } else {
                if (bRename) {
                    mRenameEdit.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
                    mComfirmBtn.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
                }
                mTitleText.setVisibility(View.INVISIBLE);
                mRenameEdit.setVisibility(View.VISIBLE);
                mRenameEdit.selectAll();
                mRenameEdit.requestFocus();
                mComfirmBtn.setVisibility(View.VISIBLE);
                mImm.showSoftInput(mRenameEdit, 0);
            }
            DropableGridView dropablegridview = mContent;
            
            if (!bEdit) {
                dropablegridview.setAlpha(1F);
            } else {
                dropablegridview.setAlpha(0.3F);
            }
            
            if (bEdit){
                dropablegridview.setEnabled(false);
            } else {
                dropablegridview.setEnabled(true);
            }
            
            if (bEdit){
                dropablegridview.setClickable(false);
            } else {
                dropablegridview.setClickable(true);
            }
            
            if (bEdit){
                dropablegridview.setLongClickable(false); 
            }
            
            mIsEditing = bEdit;
        }
    }
    
    public void bind(FolderInfo folderinfo) {
        mInfo = folderinfo;
        updateAppearance();
        if (folderinfo != null) {
            setContentAdapter(folderinfo.getAdapter(mContext));
        } else {
            setContentAdapter(null);
        }
    }
    
    public ShortcutInfo getDragedItem() {
        return mDragItem;
    }

    public FolderInfo getInfo() {
        return mInfo;
    }

    public int getLastAtMostMeasureHeight() {
        return mLastAtMostMeasureHeight;
    }
    
    void onOpen(boolean flag) {
        mClosing = false;
        mInfo.opened = true;
        mContent.requestLayout();
        clearAnimation();
        setVisibility(View.VISIBLE);
        mDragController.addDropTarget(mContent);
        requestFocus();
        if (mInfo != null) {
            mInfo.icon.onOpen();
            requestOpenAnimation = flag;
        }
    }

    public void removeItem(ShortcutInfo shortcutinfo) {
        mInfo.remove(shortcutinfo);
        mInfo.notifyDataSetChanged();
    }

    void setContentAdapter(BaseAdapter baseadapter){
        mContent.setAdapter(baseadapter);
    }

    public void setDragController(DragController dragcontroller) {
        mDragController = dragcontroller;
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    void showOpenAnimation() {
        if (requestOpenAnimation) {
            if (getAnimation() != null) {
                clearAnimation();
            }
            prepairAnimationSet(mOpenAnimation);
            int ai[] = new int[2];
            mInfo.icon.getLocationInWindow(ai);
            mOpenAnimation.addAnimation(new ScaleAnimation(0.6F, 1F, 0.6F, 1F, 
                    (ai[0] + mInfo.icon.getWidth() / 2) - getLeft(), ai[1] - getTop()));
            startAnimation(mOpenAnimation);
            requestOpenAnimation = false;
        }
    }
    
    protected void updateAppearance() {
        CharSequence charsequence = getEditText(mInfo.title);
        if (!mTitleText.getText().equals(charsequence)) {
            mTitleText.setText(charsequence);
            mInfo.icon.setTitle(mInfo.title);
        }
        if (!mRenameEdit.getText().toString().equals(charsequence)) {
            mRenameEdit.setText(charsequence);
        }
    }
    
    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        post(new Runnable() {
            @Override
            public void run() {
                finishShow();
            }
        });
    }

    @Override
    protected void onAnimationStart() {
        // TODO Auto-generated method stub
        super.onAnimationStart();
    }

    @Override
    public void onDropCompleted(View view, DragObject dragobject, boolean flag) {
        if (flag) {
            ShortcutsAdapter shortcutsadapter = mInfo.getAdapter(mContext);
            if (view != mContent) {
                if (dragobject.dragInfo.container != mInfo.id)
                    removeItem(mDragItem);
            } else{
                if (mDragPos != dragobject.dragInfo.cellX) {
                    shortcutsadapter.saveContentPosition();
                    mInfo.notifyDataSetChanged();
                }
            }
        }
        if (mInfo.getAdapter(mContext).isEmpty()) {
            if (!flag) {
                mDragItem.copyPosition(mInfo);
                mLauncher.addItem(mDragItem, false);
            }
            LauncherModel.updateItemInDatabase(mContext, mDragItem);
        }
        mDragItem = null;
        mDragedView.setVisibility(View.VISIBLE);
        mDragController.setTouchTranslator(null);
        mInfo.icon.invalidate();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = (DropableGridView)findViewById(R.id.folder_content);
        mContent.setOnItemClickListener(this);
        mContent.setOnItemLongClickListener(this);
        mTitleText = (TextView)findViewById(R.id.title);
        findViewById(R.id.folder_header).setOnClickListener(this);
        mComfirmBtn = findViewById(R.id.confirm);
        mComfirmBtn.setOnClickListener(this);
        mRenameEdit = (EditText)findViewById(R.id.rename_edit);
        mImm = (InputMethodManager)mContext.getSystemService("input_method");
        mIsEditing = false;
        mOpenAnimation = new AnimationSet(true);
        mOpenAnimation.addAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
        mOpenAnimation.setDuration(200L);
        mOpenAnimation.setInterpolator(new DecelerateInterpolator());
        mCloseAnimation = new AnimationSet(true);
        mCloseAnimation.addAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_out));
        mCloseAnimation.setDuration(200L);
        mCloseAnimation.setFillAfter(true);
        mCloseAnimation.setInterpolator(new AccelerateInterpolator());
        Resources resources = mContext.getResources();
        mDefaultFolderName = resources.getString(R.string.folder_name);
        mDefaultUnnamedFolderName = resources.getString(R.string.unnamed_folder_name);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterview, View view, int i,
            long l) {
        boolean flag = false;
        if (view.isInTouchMode() 
                && !mLauncher.isSceneShowing()) {
            ShortcutInfo shortcutInfo = (ShortcutInfo)view.getTag();
            mDragController.setTouchTranslator(null);
            mDragController.startDrag(view, this, shortcutInfo, DragController.DRAG_ACTION_MOVE);
            mDragItem = shortcutInfo;
            mDragedView = view;
            mDragPos = i;
            flag = true;
        }
        return flag;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterview, View view, int i, long l) {
        mLauncher.onClick(view);
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title: 
        case R.id.rename_edit: 
        default:
            break;

        case R.id.folder_header: 
            showEditPanel(true, true);
            break;

        case R.id.confirm: 
            String s = mRenameEdit.getText().toString();
            if (!s.equals(getEditText(mInfo.title)))
                mInfo.setTitle(s, mLauncher);
            updateAppearance();
            showEditPanel(false, true);
            break;
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (View.MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            mLastAtMostMeasureHeight = android.view.View.MeasureSpec.getSize(heightMeasureSpec);
            return;
        } else {
            throw new RuntimeException("folder height must be wrap_content!");
        }
    }

    void onClose(boolean bClose, Runnable runnable) {
        if (!mClosing) {
            mClosing = true;
            mInfo.opened = false;
            clearAnimation();
            showEditPanel(false, false);
            mDragController.removeDropTarget(mContent);
            mOnFinishClose = runnable;
            if (mInfo.icon == null) {
                finishShow();
            } else {
                mInfo.icon.onClose();
                if (bClose) {
                    prepairAnimationSet(mCloseAnimation);
                    int ai[] = new int[2];
                    mInfo.icon.getLocationInWindow(ai);
                    mCloseAnimation.addAnimation(new ScaleAnimation(1F, 0.6F, 1F, 0.6F, ai[0] + mInfo.icon.getWidth() / 2, ai[1] - getTop()));
                    startAnimation(mCloseAnimation);
                } else {
                    finishShow();
                }
            }
        }
    }
    
}