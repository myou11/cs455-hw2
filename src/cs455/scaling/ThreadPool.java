package cs455.scaling;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Random;

public class ThreadPool {
    private Thread[] threadPool;
    private WorkQueue workQueue;

    public ThreadPool(int threadPoolSize, HashMap<String, Integer> clientThroughput) {
        this.threadPool = new Thread[threadPoolSize];
        this.workQueue = new WorkQueue();

        for (int i = 0; i < threadPoolSize; ++i) {
            threadPool[i] = new Thread(new TaskHandler(workQueue, clientThroughput));
            threadPool[i].start();
        }
    }

    public void addWork(SelectionKey work) {
        workQueue.add(work);
    }

    /*public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(Integer.parseInt(args[0]));
        threadPool.initialize();
        threadPool.startThreads();

        Random r = new Random();

        for (int i = 0; i < 100; ++i) {
            byte[] randomBytes = new byte[8000];
            r.nextBytes(randomBytes);
            threadPool.addWork(randomBytes);
        }
    }*/

}
