package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer
    implements Service, Registration, Serializable
{
    private final boolean IS_FILE = true;
    private final boolean IS_DIRECTORY = false;

    FileSystem fileSystem = new FileSystem();
    HashSet<Storage> registry = new HashSet<>();
    InetSocketAddress serviceSocketAddress;
    InetSocketAddress registrationSocketAddress;
    Skeleton<Service> serviceSkeleton;
    Skeleton<Registration> registrationSkeleton;
    boolean isServiceSkeletonStarted;
    boolean isRegistrationSkeletonStarted;
    Lock lock = new ReentrantLock(true);

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start()
        throws RMIException
    {
        this.initializeSkeletons();
    }

    private void initializeSkeletons()
        throws RMIException
    {
        this.serviceSocketAddress = new InetSocketAddress(
            "127.0.0.1",
            NamingStubs.SERVICE_PORT
        );
        this.registrationSocketAddress = new InetSocketAddress(
            "127.0.0.1",
            NamingStubs.REGISTRATION_PORT
        );
        try
        {
            this.serviceSkeleton = new Skeleton<Service>(
                Service.class,
                this,
                this.serviceSocketAddress
            );
            this.serviceSkeleton.start();
            this.isServiceSkeletonStarted = true;
        }
        catch (Exception e)
        {
            throw new RMIException("Service Skeleton could not start", e.getCause());
        }

        try
        {
            this.registrationSkeleton = new Skeleton<Registration>(
                Registration.class,
                this,
                this.registrationSocketAddress
            );
            this.registrationSkeleton.start();
            this.isRegistrationSkeletonStarted = true;
        }
        catch (Exception e)
        {
            throw new RMIException("Registration Skeleton could not start", e.getCause());
        }

    }

    /** Stops the naming server.

        <p>
        This method commands both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        this.serviceSkeleton.stop();
        this.isServiceSkeletonStarted = false;
        this.registrationSkeleton.stop();
        this.isRegistrationSkeletonStarted = false;
        stopped(null);
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
        System.out.println("[NamingServer stopped]");
        System.out.println((cause == null ? "" : cause.toString()) + "\n");
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive)
        throws FileNotFoundException, RMIException
    {
        Status status = exclusive ? Status.EXCLUSIVE : Status.SHARED;
        if (path == null)
        {
            throw new NullPointerException("path is null");
        }
        if (!this.fileSystem.hasPath(path))
        {
            throw new FileNotFoundException("path not found for lock");
        }
        if (exclusive) this.lock.lock();
        if (!exclusive)
        {
            ArrayList<StorageContainer> storageContainers = this.fileSystem.get(path).shouldReplicate();
            if (storageContainers != null)
            {
                this.fileSystem.attemptReplicate(path, storageContainers);
            }
        }
        else
        {
            this.fileSystem.consolidateWriteData(path);
        }
        this.fileSystem.lock(path, status);
        if (exclusive) this.lock.unlock();
    }

    @Override
    public void unlock(Path path, boolean exclusive)
        throws RMIException
    {
        Status status = exclusive ? Status.EXCLUSIVE : Status.SHARED;
        if (path == null)
            throw new NullPointerException("path is null");
        if (!this.fileSystem.hasPath(path))
            throw new IllegalArgumentException("cannot find path");
        try
        {
            this.fileSystem.unlock(path, status);
        }
        catch (FileNotFoundException e)
        {
            throw new IllegalArgumentException("cannot find path");
        }
    }

    @Override
    public boolean isDirectory(Path path)
        throws FileNotFoundException
    {
        return this.fileSystem.isDirectory(path);
    }

    @Override
    public String[] list(Path directory)
        throws FileNotFoundException
    {
        if (!this.fileSystem.hasPath(directory))
            throw new FileNotFoundException("directory does not exist");
        return this.fileSystem.getChildrenStrings(directory);
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        return this.create(file, IS_FILE);
    }


    @Override
    public boolean createDirectory(Path directory)
        throws  RMIException, FileNotFoundException
    {
        return this.create(directory, IS_DIRECTORY);
    }

    private boolean create(Path path, boolean isFile)
        throws RMIException, FileNotFoundException, IllegalStateException
    {
        if (!path.toString().equals("/") && !this.fileSystem.hasPath(path.parent()))
            throw new FileNotFoundException("path's parent does not exist");
        if (!this.fileSystem.hasStorageConnected())
            throw new IllegalStateException("no storage connected");
        if (this.fileSystem.hasPath(path))
            return false;
        try
        {
            Command command = this.fileSystem.get(path.parent()).getCommand();
            if (command == null)
            {
                command = this.fileSystem.findAvailableStorageContainer(null).getCommand();
            }
            if (isFile)
                command.create(path);
            this.fileSystem.add(path, (isFile ? Type.FILE : Type.DIRECTORY));
            return true;
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new RMIException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public boolean delete(Path path)
        throws FileNotFoundException, RMIException
    {
        if (!this.fileSystem.hasPath(path))
            throw new FileNotFoundException("path does not exist");
        this.lock.lock();
        try
        {
            boolean result = false;
            for (StorageContainer sc : this.fileSystem.get(path).getStorageContainers())
            {
                result = sc.getCommand().delete(path);
                if (!result) return false;
            }
            result = result || this.fileSystem.remove(path);
            lock.unlock();
            return result;
        }
        catch (FileNotFoundException e)
        {
            lock.unlock();
            throw e;
        }
        catch (RMIException e)
        {
            lock.unlock();
            throw e;
        }
    }


    @Override
    public Storage getStorage(Path file)
        throws FileNotFoundException
    {
        if (!this.fileSystem.hasPath(file) || file.toString().equals("/"))
            throw new FileNotFoundException("path does not exist");
        if (this.fileSystem.isDirectory(file))
            throw new FileNotFoundException("cannot pass in directory");
        return this.fileSystem.get(file).getStorage();
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub, Path[] files)
        throws IllegalStateException, NullPointerException
    {
        if (client_stub == null)
            throw new NullPointerException("Storage is null");
        if (command_stub == null)
            throw new NullPointerException("Command is null");
        if (files == null)
            throw new NullPointerException("Path[] is null");

        if (this.registry.contains(client_stub))
            throw new IllegalStateException("storage server already registered");

        System.err.println("register(" + client_stub + ") files.length: " + files.length);

        this.registry.add(client_stub);

        if (files.length == 0)
        {
            System.err.println("adding empty server");
            this.fileSystem.emptyServers.add(new StorageContainer(client_stub, command_stub));
            return new Path[0];
        }

        HashSet<Path> allPaths = new HashSet<>();
        ArrayList<Path> duplicatePaths = new ArrayList<Path>();
        for (Path path : files)
        {
            if (path.toString().equals("/"))
                continue;
            try
            {
                if (this.fileSystem.hasPath(path))
                {
                    duplicatePaths.add(path);
                }
                else
                {
                    this.fileSystem.register(path, client_stub, command_stub);
                }
            }
            catch (FileNotFoundException e)
            {
                // This should NEVER happen
                System.err.println("<ERROR> Path should always be valid for register");
                e.printStackTrace();
            }

        }
        return duplicatePaths.toArray(new Path[duplicatePaths.size()]);
    }
//    private boolean isAnyStorageConnected()
//    {
//        return this.fileSystem.getRoot().getChildren().size() > 0;
//    }

    public boolean hasStorageConnected()
    {
        return this.fileSystem.hasStorageConnected();

    }


}
