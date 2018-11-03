package directoryServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DirectoryServerMain {

    public static final int SERVER_PORT = 8888;
    public static final int NUMBER_OF_THREADS = 10;
    public static final String DATA_SOCKET_IDENTIFIER = "DATA";
    public static final String CONTROL_SOCKET_IDENTIFIER = "CONTROL";
    public static final String SIGNAL_SOCKET_IDENTIFIER = "SIGNAL";
    public static final Hashtable<String, ArrayList<Entry>> entryList = new Hashtable<>();
    public static final List<FilePair> fileNameList = new ArrayList<>();
    public static final List<DataIPSocketPair> DataIPToSocketMapping = new ArrayList<>();
    public static final List<SignalIPSocketPair> SignalIPToSocketMapping = new ArrayList<>();

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
                System.out.println("Server is waiting... ");
                connectionSocket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e);
                System.exit(1);
            }


            // Asking peer what kind of socket connection this is
            PrintWriter toClient = null;
            Scanner fromServer = null;
            try {
                toClient = new PrintWriter(connectionSocket.getOutputStream(), true);
                fromServer = new Scanner(connectionSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            String IP = connectionSocket.getRemoteSocketAddress().toString();
            String reply = fromServer.nextLine();


            if(reply.equals(DATA_SOCKET_IDENTIFIER)) {
                System.out.println("Data Socket.");
                DataIPSocketPair dataMapping = new DataIPSocketPair(IP, connectionSocket);
                DataIPToSocketMapping.add(dataMapping);
            }else if(reply.equals(CONTROL_SOCKET_IDENTIFIER)) {
                System.out.println("Control Socket.");
                Worker requestToHandle = new Worker(connectionSocket);
                threadPool.execute(requestToHandle);
            }else if(reply.equals(SIGNAL_SOCKET_IDENTIFIER)) {
                System.out.println("Signal Socket.");
                SignalIPSocketPair signalMapping = new SignalIPSocketPair(IP, connectionSocket);
                SignalIPToSocketMapping.add(signalMapping);
            }


        }

    }
}
