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
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CellScreen extends FrameLayout{
    private CellLayout mCellLayout;
    
    public CellScreen(Context context, AttributeSet attributeset){
        super(context, attributeset);
    }
    
    public CellLayout getCellLayout(){
        return mCellLayout;
    }

    @Override
    protected void onFinishInflate() {
        mCellLayout = (CellLayout)findViewById(R.layout.cell_screen);
    }
    
    
}