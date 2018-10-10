package directoryServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class DirectoryServerMain {


    private static Hashtable<String, ArrayList<Entry>> entryList;
    private static List<String> fileNameList;
    private static int port = 8888;

    public static void main(String[] args) {
        entryList = new Hashtable<String, ArrayList<Entry>>();
        fileNameList = new ArrayList<String>();

        Server T1 = new Server( "Thread-1");
        T1.start();

        Server T2 = new Server( "Thread-2");
        T2.start();
    }
}
