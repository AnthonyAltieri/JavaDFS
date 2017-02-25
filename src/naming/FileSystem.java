package naming;

import common.Component;
import common.DFSException;
import common.Path;
import storage.Storage;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

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

    FileSystem(Path root)
    {
        this.root = new FileNode(root, null, null);
    }

    public void add(Path path, Storage storage)
        throws FileNotFoundException
    {
        FileNode parent = getNode(path.getParent());
        parent
            .getChildren()
            .put(path, new FileNode(path, storage, parent));
    }

    public FileNode remove(Path path)
        throws FileNotFoundException
    {
        FileNode parent = getNode(path.getParent());
        return parent.getChildren().remove(path.getComponent());
    }


    public FileNode getNode(Path path)
        throws FileNotFoundException
    {
        if (this.root == null)
            throw new FileNotFoundException("root is null");
        FileNode focus = this.root;
        ArrayList<Component> components = (ArrayList<Component>) path.getComponents();
        while (components.size() > 0)
        {
            Component nextComponent = components.remove(0);
            if (components.size() > 0 && nextComponent.isFile())
                throw new FileNotFoundException("cannot traverse to file's children");
            FileNode nextNode = focus.getChildren().get(nextComponent);
            if (nextNode == null)
                throw new FileNotFoundException("provided path is not valid for add");
            focus = nextNode;
        }
        return focus;
    }

    public String[] getChildrenStrings(Path path)
        throws FileNotFoundException
    {
        FileNode node = getNode(path);
        if (!node.isDirectory())
            throw new FileNotFoundException("path is not to a directory");
        ArrayList<String> children = new ArrayList<>();
        Arrays.asList(getNode(path).getChildren().keySet().toArray())
            .stream()
            .forEach(key -> {
                children.add(((Path) key).getComponent().getName());
            });
        return (String[]) children.toArray();
    }

    public Storage getStorage(Path file)
        throws FileNotFoundException
    {
        return getNode(file).getStorage();
    }

    public boolean hasPath(Path path)
        throws FileNotFoundException
    {
        try
        {
            FileNode parent = getNode(path.getParent());
            return parent.getChildren().keySet().contains(path);
        }
        catch (FileNotFoundException e)
        {
            throw e;
        }
    }
}
