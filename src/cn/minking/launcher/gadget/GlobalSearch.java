package cn.minking.launcher.gadget;
/**
 * 作者：      minking
 * 文件名称:    GlobalSearch.java
 * 创建时间：    2014
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140402 :  搜索栏信息
 * ====================================================================================
 */
import cn.minking.launcher.R;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class GlobalSearch extends FrameLayout
    implements View.OnClickListener, Gadget {

    public GlobalSearch(Context context) {
        super(context);
        inflate(context, R.layout.gadget_global_search, this);
        findViewById(R.id.searcher_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        SearchManager searchmanager = (SearchManager)mContext.getSystemService("search");
        if (searchmanager != null) {
            searchmanager.startSearch(null, false, null, null, true);
        }
    }
    
    @Override
    public void onAdded() {
        
    }

    @Override
    public void onCreate() {
        
    }

    @Override
    public void onDeleted() {
        
    }

    @Override
    public void onDestroy() {
    
    }

    @Override
    public void onEditDisable() {
    
    }

    @Override
    public void onEditNormal() {
        
    }

    @Override
    public void onPause() {
        
    }

    @Override
    public void onResume() {
        
    }

    @Override
    public void onStart() {
        
    }

    @Override
    public void onStop() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateConfig(Bundle bundle) {
        // TODO Auto-generated method stub
        
    }
}