package P2PClient;

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class P2PClientUserWorker extends Thread {

    public static final String GET_COMMAND = "GET";

    List<Integer> chunksToDownload;
    private P2PFile fileToDownload;
    private Socket downloadSocket;
    PrintWriter downloadSocketOutput;
    BufferedInputStream fromTransientServer;


    public P2PClientUserWorker(List<Integer> chunksToDownload, P2PFile fileToDownload, Socket downloadSocket) {
        this.downloadSocket = downloadSocket;
        this.fileToDownload = fileToDownload;
        this.chunksToDownload = chunksToDownload;

        try {
            downloadSocketOutput = new PrintWriter(downloadSocket.getOutputStream(), true);
            fromTransientServer = new BufferedInputStream(downloadSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }

    private void downloadChunks() {
        String filename = fileToDownload.getFileName();
        for(Integer chunkNumber : chunksToDownload) {
            String clientRequest = GET_COMMAND + " " + filename + " " + chunkNumber + "\n";
            downloadSocketOutput.write(clientRequest);
            downloadSocketOutput.flush();


        }
    }

    public void run() {

    }
}
