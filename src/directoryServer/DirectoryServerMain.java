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
import java.util.concurrent.Semaphore;

public class DirectoryServerMain {

    public static final int SERVER_PORT = 8888;
    public static final int NUMBER_OF_THREADS = 10;
    public static final String DATA_SOCKET_IDENTIFIER = "DATA";
    public static final String CONTROL_SOCKET_IDENTIFIER = "CONTROL";
    public static final String SIGNAL_SOCKET_IDENTIFIER = "SIGNAL";
    public static final Hashtable<String, ArrayList<Entry>> entryList = new Hashtable<>();
    public static final List<FilePair> fileNameList = new ArrayList<>();
    public static final List<DataHostNameSocketPair> DataHostNameToSocketMapping = new ArrayList<>();
    public static final List<SignalHostNameSocketPair> SignalHostNameToSocketMapping = new ArrayList<>();
    public static final List<String> hostNameList = new ArrayList<>();
    public static final Semaphore mappingMutex = new Semaphore(1);

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
                System.out.println("Waiting for request from " + connectionSocket.getRemoteSocketAddress());

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e);
                System.exit(1);
            }


            // Asking peer what kind of socket connection this is
            PrintWriter toClient = null;
            Scanner fromClient = null;
            try {
                toClient = new PrintWriter(connectionSocket.getOutputStream(), true);
                fromClient = new Scanner(connectionSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
//
//            String IPMixed = connectionSocket.getRemoteSocketAddress().toString();
//            String[] IPSplit = IPMixed.split(":");
//            String IP = IPSplit[0].replace("/", "");
//            String port = IPSplit[1];

            String replyFromClient = "";
            while(true) {
                if (fromClient.hasNextLine()) {
                    replyFromClient = fromClient.nextLine();
                    break;
                }
            }


//            String replyFromClient = fromClient.nextLine();
            String[] splitReply = replyFromClient.split("\\s+");
            String reply = splitReply[0];

            String hostName = "";

            if (!reply.equals("INITIALIZATION")) {
                hostName = splitReply[1];
            }

            if(reply.equals(DATA_SOCKET_IDENTIFIER)) {
                System.out.println("Data Socket.");
                DataHostNameSocketPair dataMapping = new DataHostNameSocketPair(hostName, connectionSocket);
                try {
                    mappingMutex.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                DataHostNameToSocketMapping.add(dataMapping);
                mappingMutex.release();
            }else if(reply.equals(CONTROL_SOCKET_IDENTIFIER)) {
                System.out.println("Control Socket.");
                Worker requestToHandle = new Worker(connectionSocket);
                threadPool.execute(requestToHandle);
            }else if(reply.equals(SIGNAL_SOCKET_IDENTIFIER)) {
                System.out.println("Signal Socket.");
                SignalHostNameSocketPair signalMapping = new SignalHostNameSocketPair(hostName, connectionSocket);
                try {
                    mappingMutex.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SignalHostNameToSocketMapping.add(signalMapping);
                mappingMutex.release();
            }else{
                System.out.println("Initialization Socket.");
                while(true) {
                    String request = "";
                    while(true) {
                        if (fromClient.hasNextLine()) {
                            request = fromClient.nextLine();
                            break;
                        }
                    }

                    String[] splitRequest = request.split("\\s+");
                    String requestedHostName = splitRequest[1].trim();
                    if (!splitRequest[0].equals("CHECK")) {
                        // tell client wrong command.
                        break;
                    }
                    if(hostNameList.contains(requestedHostName)) {
                        // tell client hostname has been used.
                        toClient.write("NOT AVAILABLE\n");
                        toClient.flush();
                    }else{
                        toClient.write("AVAILABLE\n");
                        toClient.flush();
                        hostNameList.add(requestedHostName);
                        toClient.close();
                        fromClient.close();
                        break;
                    }

                }
            }
        }
    }
}
