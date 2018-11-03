package directoryServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DirectoryServerMain {

    public static final int SERVER_PORT = 8888;
    public static final int NUMBER_OF_THREADS = 10;
    public static final Hashtable<String, ArrayList<Entry>> entryList = new Hashtable<>();
    public static final List<FilePair> fileNameList = new ArrayList<>();
    public static final List<String> hostNameList = new ArrayList<>();

    private static ExecutorService threadPool;

    public static void main(String[] args) {
        threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        ServerSocket serverSocket = null;
        Socket connectionSocket = null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

        while(true) {
            try {
                connectionSocket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e);
                System.exit(1);
            }

            Worker requestToHandle = new Worker(connectionSocket);
            threadPool.execute(requestToHandle);

        }

    }
}
