package com.lucaslouca;

import java.io.*;
import java.util.*;

/**
 * If you have a 2 GB file with one string per line, which sorting algorithm
 * would you use to sort the file and why?
 *
 * @author lucas
 */
public class Main {
    private static final int RAM_IN_MB = 200;
    private static final int LARGE_FILE_SIZE_IN_MB = 2000000;

    /**
     * Return random between min (inclusive) and max (inclusive).
     *
     * @param min
     * @param max
     * @return
     */
    private static int random(int min, int max) {
        Random random = new Random();
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Returns a random String of length len.
     *
     * @param len
     * @return
     */
    private static String randomString(int len) {
        final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new Random();

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    /**
     * Creates a large file of approximately mb megabytes size. Each line in the file contains
     * a String of length 10. The file gets deleted on exit.
     *
     * @param mb
     * @return the absolute path to the file.
     * @throws IOException
     */
    private static String createLargeFileOfSize(int mb) throws IOException {
        File file = File.createTempFile("largefile", ".txt");
        //file.deleteOnExit();

        // x MB = x * 1024 KB = x * 1024 * 1024 Bytes
        int targetSizeInBytes = mb * 1024 * 1024;
        int currentSizeInBytes = 0;

        PrintWriter pw = new PrintWriter(new FileWriter(file));
        String line;
        do {
            line = randomString(random(3, 10));
            pw.println(line);
            currentSizeInBytes += line.length();
        } while (currentSizeInBytes < targetSizeInBytes);

        pw.close();

        return file.getAbsolutePath();
    }

    /**
     * Writes lines into a .txt file and returns the path to the newly created file. The file gets deleted on exit.
     *
     * @param lines
     * @return
     * @throws IOException
     */
    private static String writeChunk(List<String> lines) throws IOException {
        File file = File.createTempFile("largefile-chunk", ".txt");
        file.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(file));
        for (String line : lines) {
            pw.println(line);
        }
        pw.close();

        return file.getAbsolutePath();
    }

    /**
     * Reads the file in chunks of size mb megabytes, sort each chunk and writes it to a temp file.
     *
     * @param mb
     * @param path
     * @return List of paths to chunk files.
     * @throws IOException
     */
    private static List<String> readFileInChunksOfSize(int mb, String path) throws IOException {
        File file = new File(path);
        List<String> chunkPaths = new ArrayList<String>();
        int chunkSizeInBytes = mb * 1024 * 1024;
        int currentBytesRead = 0;

        List<String> lineList = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String inline;
        while ((inline = br.readLine()) != null) {
            lineList.add(inline);
            currentBytesRead += inline.length();
            if (currentBytesRead >= chunkSizeInBytes) {
                Collections.sort(lineList);
                chunkPaths.add(writeChunk(lineList));
                currentBytesRead = 0;
                lineList.clear();
            }
        }

        if (!lineList.isEmpty()) {
            // Write remaining
            Collections.sort(lineList);
            chunkPaths.add(writeChunk(lineList));
        }
        br.close();

        return chunkPaths;
    }


    private static String mergeChunks(List<String> chunkPaths) throws IOException {
        File resultFile = File.createTempFile("largefile-result", ".txt");
        PrintWriter resultPrintWriter = new PrintWriter(new FileWriter(resultFile));

        int numberOfChunks = chunkPaths.size();
        int bytesToReadPerChunk = (int) Math.ceil((RAM_IN_MB * 1024 * 1024) / (double) numberOfChunks);
        System.out.println("numberOfChunks = " + numberOfChunks);
        System.out.println("bytesToReadPerChunk = " + bytesToReadPerChunk);

        // Open a separate buffered reader for each chunk
        BufferedReader[] bufferedReaders = new BufferedReader[numberOfChunks];
        for (int c = 0; c < numberOfChunks; c++) {
            bufferedReaders[c] = new BufferedReader(new FileReader(new File(chunkPaths.get(c))));
        }

        List<String> lineList = new ArrayList<String>();
        String inline;
        int currentBytesRead;
        int finishedChunks = 0;
        while (finishedChunks < numberOfChunks) {
            for (BufferedReader br : bufferedReaders) {
                currentBytesRead = 0;

                BUFFERED_READER_LOOP:
                while ((inline = br.readLine()) != null) {
                    lineList.add(inline);
                    currentBytesRead += inline.length();
                    if (currentBytesRead >= bytesToReadPerChunk) {
                        break BUFFERED_READER_LOOP;
                    }
                }

                if (inline == null) {
                    finishedChunks++;
                }
            }

            Collections.sort(lineList);
            for (String line : lineList) {
                resultPrintWriter.println(line);
            }
            lineList.clear();
        }

        // Close buffers
        for (BufferedReader br : bufferedReaders) {
            br.close();
        }

        resultPrintWriter.close();
        return resultFile.getAbsolutePath();
    }


    public static void main(String[] args) {
        try {
            String path = createLargeFileOfSize(LARGE_FILE_SIZE_IN_MB);
            System.out.println("Created file of size "+LARGE_FILE_SIZE_IN_MB+"MB at '"+path+"'");

            List<String> chunkPaths = readFileInChunksOfSize(RAM_IN_MB, path);
            String resultPath = mergeChunks(chunkPaths);
            System.out.println("Created sorted file at '"+resultPath+"'");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
