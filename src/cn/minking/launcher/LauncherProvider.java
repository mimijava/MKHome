package cn.minking.launcher;
/**
 * 作者：      minking
 * 文件名称:    LauncherProvider.java
 * 创建时间：    2013
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 20140228: 数据存储类
 * ====================================================================================
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import cn.minking.launcher.LauncherSettings.Favorites;
import cn.minking.launcher.ScreenUtils.ScreenInfo;
import android.app.SearchManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

public class LauncherProvider extends ContentProvider {
    private static final String TAG = "MKHome.Provider";
    private static final boolean LOGD = true;
    
    // 用于存储的数据库名称及版本号
    private static final String DATABASE_NAME = "mkhome.db";
    private static final int DATABASE_VERSION = 1;

    static final String AUTHORITY = "cn.minking.launcher.settings";
    
    static final Uri CONTENT_APPWIDGET_RESET_URI =
            Uri.parse("content://" + AUTHORITY + "/appWidgetReset");
    
    static final String TABLE_FAVORITES = "favorites";
    static final String PARAMETER_NOTIFY = "notify";
    static final String DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED =
            "DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED";
    static final String DEFAULT_WORKSPACE_RESOURCE_ID =
            "DEFAULT_WORKSPACE_RESOURCE_ID";
    
    /// M: LOCK给数据库操作使用
    private final Object mLock = new Object();
    
    /// M: 保存场景数据
    private static DatabaseHelper sOpenHelper;
    private ArrayList<ScreenInfo> mScreens;
    
    
    /*
     * 查询数据库参数定义内部类
     */
    static class SqlArguments{
        public final long id;
        public final String args[];
        public final String table;
        public final String where;
        
        private String selectTable(String table){
            return table;
        }
        
        /**
         * 功能： 根据URI得到表名
         * @param uri
         */
        SqlArguments(Uri uri) {
            // 如果SQL语句的条件为1，解析出table
            if (uri.getPathSegments().size() == 1) {
                table = selectTable((String)uri.getPathSegments().get(0));
                where = null;
                args = null;
                id = -1L;
                return;
            }else {
                throw new IllegalArgumentException((new StringBuilder()).append("Invalid URI: ").append(uri).toString());
            }
        }
        
        SqlArguments(Uri uri, String selection, String selectionArgs[]){
            if (uri.getPathSegments().size() == 1){
                // 如果SQL语句的条件为1，解析出table
                table = selectTable((String)uri.getPathSegments().get(0));
                where = selection;
                args = selectionArgs;
                id = -1L;
            } else if (uri.getPathSegments().size() == 2) {
                // 如果SQL语句的条件多于2个，解析出table及 where
                if (TextUtils.isEmpty(selection)) {
                    table = selectTable(uri.getPathSegments().get(0));
                    id = ContentUris.parseId(uri);
                    if (!LauncherProvider.TABLE_FAVORITES.equals(table))
                        where = (new StringBuilder()).append("screens._id=").append(id).toString();
                    else
                        where = (new StringBuilder()).append("favorites._id=").append(id).toString();
                    args = null;
                }else {
                    throw new UnsupportedOperationException((new StringBuilder()).append("WHERE clause not supported: ").append(uri).toString());
                }
            } else {
                throw new IllegalArgumentException((new StringBuilder()).append("Invalid URI: ").append(uri).toString());
            }
        }
    }
    
    /**
     * 功能： Launcher Provider创建
     */
    @Override
    public boolean onCreate() {
        resetDatabaseIfNeeded();
        sOpenHelper = new DatabaseHelper(getContext());
        ((LauncherApplication)getContext().getApplicationContext()).setLauncherProvider(this);
        return true;
    }
    
    

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 功能： 数据库删除
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        synchronized (mLock) {
            SqlArguments sqlarguments = new SqlArguments(uri, selection, selectionArgs);
            SQLiteDatabase db = sOpenHelper.getWritableDatabase();
            int i = db.delete(sqlarguments.table, sqlarguments.where, sqlarguments.args);
            sOpenHelper.updateMaxId(db);
            return i;
        }
    }
    
    /**
     * 功能： 数据库查询
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        synchronized (mLock) {
            // 解析SQL参数
            SqlArguments sqlArguments = new SqlArguments(uri, selection, selectionArgs);
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            SQLiteDatabase db = sOpenHelper.getReadableDatabase();
            
            // 设置需要查询的表
            qb.setTables(sqlArguments.table);
            cursor = qb.query(db, projection, sqlArguments.where, sqlArguments.args, null, null, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        
        return cursor;
    }

    /**
     * 功能： 数据库更新
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        synchronized (mLock) {
            SqlArguments sqlArguments = new SqlArguments(uri, selection, selectionArgs);
            SQLiteDatabase db = sOpenHelper.getWritableDatabase();
            int i = 0;
            
            if ("packages".equals(sqlArguments.table)) {
                String name = (String)values.get("name");
                if (Boolean.TRUE.equals(values.getAsBoolean("delete"))) {
                    ScreenUtils.removePackage(getContext(), db, name);
                } else {
                    ScreenUtils.updateHomeScreen(getContext(), db, 
                            loadScreens(db), name, values.getAsBoolean("keepItem").booleanValue());
                }
                sOpenHelper.updateMaxId(db);
                i = 0;
            } else if ("screens".equals(sqlArguments.table)) {
                String colum[] = new String[]{"screenOrder"};
                Cursor cursor = db.query("screens", colum, null, null, null, null, null);
                ArrayList<String> orderList = new ArrayList<String>();
                if (cursor == null) {
                    return 0;
                }
                while (cursor.moveToNext()) {
                    orderList.add(String.valueOf(cursor.getInt(0)));
                }
                cursor.close();
                
                // 采用事务的方式进行数据库操作
                db.beginTransaction();
                int j = 0;
                try {
                    while (j < orderList.size()){
                        ContentValues valuesT = new ContentValues();
                        valuesT.put("screenOrder", Integer.valueOf(i));
                        String table = sqlArguments.table;
                        String as[] = new String[]{orderList.get(j)};
                        
                        i += db.update(table, valuesT, "_id=?", as);
                        j++;
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    mScreens = null;
                } catch (Exception e) {
                    db.endTransaction();
                }
                
            } else if ("favorites".equals(sqlArguments.table) 
                    && selection == null && values != null){
                Long container = values.getAsLong("container");
                Long screen = values.getAsLong("screen");
                if ((container != null && -100L == container.longValue())
                        && (screen != null && -1L == screen.longValue()))
                    ScreenUtils.fillEmptyCell(getContext(), db, loadScreens(db), values);
            }
            i = db.update(sqlArguments.table, values, sqlArguments.where, sqlArguments.args);
            return i;
        }
        
    }
    
    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }
    
    private ArrayList<ScreenInfo> loadScreens(SQLiteDatabase db){
        if (mScreens == null){
            mScreens = ScreenUtils.loadScreens(db);
        }
        return mScreens;
    }
    
    /**
     * 功能： 重置数据库
     */
    private void resetDatabaseIfNeeded(){
        if (sOpenHelper != null){
            sOpenHelper.close();
            mScreens = null;
        }
    }
    
    /**
     * 功能： Provider的代理Client的call函数， LauncherModel中使用
     */
    public Bundle call(String cmd, String id, Bundle bundle){
        Bundle bd = null;
        if (cmd.equals("updateInstalledComponentsArg")) {
            ScreenUtils.updateInstalledComponentsArg(getContext());
        }else if(cmd.equals("ensureItemUniquePosition")){
            if (ScreenUtils.verifyItemPosition(sOpenHelper.getWritableDatabase(), Long.parseLong(id))){
                bd = new Bundle();
                bd.putBoolean("resultBoolean", true);       
            }
        }
        return bd;
    }
    
    public long generateNewId(){
        return sOpenHelper.generateNewId();
    }

    static class DatabaseHelper extends SQLiteOpenHelper{
        
        class FakedTypedArray{
            private AttributeSet mSet;
            private TypedArray mTypedArray;
            private String mValues[];
            
            boolean getBoolean(int i, boolean flag){
                if (mTypedArray == null) {
                    if ("true".equals(mValues[i])) {
                        flag = true;
                    }
                }else {
                    flag = mTypedArray.getBoolean(i, flag);
                }
                return flag;
            }
            
            int getInt(int i, int j){
                if (mTypedArray != null) {
                    j = mTypedArray.getInt(i, j);
                }else {
                    j = Integer.valueOf(mValues[i]).intValue();
                }
                return j;
            }
            
            /*
             * 得到属性里的内容
             */
            String getString(int i){
                String string;
                if (mTypedArray == null) {
                    string = mValues[i];
                }else {
                    string = mTypedArray.getString(i);
                }
                return string;
            }
            
            /*
             * 回收数组
             */
            void recycle(){
                if (mTypedArray != null)
                    mTypedArray.recycle();
            }
            
            public FakedTypedArray(AttributeSet aSet, int ai[]) {
                
                // 如果aSet不是Xml parser的类型，即XML非手机内存储的文件，跳转至  // MK : 201402251535 - default_workspace.xml
                if (!(aSet instanceof XmlResourceParser)) {
                    mValues = new String[ai.length];
                    mSet = aSet;
                    for (int i = 0; i < mSet.getAttributeCount(); i++) {
                        String string = mSet.getAttributeName(i);
                        string = string.substring("launcher:".length(), string.length());
                        
                        // 解析XML文件的属性， 此需要与attrs.xml中的<declare-styleable name="Favorite">次序一致
                        if (!"className".equals(string))
                        {
                            if (!"packageName".equals(string))
                            {
                                if (!"container".equals(string))
                                {
                                    if (!"screen".equals(string))
                                    {
                                        if (!"x".equals(string))
                                        {
                                            if (!"y".equals(string))
                                            {
                                                if (!"spanX".equals(string))
                                                {
                                                    if (!"spanY".equals(string))
                                                    {
                                                        if (!"icon".equals(string))
                                                        {
                                                            if (!"title".equals(string))
                                                            {
                                                                if (!"uri".equals(string))
                                                                {
                                                                    if (!"action".equals(string))
                                                                    {
                                                                        if (!"iconResource".equals(string))
                                                                        {
                                                                            if (!"retained".equals(string))
                                                                            {
                                                                                if ("presets_container".equals(string))
                                                                                    mValues[R.styleable.Favorite_presets_container] = mSet.getAttributeValue(i);
                                                                            } else
                                                                            {
                                                                                mValues[R.styleable.Favorite_retained] = mSet.getAttributeValue(i);
                                                                            }
                                                                        } else
                                                                        {
                                                                            mValues[R.styleable.Favorite_iconResource] = mSet.getAttributeValue(i);
                                                                        }
                                                                    } else
                                                                    {
                                                                        mValues[R.styleable.Favorite_action] = mSet.getAttributeValue(i);
                                                                    }
                                                                } else
                                                                {
                                                                    mValues[R.styleable.Favorite_uri] = mSet.getAttributeValue(i);
                                                                }
                                                            } else
                                                            {
                                                                mValues[R.styleable.Favorite_title] = mSet.getAttributeValue(i);
                                                            }
                                                        } else
                                                        {
                                                            mValues[R.styleable.Favorite_icon] = mSet.getAttributeValue(i);
                                                        }
                                                    } else
                                                    {
                                                        mValues[R.styleable.Favorite_spanY] = mSet.getAttributeValue(i);
                                                    }
                                                } else
                                                {
                                                    mValues[R.styleable.Favorite_spanX] = mSet.getAttributeValue(i);
                                                }
                                            } else
                                            {
                                                mValues[R.styleable.Favorite_y] = mSet.getAttributeValue(i);
                                            }
                                        } else
                                        {
                                            mValues[R.styleable.Favorite_x] = mSet.getAttributeValue(i);
                                        }
                                    } else
                                    {
                                        mValues[R.styleable.Favorite_screen] = mSet.getAttributeValue(i);
                                    }
                                } else
                                {
                                    mValues[R.styleable.Favorite_container] = mSet.getAttributeValue(i);
                                }
                            } else
                            {
                                mValues[R.styleable.Favorite_packageName] = mSet.getAttributeValue(i);
                            }
                        } else
                        {
                            mValues[R.styleable.Favorite_className] = mSet.getAttributeValue(i);
                        }
                    }
                } else {
                    // MK : 201402251535 - default_workspace.xml
                    mTypedArray = mContext.obtainStyledAttributes(aSet, ai);
                }
            }
        };
        
        private static final String TAG_FAVORITES = "favorites";
        private static final String TAG_FAVORITE = "favorite";
        private static final String TAG_CLOCK = "clock";
        private static final String TAG_SEARCH = "search";
        private static final String TAG_APPWIDGET = "appwidget";
        private static final String TAG_SHORTCUT = "shortcut";
        private static final String TAG_FOLDER = "folder";
        private static final String TAG_EXTRA = "extra";

        
        private final Context mContext;
        private final AppWidgetHost mAppWidgetHost;
        private long mMaxId;
        private long mPresetsContainerId;
        
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
            mMaxId = -1L;
            mPresetsContainerId = -1L;
            mAppWidgetHost = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);
            if (mMaxId == -1) {
                mMaxId = initializeMaxId(getWritableDatabase());
            }
        }
        
        private void sendAppWidgetResetNotify() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null);
        }
        
        /**
         * 功能： 添加APP到数据库中
         */
        private boolean addAppShortcut(SQLiteDatabase db, 
                ContentValues values, 
                FakedTypedArray array, 
                PackageManager pack, Intent intent){
            String packageName;
            String className;
            ActivityInfo activityinfo;
            
            // 得到包名及入口类名
            if (array != null) {
                packageName = array.getString(R.styleable.Favorite_packageName);
                className = array.getString(R.styleable.Favorite_className);
            }else {
                packageName = intent.getComponent().getPackageName();
                className = intent.getComponent().getClassName();
            }
            try {
                ComponentName cn;
                try {
                    cn = new ComponentName(packageName, className);
                    activityinfo = pack.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe){
                    String[] packages = pack.currentToCanonicalPackageNames(new String[] {packageName});
                    cn = new ComponentName(packages[0], className);
                    activityinfo = pack.getActivityInfo(cn, 0);
                }
                
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                values.put(Favorites.INTENT, intent.toUri(0));
                values.put(Favorites.TITLE, activityinfo.loadLabel(pack).toString());
                values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPLICATION);
                values.put(Favorites.SPANX, 1);
                values.put(Favorites.SPANY, 1);
                
                // 插入数据库favorites表中，aa如果不为0表示插入成功
                long aa = db.insert(TABLE_FAVORITES, null, values);
                if (aa != 0) {
                    Log.w(TAG, "MK : Insert: " + aa);
                }

            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "MK : Unable to add favorite: " + packageName +
                        "/" + className, e);
                return false;
            }
            return true;
        }
        
        private boolean addClockWidget(SQLiteDatabase db, ContentValues values){
            return true;
        }
        
        private boolean addAppWidget(SQLiteDatabase db, 
                ContentValues values, 
                ComponentName name, int i, int j){
            boolean allocatedAppWidgets = false;
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            
            try {
                int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                
                values.put(Favorites.ITEM_TYPE, Integer.valueOf(Favorites.ITEM_TYPE_APPWIDGET));
                values.put(Favorites.SPANX, Integer.valueOf(i));
                values.put(Favorites.SPANY, Integer.valueOf(j));
                values.put(Favorites.APPWIDGET_ID, Integer.valueOf(appWidgetId));
                long aa = db.insert(TABLE_FAVORITES, null, values);
                if (aa != 0) {
                    Log.w(TAG, "MK : Insert: " + aa);
                }
                allocatedAppWidgets = true;
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, name);
                
            } catch (RuntimeException ex) {
                Log.e(TAG, "MK : Problem allocating appWidgetId", ex);
            }
            
            return allocatedAppWidgets;
        }
        
        private boolean addAppWidget(SQLiteDatabase db, 
                ContentValues values, 
                FakedTypedArray array, 
                PackageManager pack){
            String packageName = array.getString(R.styleable.Favorite_packageName);
            String className = array.getString(R.styleable.Favorite_className);
            
            if (packageName == null || className == null) {
                return false;
            }

            boolean hasPackage = true;
            
            ComponentName cn = new ComponentName(packageName, className);
            
            try {
                pack.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                String[] packages = pack.currentToCanonicalPackageNames(
                        new String[] { packageName });
                cn = new ComponentName(packages[0], className);
                try {
                    pack.getReceiverInfo(cn, 0);
                } catch (Exception e1) {
                    hasPackage = false;
                }
            }
            
            if (hasPackage)
                hasPackage = addAppWidget(db, values, cn, array.getInt(R.styleable.Favorite_spanX, 0), array.getInt(R.styleable.Favorite_spanY, 0));
            else
                hasPackage = false;
            return hasPackage;
        }
        
        private boolean addFolder(SQLiteDatabase db, 
                ContentValues values, FakedTypedArray array){
            return true;
        }
        
        private boolean addGadget(SQLiteDatabase db, 
                ContentValues values, int i){
            return true;
        }
        
        private ComponentName getProviderInPackage(String packageName) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            List<AppWidgetProviderInfo> providers = appWidgetManager.getInstalledProviders();
            if (providers == null) return null;
            final int providerCount = providers.size();
            for (int i = 0; i < providerCount; i++) {
                ComponentName provider = providers.get(i).provider;
                if (provider != null && provider.getPackageName().equals(packageName)) {
                    return provider;
                }
            }
            return null;
        }
        
        private ComponentName getSearchWidgetProvider() {
            SearchManager searchManager =
                    (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
            ComponentName searchComponent = searchManager.getGlobalSearchActivity();
            if (searchComponent == null) return null;
            return getProviderInPackage(searchComponent.getPackageName());
        }
        
        private boolean addSearchWidget(SQLiteDatabase db, 
                ContentValues values){
            ComponentName cn = getSearchWidgetProvider();
            return addAppWidget(db, values, cn, 4, 1);
        }
        
        private boolean addUriShortcut(SQLiteDatabase db, 
                ContentValues values, FakedTypedArray array)        {
            return true;
        }
        
        /**************************
         * 功能：  读取桌面的默认布局 
         * 描述：  如果/data/media/customized目录下存有default_workspace.xml则以此为默认的布局，
         *      否则使用R.xml.default_workspace为默认布局
         * 关键：  1. XmlPullParser解析XML文件
         *      2. 使用FileReader打开手机内存中的XML文件
         *      3. XmlUtils XML工具类的使用
         */
        private int loadFavorites(SQLiteDatabase db){
            int iAdd = 0;
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ContentValues contentValues = new ContentValues();
            PackageManager packageManager = mContext.getPackageManager();
            
            try {
                // 解析WORKSPACE
                XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
                if (xmlPullParser == null) {
                    // 如果NEW资源失败则直接使用默认WORKSPACE
                    xmlPullParser = mContext.getResources().getXml(ResConfig.getDefaultWorkspaceXmlId());
                }else {
                    try {
                        // 从文件中读取default workspace 的布局, 路径为/data/media/customized/default_workspace.xml
                        FileReader fileReader = new FileReader(ResConfig.getCustomizedDefaultWorkspaceXmlPath());
                        xmlPullParser.setInput(fileReader);
                    } catch (FileNotFoundException fileNotFoundException) {
                        Log.w(TAG, "MK : Got exception parsing favorites.", fileNotFoundException);
                        xmlPullParser = mContext.getResources().getXml(ResConfig.getDefaultWorkspaceXmlId());
                    }
                }
                
                // 由资源xml文件获得的各属性接口类
                AttributeSet attributeSet = Xml.asAttributeSet(xmlPullParser);
                
                // 遍析favorites中的内容，减少不必要的存储
                XmlUtils.beginDocument(xmlPullParser, TAG_FAVORITES);
                int depth = xmlPullParser.getDepth();
                
                int type;
                while (((type = xmlPullParser.next()) != XmlPullParser.END_TAG ||
                        xmlPullParser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue;
                    }
                    
                    boolean added = false;
                    final String name = xmlPullParser.getName();
                    String containerString;
                    if (LOGD) {
                        Log.w(TAG, "MK : LoadFavorites: name = " + name);
                    }
                    
                    // 将读取的XML属性值存入TypedArray数组中
                    FakedTypedArray fakedTypedArray = new FakedTypedArray(attributeSet, R.styleable.Favorite);
                    
                    // If we are adding to the hotseat, the screen is used as the position in the
                    // hotseat. This screen can't be at position 0 because AllApps is in the
                    // zeroth position.
                    
                    // if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    // throw new RuntimeException("Invalid screen position for hotseat item");
                    // }

                    // 将原存储的数据清除，重新写入数据
                    contentValues.clear();
                    containerString = fakedTypedArray.getString(R.styleable.Favorite_container);
                    long container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    if (!TextUtils.isEmpty(containerString)) {
                        container = Integer.parseInt(containerString);
                    }
                    
                    if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        containerString = String.valueOf(container);
                        contentValues.put(LauncherSettings.Favorites.SCREEN, fakedTypedArray.getString(R.styleable.Favorite_screen));
                    }
                                       
                    if (container < 0) {
                        contentValues.put(LauncherSettings.Favorites.CELLX, fakedTypedArray.getString(R.styleable.Favorite_x));
                        contentValues.put(LauncherSettings.Favorites.CELLY, fakedTypedArray.getString(R.styleable.Favorite_y));
                    }
                    
                    contentValues.put(LauncherSettings.Favorites.CONTAINER, containerString);
                    
                    if ("default".equals(name)) {
                        Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                        editor.putLong("pref_default_screen", contentValues.getAsLong("screen").longValue());
                        editor.commit();
                    }
                    
                    // 不同类别使用不同的处理函数方法
                    if (TAG_FAVORITE.equals(name)) {
                        added = addAppShortcut(db, contentValues, fakedTypedArray, packageManager, intent);
                    } else if (TAG_SEARCH.equals(name)) {
                        added = addSearchWidget(db, contentValues);
                    } else if (TAG_CLOCK.equals(name)) {
                        added = addClockWidget(db, contentValues);
                    } else if (TAG_APPWIDGET.equals(name)) {
                        added = addAppWidget(db, contentValues, fakedTypedArray, packageManager);
                    } else if (TAG_SHORTCUT.equals(name)) {
                        added = addUriShortcut(db, contentValues, fakedTypedArray);
                    } else if (TAG_FOLDER.equals(name)) {
                        //folder属性里面的参数要多于2个，才能形成文件夹。
                        added = addFolder(db, contentValues, fakedTypedArray);
                    }
                    
                    if (added) iAdd++;
                    fakedTypedArray.recycle();
                }
                
            } catch (XmlPullParserException xmlPullParserException) {
                Log.w(TAG, "MK : Got exception xml parser. ", xmlPullParserException);
            } catch (IOException ioException) {
                Log.w(TAG, "MK : Got exception parsing favorites.", ioException);
            }
            
            return iAdd;
        }
        
        private int loadPresetsApps(SQLiteDatabase db){
            int k = 0;
            if (mPresetsContainerId >= 0L) {
                File file;
                file = new File("/data/media/preset_apps");
                if (!file.isDirectory()){
                    return k;
                }
            
                File aFile[] = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.toLowerCase().endsWith(".apk");
                    }
                });
                
                if (aFile == null) {
                    return k;
                }
                
                int length = aFile.length;
                
                if (length <= 0) {
                    return k;
                }
                
                Resources resources = mContext.getResources();;
                PackageManager packagemanager = mContext.getPackageManager();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                ContentValues contentvalues = new ContentValues();
                
                int i = 0;
                while (i < length) {
                    File fileT = aFile[i];
                    PackageInfo packageinfo = packagemanager.getPackageArchiveInfo(fileT.getAbsolutePath(), PackageManager.PERMISSION_GRANTED);;
                    if (packageinfo == null){
                        break;
                    }
                    ApplicationInfo aInfo = packageinfo.applicationInfo;
                    
                    try {
                        AssetManager assetManager = resources.getAssets();
                        assetManager.list(fileT.getAbsolutePath());
                        Resources resourcesT = new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
                        assetManager = null;
                        
                        if (packageinfo.applicationInfo.labelRes == 0){
                            break;
                        }
                        
                        CharSequence label = resourcesT.getText(aInfo.labelRes);
                        if (label == null) {
                            if (aInfo.nonLocalizedLabel != null)
                                label = aInfo.nonLocalizedLabel;
                            else
                                label = aInfo.packageName;
                        }
                        
                        if (aInfo.icon == 0) {
                            break;
                        }
                        
                        Drawable drawable = resourcesT.getDrawable(aInfo.icon);
                        
                        if (drawable == null) {
                            drawable = mContext.getPackageManager().getDefaultActivityIcon();
                        }
                        
                        if (drawable != null && label != null) {
                            contentvalues.put("title", label.toString());
                            contentvalues.put("container", Long.valueOf(mPresetsContainerId));
                            contentvalues.put("iconPackage", packageinfo.packageName);
                            contentvalues.put("spanX", Integer.valueOf(1));
                            contentvalues.put("spanY", Integer.valueOf(1));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            intent.setDataAndType(Uri.fromFile(fileT), MimeUtils.guessMimeTypeFromExtension("apk"));
                            contentvalues.put("intent", intent.toUri(0));
                            contentvalues.put("itemType", Integer.valueOf(1));
                            contentvalues.put("itemFlags", Integer.valueOf(1));
                            contentvalues.put("iconType", Integer.valueOf(1));
                            ItemInfo.writeBitmap(contentvalues, ((BitmapDrawable)drawable).getBitmap());
                            db.insert("favorites", null, contentvalues);
                            contentvalues.clear();
                                k++;
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    i++;
                }
            }
            return k;
        }
        
        private void createPackagesTable(SQLiteDatabase db){
            // 创建 screens 数据库表
            db.execSQL("DROP TABLE IF EXISTS packages");
            db.execSQL("CREATE TABLE packages("
                    + "_id INTEGER PRIMARY KEY,"
                    + "name TEXT,"
                    + "keepItem BOOLEAN);");
        }
        
        /**
         * 功能： 创建screen表， screenOrder来排列屏幕的位置 
         * @param db
         */
        private void createScreensTable(SQLiteDatabase db){
            // 创建 screens 数据库表
            db.execSQL("DROP TABLE IF EXISTS screens");
            db.execSQL("CREATE TABLE screens("
                    + "_id INTEGER PRIMARY KEY,"
                    + "title TEXT,"
                    + "screenOrder INTEGER NOT NULL DEFAULT -1);");
            
            // 查询favorites表中最大screen值
            String columns[] = new String[]{"MAX(screen)"};
            Cursor cursor = db.query("favorites", columns, null, null, null, null, null);
            
            if (cursor == null) {
                return;
            }
            
            try {
                ContentValues contentValues;
                if (cursor.moveToNext()) {
                    int j = cursor.getInt(0) + 1;
                    long al[] = new long[j];
                    
                    contentValues = new ContentValues();
                    for (int i = 0; i < j; i++) {
                        contentValues.clear();
                        contentValues.put("screenOrder", Integer.valueOf(i));
                        al[i] = db.insert("screens", null, contentValues);
                    }
                    /*   ***** 无效 ***
                    j--;
                    if (j >= 0) {
                        contentValues.clear();
                        contentValues.put("screen", Long.valueOf(al[j]));
                        String selString[] = new String[]{String.valueOf(j)};
                        
                        db.update("favorites", contentValues, "screen=?", selString);   
                    }*/
                }   
                cursor.close();
            } catch (Exception e) {
                cursor.close();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mMaxId = 1L;
            
            // 创建 favorites 数据库表
            db.execSQL("DROP TABLE IF EXISTS favorites");
            db.execSQL("CREATE TABLE favorites("
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
            
            if (mAppWidgetHost != null) {
                mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }
            loadFavorites(db);
            loadPresetsApps(db);
            createScreensTable(db);
            createPackagesTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            
        }
        
        private long initializeMaxId(SQLiteDatabase db) {
            long id = -1L;
            Cursor cursor = db.rawQuery("SELECT MAX(_id) FROM favorites", null);
            if (cursor != null && cursor.moveToNext())
                id = cursor.getLong(0);
            if (cursor != null)
                cursor.close();
            if (id != -1L)
                return id;
            else
                throw new RuntimeException("Error: could not query max id");
        }

        public void updateMaxId(SQLiteDatabase db){
            mMaxId = initializeMaxId(db);
        }
        
        public long generateNewId(){
            if (mMaxId >= 0L){
                mMaxId = 1L + mMaxId;
                return mMaxId;
            }else{
                throw new RuntimeException("Error: max id was not initialized");
            }
        }       
    }
}
