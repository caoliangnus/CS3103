package directoryServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ResolveHostNameWorker extends Thread{

    private Socket connectionSocket;
    private PrintWriter toClient;
    private Scanner fromClient;

    private List<String> hostNameList = DirectoryServerMain.hostNameList;



    public ResolveHostNameWorker (Socket connectionSocket) throws IOException {

        this.connectionSocket = connectionSocket;
        this.fromClient = new Scanner(this.connectionSocket.getInputStream());
        this.toClient = new PrintWriter(this.connectionSocket.getOutputStream(),true);

    }



    public void run() {

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
