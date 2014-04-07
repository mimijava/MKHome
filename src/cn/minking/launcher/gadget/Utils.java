package cn.minking.launcher.gadget;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import cn.minking.os.Shell;
import android.os.FileUtils;


public class Utils {
    public static boolean copyFile(String dest, String org) {
        boolean flag;
        if (!org.equals(dest)) {
            flag = FileUtils.copyFile(new File(org), new File(dest));
        } else {
            flag = false;
        }
        return flag;
    }

    public static boolean extract(String backupPath, String filePath, String filePathBack) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            file = new File(filePathBack);
        }
        return Shell.copy(file.getAbsolutePath(), backupPath);
    }
    
    public static Element parseManifestInZip(String name) {
        Element element = null;
        InputStream inputStream = null;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(name);
            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            try {
                inputStream = zipFile.getInputStream(zipFile.getEntry("manifest.xml")); 
                try {
                    element = dFactory.newDocumentBuilder().parse(inputStream).getDocumentElement();    
                } catch (Exception e) {
                    // TODO: handle exception
                }
            } catch (IOException e) {
                // TODO: handle exception
            }
        } catch (IOException e) {
            // TODO: handle exception
        }

        if (inputStream != null)
            try {
                inputStream.close();
            }
            catch (IOException _ex) { }
        if (zipFile != null) {
            try {
                zipFile.close();
            }
            catch (IOException _ex) { }
        }
        return element;
    }
}