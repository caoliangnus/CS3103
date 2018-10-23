package P2PClient;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Requester  implements Runnable{

    public static final int CLIENT_SERVER_PORT = 9999;
    public static final String DOWNLOAD_COMMAND = "DOWNLOAD";
    private static final String CHUNK_NOT_PRESENT_MESSAGE = "409 There is no such chunk.";
    public static final String GET_COMMAND = "GET";

    public static final int CHUNK_SIZE = 1024; //following MTU byte size of 1500, to play safe make it slightly lesser



    private String fileName;
    private int chunkIndex;
    private int numOfChunksPerPeer;
    private ArrayList<String> contentByChunk;


    private Socket peerSocket;
    private Scanner fromPeer;
    private BufferedOutputStream toPeer;

    // This is for easy write to Peer.
    private PrintWriter toPeerSimplified;


    public Requester(String fileName, int chunkIndex, int numOfChunksPerPeer) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.numOfChunksPerPeer = numOfChunksPerPeer;
        this.contentByChunk = new ArrayList<>();

        try {
            fromPeer = new Scanner(this.peerSocket.getInputStream());
            toPeer = new BufferedOutputStream(this.peerSocket.getOutputStream());
            toPeerSimplified = new PrintWriter(this.peerSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        handlePeerDownload();
    }

    private void handlePeerDownload() {
        // Do we want to multithread here? For now, I will be doing NO multithreading.

        int chunkNumber = 1;
        String reply;
        while(true) {
            String request = DOWNLOAD_COMMAND + " " + fileName + " " + chunkNumber + "\n";
            toPeerSimplified.write(request);
            toPeerSimplified.flush();

            while (true) {
                if (fromPeer.hasNextLine()) {
                    reply = fromPeer.nextLine();
                    break;
                }
            }

            boolean hasReadChunk = false;

            // Check reply. If reply says there is no more chunks, then download of
            // file has completed.
            if (reply.equals(CHUNK_NOT_PRESENT_MESSAGE)) {
                System.out.println("Download of " + fileName + " is completed.");

                return;
            }
            String[] listOfAddresses = reply.split(",");


            // Now, loop through the list of addresses and try to establish a connection and download chunk

            for (int i = 0; i < listOfAddresses.length; i++) {
                try {
                    System.out.println("CONNECTING: " + listOfAddresses[i]);
                    Socket downloadSocket = new Socket(listOfAddresses[i], CLIENT_SERVER_PORT);

                    // Send request to peer-transient-server via PrintWriter
                    PrintWriter downloadSocketOutput = new PrintWriter(downloadSocket.getOutputStream(), true);
                    // Read in chunks in bytes via InputStream
                    BufferedInputStream fromTransientServer = new BufferedInputStream(downloadSocket.getInputStream());
                    // Buffer to store byte data from transient server to write into file
                    byte[] buffer = new byte[CHUNK_SIZE];

                    String clientRequest = GET_COMMAND + " " + fileName + " " + chunkNumber + "\n";
                    downloadSocketOutput.write(clientRequest);
                    downloadSocketOutput.flush();

                    int bytesRead = fromTransientServer.read(buffer, 0, CHUNK_SIZE);

                    if(bytesRead > 0) {
                        hasReadChunk = true;

                        String line = new String(buffer, "UTF-8");
                        contentByChunk.add(line);

                        System.out.println("Chunk " + chunkNumber + " has been downloaded.");
                        downloadSocket.close();
                        break;
                    } else {
                        // Cannot read data from the peer despite being able to connect. Continue to the next IP.
                        downloadSocket.close();
                        continue;
                    }

                } catch (Exception e) {
                    // for now, we just continue to the next IP to download the chunk
                    continue;
                }
            }
            if (hasReadChunk) {
                // current chunk has been read and written to file. Move on to the next chunk
                chunkNumber++;
            }

        }
    }

    public ArrayList<String> getContent() {
        return contentByChunk;

    }
}
