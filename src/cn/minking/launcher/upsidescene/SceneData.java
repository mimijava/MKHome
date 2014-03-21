package cn.minking.launcher.upsidescene;
/**
 * 作者：      minking
 * 文件名称:    SceneData.java
 * 创建时间：    2014-03-03
 * 描述：  
 * 更新内容
 * ====================================================================================
 * 2014030301 ： 
 * ====================================================================================
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import cn.minking.launcher.upsidescene.SceneData.SpriteCell.Shortcuts;
import android.content.Context;

public class SceneData {
    public class Screen{
        private int mWidth;
        private int mHeight;
        private int mHome;
        private boolean mInteraction;
        private LinkedList<Sprite> mSprites;
        private LinkedList<Sprite> mSortedSprites;
        
        public int getHeight() {
            return mHeight;
        }
        public int getWidth() {
            return mWidth;
        }
        public Collection<Sprite> getSprites() {
            return mSortedSprites;
        }
        
        public Screen() {
            mHome = 0;
            mInteraction = false;
            mSprites = new LinkedList<Sprite>();
            mSortedSprites = new LinkedList<Sprite>();
        }
    }
    
    public class SpriteCell extends Sprite{

        public class Shortcuts{
            private String mFolderTitle;
            
            public boolean setFolderTitle(String s) {
                boolean flag = true;
                if (mCurrentContentType != 1) {
                    flag = false;
                } else {
                    mFolderTitle = s;
                }
                return flag;
            }
        }
        
        @Override
        public int getType() {
            // TODO Auto-generated method stub
            return 3;
        }
        
        public Shortcuts getShortcuts() {
            return mShortcuts;
        }
        
        
    }
    
    public abstract class Sprite {
        protected int mBottom;
        private int mIndex;
        private int mLeft;
        protected int mRight;
        private int mTop;

        public void calcSize(float f) {
            mLeft = Math.round(f * (float)mLeft);
            mTop = Math.round(f * (float)mTop);
        }

        public boolean checkValidate() {
            boolean flag;
            if (mLeft == -1 || mTop == -1 || mIndex == -1){
                flag = false;
            } else {
                flag = true;
            }
            return flag;
        }

        public int getBottom() {
            return mBottom;
        }

        public int getIndex() {
            return mIndex;
        }

        public int getLeft() {
            return mLeft;
        }

        public int getRight() {
            return mRight;
        }

        public int getTop() {
            return mTop;
        }

        public abstract int getType();

        public boolean load(XmlPullParser xmlpullparser, BufferedReader bufferedreader, ZipFile zipfile)
            throws XmlPullParserException, IOException {
            for (int i = 0; i < xmlpullparser.getAttributeCount() - 1; i++) {
                String s = xmlpullparser.getAttributeName(i);
                if ("left".equals(s)) {
                    mLeft = Integer.parseInt(xmlpullparser.getAttributeValue(i));
                } else {
                    mIndex = Integer.parseInt(xmlpullparser.getAttributeValue(i));
                }
                if ("top".equals(s)){
                    mTop = Integer.parseInt(xmlpullparser.getAttributeValue(i));
                }
                
            }
            return true;
        }

        public void save(BufferedWriter bufferedwriter) throws IOException {
        }

        public String toString() {
            return (new StringBuilder()).append("[index:").append(mIndex).append(",left:").append(mLeft).append(",top:").append(mTop).append(",right:").append(mRight).append(",bottom:").append(mBottom).append("]").toString();
        }

        public Sprite() {
            mLeft = -1;
            mTop = -1;
            mRight = -1;
            mBottom = -1;
            mIndex = -1;
        }
    }
    
    private int mCurrentContentType;
    private int mDefaultContentType;
    private Shortcuts mShortcuts;
    
    public boolean loadData(Context context){
        return false;
    }
}
