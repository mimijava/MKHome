package cn.minking.launcher.gadget;
/**
 * 作者：      minking
 * 文件名称:    GadgetInfo.java
 * 创建时间：    2014
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140402 :  桌面Gadget信息
 * ====================================================================================
 */

import android.content.ContentValues;
import cn.minking.launcher.ItemInfo;

public class GadgetInfo extends ItemInfo {
    public int mGadgetId;
    public int mIconId;
    public int mTitleId;

    public GadgetInfo(int id) {
        itemType = 5;
        mGadgetId = id;
    }

    public int getGadgetId() {
        return mGadgetId;
    }

    public void onAddToDatabase(ContentValues contentvalues) {
        super.onAddToDatabase(contentvalues);
        contentvalues.put("appWidgetId", Integer.valueOf(mGadgetId));
    }

    public String toString() {
        return (new StringBuilder()).append("Gadget(id=").
                append(Integer.toString(mGadgetId)).append(")").toString();
    }
}
