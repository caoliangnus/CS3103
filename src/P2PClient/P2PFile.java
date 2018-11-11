package P2PClient;

import java.io.*;
import java.util.*;


public class P2PFile {

    public static int CHUNK_SIZE = 1024;
    private BufferedOutputStream bos;

    // Open the file for writing first
    private String filename;
    private TreeMap<Integer, byte[]> chunks;

    private int numberOfChunks;
    private int counter = 0;

    public P2PFile(String filename, int numberOfChunks) {
        this.filename = filename;
        this.numberOfChunks = numberOfChunks;
        chunks = new TreeMap<>();

        bos = null;
        try {
            // We put true to append because we want to add on to the end of the file, chunk by chunk.
            bos = new BufferedOutputStream(new FileOutputStream(filename, true));
        } catch (FileNotFoundException e) {
            // It means that either the path given is to a directory, or if the file
            // does not exist, it cannot be created.
            e.printStackTrace();
        }
    }

    public String getFileName() {
        return this.filename;
    }


    public void setChunk(int chunkNumber, byte[] data) {
        // Do a check if the chunk exists first
        chunks.put(chunkNumber, data);
    }

    public boolean writeToFile() {
        System.out.println("Writing to file: " + filename);
        System.out.println("Location: " + (P2PClientUser.folderDirectory + File.separator +  filename));
        System.out.println();
        Set<Integer> keySet = chunks.keySet();
        Iterator<Integer> itr = keySet.iterator();

        // Open the file for writing first
        BufferedOutputStream bos = null;
        try {
            // We put true to append because we want to add on to the end of the file, chunk by chunk.
            bos = new BufferedOutputStream(new FileOutputStream(P2PClientUser.folderDirectory + File.separator +  filename, true));
        } catch (FileNotFoundException e) {
            // It means that either the path given is to a directory, or if the file
            // does not exist, it cannot be created.
            e.printStackTrace();
        }


        while(itr.hasNext()) {
            Integer key = itr.next();
            byte[] dataToWrite = chunks.get(key);
            int numberOfBytesToRead = 0;

            // First, find out how much to write to file
            if (dataToWrite[CHUNK_SIZE-1] != '\u0000') {
                numberOfBytesToRead = CHUNK_SIZE;
            }else{
                for(int i=0; i<CHUNK_SIZE; i++){
                    if (dataToWrite[i] == '\u0000'){
                        numberOfBytesToRead = i;
                        break;
                    }
                }
            }

            // Write the data into the file
            try {
                bos.write(dataToWrite, 0, numberOfBytesToRead);
//                bos.flush();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
                System.exit(1);
            }
        }

        try {
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }



}
