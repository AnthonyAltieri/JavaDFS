package common;

import java.io.File;

/**
 * Created by anthonyaltieri on 2/23/17.
 */
public class FileService
{
    public static boolean doesExist(String path)
    {
        File file = new File(path);
        return file.exists();
    }

    public static boolean isFile(String path)
    {
        File file = new File(path);
        return file.isFile();
    }

    public static boolean isDirectory(String path)
    {
        File file = new File(path);
        return file.isDirectory();
    }

    public static long getSize(String path)
    {
        File file = new File(path);
        return file.length();
    }

    public static File getFile(String path)
    {
        return new File(path);
    }

}
