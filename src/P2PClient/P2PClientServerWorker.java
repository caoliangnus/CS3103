package P2PClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class P2PClientServerWorker implements Runnable {


    private Socket peerSocket;
    private Scanner fromPeer;
    private BufferedOutputStream toPeer;

    // This is for easy write to Peer.
    private PrintWriter toPeerSimplified;

    public static final String GET_COMMAND = "GET";
    private static final String INVALID_CHUNK_NUMBER_MESSAGE = "408 Chunk number given is invalid.";
    public static final String  INVALID_COMMAND = "404 There is no such command.";
    public static final int CHUNK_SIZE = 1200; //following MTU byte size of 1500, to play safe make it slightly lesser

    public P2PClientServerWorker(Socket peerSocket) {
        this.peerSocket = peerSocket;
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
            return;
        }

        if (Integer.parseInt(splitRequest[2]) < 1) {
            toPeerSimplified.write(INVALID_CHUNK_NUMBER_MESSAGE);
            toPeerSimplified.flush();
            return;
        }

        String filename = splitRequest[1];
        int requestChunk = Integer.parseInt(splitRequest[2]);
        byte[] buffer = new byte[CHUNK_SIZE];
        File requestedFile = new File(filename);
        int noOfChunksOfFile = (int) (requestedFile.length() / CHUNK_SIZE);
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(filename));
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

        if (requestChunk > noOfChunksOfFile) {
            toPeerSimplified.write(INVALID_CHUNK_NUMBER_MESSAGE);
            toPeerSimplified.flush();
            return;
        }

        try {
            int numberOfBytesRead = bis.read(buffer, (requestChunk - 1) * CHUNK_SIZE, CHUNK_SIZE);
            if (numberOfBytesRead < 1) {
                // Need to do something and inform peer.
            }else {
                toPeer.write(buffer, 0, CHUNK_SIZE);
                toPeer.flush();
                toPeerSimplified.write("Chunk " + requestChunk + " of file "
                        + filename + " has been sent.");
                toPeerSimplified.flush();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }
    @Override
    public void run() {
        processPeerFileDownloadRequest();
    }
}
