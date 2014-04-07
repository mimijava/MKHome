package cn.minking.launcher.gadget;
/**
 * 作者：      minking
 * 文件名称:    Clock.java
 * 创建时间：    2014-03-28
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140328: 时钟类
 * ====================================================================================
 */
import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class Clock {
    public static interface ClockStyle {
        public abstract int getUpdateInterval();
        public abstract void initConfig(String s);
        public abstract void updateAppearance(Calendar calendar);
    }

    private class TimeZoneChangedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                mCalendar = Calendar.getInstance();
            }
            if (!mTickerStopped && mClockStyle != null) {
                updateCurTime();
            }
        }

        private TimeZoneChangedReceiver() {
        }
    }

    /********* 常量  *********/
    private final static boolean LOGD = true;
    private final static String TAG = "MKhome.gadget.Clock";
    
    /********* 状态  *********/
    private boolean mTickerStopped;
    
    /********* 内容  *********/
    protected Calendar mCalendar;
    private final Context mContext;
    protected ClockStyle mClockStyle;
    
    /********* 行为  *********/
    private Handler mHandler;
    private Runnable mTicker;
    private TimeZoneChangedReceiver mTimeZoneChangedReceiver;

    public Clock(Context context) {
        mTickerStopped = false;
        mContext = context;
    }

    /**
     * 功能： 更新当前时间
     */
    private void updateCurTime() {
        if (mClockStyle != null && mCalendar != null) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            try {
                mClockStyle.updateAppearance(mCalendar);
            } catch (Exception exception) {
                if (LOGD) Log.e(TAG, exception.toString());
            }
        }
    }

    /**
     * 描述 ： 时钟初始化
     */
    public void init() {
        mTickerStopped = false;
        mHandler = new Handler();
        mTicker = new Runnable() {
            @Override
            public void run() {
                if (!mTickerStopped && mClockStyle != null) {
                    updateCurTime();
                    int interval = mClockStyle.getUpdateInterval();
                    mHandler.postAtTime(mTicker, SystemClock.uptimeMillis() + 
                            ((long)interval - System.currentTimeMillis() % (long)interval));
                }
            }
        };
    }

    /**
     * 描述： 暂停时钟
     */
    public void pause() {
        mTickerStopped = true;
        mHandler.removeCallbacks(mTicker);
        if (mTimeZoneChangedReceiver != null) {
            mContext.unregisterReceiver(mTimeZoneChangedReceiver);
            mTimeZoneChangedReceiver = null;
        }
    }

    /**
     * 描述： 继续
     */
    public void resume() {
        mCalendar = Calendar.getInstance();
        mHandler.removeCallbacks(mTicker);
        mTickerStopped = false;
        mTicker.run();
        if (mTimeZoneChangedReceiver == null) {
            mTimeZoneChangedReceiver = new TimeZoneChangedReceiver();
            IntentFilter intentfilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            mContext.registerReceiver(mTimeZoneChangedReceiver, intentfilter);
        }
    }

    /**
     * 功能： 设置时钟风格
     * @param clockstyle
     */
    public void setClockStyle(ClockStyle clockstyle)  {
        mClockStyle = clockstyle;
        updateCurTime();
    }
}