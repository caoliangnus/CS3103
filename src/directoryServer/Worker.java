package directoryServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class Worker implements Runnable {

    // List of commands
    private static final String LIST_COMMAND = "LIST";
    private static final String QUERY_COMMAND = "FIND";
    private static final String EXIT_COMMAND = "EXIT";

    // List of code to return
    private static final String FILE_FOUND_MESSAGE = "201 There is such a file.";
    private static final String EXIT_SUCCESSFUL_MESSAGE = "202 Exit is successful. " +
            "Data about user has been completely removed from directory server.";
    private static final String INVALID_COMMAND_MESSAGE = "404 There is no such command.";
    private static final String FILE_NOT_PRESENT_MESSAGE = "403 There is no such file.";


    private Socket connectionSocket;
    private PrintWriter toClient;
    private List<String> fileNameList = DirectoryServerMain.fileNameList;
    private Hashtable<String, ArrayList<Entry>> entryList = DirectoryServerMain.entryList;

    public Worker(Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
        try {
            toClient = new PrintWriter(connectionSocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }
    }

    private void handleRequest() {
        try {
            Scanner input = new Scanner(connectionSocket.getInputStream());
            String request = input.nextLine();

            // The regex split by one or more white space
            String[] splitRequest = request.split("\\s+");
            String requestType = splitRequest[0];

            switch(requestType) {
                // do switching based on request type and then delegate to correct method to handle.
                case LIST_COMMAND:
                    sendListOfAvailableFiles();
                    break;
                case QUERY_COMMAND:
                    searchForFile(splitRequest[1]);
                    break;
                case EXIT_COMMAND:
                    initializeClientExit(splitRequest[1]);
                    break;
                default:
                    // Should not come here. We should return an error code and message here.
                    toClient.println(INVALID_COMMAND_MESSAGE);
                    toClient.flush();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }
    }

    private synchronized void initializeClientExit(String IPAddress) {
        entryList.forEach((filename, list) -> {
            Iterator<Entry> iterator = list.iterator();
            while(iterator.hasNext()){
                Entry entry = iterator.next();
                if(entry.getAddress().equals(IPAddress)){
                    iterator.remove();

                    // I am not sure if we should do it this way
                    // We can have a flag to prevent all the extra looping
                    if (fileNameList.contains(filename)){
                        fileNameList.remove(filename);
                    }
                }
            }
        });
        toClient.write(EXIT_SUCCESSFUL_MESSAGE);
        toClient.flush();
    }

    private synchronized void searchForFile(String filename) {
        if (fileNameList.contains(filename)) {
            toClient.write(FILE_FOUND_MESSAGE);
            toClient.flush();
        }else{
            toClient.write(FILE_NOT_PRESENT_MESSAGE);
            toClient.flush();
        }
    }

    private synchronized void sendListOfAvailableFiles() {
        /*int counter = 1;
        StringBuilder resultString = new StringBuilder();
        for (String entry : fileNameList) {
            resultString.append(counter + ". " + entry + "\n");
        }
        String result = resultString.toString();
        // I am not sure if PrintWriter is the best kind of stream to write large amount of data.
        toClient.write(result);
        toClient.flush();*/

        // Write this first as the implementation is not complete yet.
        toClient.write("This is the list command.");
        toClient.flush();
    }

    @Override
    public void run() {
        handleRequest();
        try {
            // Close connection socket before this thread finishes its work
            connectionSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }
    }
}
