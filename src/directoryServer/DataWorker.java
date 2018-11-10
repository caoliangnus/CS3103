package directoryServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class DataWorker {

    public static final int CHUNK_SIZE = 1024;
    public static final String GET_COMMAND = "GET";


    private Socket uploaderDataSocket, downloaderDataSocket, uploaderSignalSocket;
    private String fileName;
    private int chunkNum;
    private BufferedInputStream DatafromUploaderSocket;
    private PrintWriter SignaltoUploaderSocket;
    private BufferedOutputStream relayToDownloaderSocket;
    private String downloaderHostName;


    public DataWorker (String downloaderHostName, Socket uploadDataSocket, Socket downloadDataSocket, Socket uploadSignalSocket ,String fileName, int chunkNum) throws IOException {

        this.downloaderHostName = downloaderHostName;
        this.uploaderDataSocket = uploadDataSocket;
        this.downloaderDataSocket = downloadDataSocket;
        this.uploaderSignalSocket = uploadSignalSocket;
        this.fileName = fileName;
        this.chunkNum = chunkNum;
        this.DatafromUploaderSocket = new BufferedInputStream(this.uploaderDataSocket.getInputStream());
        this.relayToDownloaderSocket = new BufferedOutputStream(this.downloaderDataSocket.getOutputStream());
        this.SignaltoUploaderSocket = new PrintWriter(this.uploaderSignalSocket.getOutputStream(),true);

    }

    public static final Semaphore threadListMutex = new Semaphore(1);


    public  void relayChunk() throws IOException {

        try {
            threadListMutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Buffer to store chunk
        byte[] buffer = new byte[CHUNK_SIZE];

        // Download chunk from uploader
        String clientRequest = GET_COMMAND + " " + fileName + " " + chunkNum + "\n";
        SignaltoUploaderSocket.write(clientRequest);
        SignaltoUploaderSocket.flush();

        // Relay chunk to downloader
        int size = DatafromUploaderSocket.read(buffer, 0, CHUNK_SIZE);

        String content = new String(buffer);
        System.out.println("UploaderDataSocket: " + uploaderDataSocket +
                " downloaderDataSocket: " + downloaderDataSocket +
                " uploaderSingalSocket: " + uploaderSignalSocket);
        System.out.println("Name: " + downloaderHostName +" Chunk No: " + chunkNum + " \n" + content);

        System.out.println();

        relayToDownloaderSocket.write(buffer, 0, size);
        relayToDownloaderSocket.flush();

        threadListMutex.release();

    }

    public void run() {

        try {
            relayChunk();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
