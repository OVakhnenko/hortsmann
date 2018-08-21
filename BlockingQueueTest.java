import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTest {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Enter base dir e.g. e:/shared");
        String directory = in.nextLine();
        System.out.println("Enter keyword e.g. volatile:");
        String keyword = in.nextLine();
        final int FILE_QUEUE_SIZE = 10;
        final int SEARCH_THREADS = 100;
        BlockingQueue<File> queue = new ArrayBlockingQueue<File>(FILE_QUEUE_SIZE);
        FileEnumerationTask enumerator = new FileEnumerationTask(queue, new File(directory));

        new Thread(enumerator).start(); //producer

        for (int i = 1; i <= SEARCH_THREADS; i++)
            new Thread(new SearchTask(queue, keyword)).start(); //consumers
    }
}

class FileEnumerationTask implements Runnable {
    public static File DUMMY = new File("");
    private BlockingQueue<File> queue;
    private File startingDirectory;

    public FileEnumerationTask(BlockingQueue<File> queue, File startingDirectory) {
        this.queue = queue;
        this.startingDirectory = startingDirectory;
    }

    @Override
    public void run() {
        try {
            enumerate(startingDirectory);
            queue.put(DUMMY);
        } catch (InterruptedException e) {
        }
    }

    public void enumerate(File directory) throws InterruptedException {
        File[] files = directory.listFiles();
        for (File file : files) {
            // DFS
            if (file.isDirectory()) enumerate(file);
            else queue.put(file);
        }
    }
}

class SearchTask implements Runnable {
    private BlockingQueue<File> queue;
    private String keyword;

    public SearchTask(BlockingQueue<File> queue, String keyword) {
        this.queue = queue;
        this.keyword = keyword;
    }

    @Override
    public void run() {
        try {
            boolean done = false;
            while (!done) {
                File file = queue.take();
                if (file == FileEnumerationTask.DUMMY) {
                    queue.put(file);
                    done = true;
                } else search(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        }
    }

    public void search(File file) throws IOException {
        Scanner in = new Scanner(file);
        int lineNumber = 0;
        while (in.hasNextLine()) {
            lineNumber++;
            String line = in.nextLine();
            if (line.contains(keyword))
                System.out.printf("%-30s:%d:%s%n", file.getName(), lineNumber, line);
        }
    }
}
