package naming;

import common.Path;
import common.Type;
import rmi.RMIException;
import storage.Command;
import storage.Storage;

import java.io.FileNotFoundException;
import java.io.IOException;
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
    ArrayList<StorageContainer> emptyServers = new ArrayList<>();

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

    FileNode getRoot()
    {
        return this.root;
    }

    boolean attemptReplicate(Path path, ArrayList<StorageContainer> alreadyIn)
        throws RMIException
    {
        StorageContainer storageContainer = findAvailableStorageContainer(alreadyIn);
        if (storageContainer == null) return false;
        try
        {
            boolean result = storageContainer.getCommand().copy(path, alreadyIn.get(0).getStorage());
            if (result)
            {
                this.get(path).getStorageContainers().add(storageContainer);
            }
            return result;
        }
        catch (IOException e)
        {
            throw new RMIException(e.getMessage(), e.getCause());
        }
    }

    StorageContainer findAvailableStorageContainer(ArrayList<StorageContainer> alreadyIn)
    {
        if (this.emptyServers.size() > 0) return this.emptyServers.remove(0);
        Set<StorageContainer> alreadyInSet = alreadyIn == null
            ? new HashSet<>()
            : new HashSet<>(alreadyIn);
        return findStorageContainer(this.root, alreadyInSet);
    }



    public void lock(Path path, Status status)
        throws FileNotFoundException
    {
        this.lockHelper(path, status, false);
        FileNode node = this.get(path);
    }

    private void lockHelper(Path path, Status status, boolean isRipple)
        throws FileNotFoundException
    {
        FileNode node = get(path);
        node.lock();
        if (status == Status.EXCLUSIVE)
        {
            try
            {
                // Wait until there are no locks on the node
                while (node.getStatus() != Status.OPEN)
                {
                    node.waitQueue.add(Status.EXCLUSIVE);
                    node.getInUse().await();
                    node.removeWaitQueue();
                }
                node.addExclusiveLock();
                node.setStatus(Status.EXCLUSIVE);

                // Set all of the parent nodes to SHARED
                if (!isRipple)
                {
                    ArrayList<Path> subPaths = path.getSubPaths();
                    subPaths.remove(subPaths.size() - 1);
                    for (int i = (subPaths.size() - 1) ; i >= 0 ; i--)
                    {
                        Path p = subPaths.get(i);
                        this.lockHelper(p, Status.SHARED, true);
                    }
                    // Set all children nodes to EXCLUSIVE
                    for (FileNode child : this.getChildren(node))
                    {
                        this.lockHelper(child.getPath(), Status.EXCLUSIVE, true);
                    }

                }
                node.unlock();
            }
            catch (InterruptedException e)
            {
                node.unlock();
                throw new IllegalStateException("lock interrupted");
            }
        }
        else
        {
            try
            {
                // Wait until there are no Exclusive locks on the node
                while (node.getStatus() == Status.EXCLUSIVE || node.peekWaitQueue() == Status.EXCLUSIVE)
                {
                    node.addWaitQueue(Status.SHARED);
                    node.getInUse().await();
                    node.removeWaitQueue();
                    if (node.peekWaitQueue() == Status.SHARED)
                        node.getInUse().signal();
                }
                node.addSharedLock();
                node.setStatus(Status.SHARED);
                node.unlock();
            }
            catch (InterruptedException e)
            {
                node.unlock();
                throw new IllegalStateException("lock interrupted");
            }
        }

    }


    public void unlock(Path path, Status status)
        throws FileNotFoundException
    {
        this.unlockHelper(path, status, false);
    }

    private void unlockHelper(Path path, Status status, boolean isRipple)
        throws FileNotFoundException
    {
        FileNode node = this.get(path);
        node.lock();
        if (status == Status.EXCLUSIVE)
        {
            node.removeExclusiveLock();
            node.setStatus(Status.OPEN);
            node.getInUse().signal();
            if (!isRipple)
            {
                ArrayList<Path> subPaths = path.getSubPaths();
                subPaths.remove(subPaths.size() - 1);
                // Set all of the parent nodes to SHARED
                for (int i = (subPaths.size() - 1) ; i >= 0 ; i--)
                {
                    Path p = subPaths.get(i);
                    this.unlockHelper(p, Status.SHARED, true);
                }
                // Set all children nodes to EXCLUSIVE
                for (FileNode child : this.getChildren(node))
                {
                    this.unlockHelper(child.getPath(), Status.EXCLUSIVE, true);
                }
            }
        }
        else
        {
            node.removeSharedLock();
            if (node.getSharedLocks() == 0)
            {
                node.setStatus(Status.OPEN);
                node.getInUse().signal();
            }
        }
        node.unlock();
    }

    public ArrayList<StorageContainer> consolidateWriteData(Path path)
        throws FileNotFoundException, RMIException
    {
        FileNode node = this.get(path);
        ArrayList<StorageContainer> storageContainers = node.getStorageContainers();
        ArrayList<StorageContainer> invalidStorageContainers = null;
        if (storageContainers.size() > 1)
        {
            invalidStorageContainers = new ArrayList<>();
            for (int i = 0 ; i < storageContainers.size() - 1 ; i++)
            {
                StorageContainer invalid = storageContainers.remove(i);
                invalid.getCommand().delete(path);
                invalidStorageContainers.add(invalid);
            }
        }
        return invalidStorageContainers;
    }

    private ArrayList<FileNode> getChildren(FileNode node)
    {
        ArrayList<FileNode> children = new ArrayList<>();
        Queue<FileNode> nextNode = new LinkedList<>();
        nextNode.add(node);
        while (!nextNode.isEmpty())
        {
            FileNode next = nextNode.remove();
            children.add(next);
            for (FileNode child : next.getChildren().values())
            {
                nextNode.add(child);
            }
        }
        // Remove node that you are getting children from
        children.remove(0);
        return children;
    }

    public Status getStatus(Path path)
        throws FileNotFoundException
    {
        return this.get(path).getStatus();
    }

    public boolean hasStorageConnected()
    {
        if (this.root == null) return false;
        return (this.findStorageContainer(this.root, null) != null);
    }

    public int getNumberStorageContainers()
    {
        Set<StorageContainer> alreadyIn = new HashSet<>();
        while (true)
        {
            StorageContainer found = findStorageContainer(this.root, alreadyIn);
            if (found == null) break;
            alreadyIn.add(found);
        }
        return alreadyIn.size() + this.emptyServers.size();
    }

    private StorageContainer findStorageContainer(FileNode node, Set<StorageContainer> alreadyIn)
    {
        boolean result;
        Stack<FileNode> children = new Stack<>();
        children.push(node);
        while (children.size() > 0)
        {
            FileNode popped = children.pop();
            ArrayList<StorageContainer> storageContainers = popped.getStorageContainers();
            for (StorageContainer sc : storageContainers)
            {
                 if (sc.getCommand() != null)
                 {
                     if(alreadyIn == null)
                     {
                         return sc;
                     }
                     else if(!alreadyIn.contains(sc))
                     {
                         return sc;
                     }
                 }
            }
            for (FileNode child : popped.getChildren().values())
            {
                children.push(child);
            }
        }
        return null;
    }

}
