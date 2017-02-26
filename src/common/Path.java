package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path
    implements Iterable<String>, Comparable<Path>, Serializable
{
    ArrayList<Component> components = new ArrayList<>();
    String localPath;
    Path parent;


    /** Creates a new path which represents the root directory. */
    public Path()
    {
        Component component = new Component(Type.ROOT, "ROOT");
        components.add(component);
        this.parent = null;
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        if (component.trim().equals(""))
            throw new IllegalArgumentException("component cannot be empty String");
        if (component.indexOf(':') != -1)
            throw new IllegalArgumentException("component had character `:`");
        if (component.indexOf('/') != -1)
            throw new IllegalArgumentException("component had `/`");
        if ((path.toString() + component).charAt(0) != '/')
            throw new IllegalArgumentException("path didn't start with character `/`");

        Component comp = new Component(Type.CHILD, component);
        path.components.forEach(c -> {
            this.components.add(new Component(c.type, c.name));
        });
        this.components.add(comp);
        this.parent = path;
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if (path.trim().equals(""))
            throw new IllegalArgumentException("component cannot be empty String");
        if (path.indexOf(':') != -1)
            throw new IllegalArgumentException("component had character `:`");
        if (path.charAt(0) != '/')
            throw new IllegalArgumentException("component didn't start with character `/`");
        this.components.add(new Component(Type.ROOT, "ROOT"));
        String[] split = path.split("/");
        for (int i = 1 ; i < split.length ; i++)
        {
            if (split[i].equals("")) continue;
            this.components.add(new Component(
                ((i == split.length - 1) ? Type.CHILD : Type.CHILD),
                split[i]
            ));
        }
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        ArrayList<String> componentStrings = new ArrayList<>();
        for (int i = 0 ; i < this.components.size() ; i++)
        {
            componentStrings.add(new String(this.components.get(i).getName()));
        }
        return new Iterator<String>() {
            int index = 1;
            ArrayList<String> strings = new ArrayList<>(componentStrings);

            @Override
            public boolean hasNext() {
                return this.index < this.strings.size();
            }

            @Override
            public String next() {
                if (this.index >= this.strings.size())
                    throw new NoSuchElementException("iterator doesn't have next");
                String nextString = this.strings.get(index);
                this.index += 1;
                return nextString;
            }
        };
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        if (!directory.exists())
            throw new FileNotFoundException("directory does not exist");
        if (!directory.isDirectory())
            throw new IllegalArgumentException("directory is not a directory");
        File[] files = directory.listFiles();
        ArrayList<Path> paths = new ArrayList<>();
        String directoryPath = directory.getPath();
        for (int i = 0 ; i < files.length ; i++)
        {
            File file = files[i];
            if (file.isDirectory())
            {
                ArrayList<Path> subdirPaths = new ArrayList<Path>(
                    Arrays.asList(Path.listWithOGDirectory(file, directory))
                );
                ArrayList<Path> temp = new ArrayList<>();
                temp.addAll(paths);
                temp.addAll(subdirPaths);
                paths = temp;
            }
            else
            {
                paths.add(new Path(file.getPath().substring(directoryPath.length())));
            }
        }
        Path[] toReturn = new Path[paths.size()];
        for (int i = 0 ; i < paths.size() ; i++)
        {
            toReturn[i] = paths.get(i);
        }
        return toReturn;
    }

    private static Path[] listWithOGDirectory(File directory, File originalDirectory)
        throws FileNotFoundException
    {
        if (!directory.exists())
            throw new FileNotFoundException("directory does not exist");
        if (!directory.isDirectory())
            throw new IllegalArgumentException("directory is not a directory");
        File[] files = directory.listFiles();
        ArrayList<Path> paths = new ArrayList<>();
        String directoryPath = originalDirectory.getPath();
        for (int i = 0 ; i < files.length ; i++)
        {
            File file = files[i];
            if (file.isDirectory())
            {
                ArrayList<Path> subdirPaths = new ArrayList<Path>(
                    Arrays.asList(Path.listWithOGDirectory(file, originalDirectory))
                );
                ArrayList<Path> temp = new ArrayList<>();
                temp.addAll(paths);
                temp.addAll(subdirPaths);
                paths = temp;
            }
            else
            {
                paths.add(new Path(file.getPath().substring(directoryPath.length())));
            }
        }
        Path[] toReturn = new Path[paths.size()];
        for (int i = 0 ; i < paths.size() ; i++)
        {
            toReturn[i] = paths.get(i);
        }
        return toReturn;

    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return this.getComponent().isRoot();
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (this.getComponent().isRoot())
            throw new IllegalArgumentException("cannot call last() on root");
        String parentString = "/";
        for (int i = 1 ; i < this.components.size() - 1 ; i++)
        {
            parentString += this.components.get(i).getName();
        }
        return new Path(parentString);
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (this.getComponent().isRoot())
            throw new IllegalArgumentException("cannot call last() on root");
        return this.getComponent().getName();
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if it is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        String thisString = this.toString();
        String otherString = other.toString();
        if (otherString.length() > thisString.length())
            return false;
        for (int i = 0 ; i < otherString.length() ; i++)
        {
            if (thisString.charAt(i) != otherString.charAt(i))
                return false;
        }
        return true;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {

        return new File(root.getPath() + this.toString().substring(1));
    }

    /** Compares this path to another.

        <p>
        An ordering upon <code>Path</code> objects is provided to prevent
        deadlocks between applications that need to lock multiple filesystem
        objects simultaneously. By convention, paths that need to be locked
        simultaneously are locked in increasing order.

        <p>
        Because locking a path requires locking every component along the path,
        the order is not arbitrary. For example, suppose the paths were ordered
        first by length, so that <code>/etc</code> precedes
        <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.

        <p>
        Now, suppose two users are running two applications, such as two
        instances of <code>cp</code>. One needs to work with <code>/etc</code>
        and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
        <code>/etc/dfs/conf.txt</code>.

        <p>
        Then, if both applications follow the convention and lock paths in
        increasing order, the following situation can occur: the first
        application locks <code>/etc</code>. The second application locks
        <code>/bin/cat</code>. The first application tries to lock
        <code>/bin/cat</code> also, but gets blocked because the second
        application holds the lock. Now, the second application tries to lock
        <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
        need to acquire the lock for <code>/etc</code> to do so. The two
        applications are now deadlocked.

        @param other The other path.
        @return Zero if the two paths are equal, a negative number if this path
                precedes the other path, or a positive number if this path
                follows the other path.
     */
    @Override
    public int compareTo(Path other)
    {
        if (this.equals(other)) return 0;
        if (other.isSubpath(this)) return 1;
        return -1;
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Path && (this.hashCode() == other.hashCode());
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return this.toString().hashCode();
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        String string = "";
        for (int i = 0 ; i < this.components.size() ; i++)
        {
            Component component = this.components.get(i);
            if (component.getName().equals("ROOT"))
            {
                string += "/";
            }
            else
            {
                string += component.getName();
                if (i != this.components.size() - 1)
                    string += "/";
            }
        }
        return string;
    }

    public String getLocalPath()
    {
        return this.localPath;
    }

    public ArrayList<Component> getComponents()
    {
        return this.components;
    }

    public boolean isDirectory()
    {
        return false;
    }

    public boolean isFile()
    {
        return false;
    }

    public Component getComponent()
    {
        if (this.components.size() == 0) return null;
        return this.components.get(this.components.size() - 1);
    }

    public Path getParent()
    {
        return this.parent;
    }

    public Type getType()
    {
        return this.getComponent().type;
    }
}
