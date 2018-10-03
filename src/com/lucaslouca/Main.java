package com.lucaslouca;

import java.io.*;
import java.util.*;

/**
 * If you have a 2 GB file with one string per line, which sorting algorithm would you use to sort the file and why?
 *
 * @author lucas
 */
public class Main {
    private final int RAM_IN_MB = 20;

    public File sort(String path) {
        File ans = null;
        try {
            List<String> chunkPaths = this.readFileInChunksOfSize(RAM_IN_MB, path);
            String resultPath = this.mergeChunks(chunkPaths, path + "_sorted");
            ans = new File(resultPath);

            // clean
            for (String chuckPath : chunkPaths) {
                new File(chuckPath).delete();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return ans;
    }

    /**
     * Writes lines into a .txt file and returns the path to the newly created file. The file gets deleted on exit.
     *
     * @param lines
     * @return
     * @throws IOException
     */
    private String writeChunk(List<String> lines) throws IOException {
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
    private List<String> readFileInChunksOfSize(int mb, String path) throws IOException {
        File file = new File(path);
        List<String> chunkPaths = new ArrayList<String>();
        int chunkSizeInBytes = mb * 1024 * 1024;
        int currentBytesRead = 0;

        List<String> lineList = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        for (String inline; (inline = br.readLine()) != null; ) {
            lineList.add(inline.intern());
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
            lineList.clear();
        }
        br.close();

        return chunkPaths;
    }


    private String nextSmallestLine(BufferedReader[] bufferedReaders) throws IOException {
        // Find BufferedReader br with smallest line on top
        // and read from that br until line on top is not the
        // smallest one any more.

        // Init line to something
        final int READ_AHEAD_LIMIT = 100000;
        int smallestLineBrIndex = -1;
        String smallestLine = null;

        for (int i = 0; i < bufferedReaders.length; i++) {
            BufferedReader br = bufferedReaders[i];

            // Mark br
            br.mark(READ_AHEAD_LIMIT);

            String currentLine = br.readLine();
            if (currentLine != null && (smallestLine == null || (currentLine.compareTo(smallestLine)<0))) {
                smallestLine = currentLine;
                smallestLineBrIndex = i;
            }
        }

        // Reset all BufferedReader except the one that had the smallest line
        for (int i = 0; i < bufferedReaders.length; i++) {
            if (i != smallestLineBrIndex) {
                bufferedReaders[i].reset();
            }
        }


        return smallestLine;
    }


    private String mergeChunks(List<String> chunkPaths, String outPath) throws IOException {
        File resultFile = new File(outPath);
        PrintWriter resultPrintWriter = new PrintWriter(new FileWriter(resultFile));

        int numberOfChunks = chunkPaths.size();
        int bytesToReadPerChunk = (int) Math.ceil((RAM_IN_MB * 1024 * 1024) / (double) numberOfChunks);

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
            currentBytesRead = 0;
            BUFFERED_READER_LOOP:
            while ((inline = this.nextSmallestLine(bufferedReaders)) != null){
                lineList.add(inline.intern());
                currentBytesRead += inline.length();
                if (currentBytesRead >= bytesToReadPerChunk) {
                    break BUFFERED_READER_LOOP;
                }
            }

            if (inline == null) {
                finishedChunks++;
            }

            Collections.sort(lineList);
            for (String line : lineList) {
                resultPrintWriter.println(line);
            }
            resultPrintWriter.flush();
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
        String path = "INPUT_DIR/pre.txt";
        DataSetSorter sorter = new DataSetSorter();
        File result = sorter.sort(path);
        System.out.println(result.getAbsolutePath());
    }
}
