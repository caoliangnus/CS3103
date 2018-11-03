package directoryServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Worker implements Runnable {

    // List of commands
    private static final String LIST_COMMAND = "LIST";
    private static final String QUERY_COMMAND = "FIND";
    private static final String EXIT_COMMAND = "EXIT";
    private static final String INFORM_COMMAND = "INFORM";
    private static final String RETURN_SERVER_IP_COMMAND= "RETRIEVE";
    private static final String DOWNLOAD_COMMAND = "GET";
    private static final String CHUNK_COMMAND = "CHUNK";

    // List of success code and message to return
    private static final String FILE_FOUND_MESSAGE = "201 There is such a file.\n";
    private static final String EXIT_SUCCESSFUL_MESSAGE = "202 Exit is successful. " +
            "Data about user has been completely removed from directory server.\n";
    private static final String UPDATE_SUCCESSFUL_MESSAGE = "203 Advertisement is updated\n";


    // List of error code and message to return
    private static final String INVALID_COMMAND_MESSAGE = "404 There is no such command.\n";
    private static final String FILE_NOT_PRESENT_MESSAGE = "403 There is no such file.\n";
    private static final String INVALID_FORMAT_IP_ADDRESS_MESSAGE = "405 IP Address given is not of valid format.\n";
    private static final String INVALID_FILE_TYPE_MESSAGE = "406 File type advertising is not supported, " +
            "please choose a .txt file.\n";
    private static final String INVALID_CHUNK_NUMBER_MESSAGE = "408 Chunk number given is invalid, " +
            "please provide a positive chunk number that is more than 0.\n";
    private static final String CHUNK_NOT_PRESENT_MESSAGE = "409 There is no such chunk.\n";
    private static final String FILE_LIST_EMPTY_MESSAGE = "410 List is empty.\nEOF\n";


    private static final int MAX_IP_ADDRESS_RETURNED = 10;


    private Socket connectionSocket;
    private PrintWriter toClient;
    private Semaphore fileNameListMutex = new Semaphore(1);
    private Semaphore entryListMutex = new Semaphore(1);
    private List<FilePair> fileNameList = DirectoryServerMain.fileNameList;
    private Hashtable<String, ArrayList<Entry>> entryList = DirectoryServerMain.entryList;
    private List<DataIPSocketPair> dataIPToSocketMapping = DirectoryServerMain.DataIPToSocketMapping ;
    private List<SignalIPSocketPair> signalIPToSocketMapping = DirectoryServerMain.SignalIPToSocketMapping;

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
                System.out.println("Waiting for request from " + connectionSocket.getRemoteSocketAddress());
                while(true) {
                    if(input.hasNextLine()) {
                        request = input.nextLine();
                        System.out.println("From " + connectionSocket.getRemoteSocketAddress() + ": " + request);
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
                    case RETURN_SERVER_IP_COMMAND:
                        returnIPAddressesAndPortForFile(splitRequest[1], splitRequest[2]);
                        break;
                    case EXIT_COMMAND:
                        initializeClientExit(splitRequest[1]);
                        break;
                    case CHUNK_COMMAND:
                        returnTotalChunkNumber(splitRequest[1]);
                        break;
                    case DOWNLOAD_COMMAND:
                        createDownloadThread(splitRequest[1],splitRequest[2],splitRequest[3]);
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


    private synchronized void returnIPAddressesAndPortForFile(String filename, String chunkNum) {
        boolean doesChunkExist = false;

        // Check if chunkNum is a valid positive number than one or more
        if(Integer.parseInt(chunkNum)<1){
            toClient.write(INVALID_CHUNK_NUMBER_MESSAGE);
            toClient.flush();
            return;
        }

        try {
            entryListMutex.acquire();
        } catch (InterruptedException e) {
            entryListMutex.release();
            e.printStackTrace();
        }
        List<Entry> listOfEntries = new ArrayList(entryList.get(filename));
        entryListMutex.release();
        if (listOfEntries == null) {
            toClient.write(FILE_NOT_PRESENT_MESSAGE);
            toClient.flush();
            return;
        }

        // This will be the list to store the results
        StringBuilder IPAddressesAndPorts = new StringBuilder();
        int chunkNumber = Integer.parseInt(chunkNum);
        int counter = 0;

        //extract list of ip addresses which have this chunk
        List<String> chunkList = new ArrayList<>();
        for(int k=0;k<listOfEntries.size();k++){
            if(listOfEntries.get(k).getChunkNumber() == chunkNumber) {
                chunkList.add(listOfEntries.get(k).getAddress());
            }
        }

        //randomize the list of IP addresses
        shuffleList(chunkList);

        //generate message with at most 10 ip addresses
        for(String ip : chunkList) {
            IPAddressesAndPorts.append(ip);
            IPAddressesAndPorts.append(':');
            counter++;
            doesChunkExist = true;
            if (counter == MAX_IP_ADDRESS_RETURNED) {
                break;
            }
        }

        // If there is the filename, but no such chunks, then it means this
        // chunk does not exist.
        if (!doesChunkExist) {
            toClient.write(CHUNK_NOT_PRESENT_MESSAGE);
            toClient.flush();
            return;
        }

        String reply = IPAddressesAndPorts.toString().substring(0, IPAddressesAndPorts.length()-1);
        reply = reply + "\n";

        System.out.println("Return IP address list: " + reply);
        toClient.write(reply);
        toClient.flush();
    }

    private static void shuffleList(List<String> entryList) {
        int n = entryList.size();
        Random random = new Random(System.currentTimeMillis());
        random.nextInt();
        for (int i = 0; i < n; i++) {
            int randomIndex = i + random.nextInt(n - i);
            swap(entryList, i, randomIndex);
        }
    }

    private static void swap(List<String> entryList, int i, int randomIndex) {
        String tempEntry = entryList.get(i);
        entryList.set(i, entryList.get(randomIndex));
        entryList.set(randomIndex, tempEntry);
    }

    private synchronized void initializeClientExit(String IPAdress) {
        try {
            entryListMutex.acquire();
        } catch (InterruptedException e) {
            entryListMutex.release();
            e.printStackTrace();
        }

        entryList.forEach((filename, list) -> {
            Iterator<Entry> iterator = list.iterator();
            while(iterator.hasNext()){
                Entry entry = iterator.next();
                if(entry.getAddress().equals(IPAdress)){
                    iterator.remove();

                    // I am not sure if we should do it this way
                    // We can have a flag to prevent all the extra looping
                    FilePair temp = new FilePair(filename, 0);
                    if(list.size() == 0){
                        try {
                            fileNameListMutex.acquire();
                        } catch (InterruptedException e) {
                            fileNameListMutex.release();
                            e.printStackTrace();
                        }
                        fileNameList.remove(temp);
                        fileNameListMutex.release();
                    }
                }
            }
        });

        entryListMutex.release();
        toClient.write(EXIT_SUCCESSFUL_MESSAGE);
        toClient.flush();
    }

    private synchronized void searchForFile(String filename) {
        FilePair temp = new FilePair(filename, 0);
        try {
            fileNameListMutex.acquire();
        } catch (InterruptedException e) {
            fileNameListMutex.release();
            e.printStackTrace();
        }
        if (fileNameList.contains(temp)) {
            toClient.write(FILE_FOUND_MESSAGE);
            toClient.flush();
        }else{
            toClient.write(FILE_NOT_PRESENT_MESSAGE);
            toClient.flush();
        }
        fileNameListMutex.release();
    }

    private synchronized void sendListOfAvailableFiles() {

        try {
            fileNameListMutex.acquire();
        } catch (InterruptedException e) {
            fileNameListMutex.release();
            e.printStackTrace();
        }

        if (fileNameList.isEmpty()) {
            toClient.write(FILE_LIST_EMPTY_MESSAGE);
            toClient.flush();
        } else {

            int counter = 1;
            StringBuilder resultString = new StringBuilder();
            for (FilePair entry : fileNameList) {
                resultString.append(counter++ + ". " + entry + "\n");
            }
            resultString.append("EOF\n");
            String result = resultString.toString();
            toClient.write(result);
            toClient.flush();
        }

        fileNameListMutex.release();
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



        FilePair temp = new FilePair(fileName, Integer.parseInt(chunkNum));
        //Check if file already exists, if not update the fileNameList
        try {
            fileNameListMutex.acquire();
        } catch (InterruptedException e) {
            fileNameListMutex.release();
            e.printStackTrace();
        }

        if(!fileNameList.contains(temp)){
            fileNameList.add(temp);
            fileExisted = false;
        }
        fileNameListMutex.release();
        //Update entry table (Subject to discussion, for now i just assume the client will advertise when he get the
        //whole file)
        if(!fileExisted){
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i =0;i<Integer.parseInt(chunkNum);i++){
                entries.add(new Entry(i+1, ip));
            }
            try {
                entryListMutex.acquire();
            } catch (InterruptedException e) {
                entryListMutex.release();
                e.printStackTrace();
            }
            entryList.put(fileName, entries);
            entryListMutex.release();
        }else {
            //Check if this file was advertised by this host earlier, if yes then don't add duplicate entries

            if(entryList.get(fileName).get(0).getAddress().equals(ip)){
                System.out.println(fileName + " has been advertised earlier. ");
            }else {
                for (int j = 0; j < Integer.parseInt(chunkNum); j++) {
                    entryList.get(fileName).add(new Entry(j + 1, ip));

                }
            }

            entryListMutex.release();
        }
        toClient.write(UPDATE_SUCCESSFUL_MESSAGE);
        toClient.flush();

    }

    public void createDownloadThread(String peerServerIP,String fileName,String chunkNumber) {
        Socket uploaderDataSocket = null;
        Socket downloaderDataSocket;
        Socket uploaderSignalSocket= null;
        downloaderDataSocket = connectionSocket;
        DataWorker dataWorker;
        int requestChunk = Integer.parseInt(chunkNumber);
        for(int i=0; i< dataIPToSocketMapping.size(); i++ ) {
            DataIPSocketPair dataIPPair =  dataIPToSocketMapping.get(i);
            if(dataIPPair.getIPAddress().equals(peerServerIP)){
                uploaderDataSocket = dataIPPair.getSocket();
            }
        }
        for(int i=0; i< signalIPToSocketMapping.size(); i++ ) {
            SignalIPSocketPair signalIPPair =  signalIPToSocketMapping.get(i);
            if(signalIPPair.getIPAddress().equals(peerServerIP)){
                uploaderSignalSocket = signalIPPair.getSocket();
            }
        }

        try {
            dataWorker = new DataWorker(uploaderDataSocket,downloaderDataSocket,uploaderSignalSocket, fileName, requestChunk);
            dataWorker.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void returnTotalChunkNumber(String filename) {

        FilePair temp = new FilePair(filename, 0);

        try {
            fileNameListMutex.acquire();
        } catch (InterruptedException e) {
            fileNameListMutex.release();
            e.printStackTrace();
        }

        if (!fileNameList.contains(temp)) {
            toClient.write(FILE_NOT_PRESENT_MESSAGE);
            toClient.flush();
            return;
        }

        for(FilePair pair : fileNameList) {
            if (pair.getFilename().equals(filename)) {
                int totalChunkNumber = pair.getTotalChunkNumber();
                String reply = totalChunkNumber + "\n";

                // For now we just return the number only
                toClient.write(reply);
                toClient.flush();
                return;
            }
        }

        fileNameListMutex.release();

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
