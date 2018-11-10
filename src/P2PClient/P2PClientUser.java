package P2PClient;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class P2PClientUser extends Thread {

    public static final int SERVER_PORT = 8888;
    public static final int CLIENT_SERVER_PORT = 9999;
    public static final String LIST_COMMAND = "LIST";
    public static final String QUERY_COMMAND = "FIND";
    public static final String EXIT_COMMAND = "EXIT";
    public static final String INFORM_COMMAND = "INFORM";
    public static final String RETURN_HOST_NAMES_IP_COMMAND = "RETRIEVE";
    public static final String DOWNLOAD_COMMAND = "GET";

    public static Semaphore mapMutex = new Semaphore(1);

    public static final int NUMBER_OF_THREADS_FOR_DOWNLOAD = 10;

    private static final String CHUNK_NOT_PRESENT_MESSAGE = "409 There is no such chunk.";
    private static final String INVALID_USER_INPUT = "Invalid User Input. Please enter one number only.\n";
    private static final String INVALID_USER_INPUT_NUMBER = "Please enter number only.\n";
    private static final String INVALID_OPTION_NUMBER = "There is no such option number!\n";

    public static final int CHUNK_SIZE = 1024; //following MTU byte size of 1500, to play safe make it slightly lesser

    public static Socket clientInitializationSocket;
    public static PrintWriter initializationToServer;
    public static Scanner initializationFromServer;
    public static Socket clientControlSocket;
    public static Socket clientDataSocket;
    public static Socket clientSignalSocket;
    public static PrintWriter dateToServer;
    public static BufferedOutputStream dataToTracker;
    public static Scanner dataFromServer;
    public static BufferedInputStream dataFromTracker;

    public static PrintWriter signalToServer;
    public static Scanner signalFromServer;
    public static PrintWriter toServer;
    public static Scanner fromServer;
    private static Scanner input = new Scanner(System.in);
    public static String folderDirectory = "";

    private static String userName = "";



    private void handleUser() {
        try {

            //Ask IP
            System.out.println("Please enter Server IP: ");
            String ip = input.nextLine().trim();

//           ip = "104.248.153.253";
//           ip = "172.25.106.54";

            // First, establish a username.
            String tempUserName = "";
            boolean firstTimeAskingForUsername = true;
            // This socket and streams are purely for establishing a username with the tracker.
            clientInitializationSocket = new Socket(ip, SERVER_PORT);
            initializationToServer = new PrintWriter(clientInitializationSocket.getOutputStream());
            initializationFromServer = new Scanner(clientInitializationSocket.getInputStream());


            // Tell server this socket is for establishing username.
            initializationToServer.write("INITIALIZATION\n");
            initializationToServer.flush();
            while(true) {
                if (firstTimeAskingForUsername) {
                    System.out.println("Please provide your username: ");
                } else {
                    System.out.println("Username " + tempUserName + " has already been used. " +
                            "Please provide another username:");
                }

                tempUserName = input.nextLine().trim();
                initializationToServer.write("CHECK " + tempUserName + "\n");
                initializationToServer.flush();


                String replyFromServer = "";
                while (true) {
                    if (initializationFromServer.hasNextLine()) {
                        replyFromServer = initializationFromServer.nextLine();
                        break;
                    }
                }

//                System.out.println(replyFromServer);
//                String replyFromServer = initializationFromServer.nextLine();
                initializationToServer.flush();
                if (replyFromServer.equals("AVAILABLE")) {
                    userName = tempUserName;
                    initializationToServer.close();
                    initializationFromServer.close();
                    break;
                } else {
                    firstTimeAskingForUsername = false;
                }
            }


            //Signaling Connection Socket
            clientSignalSocket = new Socket(ip, SERVER_PORT);
            signalToServer = new PrintWriter(clientSignalSocket.getOutputStream(), true);
            signalFromServer = new Scanner(clientSignalSocket.getInputStream());

            signalToServer.println("SIGNAL " + userName + "\n");

            P2PClientUserSignalWorker signalWorker = new P2PClientUserSignalWorker(clientSignalSocket);
            signalWorker.start();


            //Data Connection Socket
            clientDataSocket = new Socket(ip, SERVER_PORT);
            dateToServer = new PrintWriter(clientDataSocket.getOutputStream(), true);
            dataFromServer = new Scanner(clientDataSocket.getInputStream());
            dataToTracker = new BufferedOutputStream(clientDataSocket.getOutputStream());
            dataFromTracker = new BufferedInputStream(clientDataSocket.getInputStream());

            dateToServer.println("DATA "+ userName + "\n");

            //Control Connection Socket
            clientControlSocket = new Socket(ip, SERVER_PORT);
            // Use toServer to send the request.
            toServer = new PrintWriter(clientControlSocket.getOutputStream(), true);
            fromServer = new Scanner(clientControlSocket.getInputStream());

            toServer.println("CONTROL " + userName + "\n");

            changeFileDirectory();

            while(true) {
                int option;
                displayMenu();

                System.out.println("Please enter your option (1-6): ");
                // Might want to catch error and warn user to input correctly before looping back.

                String userInput = "";
                while (true) {
                    if (input.hasNextLine()) {
                        userInput = input.nextLine();
                        break;
                    }
                }

                String[] splitUserInput = userInput.split("\\s+");

                if (splitUserInput.length > 1) {
                    System.out.println(INVALID_USER_INPUT);
                    continue;
                }

                try {
                    option = Integer.parseInt(splitUserInput[0]);
                } catch (NumberFormatException e) {
                    System.out.println(INVALID_USER_INPUT_NUMBER);
                    continue;
                }

                switch(option) {
                    case 1:
                        requestForListOfFiles();
                        break;
                    case 2:
                        queryForSpecificFile();
                        break;
                    case 3:
                        // Download method
                        downloadFile();
                        break;
                    case 4:
                        // Inform method
                        informAndUpdate();
                        break;
                    case 5:
                        changeFileDirectory();
                        break;
                    case 6:
                        exitFromProgram();
                        break;
                    default:
                        System.out.println(INVALID_OPTION_NUMBER);
                        break;
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }

    private void changeFileDirectory() {

        boolean firstTime = true;
        while(true) {
            if (firstTime) {
                System.out.println("Please specify the folder path for sharing folder: ");
            } else {
                System.out.println("Folder not found, please re-enter valid folder directory: ");
            }

            folderDirectory = input.nextLine().trim();

            File advertisingFolder = new File(folderDirectory);
            //check whether if the directory indicated is indeed a directory and exits
            if (!advertisingFolder.exists() || !advertisingFolder.isDirectory()) {
                firstTime = false;
            } else {
                return;
            }
        }
    }

    private void displayMenu() {

        System.out.println("***************************************");
        System.out.println("************* P2P Client **************");
        System.out.println("***************************************");
        System.out.println("1. List all files available");
        System.out.println("2. Check if file exists");
        System.out.println("3. Download file");
        System.out.println("4. Update directory server");
        System.out.println("5. Change folder directory");
        System.out.println("6. Quit");
        System.out.println("***************************************");
        System.out.println("***************************************");
        System.out.println();
    }

//    private void downloadFile() {
//        System.out.println("Please enter the name of the file to download: ");
//        String filename = input.nextLine();
//        String reply;
//
//        BufferedOutputStream bos = null;
//        try {
//            // We put true to append because we want to add on to the end of the file, chunk by chunk.
//            bos = new BufferedOutputStream(new FileOutputStream(filename, true));
//        } catch (FileNotFoundException e) {
//            // It means that either the path given is to a directory, or if the file
//            // does not exist, it cannot be created.
//            e.printStackTrace();
//        }
//
//        // Do we want to multithread here? For now, I will be doing NO multithreading.
//        int chunkNumber = 1;
//        while(true) {
//            String request = DOWNLOAD_COMMAND + " " + filename + " " + chunkNumber + "\n";
//            toServer.write(request);
//            toServer.flush();
//
//            while (true) {
//                if (fromServer.hasNextLine()) {
//                    reply = fromServer.nextLine();
//                    break;
//                }
//            }
//
//            boolean hasReadChunk = false;
//
//            // Check reply. If reply says there is no more chunks, then download of
//            // file has completed.
//            if (reply.equals(CHUNK_NOT_PRESENT_MESSAGE)) {
//                System.out.println("Download of " + filename + " is completed.");
//                try {
//                    bos.close();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                return;
//            }
//            String[] listOfAddresses = reply.split(",");
//
//
//            // Now, loop through the list of addresses and try to establish a connection and download chunk
//
//            for (int i = 0; i < listOfAddresses.length; i++) {
//                try {
//                    System.out.println("CONNECTING: " + listOfAddresses[i]);
//                    Socket downloadSocket = new Socket(listOfAddresses[i], CLIENT_SERVER_PORT);
//
//                    // Send request to peer-transient-server via PrintWriter
//                    PrintWriter downloadSocketOutput = new PrintWriter(downloadSocket.getOutputStream(), true);
//                    // Read in chunks in bytes via InputStream
//                    BufferedInputStream fromTransientServer = new BufferedInputStream(downloadSocket.getInputStream());
//                    // Buffer to store byte data from transient server to write into file
//                    byte[] buffer = new byte[CHUNK_SIZE];
//
//                    String clientRequest = GET_COMMAND + " " + filename + " " + chunkNumber + "\n";
//                    downloadSocketOutput.write(clientRequest);
//                    downloadSocketOutput.flush();
//
//                    int bytesRead = fromTransientServer.read(buffer, 0, CHUNK_SIZE);
//
//                    if(bytesRead > 0) {
//                        hasReadChunk = true;
//
//
//                        // Append to file.
//                        bos.write(buffer, 0, bytesRead);
//                        bos.flush();
//                        System.out.println("Chunk " + chunkNumber + " has been downloaded.");
//                        downloadSocket.close();
//                        break;
//                    } else {
//                        // Cannot read data from the peer despite being able to connect. Continue to the next IP.
//                        downloadSocket.close();
//                        continue;
//                    }
//
//                } catch (Exception e) {
//                    // for now, we just continue to the next IP to download the chunk
//                    continue;
//                }
//            }
//            if (hasReadChunk) {
//                // current chunk has been read and written to file. Move on to the next chunk
//                chunkNumber++;
//            }
//
//        }
//    }

    private void downloadFile() {
        // Initialization
        System.out.println("Please enter the name of the file to download: ");
        String filename = input.nextLine().trim();

        File temp = new File(P2PClientUser.folderDirectory + File.separator +  filename);

        if (temp.exists()) {
            System.out.println("File with same name already exist! " + temp.getAbsolutePath());
            return;
        }

        // Get total chunk number first
        toServer.println("CHUNK " + filename + "\n");
        String reply;
        while(true) {
            if(fromServer.hasNextLine()) {
                reply = fromServer.nextLine();
                break;
            }
        }

        if(reply.equals("403 There is no such file.")){
            System.out.println("403 There is no such file.");
            return;
        }

        int numberOfChunks = Integer.parseInt(reply);
        P2PFile fileToDownload = new P2PFile(filename, numberOfChunks);
        //AtomicIntegerArray map = new AtomicIntegerArray(tempMap);

//        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        String HostNameReply = "";


        System.out.println("Please wait... ...");

        // Check for any chunks that is available for downloading
        for (int i = 0; i < numberOfChunks; i++) {
            // Obtain a list of IP address to download from
            String IPRequest = RETURN_HOST_NAMES_IP_COMMAND + " " + filename + " " + (i + 1) + "\n";
            toServer.write(IPRequest);

            toServer.flush();

            while (true) {
                if (fromServer.hasNextLine()) {
                    HostNameReply = fromServer.nextLine();
                    break;
                }
            }

            String[] splitHostNames = HostNameReply.split(",");

            downloadChunks(splitHostNames, fileToDownload, i+1);

        }

        fileToDownload.writeToFile();

    }


    private void downloadChunks(String[] hostNames, P2PFile fileToDownload, int chunkToDownload) {

        for (int i = 0; i < hostNames.length; i++) {
            try {

                // Buffer to store byte data from transient server to write into file
                byte[] buffer = new byte[CHUNK_SIZE];

                String clientRequest = DOWNLOAD_COMMAND + " " + userName + " " + hostNames[0] + " " + fileToDownload.getFileName() + " " + chunkToDownload + "\n";

                System.out.println(userName + " Requesting: " + fileToDownload.getFileName() + "#" +chunkToDownload);

                toServer.write(clientRequest);
                toServer.flush();

                int size = dataFromTracker.read(buffer, 0, CHUNK_SIZE);
//                    System.out.println("Chunk: " + chunkToDownload + " SIZE " + size);

                System.out.println("Size is: " + size);
                if (size >= 0) {
                    fileToDownload.setChunk(chunkToDownload-1, buffer);
                    System.out.println("Chunk: " + chunkToDownload + " SIZE " + size);

                    return;
                } else {
                    continue;
                }

            } catch (Exception e) {
                // for now, we just continue to the next IP to download the chunk
                continue;
            }
        }


    }

    private void informAndUpdate() {

        try {

            //get the local IP address
            String localAddress = InetAddress.getLocalHost().getHostAddress();

            File advertisingFolder = new File(folderDirectory);
            //check whether if the directory indicated is indeed a directory and exits
            if (!advertisingFolder.exists() || !advertisingFolder.isDirectory()) {
                System.out.println("Folder not found, please re-enter valid folder directory: ");
                return;
            }

            //Obtaining file name
            String fileName;
            File advertisingFile;
            System.out.println("Please provide the file name you wish to advertise ");
            while(true) {
                fileName = input.nextLine().trim();
                if (fileName.equals("Quit")) {
                    return;
                }
                advertisingFile = new File(advertisingFolder + File.separator + fileName);
                //check whether if the indicated file exists and whether if it is a file
                if (!advertisingFile.exists() || !advertisingFile.isFile()) {
                    System.out.println("File not found, please re-enter valid file name of existing file in this folder (or type Quit to go back to Menu): ");
                }else{
                    break;
                }
            }

            //calculate number of chunk
            int fileSize = (int) advertisingFile.length();
            int numOfChunks = calculateChunkSize(fileSize);

            String request = INFORM_COMMAND  + " " + fileName + " " + numOfChunks + " " + userName + "\n";
            // System.out.println(request);
            toServer.println(request);
            toServer.flush();

            while(true) {
                if(fromServer.hasNextLine()) {
                    String replyFromServer = fromServer.nextLine();
                    System.out.println(replyFromServer);
                    P2PClientMain.mutexMapping.put(fileName, new Semaphore(1));
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }

    private int calculateChunkSize(int fileSize){
        int numOfChunks;

        if(fileSize <= CHUNK_SIZE){
            numOfChunks = 1;
        }else if ((fileSize % CHUNK_SIZE) > 0) {
            numOfChunks = (fileSize / CHUNK_SIZE) + 1;
        } else {
            numOfChunks = (fileSize / CHUNK_SIZE);
        }

        return numOfChunks;

    }

    private void exitFromProgram() {
        try {
            String request = EXIT_COMMAND + " " + userName + "\n";
            toServer.println(request);

            while(true) {
                if(fromServer.hasNextLine()) {
                    String replyFromServer = fromServer.nextLine();
                    System.out.println(replyFromServer);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }

    private void requestForListOfFiles() {
        toServer.println(LIST_COMMAND);
        while (true) {
            if (fromServer.hasNextLine()) {
                String replyFromServer = fromServer.nextLine();
                if (replyFromServer.equals("EOF")) {
                    return;
                } else {
                    System.out.println(replyFromServer);
                }
            }
        }
    }

    private void queryForSpecificFile() {

        // Should we do some parsing and checking here? Valid file name or something similar?

        System.out.println("Please enter the name of the file to check: ");
        String filename = input.nextLine().trim();
        String requestString = QUERY_COMMAND + " " + filename;
        toServer.println(requestString);

        while(true) {
            if(fromServer.hasNextLine()) {
                String replyFromServer = fromServer.nextLine();
                System.out.println(replyFromServer);
                break;
            }
        }
    }

    public void run() {
        handleUser();
    }
}
