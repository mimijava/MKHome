package cn.minking.launcher.gadget;
/**
 * 作者：      minking
 * 文件名称:    AwesomeClock.java
 * 创建时间：    2014
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140402: 桌面时钟
 * ====================================================================================
 */
import java.util.Calendar;

import org.w3c.dom.Element;

import cn.minking.launcher.screenelement.MkAdvancedView;
import cn.minking.launcher.screenelement.RenderThread;
import cn.minking.launcher.screenelement.ScreenContext;
import cn.minking.launcher.screenelement.ScreenElementRoot;
import cn.minking.launcher.screenelement.elements.ButtonScreenElement;
import cn.minking.launcher.screenelement.util.ZipResourceLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class AwesomeClock extends FrameLayout
    implements Clock.ClockStyle, Gadget {
    /********* 常量  *********/
    private final static boolean LOGD = true;
    private final static String TAG = "MKhome.gadget.AwesomeClock";
    
    /********* 内容  *********/
    private MkAdvancedView mAwesomeView;
    private ScreenContext mElementContext;
    private ScreenElementRoot mRoot;
    private int mUpdateInterval;
    
    public AwesomeClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
    }

    public AwesomeClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public AwesomeClock(Context context) {
        super(context);
        // 复写onDraw
        setWillNotDraw(false);
    }

    @Override
    public void onAdded() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onCreate() {
        if (mElementContext == null) return;
        
        Element element = mElementContext.mResourceManager.getManifestRoot();
        if ("clock".equalsIgnoreCase(element.getNodeName())) {
            try {
                mUpdateInterval = Integer.parseInt(element.getAttribute("update_interval"));
            } catch (NumberFormatException e) {
                mUpdateInterval = 60000;
            }
            
            try {
                getLayoutParams().width = Integer.parseInt(element.getAttribute("width"));
                getLayoutParams().height = Integer.parseInt(element.getAttribute("height"));
            } catch (NumberFormatException _ex) { }
            
            mRoot = new ScreenElementRoot(mElementContext);
            mRoot.setDefaultFramerate(1000F / (float)mUpdateInterval);
            mRoot.load();
            
            if (mRoot != null) {
                RenderThread rThread = RenderThread.globalThread();
                if (!rThread.isStarted()) {
                    try {
                        rThread.start();
                    }catch (IllegalThreadStateException _ex) { }
                }
                mAwesomeView = new MkAdvancedView(mContext, mRoot, rThread);
                mAwesomeView.setFocusable(false);
                FrameLayout.LayoutParams lParams = 
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 
                                FrameLayout.LayoutParams.MATCH_PARENT);
                addView(mAwesomeView, ((ViewGroup.LayoutParams)lParams));
            }
        }
    }

    @Override
    public void onDeleted() {
        mAwesomeView.cleanUp();
    }

    @Override
    public void onDestroy() {
        if (mAwesomeView != null) {
            mAwesomeView.cleanUp();
        }
    }

    @Override
    public void onEditDisable() {
        
    }

    @Override
    public void onEditNormal() {
        
    }

    @Override
    public void onPause() {
        if (mRoot != null) {
            synchronized (mRoot) {
                mRoot.onCommand("pause");
            }
        }
        if (mAwesomeView != null) {
            mAwesomeView.invalidate();
            mAwesomeView.onPause();
        }
    }

    @Override
    public void onResume() {
        if (mRoot != null) {
            synchronized (mRoot) {
                mRoot.onCommand("resume");
            }
        }
        if (mAwesomeView != null) {
            mAwesomeView.onResume();
        }
        return;
    }

    @Override
    public void onStart() {
        RenderThread.globalThread().setPaused(false);
    }

    @Override
    public void onStop() {
        RenderThread.globalThread().setPaused(true);
    }

    @Override
    public void updateConfig(Bundle bundle) {
        
    }
    
    @Override
    public void updateAppearance(Calendar calendar) {
        if (mElementContext != null && mAwesomeView != null) {
            mRoot.requestUpdate();
        }
    }

    @Override
    public int getUpdateInterval() {
        int interval = 0;
        if (mAwesomeView != null) {
            interval = mUpdateInterval;
        }
        if (interval <= 0) {
            interval = 1000;
        }
        return interval;
    }

    @Override
    public void initConfig(String config) {
        mElementContext = new ScreenContext(mContext, 
                (new ZipResourceLoader(config)).setLocal(mContext.getResources().getConfiguration().locale));
    }
    
    public boolean setClockButtonListener(ButtonScreenElement.ButtonActionListener buttonactionlistener) {
        boolean flag = false;
        if (mRoot != null) {
            ButtonScreenElement buttonscreenelement = (ButtonScreenElement)mRoot.findElement("clock_button");
            if (buttonscreenelement != null) {
                buttonscreenelement.setListener(buttonactionlistener);
                flag = true;
            } else {
                Log.w(TAG, "No clock button in this clock.");
            }
        }
        return flag;
    }
}