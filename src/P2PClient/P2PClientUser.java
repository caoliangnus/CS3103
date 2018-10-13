package P2PClient;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class P2PClientUser extends Thread {

    public static final int SERVER_PORT = 8888;
    public static final String LIST_COMMAND = "LIST";
    public static final String QUERY_COMMAND = "FIND";
    public static final String EXIT_COMMAND = "EXIT";
    public static final String INFORM_COMMAND = "INFORM";

    public static final int CHUNK_SIZE = 1200; //following MTU byte size of 1500, to play safe make it slightly lesser

    private Socket clientRequestSocket;
    private PrintWriter toServer;
    private Scanner fromServer;
    private static Scanner input = new Scanner(System.in);

    private void handleUser() {
        try {
            clientRequestSocket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);

            // Use toServer to send the request.
            toServer = new PrintWriter(clientRequestSocket.getOutputStream(), true);
            fromServer = new Scanner(clientRequestSocket.getInputStream());

             while(true) {
                 int option;
                displayMenu();

                System.out.println("Please enter your option (1-5): ");
                // Might want to catch error and warn user to input correctly before looping back.
                option = input.nextInt();
                input.nextLine();
                switch(option) {
                    case 1:
                        requestForListOfFiles();
                        break;
                    case 2:
                        queryForSpecificFile();
                        break;
                    case 3:
                        // Download method
                        break;
                    case 4:
                        // Inform method
                        informAndUpdate();
                        break;
                    case 5:
                        exitFromProgram();
                        break;
                    default:
                        break;
                }


             }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
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
        System.out.println("5. Quit");
        System.out.println("***************************************");
        System.out.println("***************************************");
        System.out.println();
    }

    private void informAndUpdate() {

        try {

            //get the local IP address
            String localAddress = InetAddress.getLocalHost().getHostAddress();

            //Get the directory of the folder
            //(I am thinking in the future at the start of the application we prompt this step to the user and let user
            //specify which folder he is using to download/upload file, assuming we only allows him to use one folder)
            System.out.println("Please enter the directory of the folder where the file reside: ");
            input.nextLine(); //consume the "\n" previously not captured by nextInt();

            String fileDirectory = input.nextLine().trim();
            File advertisingFolder = new File(fileDirectory);
            //check whether if the directory indicated is indeed a directory and exits
            if (!advertisingFolder.exists() || !advertisingFolder.isDirectory()) {
                System.out.println("Folder not found, please re-enter valid folder directory: ");
                return;
            }

            //Obtaining file name
            System.out.println("Please provide the file name you wish to advertise ");
            String fileName = input.next().trim();
            File advertisingFile = new File(advertisingFolder + "\\" + fileName);
            //check whether if the indicated file exists and whether if it is a file
            if (!advertisingFile.exists() || !advertisingFile.isFile()) {
                System.out.println("File not found, please re-enter valid file name of existing file in this folder: ");
                return;
            }

            //calculate number of chunk
            int fileSize = (int) advertisingFolder.length();
            int numOfChunks;
            if (fileSize % CHUNK_SIZE > 0) {
                numOfChunks = fileSize / CHUNK_SIZE + 1;
            } else {
                numOfChunks = fileSize / CHUNK_SIZE;
            }

            String request = INFORM_COMMAND + " " + localAddress + " " + fileName + " " + numOfChunks;
            System.out.println(request);
            toServer.println(request);
            toServer.flush();

            while(true) {
                if(fromServer.hasNextLine()) {
                    String replyFromServer = fromServer.nextLine();
                    System.out.println(replyFromServer);
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }

    }

    private void exitFromProgram() {
        try {
            String localAddress = InetAddress.getLocalHost().getHostAddress();
            String request = EXIT_COMMAND + " " + localAddress;
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
        while(true) {
            if(fromServer.hasNextLine()) {
                String replyFromServer = fromServer.nextLine();
                System.out.println(replyFromServer);
                break;
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
