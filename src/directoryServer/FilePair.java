package directoryServer;

public class FilePair {
    private String filename;
    private int totalChunkNumber;

    public FilePair(String filename, int totalChunkNumber) {
        this.filename = filename;
        this.totalChunkNumber = totalChunkNumber;
    }

    public String getFilename() {
        return this.filename;
    }

    public int getTotalChunkNumber() {
        return this.totalChunkNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilePair fp = (FilePair) o;
        return this.filename.equals(fp.filename);
    }

    @Override
    public String toString() {
        return this.filename;
    }
}
