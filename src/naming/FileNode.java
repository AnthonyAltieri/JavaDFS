package naming;

import common.Component;
import common.Path;
import storage.Storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Created by anthonyaltieri on 2/24/17.
 */
public class FileNode
{
    Path path;
    Storage storage;
    FileNode parent;
    Hashtable<Path, FileNode> children = new Hashtable<>();

    FileNode(Path path, Storage storage, FileNode parent)
    {
        this.path = path;
        this.storage = storage;
        this.parent = parent;
    }

    public Path getPath()
    {
        return this.path;
    }

    public Storage getStorage()
    {
        return this.storage;
    }

    public FileNode getParent()
    {
        return this.parent;
    }

    public Hashtable<Path, FileNode> getChildren()
    {
        return this.children;
    }

    public void addChild(Path path, FileNode fileNode)
    {
        this.children.put(path, fileNode);
    }

    public boolean isDirectory()
    {
        return this.path.isDirectory();
    }

    public boolean isFile()
    {
        return this.path.isFile();
    }
}
