package naming;

import common.Component;
import common.Path;
import storage.Command;
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
    Command command;
    FileNode parent;
    Hashtable<Path, FileNode> children = new Hashtable<>();

    FileNode(Path path, Storage storage, Command command)
    {
        this.path = path;
        this.storage = storage;
        this.command = command;
    }

    public Path getPath()
    {
        return this.path;
    }

    public Storage getStorage()
    {
        return this.storage;
    }

    public Command getCommand()
    {
        return this.command;
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
