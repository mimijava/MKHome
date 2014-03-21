package cn.minking.launcher.snapshot;
/**
 * 作者：      minking
 * 文件名称:    HomeSnapshotHelperService.java
 * 创建时间：    2014-03-03
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 2014030301: 
 * ====================================================================================
 */
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class HomeSnapshotHelperService extends Service{
    private Binder mBinder;
    
    public HomeSnapshotHelperService(){
        
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
