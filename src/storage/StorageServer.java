package storage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    static final int SYSTEM_DECIDE_PORT = 0;
    File root;
    int clientPort;
    int commandPort;
    boolean shouldFindClientPort;
    boolean shouldFindCommandPort;
    List<Path> files = new ArrayList<>();
    Thread clientMain;
    Thread commandMain;
    InetSocketAddress clientSocketAddress;
    InetSocketAddress commandSocketAddress;
    Skeleton<Storage> storageSkeleton;
    Skeleton<Command> commandSkeleton;
    boolean isStorageSkeletonStarted;
    boolean isCommandSkeletonStarted;


    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
        throws IOException, NullPointerException
    {
        System.err.println("StorageServer(" + root.toString() + ", " + client_port + ", " + command_port + ")");
        if (root == null) throw new NullPointerException("root is null");
        this.root = root;
        if (client_port == 0)
        {
            this.shouldFindClientPort = true;
        }
        else
        {
            this.clientPort = client_port;
        }
        if (command_port == 0)
        {
            this.shouldFindCommandPort = true;
        }
        else
        {
            this.commandPort = command_port;
        }
    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
        throws NullPointerException
    {
        if (root == null) throw new NullPointerException("root is null");
        this.root = root;
        this.shouldFindClientPort = true;
        this.shouldFindCommandPort = true;
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if (!FileService.doesExist(this.root.getPath()))
            throw new FileNotFoundException("root does not exist");
        if (FileService.isFile(this.root.getPath()))
            throw new FileNotFoundException("root cannot be a file");

        InetAddress clientAddress = InetAddress.getByName(hostname);
        if (this.shouldFindClientPort)
        {
            this.clientPort = NetworkService.findAvailablePort(clientAddress);
            this.shouldFindClientPort = false;
        }
        this.clientSocketAddress = new InetSocketAddress(
            clientAddress,
            this.clientPort
        );
        this.storageSkeleton = new Skeleton<Storage>(Storage.class, this, this.clientSocketAddress);
        this.storageSkeleton.start();
        this.isStorageSkeletonStarted = true;

        InetAddress commandAddress = InetAddress.getByName(hostname);
        if (this.shouldFindCommandPort)
        {
            this.commandPort = NetworkService.findAvailablePort(commandAddress);
            this.shouldFindCommandPort = false;
        }
        this.commandSocketAddress = new InetSocketAddress(
            commandAddress,
            this.commandPort
        );
        this.commandSkeleton = new Skeleton<Command>(Command.class, this, this.commandSocketAddress);
        this.commandSkeleton.start();
        this.isCommandSkeletonStarted = true;
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    // TODO: maybe synchronized?
    public void stop()
    {
        this.commandSkeleton.stop();
        this.isCommandSkeletonStarted = false;
        this.storageSkeleton.stop();
        this.isStorageSkeletonStarted = false;
        stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        if (!FileService.doesExist(file.getLocalPath()))
            throw new FileNotFoundException("file does not exist");
        return FileService.getSize(file.getLocalPath());
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        if (!FileService.doesExist(file.getLocalPath()))
            throw new FileNotFoundException("file does not exist");
        if (length < 0)
            throw new IndexOutOfBoundsException("length can't be negative");
        if (offset < 0)
            throw new IndexOutOfBoundsException("negative offset");
        if ((offset + length) > size(file))
            throw new IndexOutOfBoundsException("offset and length exceed file size");

        FileInputStream fis = new FileInputStream(FileService.getFile(file.getLocalPath()));
        byte[] buffer = new byte[length];
        if (fis.read(buffer, (int) offset, length) != length)
            throw new IOException("Did not read the desired length");
        fis.close();
        return buffer;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        if (!FileService.doesExist(file.getLocalPath()))
            throw new FileNotFoundException("file does not exist");
        if (offset < 0)
            throw new IndexOutOfBoundsException("negative offset");
        FileOutputStream fos = new FileOutputStream(FileService.getFile(file.getLocalPath()));
        fos.write(data, (int) offset, data.length);
        fos.close();
        fos.flush();
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
        throws IOException
    {
        return FileService.getFile(file.getLocalPath()).createNewFile();
    }

    @Override
    public synchronized boolean delete(Path file)
    {
        return FileService.getFile(file.getLocalPath()).delete();
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

}
