package P2PClient;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class P2PClientUser extends Thread {
    public static final int SERVER_PORT = 8888;
    public static final String LIST_COMMAND = "LIST";
    public static final String QUERY_COMMAND = "FIND";

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
                        break;
                    case 5:
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

    private void requestForListOfFiles() {
        toServer.println(LIST_COMMAND);
        String replyFromServer = fromServer.nextLine();
        System.out.println(replyFromServer);
    }

    private void queryForSpecificFile() {
        System.out.println("Please enter the name of the file to check: ");
        String filename = input.nextLine().trim();
        String requestString = QUERY_COMMAND + " " + filename;
        toServer.println(requestString);
        String replyFromServer = fromServer.nextLine();
        System.out.println(replyFromServer);
    }

    public void run() {
        handleUser();
    }
}
