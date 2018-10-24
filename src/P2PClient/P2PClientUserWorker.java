package P2PClient;

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class P2PClientUserWorker implements Runnable {

    public static final String GET_COMMAND = "GET";
    public static final int CLIENT_SERVER_PORT = 9999;
    public static final int CHUNK_SIZE = 1024;

    int chunkToDownload;
    private P2PFile fileToDownload;
    PrintWriter downloadSocketOutput;
    BufferedInputStream fromTransientServer;
    String[] addresses;
    AtomicIntegerArray map;


    public P2PClientUserWorker(int chunkToDownload, P2PFile fileToDownload, String[] addresses,AtomicIntegerArray map) {
        this.fileToDownload = fileToDownload;
        this.chunkToDownload = chunkToDownload+1;
        this.addresses = addresses;
        this.map = map;

    }


    private void downloadChunks() {

        for (int i = 0; i < addresses.length; i++) {
                try {
                    System.out.println("CONNECTING: " + addresses[i]);
                    Socket downloadSocket = new Socket(addresses[i], CLIENT_SERVER_PORT);

                    // Send request to peer-transient-server via PrintWriter
                    downloadSocketOutput = new PrintWriter(downloadSocket.getOutputStream(), true);
                    // Read in chunks in bytes via InputStream
                    fromTransientServer = new BufferedInputStream(downloadSocket.getInputStream());
                    // Buffer to store byte data from transient server to write into file
                    byte[] buffer = new byte[CHUNK_SIZE];

                    String clientRequest = GET_COMMAND + " " + fileToDownload.getFileName() + " " + chunkToDownload + "\n";
                    downloadSocketOutput.write(clientRequest);
                    downloadSocketOutput.flush();
                    fromTransientServer.read(buffer, 0, CHUNK_SIZE);


                    fileToDownload.setChunk(chunkToDownload-1, buffer);
                    map.set(chunkToDownload-1,1);
                    fileToDownload.flush();
                    downloadSocket.close();


                } catch (Exception e) {
                    // for now, we just continue to the next IP to download the chunk
                    continue;
                } finally {
                    if(map.get(chunkToDownload -1)!=1 ) {
                        map.set(chunkToDownload-1, 0);
                    }
                }
            }


    }

    public void run() {
        downloadChunks();
    }
}
