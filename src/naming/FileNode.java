package naming;

import common.Path;
import common.Type;
import storage.Command;
import storage.Storage;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    Status status = Status.OPEN;
    Hashtable<Path, FileNode> children = new Hashtable<>();
    Lock lock = new ReentrantLock();
    Condition inUse = lock.newCondition();
    LinkedList<Status> lockRequests = new LinkedList<>();
    public Stack<Status> activeLocks = new Stack<>();
    int sharedLocks = 0;
    int exclusiveLocks = 0;

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
    public Status getStatus()
    {
        return this.status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
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

    public void lock()
    {
        this.lock.lock();
    }

    public void unlock()
    {
        this.lock.unlock();
    }

    public Condition getInUse()
    {
        return this.inUse;
    }
    
    public void addSharedLock()
    {
        this.sharedLocks += 1;
    }

    public void removeSharedLock()
    {
        this.sharedLocks -= 1;
    }

    public int getSharedLocks()
    {
        return this.sharedLocks;
    }

    public void addExclusiveLock()
    {
        this.exclusiveLocks += 1;
    }

    public void removeExclusiveLock()
    {
        this.exclusiveLocks -= 1;
    }

    public String getLockStatus()
    {
        String string = "[";
        for (Status status : this.lockRequests)
        {
            string += status;
            string += ", ";
        }
        string += "]";
        return string;
    }


    public Status getNextLockRequest()
    {
        try
        {
            return this.lockRequests.peek();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    public Status getNextNextLock()
    {
        if (this.lockRequests.size() < 2) return null;
        return this.lockRequests.get(1);
    }

    public void addLockRequest(Status status)
    {
        this.lockRequests.add(status);
    }

    public Status removeLockRequest()
    {
        return this.lockRequests.remove();
    }

    public boolean hasExclusiveLock()
    {
        return this.status == Status.EXCLUSIVE;
    }

    public boolean hasActiveShared()
    {
        return this.status == Status.SHARED;
    }

    public boolean hasActiveLocks()
    {
        return this.status != Status.OPEN;
    }

    public void removeActiveLock()
    {
        this.activeLocks.pop();
    }

}
