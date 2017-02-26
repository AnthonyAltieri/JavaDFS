package naming;

import common.Component;
import common.DFSException;
import common.Path;
import common.Type;
import storage.Command;
import storage.Storage;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by anthonyaltieri on 2/24/17.
 */

/**
 * A class for filesystem management, this class does not manage the physical files
 * in <bold>any</bold> capacity. This class is only for the bookkeeping that is
 * necessary in a filesystem.
 */
public class FileSystem
{
    FileNode root;

    FileSystem()
    {
        this.root = new FileNode(new Path("/"), null, null);
    }

    public void add(Path path, Storage storage, Command command)
        throws FileNotFoundException
    {
        FileNode parent = getNode(path.parent());
        parent.getChildren().put(path, new FileNode(path, storage, command));
    }

    public boolean remove(Path path)
        throws FileNotFoundException
    {
        FileNode parent = getNode(path.parent());
        return (parent.getChildren().remove(path) != null);
    }


    public FileNode getNode(Path path)
        throws FileNotFoundException
    {
        ArrayList<Path> subPaths = path.getSubPaths();
        subPaths.remove(0);
        FileNode focus = this.root;
        while (!subPaths.isEmpty())
        {
            Path currentPath = subPaths.remove(0);
            FileNode next = focus.getChildren().get(currentPath);
            if (next == null)
            {
                if (subPaths.isEmpty())
                {
                    throw new FileNotFoundException("no file for path");
                }
                FileNode newNode = new FileNode(
                    currentPath,
                    focus.getStorage(),
                    focus.getCommand()
                );
                focus.getChildren().put(currentPath,newNode);
                focus = newNode;
                continue;
            }
            focus = next;
        }
        return focus;
    }

    public String[] getChildrenStrings(Path path)
        throws FileNotFoundException
    {
        FileNode node = getNode(path);
        if (!this.isDirectory(path))
            throw new FileNotFoundException("path is not to a directory");
        ArrayList<String> children = new ArrayList<>();
        Arrays.asList(getNode(path).getChildren().keySet().toArray())
            .stream()
            .forEach(key -> {
                children.add(((Path) key).getComponent().toString());
            });
        return children.toArray(new String[children.size()]);
    }

    public Storage getStorage(Path file)
        throws FileNotFoundException
    {
        return getNode(file).getStorage();
    }

    public boolean hasPath(Path path)
    {
        try {
            FileNode parent = getNode(path);
            return true;
        } catch (FileNotFoundException e)
        {
            return false;
        }
    }

    public String toString()
    {
        ArrayList<String> leaves = new ArrayList<>();
        Stack<FileNode> stack = new Stack<>();
        stack.push(this.root);
        while (!stack.isEmpty())
        {
            FileNode node = stack.pop();
            Set<Path> children = node.getChildren().keySet();
            if (children.isEmpty())
            {
                leaves.add(node.getPath().toString());
            }
            else
            {
                for (Path path : children)
                {
                    stack.push(node.getChildren().get(path));
                }
            }
        }
        String toReturn = "[FileSystem]\n";
        for (String leaf : leaves)
        {
            toReturn += (leaf  + "\n");
        }
        return toReturn;
    }

    boolean isDirectory(Path path)
        throws FileNotFoundException
    {
        return this.getFileType(path) == Type.DIRECTORY;
    }

    boolean isFile(Path path)
        throws FileNotFoundException
    {
        return this.getFileType(path) == Type.FILE;
    }

    Type getFileType(Path path)
        throws FileNotFoundException
    {
        FileNode node = getNode(path);
        Hashtable<Path, FileNode> children = node.getChildren();
        if (children.size() > 0)
            return Type.DIRECTORY;
        return Type.FILE;
    }
}
