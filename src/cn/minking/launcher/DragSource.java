package cn.minking.launcher;

import android.view.View;

public interface DragSource{
	public abstract void onDropCompleted(View view, DropTarget.DragObject dragobject, boolean flag);
}