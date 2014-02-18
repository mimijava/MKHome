package cn.minking.launcher;

import java.util.ArrayList;
import java.util.HashMap;

import cn.minking.launcher.gadget.GadgetInfo;
import cn.minking.launcher.upsidescene.SceneData;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;

public class Launcher extends Activity implements OnClickListener,
		OnLongClickListener, LauncherModel.Callbacks{
	static final private String LTAG = "ZwLauncher";
	private Workspace mWorkspace = null;
	private DragLayer mDragLayer = null;
	private DragController mDragController = null;
	private LauncherModel mModel = null;
	private static boolean mIsHardwareAccelerated = false;
	private IconCache mIconCache = null;
	private Point mTmpPoint = new Point();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LTAG, "onCreate");
		Window localWindow = getWindow();
		localWindow.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		localWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//mIsHardwareAccelerated = ((Window)(localWindow)).getWindowManager().isHardwareAccelerated();
		LauncherApplication launcherApplication = (LauncherApplication)getApplication();
		mModel = launcherApplication.setLauncher(this);
		mIconCache = launcherApplication.getIconCache();
		mDragController = new DragController();
		setWallpaperDimension();
		setContentView(R.layout.launcher);
		setupViews();
		mModel.startLoader(getApplicationContext(),	true);
	}

	private void setWallpaperDimension() {
		WallpaperManager wallpaperManager = (WallpaperManager)getSystemService("wallpaper");
		Display display = getWindowManager().getDefaultDisplay();
		int rotation = display.getRotation();
		int width = 0;
		int height = 0;
		boolean bFlag = false;
		
		if (rotation != Surface.ROTATION_0 && rotation != Surface.ROTATION_180) {
			bFlag = false;
		}else {
			bFlag = true;
		}
		display.getSize(mTmpPoint);
		
		if (!bFlag) {
			width = mTmpPoint.y;
			height = mTmpPoint.x;
		}else {
			width = mTmpPoint.x;
			height = mTmpPoint.y;
		}
		wallpaperManager.suggestDesiredDimensions(width * 2, height);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mWorkspace.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	public boolean onLongClick(View paramView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onClick(View paramView) {
		// TODO Auto-generated method stub

	}
	
	public void startActivityEx(Intent intent, Bundle options) {
		startActivity(intent, options);
	}
	
	@Override
	public void startActivity(Intent intent, Bundle options) {
		super.startActivity(intent);
	}

	private void setupViews() {
		DragController dragController = mDragController;
		mDragLayer = (DragLayer)findViewById(R.id.drag_layer);
		mWorkspace = (Workspace)mDragLayer.findViewById(R.id.workspace);
		Workspace workspace = mWorkspace;
		workspace.setHapticFeedbackEnabled(false);
		workspace.setOnLongClickListener(this);
		workspace.setDragController(dragController);
		workspace.setLauncher(this);
	}

	@Override
	public void bindAppWidget(LauncherAppWidgetInfo launcherappwidgetinfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindAppsAdded(ArrayList arraylist) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindAppsRemoved(ArrayList arraylist) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindFolders(HashMap hashmap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindGadget(GadgetInfo gadgetinfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindItems(ArrayList arraylist, int i, int j) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindUpsideScene(SceneData scenedata) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishBindingMissingItems() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishBindingSavedItems() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishLoading() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getCurrentWorkspaceScreen() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void reloadWidgetPreview() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startBinding() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startLoading() {
		// TODO Auto-generated method stub
		
	}

	
}
