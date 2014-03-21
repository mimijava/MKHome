package cn.minking.launcher;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import android.content.Intent;
import android.text.TextUtils;


public class RetainedList{
    private final HashSet<String> mList;
    
    public RetainedList()    {
        mList = new HashSet<String>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/data/media/customized/operator.retained.list"), 1024);
            try {
                String s;
                while ((s = bufferedReader.readLine()) != null){
                    if (!TextUtils.isEmpty(s)){
                        mList.add(s);   
                    }
                }
            } catch (IOException e) { }
        } catch (FileNotFoundException e) { }
    }
    
    public boolean contain(Intent intent) {
        boolean flag;
        if (intent.getComponent() != null) {
            flag = contain(intent.getComponent().getPackageName());
        } else {
            flag = false;
        }
        return flag;
    }

    public boolean contain(String s) {
        return mList.contains(s);
    }
}