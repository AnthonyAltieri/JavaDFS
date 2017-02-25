package common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by anthonyaltieri on 2/23/17.
 */
public class NetworkService
{
    public static synchronized int findAvailablePort(InetAddress address)
    {
        int STARTING_PORT = 49152;
        int ENDING_PORT = 65535;
        List<Integer> possiblePorts = new LinkedList<Integer>();
        for (int i = STARTING_PORT ; i <= ENDING_PORT ; i++) possiblePorts.add(i);
        for (Integer port : possiblePorts)
        {
            try
            {
                ServerSocket serverSocket = new ServerSocket(port, 0, address);
                if (serverSocket != null)
                {
                    serverSocket.close();
                    return port;
                }
                else
                {
                    continue;
                }
            } catch (IOException e) {
                continue;
            }
        }
        return -1;
    }
}
