package P2PClient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class P2PClientServerWorker implements Runnable {


    private Socket peerSocket;
    private Scanner fromPeer;
    private BufferedOutputStream toPeer;

    // This is for easy write to Peer.
    private PrintWriter toPeerSimplified;

    public static final String GET_COMMAND = "GET";
    private static final String INVALID_CHUNK_NUMBER_MESSAGE = "409 There is no such chunk.\n";
    public static final String  INVALID_COMMAND = "404 There is no such command.";
    public static final int CHUNK_SIZE = 1024; //following MTU byte size of 1500, to play safe make it slightly lesser

    public P2PClientServerWorker(Socket peerSocket) {
        this.peerSocket = peerSocket;
        //System.out.println("Inside P2PClientServerWorker constructor");
        try {
            fromPeer = new Scanner(this.peerSocket.getInputStream());
            toPeer = new BufferedOutputStream(this.peerSocket.getOutputStream());
            toPeerSimplified = new PrintWriter(this.peerSocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }
    }

    private void processPeerFileDownloadRequest() {
        String request;
        //System.out.println("Inside processPeerFileDownloadRequest");
        while(true) {
            if (fromPeer.hasNextLine()) {
                request = fromPeer.nextLine();
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
            System.out.println("Invalid chunk.");
            return;
        }

        String filename = splitRequest[1];
        int requestChunk = Integer.parseInt(splitRequest[2]);
        //System.out.println("Requester IP: " + peerSocket.getRemoteSocketAddress() + ", File Name: " + filename + ", Chunk Requested: " + requestChunk);
        byte[] buffer;

        Semaphore mutex = P2PClientMain.mutexMapping.get(filename);
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        filename = P2PClientUser.folderDirectory + File.separator +  filename;
        File requestedFile = new File(filename);
        int noOfChunksOfFile = (int) (requestedFile.length() / CHUNK_SIZE) + 1;
        //BufferedInputStream bis = null;
        RandomAccessFile bis = null;
        try {
            bis = new RandomAccessFile(filename, "r");
        } catch(Exception e) {
            mutex.release();
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

        if (requestChunk > noOfChunksOfFile) {
            toPeerSimplified.write(INVALID_CHUNK_NUMBER_MESSAGE);
            toPeerSimplified.flush();
            System.out.println("Invalid chunk.");
            mutex.release();
            return;
        }

        //System.out.println(requestChunk);
        //System.out.println("Already here.");
        try {
            buffer = new byte[CHUNK_SIZE];
            bis.seek((requestChunk-1)*CHUNK_SIZE);
            int numberOfBytesRead = bis.read(buffer, 0, CHUNK_SIZE);
            if (numberOfBytesRead < 1) {
                // Need to do something and inform peer.
            }else {
                //System.out.println(numberOfBytesRead);
                toPeer.write(buffer, 0, numberOfBytesRead);
                toPeer.flush();
                toPeerSimplified.write("Chunk " + requestChunk + " of file "
                        + filename + " has been sent.");
                toPeerSimplified.flush();
                mutex.release();
                return;
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
    @Override
    public void run() {
        processPeerFileDownloadRequest();
    }
}
