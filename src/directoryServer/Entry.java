package directoryServer;

import java.util.Objects;

public class Entry {


    private int chunkNumber;
    private String address;
    private int port;
    private String hostName;

    public Entry(int chunkNumber, String address, int port, String hostName){
        this.chunkNumber = chunkNumber;
        this.address = address;
        this.port = port;
        this.hostName = hostName;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return chunkNumber == entry.chunkNumber &&
                port == entry.getPort() &&
                hostName.equals(entry.getHostName()) &&
                address.equals(entry.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkNumber, address);
    }
}
