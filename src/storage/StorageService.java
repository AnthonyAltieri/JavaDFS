package storage;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

/**
 * Created by anthonyaltieri on 2/23/17.
 */
public class StorageService
{

    protected Runnable createClientMainRunnable(ServerSocket serverSocket)
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {

            }
        };
        return runnable;
    }
    protected Thread createClientMainThread(ServerSocket socketServer)
    {
        Thread main = new Thread(createClientMainRunnable(socketServer));
        return main;
    }
}
