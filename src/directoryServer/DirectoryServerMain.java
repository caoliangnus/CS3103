import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class DirectoryServer {


    private static Hashtable<String, ArrayList<Entry>> entryList;
    private static List<String> fileNameList;
    private static int port = 8888;

    public static void main(String[] args) {
        entryList = new Hashtable<String, ArrayList<Entry>>();
        fileNameList = new ArrayList<String>();
    }
}
