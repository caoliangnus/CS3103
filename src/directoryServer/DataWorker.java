package directoryServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class DataWorker extends Thread {

    public static final int CHUNK_SIZE = 1024;
    public static final String GET_COMMAND = "GET";


    private Socket uploaderDataSocket, downloaderDataSocket, uploaderSignalSocket;
    private String fileName;
    private int chunkNum;
    private BufferedInputStream DatafromUploaderSocket;
    private PrintWriter SignaltoUploaderSocket;
    private BufferedOutputStream relayToDownloaderSocket;


    public DataWorker (Socket uploadDataSocket, Socket downloadDataSocket, Socket uploadSignalSocket ,String fileName, int chunkNum) throws IOException {

        this.uploaderDataSocket = uploadDataSocket;
        this.downloaderDataSocket = downloadDataSocket;
        this.uploaderSignalSocket = uploadSignalSocket;
        this.fileName = fileName;
        this.chunkNum = chunkNum;
        this.DatafromUploaderSocket = new BufferedInputStream(this.uploaderDataSocket.getInputStream());
        this.relayToDownloaderSocket = new BufferedOutputStream(this.downloaderDataSocket.getOutputStream());
        this.SignaltoUploaderSocket = new PrintWriter(this.uploaderSignalSocket.getOutputStream(),true);

    }


    public void relayChunk() throws IOException {

        // Buffer to store chunk
        byte[] buffer = new byte[CHUNK_SIZE];

        // Download chunk from uploader
        String clientRequest = GET_COMMAND + " " + fileName + " " + chunkNum + "\n";
        SignaltoUploaderSocket.write(clientRequest);
        SignaltoUploaderSocket.flush();

        // Relay chunk to downloader
        int size = DatafromUploaderSocket.read(buffer, 0, CHUNK_SIZE);
        relayToDownloaderSocket.write(buffer, 0, size);
        relayToDownloaderSocket.flush();

    }

    public void run() {

        try {
            relayChunk();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
