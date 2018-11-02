package directoryServer;

public class IPAndPortPair {

    private String IPAddress;
    private String port;

    public IPAndPortPair(String IPAddress, String port) {
        this.IPAddress = IPAddress;
        this.port = port;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
