package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

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
    FileSystem fileSystem;
    HashSet<Storage> registry = new HashSet<>();
    InetSocketAddress serviceSocketAddress;
    InetSocketAddress registrationSocketAddress;
    Skeleton<Service> serviceSkeleton;
    Skeleton<Registration> registrationSkeleton;
    boolean isServiceSkeletonStarted;
    boolean isRegistrationSkeletonStarted;

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
            System.err.println("Service Skeleton started: " + this.serviceSkeleton);
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
            System.err.println("Registration Skeleton started: " + this.registrationSkeleton);
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
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isDirectory(Path path)
        throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list(Path directory)
        throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory)
        throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path)
        throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file)
        throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub, Path[] files)
    {
        System.err.println("register() in NamingServer");
        if (client_stub == null)
            throw new NullPointerException("Storage is null");
        if (command_stub == null)
            throw new NullPointerException("Command is null");
        if (files == null)
            throw new NullPointerException("Path[] is null");

        if (this.registry.contains(client_stub))
            throw new IllegalStateException("storage server already registered");
        this.registry.add(client_stub);

        ArrayList<Path> duplicatePaths = new ArrayList();
        Arrays.asList(files)
            .forEach(path -> {
                System.err.println("checking this path in NS: " + path.toString());
                try
                {
                    if (this.fileSystem.hasPath(path))
                    {
                        System.err.println("hasPath() -> true");
                        duplicatePaths.add(path);
                    }
                    else
                    {
                        System.err.println("hasPath() -> false");
                        this.fileSystem.add(path, client_stub);
                    }
                }
                catch (FileNotFoundException e)
                {
                    // This should NEVER happen
                    System.err.println("<ERROR> Path should always be valid for register");
                    e.printStackTrace();
                }
            });
        return (Path[]) duplicatePaths.toArray();
    }
}
