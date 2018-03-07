package cs455.scaling.server;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedList;

public class ThreadPool {
    private Thread[] threadPool;
    private LinkedList<SelectionKey> workQueue;

    public ThreadPool(int threadPoolSize, HashMap<String, Integer> clientThroughput) {
        this.threadPool = new Thread[threadPoolSize];
        this.workQueue = new LinkedList<>();

        for (int i = 0; i < threadPoolSize; ++i) {
            threadPool[i] = new Thread(new TaskHandler(workQueue, clientThroughput));
            threadPool[i].start();
        }
    }

    public void addWork(SelectionKey work) {
        synchronized (workQueue) {
            workQueue.addLast(work);
            workQueue.notifyAll();
        }
    }
}
