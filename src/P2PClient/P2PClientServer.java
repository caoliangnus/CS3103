package P2PClient;
import java.io.*;
import java.net.*;

public class P2PClientServer extends Thread {

    public static final int SERVER_PORT = 8888;
    private Socket clientServerSocket;

    private void handleIncomingConnections()  {
        // Need to take in a filename and a chunk number and then start sending.
        String fileRequested;
        try{
            clientServerSocket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
            while(true) {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientServerSocket.getInputStream()));
                fileRequested = inFromClient.readLine();
                

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }


    public void start() {
        handleIncomingConnections();
    }
}
