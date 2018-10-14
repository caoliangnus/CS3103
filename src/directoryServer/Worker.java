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
    private static final String INFORM_COMMAND = "INFORM";

    // List of success code and message to return
    private static final String FILE_FOUND_MESSAGE = "201 There is such a file.\n";
    private static final String EXIT_SUCCESSFUL_MESSAGE = "202 Exit is successful. " +
            "Data about user has been completely removed from directory server.";
    private static final String UPDATE_SUCCESSFUL_MESSAGE = "203 Advertisement is updated";

    // List of error code and message to return
    private static final String INVALID_COMMAND_MESSAGE = "404 There is no such command.";
    private static final String FILE_NOT_PRESENT_MESSAGE = "403 There is no such file.\n";
    private static final String INVALID_FORMAT_IP_ADDRESS_MESSAGE = "405 IP Address given is not of valid format.";
    private static final String INVALID_FILE_TYPE_MESSAGE = "406 File type advertising is not supported, " +
            "please choose a .txt file.";
    private static final String INVALID_CHUNK_NUMBER_MESSAGE = "408 Chunk number given is invalid, " +
            "please provide a positive chunk number that is more than 0.";


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
        while(true) {
            try {
                Scanner input = new Scanner(connectionSocket.getInputStream());
                String request;
                while(true) {
                    if(input.hasNextLine()) {
                        request = input.nextLine();
                        System.out.println(request);
                        break;
                    }
                }

                // The regex split by one or more white space
                String[] splitRequest = request.split("\\s+");
                String requestType = splitRequest[0];

                switch (requestType) {
                    // do switching based on request type and then delegate to correct method to handle.
                    case LIST_COMMAND:
                        sendListOfAvailableFiles();
                        break;
                    case QUERY_COMMAND:
                        searchForFile(splitRequest[1]);
                        break;
                    case INFORM_COMMAND:
                        updateDirectory(splitRequest[1], splitRequest[2], splitRequest[3]);
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
        toClient.write("This is the list command.\n");
        toClient.flush();

    }

    private synchronized void updateDirectory (String ip, String fileName, String chunkNum){

        boolean fileExisted = true;

        //Check if the ip address given is of a valid format
        if(!validIP(ip)){
            toClient.write(INVALID_FORMAT_IP_ADDRESS_MESSAGE);
            toClient.flush();
            return;
        }

        //Check if the file advertised is a text file
        if(!fileName.endsWith(".txt")){
            toClient.write(INVALID_FILE_TYPE_MESSAGE);
            toClient.flush();
            return;
        }

        //Check if chunkNum is a valid positive number than is one or more
        if(Integer.parseInt(chunkNum)<1){
            toClient.write(INVALID_CHUNK_NUMBER_MESSAGE);
            toClient.flush();
            return;
        }

        //Check if file already exists, if not update the fileNameList
        if(!fileNameList.contains(fileName)){
            fileNameList.add(fileName);
            fileExisted = false;
        }

        //Update entry table (Subject to discussion, for now i just assume the client will advertise when he get the
        //whole file)
        if(!fileExisted){
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i =0;i<Integer.parseInt(chunkNum);i++){
                entries.add(new Entry(i+1,ip));
            }
            entryList.put(fileName, entries);
        }else {
            for(int j = 0; j < Integer.parseInt(chunkNum); j ++){
                entryList.get(fileName).add(new Entry(j+1,ip));
            }

        }

        toClient.println(UPDATE_SUCCESSFUL_MESSAGE);
        toClient.flush();

    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
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
