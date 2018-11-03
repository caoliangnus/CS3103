package directoryServer;

import java.util.Objects;

public class Entry {


    private int chunkNumber;
    private String address;
    private String port;

    public Entry(int chunkNumber, String address, String port){
        this.chunkNumber = chunkNumber;
        this.address = address;
        this.port = port;
    }

    public String getPort() {
        return port;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return chunkNumber == entry.chunkNumber &&
                address.equals(entry.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkNumber, address);
    }
}
