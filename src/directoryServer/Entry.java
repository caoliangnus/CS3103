import java.util.Objects;

public class Entry {


    private int chunkNumber;
    private String address;

    public Entry(int chunkNumber, String address){
        this.chunkNumber = chunkNumber;
        this.address = address;
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
                Objects.equals(address, entry.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkNumber, address);
    }
}
