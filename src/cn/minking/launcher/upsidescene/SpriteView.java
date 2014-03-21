package cn.minking.launcher.upsidescene;

import cn.minking.launcher.FolderIcon;
import cn.minking.launcher.FolderInfo;
import cn.minking.launcher.R;
import cn.minking.launcher.ShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class SpriteView extends FrameLayout {
    private class UpsideFolderInfo extends FolderInfo {
        public void setTitle(CharSequence charsequence, Context context) {
            setTitle(charsequence, context);
            ((SceneData.SpriteCell)mSpriteData).getShortcuts().setFolderTitle(charsequence.toString());
        }

        private UpsideFolderInfo() {
            super();
        }
    }

    private View mContent;
    private Context mContext;
    private SceneScreen mSceneScreen;
    private SceneData.Sprite mSpriteData;

    public SpriteView(Context context) {
        this(context, null);
    }

    public SpriteView(Context context, AttributeSet attributeset)
    {
        this(context, attributeset, 0);
    }

    public SpriteView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        mContext = context;
    }
/*
    private View createFolder(SceneData.SpriteCell.Shortcuts shortcuts, ComponentName acomponentname[]) {
        PackageManager packagemanager = mContext.getPackageManager();
        UpsideFolderInfo upsidefolderinfo = new UpsideFolderInfo();
        upsidefolderinfo.title = shortcuts.getFolderTitle();
        if (TextUtils.isEmpty(((FolderInfo) (upsidefolderinfo)).title)){
            upsidefolderinfo.title = mSceneScreen.getContext().getString(R.string.folder_name);
        }
        for (int i = 0; i < acomponentname.length; i++) {
            ComponentName componentName = acomponentname[i];
            ShortcutInfo shortcutinfo = new ShortcutInfo();
            shortcutinfo.intent = new Intent();
            shortcutinfo.intent.setComponent(componentName);
            ResolveInfo resolveInfo = packagemanager.resolveActivity(shortcutinfo.intent, 0);
            if (resolveInfo != null) {
                shortcutinfo.title = resolveInfo.loadLabel(packagemanager);
                upsidefolderinfo.add(shortcutinfo);
            }
        }
                
                
        FolderIcon folderIcon = FolderIcon.fromXml(R.layout.folder_icon, mSceneScreen.getLauncher(), null, upsidefolderinfo);
        folderIcon.setTag(upsidefolderinfo);
        folderIcon.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSceneScreen.getLauncher().openFolder((FolderInfo)view.getTag(), (FolderIcon)view);
            }
        });
        return folderIcon;
    }

    private View createShortcut(SceneData.SpriteCell spritecell) {
        View view = null;
        SceneData.SpriteCell.Shortcuts shortcuts = spritecell.getShortcuts();
        ComponentName acomponentname[] = shortcuts.getComponentNames();
        if (acomponentname.length <= 1) {
            ShortcutInfo shortcutinfo = new ShortcutInfo();
            shortcutinfo.intent = new Intent();
            shortcutinfo.intent.setComponent(acomponentname[0]);
            PackageManager packagemanager = mContext.getPackageManager();
            acomponentname = packagemanager.resolveActivity(shortcutinfo.intent, 0);
            if (acomponentname != null) {
                shortcutinfo.title = acomponentname.loadLabel(packagemanager);
                view = SpriteShortcutIcon.fromXml(mContext, mSceneScreen.getLauncher().getIconCache(), null, shortcutinfo);
                view.setIconTitleVisible(shortcuts.isShowIcon(), shortcuts.isShowTitle());
            }
        } else {
            view = createFolder(shortcuts, acomponentname);
        }
        return view;
    }

    private View createWidget(SceneData.SpriteCell spritecell)
    {
        Object obj1;
label0:
        {
            obj1 = null;
            Object obj = spritecell.getWidget();
            if (obj != null)
            {
                switch (spritecell.getContentType())
                {
                default:
                    break;

                case 2: // '\002'
                    android.appwidget.AppWidgetProviderInfo appwidgetproviderinfo = AppWidgetManager.getInstance(mContext).getAppWidgetInfo(((SceneData.SpriteCell.Widget) (obj)).getId());
                    obj1 = mSceneScreen.getAppWidgetHost().createView(mContext, ((SceneData.SpriteCell.Widget) (obj)).getId(), appwidgetproviderinfo);
                    ((AppWidgetHostView) (obj1)).setAppWidget(((SceneData.SpriteCell.Widget) (obj)).getId(), appwidgetproviderinfo);
                    break label0;

                case 3: // '\003'
                    Object obj2 = GadgetFactory.getInfo(((SceneData.SpriteCell.Widget) (obj)).getGadgetType());
                    if (obj2 != null)
                    {
                        obj1 = GadgetFactory.createGadget(mSceneScreen.getLauncher(), ((com.miui.home.launcher.gadget.GadgetInfo) (obj2)), 101);
                        ((Gadget) (obj1)).onAdded();
                        ((Gadget) (obj1)).onCreate();
                        obj2 = ((SceneData.SpriteCell.Widget) (obj)).getLocation();
                        if (obj2 != null)
                        {
                            obj = new Bundle();
                            ((Bundle) (obj)).putString("RESPONSE_PICKED_RESOURCE", ((String) (obj2)));
                            ((Gadget) (obj1)).updateConfig(((Bundle) (obj)));
                        }
                        ((Gadget) (obj1)).onStart();
                    }
                    break;

                case 4: // '\004'
                    obj1 = new AwesomeGadget(mContext);
                    ((AwesomeGadget) (obj1)).initConfig(((SceneData.SpriteCell.Widget) (obj)).getLocation());
                    obj1 = obj1;
                    ((Gadget) (obj1)).onAdded();
                    ((Gadget) (obj1)).onCreate();
                    ((Gadget) (obj1)).onStart();
                    break;
                }
                obj1 = (View)obj1;
            } else
            {
                obj1 = null;
            }
        }
        return ((View) (obj1));
    }

    private void refreshGadgetEditMode()
    {
        if (mSpriteData.getType() == 3 && ((SceneData.SpriteCell)mSpriteData).getContentType() == 3)
        {
            Gadget gadget = (Gadget)mContent;
            if (gadget != null)
                if (!mSceneScreen.isInEditMode())
                    gadget.onEditDisable();
                else
                    gadget.onEditNormal();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent motionevent)
    {
        if (motionevent.getAction() == 0 && !mSceneScreen.isInEditMode() && isEditable())
            mSceneScreen.setTouchedSprite(this);
        return dispatchTouchEvent(motionevent);
    }

    public void exitEditMode()
    {
        setClickable(false);
        refreshEditState();
        refreshGadgetEditMode();
        invalidate();
    }

    public View getContentView()
    {
        return mContent;
    }

    public SceneData.Sprite getSpriteData()
    {
        return mSpriteData;
    }

    public void gotoEditMode()
    {
        setClickable(isEditable());
        refreshEditState();
        refreshGadgetEditMode();
        invalidate();
    }

    public boolean isEditable()
    {
        boolean flag = true;
        if (mSpriteData.getType() == flag || mSpriteData.getType() == 2)
            flag = false;
        return flag;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionevent)
    {
        boolean flag;
        if (!mSceneScreen.isInEditMode() || !isEditable())
            flag = false;
        else
        if (mSpriteData.getType() != 3 || ((SceneData.SpriteCell)mSpriteData).getContentType() != 3 || mSceneScreen.getEditFocusedSprite() != this)
            flag = true;
        else
            flag = false;
        return flag;
    }

    public boolean performClick()
    {
        boolean flag;
        if (!mSceneScreen.isInEditMode() || !isEditable() || mSceneScreen.getEditFocusedSprite() == this)
        {
            flag = performClick();
        } else
        {
            mSceneScreen.setEditFocusedSprite(this);
            flag = true;
        }
        return flag;
    }

    public void rebuildContentView()
    {
        switch (mSpriteData.getType())
        {
        default:
            throw new RuntimeException((new StringBuilder()).append("unknown sprite type:").append(mSpriteData.getType()).toString());

        case 1: // '\001'
            SceneData.SpritePicture spritepicture = (SceneData.SpritePicture)mSpriteData;
            ImageView imageview = new ImageView(mContext);
            imageview.setImageBitmap(spritepicture.getPicture());
            mContent = imageview;
            setLayerType(2, null);
            break;

        case 2: // '\002'
            final SceneData.SpriteButton buttonSprite = (SceneData.SpriteButton)mSpriteData;
            Button button = new Button(mContext);
            StateListDrawable statelistdrawable = new StateListDrawable();
            statelistdrawable.addState(View.PRESSED_STATE_SET, new BitmapDrawable(getResources(), buttonSprite.getPressed()));
            statelistdrawable.addState(View.EMPTY_STATE_SET, new BitmapDrawable(getResources(), buttonSprite.getNormal()));
            button.setBackgroundDrawable(statelistdrawable);
            button.setOnClickListener(new android.view.View.OnClickListener() {

                final SpriteView this$0;
                final SceneData.SpriteButton val$buttonSprite;

                public void onClick(View view)
                {
                    if (!mSceneScreen.isInEditMode())
                        if (!buttonSprite.isBroadcast())
                            LauncherApplication.startActivity(getContext(), buttonSprite.getIntent());
                        else
                            isBroadcast.sendBroadcast(buttonSprite.getIntent());
                }

            
            {
                this$0 = SpriteView.this;
                buttonSprite = spritebutton;
                Object();
            }
            }
);
            mContent = button;
            break;

        case 3: // '\003'
            SceneData.SpriteCell spritecell = (SceneData.SpriteCell)mSpriteData;
            switch (spritecell.getContentType())
            {
            case 0: // '\0'
                mContent = new FrameLayout(mContext);
                break;

            case 1: // '\001'
                mContent = createShortcut((SceneData.SpriteCell)mSpriteData);
                break;

            case 2: // '\002'
            case 3: // '\003'
            case 4: // '\004'
                mContent = createWidget((SceneData.SpriteCell)mSpriteData);
                break;
            }
            if (spritecell.getRotation() != 0F)
                setRotation(spritecell.getRotation());
            if (spritecell.getScale() != 0F)
            {
                setScaleX(spritecell.getScale());
                setScaleY(spritecell.getScale());
            }
            break;
        }
        removeAllViews();
        if (mContent != null)
            addView(mContent, -1, -1);
        if (mContent instanceof Gadget)
            ((Gadget)mContent).onResume();
        refreshGadgetEditMode();
    }

    public void refreshEditState()
    {
        if (!mSceneScreen.isInEditMode() || !isEditable())
        {
            setBackgroundDrawable(null);
        } else
        {
            Drawable drawable;
            if (mSceneScreen.getEditFocusedSprite() != this)
                drawable = getResources().getDrawable(0x7f0200eb);
            else
                drawable = getResources().getDrawable(0x7f0200ec);
            drawable.setBounds(0, 0, getWidth(), getHeight());
            setBackgroundDrawable(drawable);
            setPadding(0, 0, 0, 0);
        }
    }

    public void setSceneScreen(SceneScreen scenescreen)
    {
        mSceneScreen = scenescreen;
    }

    public void setSpriteData(SceneData.Sprite sprite)
    {
        mSpriteData = sprite;
        rebuildContentView();
    }

    public void updateGadgetConfig(Bundle bundle)
    {
        if (mContent instanceof Gadget)
        {
            ((Gadget)mContent).updateConfig(bundle);
            ((SceneData.SpriteCell)mSpriteData).updateInternalGadget(bundle.getString("RESPONSE_PICKED_RESOURCE"));
        }
    }

*/

}