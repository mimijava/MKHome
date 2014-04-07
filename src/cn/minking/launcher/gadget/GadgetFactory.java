package cn.minking.launcher.gadget;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import cn.minking.launcher.R;
import cn.minking.launcher.ResConfig;
import cn.minking.launcher.Utilities;

public class GadgetFactory {
    private static final int DEFAULT_WIDGET_CELL_HEIGHT = Utilities.getDipPixelSize(74 - Utilities.getDipPixelSize(1));
    private static final int DEFAULT_WIDGET_CELL_WIDTH = Utilities.getDipPixelSize(80 - Utilities.getDipPixelSize(1));
    
    public static final int STATE_ONSTART = 1;
    public static final int STATE_ONSTOP = 2;
    public static final int STATE_ONPAUSE = 3;
    public static final int STATE_ONRESUME = 4;
    public static final int STATE_ONCREATE = 5;
    public static final int STATE_ONDESTORY = 6;
    public static final int STATE_ONEDITDISABLE = 7;
    public static final int STATE_ONEDITNORMAL = 8;
    
    
    public static final int ID_PLAYER = 2;
    public static final int ID_GLOBAL_SEARCH = 3;
    public static final int ID_GADGET_CLOCK_1X2 = 4;
    public static final int ID_GADGET_CLOCK_2X2 = 5;
    public static final int ID_GADGET_CLOCK_2X4 = 6;
    public static final int ID_GADGET_PHOTO_2X2 = 7;
    public static final int ID_GADGET_PHOTO_2X4 = 8;
    public static final int ID_GADGET_PHOTO_4X4 = 9;
    public static final int ID_GADGET_WEATHER_1X4 = 10;
    public static final int ID_GADGET_WEATHER_2X4 = 11;
    public static final int ID_GADGET_CLEAR_BUTTON = 12;
    
    public static final int ID_LIST[] = {
        ID_PLAYER,
        ID_GLOBAL_SEARCH,
        ID_GADGET_CLOCK_1X2,
        ID_GADGET_CLOCK_2X2,
        ID_GADGET_CLOCK_2X4,
        ID_GADGET_PHOTO_2X2,
        ID_GADGET_PHOTO_2X4,
        ID_GADGET_PHOTO_4X4,
        ID_GADGET_WEATHER_1X4,
        ID_GADGET_WEATHER_2X4,
        ID_GADGET_CLEAR_BUTTON
    };
    
    public static Gadget createGadget(Context context, GadgetInfo gadgetinfo, int i) {
        Object gadget = null;
        switch (gadgetinfo.getGadgetId())
        {
        case ID_PLAYER: // '\002'
            gadget = new Player(context);
            break;

        case ID_GLOBAL_SEARCH: // '\003'
            gadget = new GlobalSearch(context);
            break;

        case ID_GADGET_CLOCK_1X2: // '\004'
        case ID_GADGET_CLOCK_2X2: // '\005'
        case ID_GADGET_CLOCK_2X4: // '\006'
            gadget = new ClockGadgetDelegate(context, i);
            break;

        case ID_GADGET_PHOTO_2X2: // '\007'
        case ID_GADGET_PHOTO_2X4: // '\b'
        case ID_GADGET_PHOTO_4X4: // '\t'
            gadget = new PhotoFrame(context, i);
            break;

        case ID_GADGET_WEATHER_1X4: // '\n'
            gadget = new Weather_1x4(context);
            break;

        case ID_GADGET_WEATHER_2X4: // '\013'
            gadget = new Weather_2x4(context);
            break;

        case ID_GADGET_CLEAR_BUTTON: // '\f'
            gadget = new ClearButton(context);
            break;
        }
        if (gadget != null) {
            ((View)gadget).setTag(gadgetinfo);
        }
        return (Gadget)gadget;
    }
    
    public static final int[] getGadgetIdList() {
        return ID_LIST;
    }
    
    
    public static long getGadgetItemId(Bundle bundle) {
        long id = bundle.getLong("callback_id", -1L);
        if (id == -1L) {
            id = Long.valueOf(bundle.getString("RESPONSE_TRACK_ID")).longValue();
        }
        return id;
    }
    
    public static GadgetInfo getInfo(int id) {
        GadgetInfo gadgetinfo = new GadgetInfo(id);
        switch (id)
        {
        default:
            gadgetinfo = null;
            break;

        case ID_PLAYER: // '\002'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(4 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.widget_gadget_player;
            gadgetinfo.mIconId = R.drawable.gadget_player_icon;
            break;

        case ID_GLOBAL_SEARCH: // '\003'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(1 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.widget_global_search;
            gadgetinfo.mIconId = R.drawable.global_search_widget_icon;
            break;

        case ID_GADGET_CLOCK_1X2: // '\004'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(2 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(1 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_clock_12_label;
            gadgetinfo.mIconId = R.drawable.gadget_clock_12_icon;
            break;

        case ID_GADGET_CLOCK_2X2: // '\005'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(2 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(2 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_clock_22_label;
            gadgetinfo.mIconId = R.drawable.gadget_clock_22_icon;
            break;

        case ID_GADGET_CLOCK_2X4: // '\006'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(2 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_clock_24_label;
            gadgetinfo.mIconId = R.drawable.gadget_clock_24_icon;
            break;

        case ID_GADGET_PHOTO_2X2: // '\007'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(2 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(2 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_photo_22_label;
            gadgetinfo.mIconId = R.drawable.gadget_photo_22_icon;
            break;

        case ID_GADGET_PHOTO_2X4: // '\b'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(2 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_photo_24_label;
            gadgetinfo.mIconId = R.drawable.gadget_photo_24_icon;
            break;

        case ID_GADGET_PHOTO_4X4: // '\t'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(4 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_photo_44_label;
            gadgetinfo.mIconId = R.drawable.gadget_photo_44_icon;
            break;

        case ID_GADGET_WEATHER_1X4: // '\n'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(1 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_weather_title_14;
            gadgetinfo.mIconId = R.drawable.gadget_weather_icon_w14;
            break;

        case ID_GADGET_WEATHER_2X4: // '\013'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(4 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(2 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_weather_title_24;
            gadgetinfo.mIconId = R.drawable.gadget_weather_icon_w24_new;
            break;

        case ID_GADGET_CLEAR_BUTTON: // '\f'
            gadgetinfo.spanX = ResConfig.getWidgetSpanX(1 * DEFAULT_WIDGET_CELL_WIDTH);
            gadgetinfo.spanY = ResConfig.getWidgetSpanY(1 * DEFAULT_WIDGET_CELL_HEIGHT);
            gadgetinfo.mTitleId = R.string.gadget_clear_button_label;
            gadgetinfo.mIconId = R.drawable.gadget_clear_button_icon;
            break;
        }
        return gadgetinfo;
    }

    public static void updateGadgetBackup(Context context) {
        ClockGadgetDelegate.updateBackup(context);
    }
}