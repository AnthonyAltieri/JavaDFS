package naming;

import common.Component;
import common.Path;
import common.Type;
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
    Type type;
    Hashtable<Path, FileNode> children = new Hashtable<>();

    FileNode(Path path, Storage storage, Command command, Type type)
    {
        this.path = path;
        this.storage = storage;
        this.command = command;
        this.type = type;
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
        return this.type == Type.DIRECTORY || this.type == Type.ROOT;
    }

    public boolean isFile()
    {
        return this.type == Type.FILE;
    }

    public String toString()
    {
        return "[FileNode] " + path + "\nCommand: " + this.command + "\nStorage: " + this.storage;
    }
}
