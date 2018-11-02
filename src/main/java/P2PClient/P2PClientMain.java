package P2PClient;

public class P2PClientMain {
    public static void main(String args[]) {

        // Entry point for the client.
        P2PClientUser userInterface = new P2PClientUser();
        P2PClientServer transientServer = new P2PClientServer();

        // Create two threads to start
        userInterface.start();
        transientServer.start();

    }
}
