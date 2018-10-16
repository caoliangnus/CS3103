package P2PClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sun.dc.pr.PRError;

public class P2PClientServer extends Thread {

    public static final int CLIENT_SERVER_PORT = 9999;

    private ServerSocket listeningSocket;
    private Socket connectionSocket;
    private ExecutorService threadPool;

    private void handleIncomingConnections() {
        // Need to take in a filename and a chunk number and then start sending.
        try {
            listeningSocket = new ServerSocket(CLIENT_SERVER_PORT);
            threadPool = Executors.newFixedThreadPool(10);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

        while(true) {
            try {
                connectionSocket = listeningSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e);
                System.exit(1);
            }

            P2PClientServerWorker requestToHandle = new P2PClientServerWorker(connectionSocket);
            threadPool.execute(requestToHandle);
        }

    }

    public void start() {
        handleIncomingConnections();
    }
}
