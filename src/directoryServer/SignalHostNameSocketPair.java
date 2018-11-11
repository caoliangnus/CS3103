package directoryServer;

import java.net.Socket;

public class SignalHostNameSocketPair {
    private String hostName;
    private Socket socket;


    public SignalHostNameSocketPair(String hostName, Socket socket) {
        this.hostName = hostName;
        this.socket = socket;

    }

    public Socket getSocket() {
        return socket;
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignalHostNameSocketPair entry = (SignalHostNameSocketPair) o;
        return hostName.equals(entry.getHostName());

    }
}
