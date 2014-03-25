package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    TouchHandle.java
 * 创建时间：    2014-03-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140325 : 用于处理触摸的计算
 * ====================================================================================
 */
import android.R.integer;
import android.util.Log;

public class TouchHandle{
    public static final String TAG = "MKHome.TouchHandle";
    public static final int DIR_NO_MOVE = 0;
    public static final int DIR_LEFT_TO_RIGHT = 1;
    public static final int DIR_RIGHT_TO_LEFT = 2;
    public static final int DIR_TOP_TO_BOTTOM = 3;
    public static final int DIR_BOTTOM_TO_TOP = 4;
    
    public static final int SCALE_TYPE_NONE = 10;
    public static final int SCALE_TYPE_ENLARGE = 11;
    public static final int SCALE_TYPE_SMALL = 12;
    
    public static final int SCALE_DISTANCE = 120;
    
    public float startX1 = 0.0f;
    public float startY1 = 0.0f;
    public float startX2 = 0.0f;
    public float startY2 = 0.0f;
    private int mDirX;
    private int mDirY;
    private double mDistanceNew;
    private double mDistanceOld;
    private int mScalType;
    
    public TouchHandle(float x1, float y1){
        this.startX1 = x1;
        this.startY1 = y1;
        
        mDirX = DIR_NO_MOVE;
        mDirY = DIR_NO_MOVE;
    }
    
    public TouchHandle(float x1, float y1, float x2, float y2){
        this.startX1 = x1;
        this.startY1 = y1;
        this.startX2 = x2;
        this.startY2 = y2;

        float dxOld = Math.abs(startX2 - startX1);
        float dyOld = Math.abs(startY2 - startY1);
        
        mDistanceOld = Math.sqrt(dxOld * dxOld + dyOld * dyOld);
        mScalType = SCALE_TYPE_NONE; 
    }
    
    /**
     * 功能： 两点触摸移动得到新的两点距离
     */
    public void setScaleMove(float newX1, float newY1, float newX2, float newY2){
        float dx = Math.abs(newX2 - newX1);
        float dy = Math.abs(newY2 - newY1);
        mDistanceNew = Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 功能：得到新的两点位置与原有的位置的距离差，以此判断是何种手势
     */
    public int getScaleType() {
        int scalType = SCALE_TYPE_NONE;
        if ((mDistanceNew - mDistanceOld) >  SCALE_DISTANCE) {
            scalType = SCALE_TYPE_ENLARGE;
        } else if((mDistanceNew - mDistanceOld) < -SCALE_DISTANCE){
            scalType = SCALE_TYPE_SMALL;
        }
        return scalType;
    }
    
    /**
     * 功能： 单个的触摸移动方向
     * @param newX
     * @param newY
     */
    public void setTouchMoveDir(float newX, float newY){
        if (newX < this.startX1 && newY < this.startY1) {
            mDirX = DIR_RIGHT_TO_LEFT;
            mDirY = DIR_BOTTOM_TO_TOP;
        }
        
        if (newX < this.startX1 && newY > this.startY1) {
            mDirX = DIR_RIGHT_TO_LEFT;
            mDirY = DIR_TOP_TO_BOTTOM;
        }
        
        if (newX > this.startX1 && newY < this.startY1) {
            mDirX = DIR_LEFT_TO_RIGHT;
            mDirY = DIR_BOTTOM_TO_TOP;
        }
        
        if (newX < this.startX1 && newY > this.startY1) {
            mDirX = DIR_LEFT_TO_RIGHT;
            mDirY = DIR_BOTTOM_TO_TOP;
        }
    }
    
    public int getDirX() {
        return mDirX;
    }
    public int getDirY() {
        return mDirY;
    }
}