package cn.minking.launcher;
/**
 * 作者：            minking
 * 文件名称:    ItemQuery.java
 * 创建时间：    2014-02-28
 * 描述：      
 * 更新内容
 * ====================================================================================
 * 20140228: 数据查询项
 * ====================================================================================
 */

interface ItemQuery{
      /**
      + CREATE TABLE favorites("
      + "_id INTEGER PRIMARY KEY,"
      + "title TEXT,"
      + "intent TEXT,"
      + "container INTEGER,"
      + "screen INTEGER,"
      + "cellX INTEGER,"
      + "cellY INTEGER,"
      + "spanX INTEGER,"
      + "spanY INTEGER,"
      + "itemType INTEGER,"
      + "appWidgetId INTEGER NOT NULL DEFAULT -1,"
      + "isShortcut INTEGER,"
      + "iconType INTEGER,"
      + "iconPackage TEXT,"
      + "iconResource TEXT,"
      + "icon BLOB,"
      + "uri TEXT,"
      + "displayMode INTEGER,"
      + "launchCount INTEGER NOT NULL DEFAULT 1,"
      + "sortMode INTEGER,"
      + "itemFlags INTEGER NOT NULL DEFAULT 0" 
      + ");");
      */
      String as[] = new String[]{ 
                  "favorites._id",
                "favorites.title",
                "intent",
                "container",
                "screen",
                "cellX",
                "cellY",
                "spanX",
                "spanY",
                "itemType",
                "appWidgetId",
                "isShortcut",
                "iconType",
                "iconPackage",
                "iconResource",
                "icon",
                "uri",
                "displayMode",
                "launchCount",
                "sortMode",
                "itemFlags"};
                  
      public static final String COLUMNS[] = as;
      
      public enum COL{  
              ID, 
              TITLE, 
              INTENT, 
              CONTAINER,
              SCREEN,
              CELLX,
              CELLY,
              SPANX,
              SPANY,
              ITEMTYPE,
              APPWIDGETID,
              ISSHORTCUT,
              ICONTYPE,
              ICONPACKAGE,
              ICONRESOURCE,
              ICON,
              URI,
              DISPLAYMODE,
              LAUNCHERCOUNT,
              SORTMODE,
              ITEMFLAGS
            };
}
