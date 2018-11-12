package directoryServer;

import java.net.Socket;

public class DataHostNameSocketPair {

    private String hostName;
    private Socket socket;


    public DataHostNameSocketPair(String hostName, Socket socket) {
        this.hostName = hostName;
        this.socket = socket;
;
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
        DataHostNameSocketPair entry = (DataHostNameSocketPair) o;
        return hostName.equals(entry.getHostName());

    }
}
