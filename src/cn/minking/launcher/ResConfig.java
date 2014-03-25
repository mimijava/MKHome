package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    ResConfig.java
 * 创建时间：    2013-12-28
 * 描述：      桌面的一些配置
 * 更新内容
 * ====================================================================================
 * 
 * ====================================================================================
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class ResConfig {
    private static int mCellCountX = -1;
    private static int mCellCountY = -1;
    private static String mCustomizedDefaultWorkspacePath;
    private static int mDefaultWorkspaceId;
    private static String mDefaultWorkspaceName;
    private static int mHotseatCount = -1;
    private static int mIconHeight = -1;
    private static int mIconWidth = -1;
    private static String mLauncherDatabaseName;
    private static int mWidgetCellMeasureHeight;
    private static int mWidgetCellMeasureWidth;
    private static int mWidgetCellMinHeight;
    private static int mWidgetCellMinWidth;

    public static void Init(Context context) {
        int i = 0;
        Resources resources = context.getResources();
        
        // 横纵的单元格
        mCellCountX = Math.max(2, resources.getInteger(R.integer.config_cell_count_x));
        mCellCountY = Math.max(2, resources.getInteger(R.integer.config_cell_count_y));
        
        // 图标的宽高
        mIconWidth = resources.getDimensionPixelSize(R.dimen.config_icon_width);
        mIconHeight = resources.getDimensionPixelSize(R.dimen.config_icon_height);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        
        if (mCellCountX != 3 || mCellCountY != 3) {
            String s = sharedPreferences.getString("pref_key_cell_layout_size", null);
            if (s != null) {
                int index = s.indexOf('x');
                if (index != -1) {
                    mCellCountX = Integer.parseInt(s.substring(0, i));
                    mCellCountY = Integer.parseInt(s.substring(i + 1, s.length()));
                    if (mCellCountX < 2 || mCellCountY < 2) {
                        mCellCountY = 4;
                        mCellCountX = 4;
                    }
                }
            }
        }
        
        // HOT SEAT 总个数
        mHotseatCount = mCellCountX + mCellCountX / 2;
        
        // 整个桌面的布局 4x5或4*4
        String cellSize = getCellSizeVal(mCellCountX, mCellCountY);
        mLauncherDatabaseName = getDatabaseNameBySuffix(cellSize);
        StringBuilder stringbuilder = (new StringBuilder()).append("default_workspace");
        
        if ("4x4".equals(cellSize)){
            cellSize = "";
        }
        
        // 默认WORKSPACE 的XML名称
        mDefaultWorkspaceName = stringbuilder.append(cellSize).toString();
        
        // 字定义的布局可以存放在/data/media/customized目录中
        mCustomizedDefaultWorkspacePath = (new StringBuilder()).append("/data/media/customized/")
                .append(mDefaultWorkspaceName).append(".xml").toString();
        
        mDefaultWorkspaceName = (new StringBuilder()).append(context.getPackageName())
                .append(":xml/").append(mDefaultWorkspaceName).toString();
        mDefaultWorkspaceId = resources.getIdentifier(mDefaultWorkspaceName, null, null);
        
        // 如果默认的WORKSPACE没有找到，则使用default_workspace_none代替
        if (mDefaultWorkspaceId == 0){
            mDefaultWorkspaceId = R.xml.default_workspace_none;
        }
        
        
        mWidgetCellMeasureWidth = resources.getDimensionPixelSize(R.dimen.workspace_widget_cell_measure_width);
        mWidgetCellMeasureHeight = resources.getDimensionPixelSize(R.dimen.workspace_widget_cell_measure_height);
        mWidgetCellMinWidth = resources.getDimensionPixelSize(R.dimen.workspace_widget_cell_min_width);
        mWidgetCellMinHeight = resources.getDimensionPixelSize(R.dimen.workspace_widget_cell_min_height);
    }
    
    public static final void calcWidgetSpans(LauncherAppWidgetProviderInfo launcherappwidgetproviderinfo){
        launcherappwidgetproviderinfo.spanX = getWidgetSpanX(launcherappwidgetproviderinfo.providerInfo.minWidth);
        launcherappwidgetproviderinfo.spanY = getWidgetSpanY(launcherappwidgetproviderinfo.providerInfo.minHeight);
    }

    public static final int getCellCountX(){
        return mCellCountX;
    }

    public static final int getCellCountY(){
        return mCellCountY;
    }

    public static final String getCellSizeVal(int i, int j){
        return (new StringBuilder()).append(i).append("x").append(j).toString();
    }

    public static final String getCustomizedDefaultWorkspaceXmlPath(){
        return mCustomizedDefaultWorkspacePath;
    }

    public static final String getDatabaseName(){
        return mLauncherDatabaseName;
    }

    public static final String getDatabaseNameBySuffix(String size){
        if ("4x4".equals(size)){
            size = "";
        }
        return (new StringBuilder()).append("launcher").append(size).append(".db").toString();
    }

    public static final int getDefaultWorkspaceXmlId(){
        return mDefaultWorkspaceId;
    }

    public static final int getHotseatCount(){
        return mHotseatCount;
    }

    public static final int getIconWidth(){
        return mIconWidth;
    }
    
    public static final int getIconHeight(){
        return mIconHeight;
    }

    public static final int getWidgetCellMinWidth(){
        return mWidgetCellMinWidth;
    }

    public static final int getWidgetCellMinHeight(){
        return mWidgetCellMinHeight;
    }

    public static final int getWidgetSpanX(int i){
        return Math.min(1 + (i + Utilities.getDipPixelSize(1)) / mWidgetCellMeasureWidth, mCellCountX);
    }

    public static final int getWidgetSpanY(int i){
        return Math.min(1 + (i + Utilities.getDipPixelSize(1)) / mWidgetCellMeasureHeight, mCellCountY);
    }
}
