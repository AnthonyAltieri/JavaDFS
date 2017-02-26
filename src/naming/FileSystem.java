package naming;

import common.Component;
import common.DFSException;
import common.Path;
import common.Type;
import storage.Command;
import storage.Storage;

import java.io.File;
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
        this.root = new FileNode(new Path("/"), null, null, Type.ROOT);
    }

    public FileNode get(Path path)
        throws FileNotFoundException
    {
        FileNode focus = this.root;
        ArrayList<Path> subPaths = path.getSubPaths();
        // Remove the root path
        subPaths.remove(0);
        while (!subPaths.isEmpty())
        {
            Path p = subPaths.remove(0);
            FileNode next = focus.getChildren().get(p);
            if (next == null)
                throw new FileNotFoundException("path not found");
            focus = next;
        }
        return focus;
    }

    public boolean remove(Path path)
        throws FileNotFoundException
    {
        FileNode parent = this.get(path.parent());
        return (parent.getChildren().remove(path) != null);
    }

    public void add(Path path, Type type)
        throws FileNotFoundException
    {
        FileNode parent = this.get(path.parent());
        FileNode toAdd = new FileNode(path, parent.getStorage(), parent.getCommand(), type);
        parent.getChildren().put(path, toAdd);
    }


    public void register(Path path, Storage storage, Command command)
        throws FileNotFoundException
    {
        ArrayList<Path> subPaths = path.getSubPaths();
        // Remove the root
        subPaths.remove(0);
        FileNode focus = this.root;
        while (!subPaths.isEmpty())
        {
            Path currentPath = subPaths.remove(0);
            FileNode next = focus.getChildren().get(currentPath);
            if (next == null)
            {
                Type nodeType = (subPaths.isEmpty()) ? Type.FILE : Type.DIRECTORY;
                FileNode newNode = new FileNode(currentPath, storage, command, nodeType);
                focus.getChildren().put(currentPath,newNode);
                focus = newNode;
                continue;
            }
            focus = next;
        }
    }

    public String[] getChildrenStrings(Path path)
        throws FileNotFoundException
    {
        FileNode node = this.get(path);
        if (!this.isDirectory(path))
            throw new FileNotFoundException("path is not to a directory");
        ArrayList<String> children = new ArrayList<>();
        Arrays.asList(this.get(path).getChildren().keySet().toArray())
            .stream()
            .forEach(key -> {
                children.add(((Path) key).getComponent().toString());
            });
        return children.toArray(new String[children.size()]);
    }

    public Storage getStorage(Path file)
        throws FileNotFoundException
    {
        return this.get(file).getStorage();
    }

    public boolean hasPath(Path path)
    {
        try {
            FileNode parent = this.get(path);
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
        return get(path).isDirectory();
    }

    boolean isFile(Path path)
        throws FileNotFoundException
    {
        return get(path).isFile();
    }

    void setRoot(Storage storage, Command command)
    {
        FileNode root = new FileNode(new Path("/"), storage, command, Type.ROOT);
        this.root = root;
    }

    FileNode getRoot()
    {
        return this.root;
    }

    Command findAvailableCommand()
    {
        // TODO: maybe do this intelligently
        return findCommand(this.root);
    }

    private Command findCommand(FileNode node)
    {
        if (node.getCommand() != null)
            return node.getCommand();
        if (node.isFile())
            return null;
        Set<Path> keys = node.getChildren().keySet();
        for (Path key : keys)
        {
            FileNode child = node.getChildren().get(key);
            Command result = this.findCommand(child);
            if (result != null)
                return result;
        }
        return null;
    }

}
