package P2PClient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class P2PClientUserSignalWorker extends Thread {

    private Socket signalSocket;
    private Scanner fromTracker;
    private BufferedOutputStream toTracker;

    // This is for easy write to Peer.
    private PrintWriter toPeerSimplified;

    public static final String GET_COMMAND = "GET";
    private static final String INVALID_CHUNK_NUMBER_MESSAGE = "409 There is no such chunk.\n";
    public static final String  INVALID_COMMAND = "404 There is no such command.";
    public static final int CHUNK_SIZE = 1024; //following MTU byte size of 1500, to play safe make it slightly lesser

    public P2PClientUserSignalWorker(Socket signalSocket) {
        this.signalSocket = signalSocket;
        //System.out.println("Inside P2PClientServerWorker constructor");
        try {
            fromTracker = new Scanner(this.signalSocket.getInputStream());
            toTracker = new BufferedOutputStream(this.signalSocket.getOutputStream());
            toPeerSimplified = new PrintWriter(this.signalSocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }
    }

    private void handleSignal() {

        while (true) {
            String request;
            //System.out.println("Inside processPeerFileDownloadRequest");
            while(true) {
                if (fromTracker.hasNextLine()) {
                    request = fromTracker.nextLine();
                    break;
                }
            }

            String[] splitRequest = request.split("\\s+");
            if (!splitRequest[0].equals(GET_COMMAND) || splitRequest.length != 3) {
                toPeerSimplified.write(INVALID_COMMAND);
                toPeerSimplified.flush();
                System.out.println("Command invalid.");
                return;
            }

            if (Integer.parseInt(splitRequest[2]) < 1) {
                toPeerSimplified.write(INVALID_CHUNK_NUMBER_MESSAGE);
                toPeerSimplified.flush();
                System.out.println("Invalid chunk." );
                return;
            }

            String filename = splitRequest[1];
            int requestChunk = Integer.parseInt(splitRequest[2]);
//            System.out.println("Requester IP: " + signalSocket.getRemoteSocketAddress() + ", File Name: " + filename + ", Chunk Requested: " + requestChunk);

            byte[] buffer;
            Semaphore mutex = P2PClientMain.mutexMapping.get(filename);
            try {
                mutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File requestedFile = new File(P2PClientUser.folderDirectory + File.separator + filename);
            int noOfChunksOfFile = (int) (requestedFile.length() / CHUNK_SIZE) + 1;
            RandomAccessFile bis = null;
            try {
                bis = new RandomAccessFile(P2PClientUser.folderDirectory + File.separator + filename, "r");
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println(e);
                mutex.release();
                System.exit(1);
            }

            if (requestChunk > noOfChunksOfFile) {
                toPeerSimplified.write(INVALID_CHUNK_NUMBER_MESSAGE);
                toPeerSimplified.flush();
                System.out.println("Invalid chunk." + " " + requestChunk + " " + noOfChunksOfFile);
                mutex.release();
                return;
            }

            //System.out.println(requestChunk);
            //System.out.println("Already here.");
            try {
                buffer = new byte[CHUNK_SIZE];
                bis.seek((requestChunk-1)*CHUNK_SIZE);
                int numberOfBytesRead = bis.read(buffer, 0, CHUNK_SIZE);
                System.out.println("Chunk number: " + requestChunk);
                System.out.println(new String(buffer));
                if (numberOfBytesRead < 1) {
                    // Need to do something and inform peer.
                }else {
                    //Liang: Data Socket to send data
                    P2PClientUser.dataToTracker.write(buffer, 0, numberOfBytesRead);
                    P2PClientUser.dataToTracker.flush();
                    toPeerSimplified.write("Chunk " + requestChunk + " of file "
                            + filename + " has been sent.\n");
                    toPeerSimplified.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
                System.exit(1);
            } finally {
                mutex.release();
                if(bis!=null){
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public void run() {
        handleSignal();
    }

}
