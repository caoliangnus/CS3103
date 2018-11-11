package directoryServer;

import java.util.Objects;

public class Entry {


    private int chunkNumber;
    private String hostName;

    public Entry(int chunkNumber, String hostName){
        this.chunkNumber = chunkNumber;
        this.hostName = hostName;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return chunkNumber == entry.chunkNumber &&
                hostName.equals(entry.getHostName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkNumber, hostName);
    }
}
