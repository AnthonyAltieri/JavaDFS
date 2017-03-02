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
    ArrayList<StorageContainer> storageContainers = new ArrayList<>();
    FileNode parent;
    Type type;
    Status status = Status.OPEN;
    Hashtable<Path, FileNode> children = new Hashtable<>();
    Lock lock = new ReentrantLock(true);
    Condition inUse = lock.newCondition();
    Queue<Status> waitQueue = new LinkedList<>();
    public Stack<Status> activeLocks = new Stack<>();
    int sharedLocks = 0;
    int exclusiveLocks = 0;
    int numberReads = 0;
    long fileSize;

    FileNode(Path path, Storage storage, Command command, Type type)
    {
        this.path = path;
        storageContainers.add(new StorageContainer(storage, command));
        this.type = type;
        this.fileSize = fileSize;
    }

    public Path getPath()
    {
        return this.path;
    }

    private StorageContainer getFirstStorageContainer()
    {
       return this.storageContainers.get(0);
    }

    public boolean hasStorageContainer()
    {
        return !this.storageContainers.isEmpty();
    }

    public ArrayList<StorageContainer> getStorageContainers()
    {
        return this.storageContainers;
    }

    public Storage getStorage()
    {
        return getFirstStorageContainer().getStorage();
    }

    public Command getCommand()
    {
        return getFirstStorageContainer().getCommand();
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
        String string = "NumExclusive: " + this.exclusiveLocks + " | NumShared: " + this.sharedLocks;
        return string;
    }

    public Status peekWaitQueue()
    {
        try
        {
            return this.waitQueue.peek();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    public void addWaitQueue(Status status)
    {
        this.waitQueue.add(status);
    }

    public void removeWaitQueue()
    {
        this.waitQueue.remove();
    }

    public String waitQueueString()
    {
        String string = "[";
        for (Status status : this.waitQueue)
        {
            string += status;
            string += ", ";
        }
        string += "]";
        return string;
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

    public void setStorageContainers(ArrayList<StorageContainer> storageContainers)
    {
        this.storageContainers = storageContainers;
    }

    public ArrayList<StorageContainer> shouldReplicate()
    {
        this.numberReads += 1;
        if (this.numberReads == 20)
        {
            this.numberReads = 0;
            return this.storageContainers;
        }
        else
        {
            return null;
        }
    }

    public boolean containsStorageContainer(StorageContainer storageContainer)
    {
        return this.storageContainers.contains(storageContainer);
    }
}
