package P2PClient;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class P2PFile {

    public static int CHUNK_SIZE = 1024;

    private String filename;
    private TreeMap<Integer, byte[]> chunks;

    private int numberOfChunks;
    private int counter = 0;

    public P2PFile(String filename, int numberOfChunks) {
        this.filename = filename;
        this.numberOfChunks = numberOfChunks;
        chunks = new TreeMap<>();
    }

    public String getFileName() {
        return this.filename;
    }

    public synchronized boolean hasCompleted() {
        return numberOfChunks == chunks.size();
    }

    public void setChunk(int chunkNumber, byte[] data) {
        // Do a check if the chunk exists first
        if (chunks.containsKey(chunkNumber)) {
            // Do something here
            return;
        }

        chunks.put(chunkNumber, data);
    }

    public void flush() {
        //If equal to null, return
        if (chunks.get(counter) == null) {
            return;
        }

        for (int i = counter; i < numberOfChunks; i++) {
            if (chunks.get(counter) != null) {
                copyToFile(i);
                counter++;
            } else {
                break;
            }
        }
    }

    public List<Integer> getChunksThatAreNotDownloaded() {
        List<Integer> list = new ArrayList<>();
        for(int i=1; i<=numberOfChunks; i++){
            if(!chunks.containsKey(i)){
                list.add(i);
            }
        }
        return list;
    }

    private void copyToFile(int index) {


        byte[] dataToWrite = chunks.get(index);
        int numberOfBytesToRead = 0;

        // Open the file for writing first
        BufferedOutputStream bos = null;
        try {
            // We put true to append because we want to add on to the end of the file, chunk by chunk.
            bos = new BufferedOutputStream(new FileOutputStream(filename, true));
        } catch (FileNotFoundException e) {
            // It means that either the path given is to a directory, or if the file
            // does not exist, it cannot be created.
            e.printStackTrace();
        }

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
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.exit(1);
        }
    }

//    public boolean writeToFile() {
//        Set<Integer> keySet = chunks.keySet();
//        Iterator<Integer> itr = keySet.iterator();
//
//        // Open the file for writing first
//        BufferedOutputStream bos = null;
//        try {
//            // We put true to append because we want to add on to the end of the file, chunk by chunk.
//            bos = new BufferedOutputStream(new FileOutputStream(filename, true));
//        } catch (FileNotFoundException e) {
//            // It means that either the path given is to a directory, or if the file
//            // does not exist, it cannot be created.
//            e.printStackTrace();
//        }
//
//
//        while(itr.hasNext()) {
//            Integer key = itr.next();
//            byte[] dataToWrite = chunks.get(key);
//            int numberOfBytesToRead = 0;
//
//            // First, find out how much to write to file
//            if (dataToWrite[CHUNK_SIZE-1] != '\u0000') {
//                numberOfBytesToRead = CHUNK_SIZE;
//            }else{
//                for(int i=0; i<CHUNK_SIZE; i++){
//                    if (dataToWrite[i] == '\u0000'){
//                        numberOfBytesToRead = i;
//                        break;
//                    }
//                }
//            }
//
//            // Write the data into the file
//            try {
//                bos.write(dataToWrite, 0, numberOfBytesToRead);
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println(e);
//                System.exit(1);
//            }
//        }
//        return true;
//    }



}
