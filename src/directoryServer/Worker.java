package directoryServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Worker implements Runnable {

    // List of commands
    private static final String LIST_COMMAND = "LIST";
    private static final String QUERY_COMMAND = "FIND";
    private static final String EXIT_COMMAND = "EXIT";
    private static final String INFORM_COMMAND = "INFORM";
    private static final String INFORM_HOSTNAME_COMMAND = "INFORMHOST";
    private static final String DOWNLOAD_COMMAND = "DOWNLOAD";
    private static final String CHUNK_COMMAND = "CHUNK";
    private static final String SET_COMMAND = "SET";

    // List of success code and message to return
    private static final String FILE_FOUND_MESSAGE = "201 There is such a file.\n";
    private static final String EXIT_SUCCESSFUL_MESSAGE = "202 Exit is successful. " +
            "Data about user has been completely removed from directory server.\n";
    private static final String CHUNK_UPDATE_SUCCESSFUL_MESSAGE = "203 Advertisement is updated\n";
    private static final String HOST_NAME_UPDATE_SUCCESSFUL_MESSAGE = "204 Host name is updated\n";
    private static final String ADDRESS_AND_PORT_UPDATE_SUCCESSFUL_MESSAGE = "205 IP Address and Port are updated\n";

    // List of error code and message to return
    private static final String INVALID_COMMAND_MESSAGE = "404 There is no such command.\n";
    private static final String FILE_NOT_PRESENT_MESSAGE = "403 There is no such file.\n";
    private static final String INVALID_FORMAT_IP_ADDRESS_MESSAGE = "405 IP Address given is not of valid format.\n";
    private static final String INVALID_FILE_TYPE_MESSAGE = "406 File type advertising is not supported, " +
            "please choose a .txt file.\n";
    private static final String INVALID_CHUNK_NUMBER_MESSAGE = "408 Chunk number given is invalid, " +
            "please provide a positive chunk number that is more than 0.\n";
    private static final String INVALID_PORT_NUMBER_MESSAGE = "411 Port number given is invalid, " +
            "please provide a port number from 0 to 65535 (inclusive of both).\n";
    private static final String CHUNK_NOT_PRESENT_MESSAGE = "409 There is no such chunk.\n";
    private static final String FILE_LIST_EMPTY_MESSAGE = "410 List is empty.\nEOF\n";
    private static final String HOST_NAME_ALREADY_USED_MESSAGE = "413 This host name has already been used. Please choose another host name.\n";


    private static final int MAX_IP_ADDRESS_RETURNED = 10;


    private Socket connectionSocket;
    private PrintWriter toClient;
    private List<FilePair> fileNameList = DirectoryServerMain.fileNameList;
    private Hashtable<String, ArrayList<Entry>> entryList = DirectoryServerMain.entryList;
    private List<String> hostNameList = DirectoryServerMain.hostNameList;

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
                        updateDirectory(splitRequest[1], splitRequest[2], splitRequest[3], splitRequest[4], splitRequest[5]);
                        break;
                    case INFORM_HOSTNAME_COMMAND:
                        updateHostName(splitRequest[1], splitRequest[2], splitRequest[3]);
                        break;
                    case DOWNLOAD_COMMAND:
                        returnIPAddressesAndPortForFile(splitRequest[1], splitRequest[2]);
                        break;
                    case EXIT_COMMAND:
                        initializeClientExit(splitRequest[1]);
                        break;
                    case CHUNK_COMMAND:
                        returnTotalChunkNumber(splitRequest[1]);
                        break;
                    case SET_COMMAND:
                        checkAndSetHostName(splitRequest[1]);
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

    private void checkAndSetHostName(String hostName) {
        if (hostNameList.contains(hostName)) {
            toClient.write(HOST_NAME_ALREADY_USED_MESSAGE);
            toClient.flush();
        }else{
            hostNameList.add(hostName);
            toClient.write(HOST_NAME_UPDATE_SUCCESSFUL_MESSAGE);
            toClient.flush();
        }
    }

    private void updateHostName(String hostName, String newAddr, String newPort) {
        // Validate newPort first
        try{
            int tempPort = Integer.parseInt(newPort);
            if (tempPort < 0 || tempPort > 65535) {
                toClient.write(INVALID_PORT_NUMBER_MESSAGE);
                toClient.flush();
                return;
            }
        }catch(NumberFormatException nfe) {
            toClient.write(INVALID_PORT_NUMBER_MESSAGE);
            toClient.flush();
            return;
        }

        entryList.forEach((filename, list) -> {
            Iterator<Entry> iterator = list.iterator();
            while(iterator.hasNext()){
                Entry entry = iterator.next();
                if(entry.getHostName().equals(hostName)){
                    entry.setAddress(newAddr);
                    entry.setPort(Integer.parseInt(newPort));
                }
            }
        });

        toClient.write(ADDRESS_AND_PORT_UPDATE_SUCCESSFUL_MESSAGE);
        toClient.flush();
    }

    private synchronized void returnIPAddressesAndPortForFile(String filename, String chunkNum) {
        boolean doesChunkExist = false;

        // Check if chunkNum is a valid positive number than one or more
        if(Integer.parseInt(chunkNum)<1){
            toClient.write(INVALID_CHUNK_NUMBER_MESSAGE);
            toClient.flush();
            return;
        }

        List<Entry> listOfEntries = entryList.get(filename);
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
        List<IPAndPortPair> chunkList = new ArrayList<>();
        for(int k=0;k<listOfEntries.size();k++){
            Entry entry = listOfEntries.get(k);
            if(entry.getChunkNumber() == chunkNumber){
                chunkList.add(new IPAndPortPair(entry.getAddress(), String.valueOf(entry.getPort())));
            }
        }

        //randomize the list of IP addresses
        shuffleList(chunkList);

        //generate message with at most 10 ip addresses
        for(IPAndPortPair ip : chunkList) {
            IPAddressesAndPorts.append(ip.getIPAddress());
            IPAddressesAndPorts.append(':');
            IPAddressesAndPorts.append(ip.getPort());
            IPAddressesAndPorts.append(',');
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

    private static void shuffleList(List<IPAndPortPair> entryList) {
        int n = entryList.size();
        Random random = new Random(System.currentTimeMillis());
        random.nextInt();
        for (int i = 0; i < n; i++) {
            int randomIndex = i + random.nextInt(n - i);
            swap(entryList, i, randomIndex);
        }
    }

    private static void swap(List<IPAndPortPair> entryList, int i, int randomIndex) {
        IPAndPortPair tempEntry = entryList.get(i);
        entryList.set(i, entryList.get(randomIndex));
        entryList.set(randomIndex, tempEntry);
    }

    private synchronized void initializeClientExit(String hostName) {
        entryList.forEach((filename, list) -> {
            Iterator<Entry> iterator = list.iterator();
            while(iterator.hasNext()){
                Entry entry = iterator.next();
                if(entry.getHostName().equals(hostName)){
                    iterator.remove();

                    // I am not sure if we should do it this way
                    // We can have a flag to prevent all the extra looping
                    FilePair temp = new FilePair(filename, 0);
                    if(list.size() == 0){
                        fileNameList.remove(temp);
                    }
                }
            }
        });

        toClient.write(EXIT_SUCCESSFUL_MESSAGE);
        toClient.flush();
    }

    private synchronized void searchForFile(String filename) {
        FilePair temp = new FilePair(filename, 0);
        if (fileNameList.contains(temp)) {
            toClient.write(FILE_FOUND_MESSAGE);
            toClient.flush();
        }else{
            toClient.write(FILE_NOT_PRESENT_MESSAGE);
            toClient.flush();
        }
    }

    private synchronized void sendListOfAvailableFiles() {

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
    }

    private synchronized void updateDirectory (String ip, String port, String fileName, String chunkNum, String hostName){

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

        //Check if port is a valid positive number than is one or more
        int portNumber = Integer.parseInt(port);
        if(portNumber < 0 || portNumber > 65535){
            toClient.write(INVALID_PORT_NUMBER_MESSAGE);
            toClient.flush();
            return;
        }

        FilePair temp = new FilePair(fileName, Integer.parseInt(chunkNum));
        //Check if file already exists, if not update the fileNameList
        if(!fileNameList.contains(temp)){
            fileNameList.add(temp);
            fileExisted = false;
        }
        //Update entry table (Subject to discussion, for now i just assume the client will advertise when he get the
        //whole file)
        if(!fileExisted){
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i =0;i<Integer.parseInt(chunkNum);i++){
                entries.add(new Entry(i+1, ip, portNumber, hostName));
            }
            entryList.put(fileName, entries);
        }else {
            //Check if this file was advertised by this host earlier, if yes then don't add duplicate entries
            List<Entry> entries = entryList.get(fileName);
            boolean hasHostAdvertised = false;
            for(Entry entry : entries) {
                if(entry.getHostName().equals(hostName)) {
                    hasHostAdvertised = true;
                    break;
                }
            }
            if(hasHostAdvertised){
                System.out.println(fileName + " has been advertised earlier by " + hostName + ".");
            }else {
                for (int j = 0; j < Integer.parseInt(chunkNum); j++) {
                    entryList.get(fileName).add(new Entry(j + 1, ip, portNumber, hostName));

                }
            }
        }
        toClient.write(CHUNK_UPDATE_SUCCESSFUL_MESSAGE);
        toClient.flush();

    }

    public void returnTotalChunkNumber(String filename) {

        FilePair temp = new FilePair(filename, 0);
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
