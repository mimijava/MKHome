package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    HotSeats.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 创建桌面的HOTSEAT， HOTSEAT支持长按拖动图标
 * ====================================================================================
 */
import java.util.Iterator;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class HotSeats extends LinearLayout
    implements android.view.View.OnLongClickListener{
    
    private static int MAX_SEATS = -1;
    private static final ItemInfo PLACE_HOLDER_SEAT = new ItemInfo();
    private Context mContext;
    private final ItemInfo mCurrentSeats[];
    private DragController mDragController;
    private ItemInfo mDraggingItem;
    private boolean mIsLoading;
    private final boolean mIsReplaceSupported = true;
    private Launcher mLauncher;
    private int mLocation[];
    private final ItemInfo mSavedSeats[];
    
    // HOTSEAT 支持长按拖动图标
    public HotSeats(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
        mIsLoading = true;
        mLocation = new int[2];
        MAX_SEATS = ResConfig.getHotseatCount();
        mCurrentSeats = new ItemInfo[MAX_SEATS];
        mSavedSeats = new ItemInfo[MAX_SEATS];
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    /**
     * 功能：  MAX个SEAT inflate layout/hotseat_button.xml的布局属性
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < MAX_SEATS; i++) {
            LayoutInflater.from(mContext).inflate(R.layout.hotseat_button, this, true);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mLauncher.getDragLayer().getLocationInDragLayer(this, mLocation);
        mLauncher.getDragController().setDeleteRegion(new RectF(l, t, r, b));
    }

    /**
     * 功能：  设置HOTSEAT的Drag控制器
     * @param dragcontroller
     */
    public void setDragController(DragController dragcontroller){
        mDragController = dragcontroller;
    }

    public void setLauncher(Launcher launcher){
        mLauncher = launcher;
        for (int i = 0; i < getChildCount() - 1; i++) {
            ((HotSeatButton)getChildAt(i)).setLauncher(launcher);
        }
    }
    
    /**
     * 功能： 给Item分配Seat位置
     */
    private void setSeat(int i, ItemInfo iteminfo){
        // i在范围之内，则当前位置上与Item不为同样的
        if ((i >= 0 || i < MAX_SEATS) && mCurrentSeats[i] != iteminfo){
            mCurrentSeats[i] = iteminfo;
            HotSeatButton hotseatbutton = (HotSeatButton)getChildAt(i);
            hotseatbutton.unbind(mDragController);
            if (iteminfo == null) {
                LinearLayout.LayoutParams layoutparams 
                    = (LinearLayout.LayoutParams)hotseatbutton.getLayoutParams();
                layoutparams.width = 0;
                layoutparams.weight = 0F;
                hotseatbutton.setLayoutParams(layoutparams);
            } else {
                if (iteminfo != PLACE_HOLDER_SEAT){
                    // 给Item分配图标
                    ItemIcon itemicon = mLauncher.createItemIcon(this, iteminfo);
                    itemicon.setCompactViewMode(true);
                    hotseatbutton.bind(itemicon, mDragController);
                }
                hotseatbutton.setTag(iteminfo);
                hotseatbutton.setOnLongClickListener(this);
                LinearLayout.LayoutParams layoutparams 
                    = (LinearLayout.LayoutParams)hotseatbutton.getLayoutParams();
                layoutparams.width = -1;
                layoutparams.weight = 1F;
                hotseatbutton.setLayoutParams(layoutparams);
            }
        }
    }
    
    /**
     * 功能：  判断是否为空的SEAT
     */
    boolean isEmptySeat(int i){
        boolean flag = false;
        if ((i < MAX_SEATS && i >= 0) 
                && (mCurrentSeats[i] == null || mCurrentSeats[i] == PLACE_HOLDER_SEAT)){
            flag = true;
        }
        return flag;
    }
    
    /**
     * 功能：  找到SEAT中的空位置
     */
    public int findEmptySeat(){
        int i = 0;
        for (i = 0; i < MAX_SEATS; i++) {
            if (isEmptySeat(i)) break;
        }
        if (i >= MAX_SEATS) {
            i = -1;
        }
        return i;
    }
    
    /**
     * 功能：  加载HOT SEAT 图标项目
     * @param iteminfo
     * @return
     */
    public boolean pushItem(ItemInfo iteminfo){
        boolean bFlag = false;
        
        if (!isEmptySeat(iteminfo.cellX)) {
            if(-1 == findEmptySeat()) return bFlag;
        }else {
            setSeat(iteminfo.cellX, iteminfo);
        }
        if (!mIsLoading) {
            saveSeats(false);
        }
        return bFlag;
    }
    
    /**
     * 功能：  开始BIND HOTSEAT， 将各BUTTON复位
     */
    public void startBinding(){
        for (int i = 0; i < MAX_SEATS; i++) {
            
            // 调用removeAllViewsInLayout()，清空以前的数据
//          ((HotSeatButton)getChildAt(i)).removeAllViewsInLayout();
            mSavedSeats[i] = null;
            mCurrentSeats[i] = null;
        }
        mIsLoading = true;
    }
    
    /**
     * 功能：  完成 BIND HOTSEAT并保存
     */
    public void finishBinding(){
        saveSeats(false);
        mIsLoading = false;
    }
    
    public ItemIcon getItemIcon(FolderInfo folderinfo){
        Object object = (HotSeatButton)findViewWithTag(folderinfo);
        if (object == null || ((HotSeatButton)(object)).getChildCount() == 0)
            object = null;
        else
            object = (ItemIcon)((HotSeatButton)(object)).getChildAt(0);
        return (ItemIcon)object;
    }
    
    private void saveSeats(){
        saveSeats(true);
    }

    private void saveSeats(boolean flag){
        
    }
    
    @Override
    public boolean onLongClick(View v) {
        // TODO Auto-generated method stub
        return false;
    }
    
    
}