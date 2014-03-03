package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    CellLayout.java
 * 创建时间：    2014-02-25
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140225: 
 * ====================================================================================
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class CellLayout extends ViewGroup{
    public CellLayout(Context context){
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attributeset){
        this(context, attributeset, 0);
    }

    public CellLayout(Context context, AttributeSet attributeset, int i){
        super(context, attributeset, i);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        
    }
    public void removeAllViewsInLayout(){
        
    }
}