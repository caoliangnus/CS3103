package directoryServer;

import java.net.Socket;

public class DataIPSocketPair {

    private String IPAddress;
    private Socket socket;

    public DataIPSocketPair(String IPAddress, Socket socket) {
        this.IPAddress = IPAddress;
        this.socket = socket;
    }


    public String getIPAddress() {
        return IPAddress;
    }

    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
