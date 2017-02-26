package common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

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

    public static boolean createFile(Path path)
    {
        if (doesExist(path.toString()))
            return false;
        ArrayList<Component> components = path.getComponents();
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(new Path(components.get(0).toString()));
        for (int i = 1 ; i < (components.size()); i++)
        {
            paths.add(new Path(paths.get(i - 1), components.get(i).toString()));
        }
        for (int i = 0 ; i < paths.size() ; i++)
        {
            Path p = paths.get(i);
            File file = new File(p.toString());
            if (!file.exists())
            {
                if (i != (paths.size() - 1))
                {
                    boolean result = file.mkdir();
                    if (!result)
                    {
                        return false;
                    }
                }
                else
                {
                    try
                    {
                        file.createNewFile();
                    }
                    catch (IOException e)
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static File getFile(String path)
    {
        return new File(path);
    }

    public static boolean deleteFile(String path)
    {
        File file = new File(path);
        boolean childResult = true;
        if (file.isDirectory())
        {
            File[] children = file.listFiles();
            for (File child : children)
            {
                if (child.isFile())
                {
                    child.delete();
                }
                else if (child.isDirectory())
                {
                    childResult = childResult && deleteFile(child.getPath());
                }
            }
            return file.delete() && childResult;
        }
        else
        {
            return file.delete();
        }
    }


    public static boolean hasChildren(String path)
    {
        return !isFile(path) && getFile(path).listFiles().length > 0;
    }

    public static void deleteEmptyDirectories(String localPath)
    {
        File file = getFile(localPath);
        if (file.isFile()) {
            return;
        }
        File[] children = file.listFiles();
        Arrays.asList(children)
            .stream()
            .forEach(child -> {
                if (child.isDirectory())
                {
                    deleteEmptyDirectories(child.getPath());
                }
            });
        if (file.listFiles().length == 0)
        {
            file.delete();
        }
    }

}
