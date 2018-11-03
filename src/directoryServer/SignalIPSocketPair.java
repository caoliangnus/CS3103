package directoryServer;

import java.net.Socket;

public class SignalIPSocketPair {
    private String IPAddress;
    private Socket socket;
    private String port;


    public SignalIPSocketPair(String IPAddress, Socket socket, String port) {
        this.IPAddress = IPAddress;
        this.socket = socket;
        this.port = port;

    }

    public String getPort() {
        return port;
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

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignalIPSocketPair entry = (SignalIPSocketPair) o;
        return IPAddress.equals(entry.getIPAddress()) &&
                port.equals(entry.getPort());

    }
}
