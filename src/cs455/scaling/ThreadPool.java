package cs455.scaling;

import java.util.ArrayList;
import java.util.Random;

public class ThreadPool {
    private Thread[] threadPool;
    private WorkQueue workQueue;

    public ThreadPool(int threadPoolSize) {
        this.threadPool = new Thread[threadPoolSize];
        this.workQueue = new WorkQueue();
    }

    public void initialize() {
        for (int i = 0; i < threadPool.length; ++i) {
            threadPool[i] = new Thread(new TaskHandler(workQueue));
        }
    }

    public void startThreads() {
        for (Thread thread : threadPool) {
            thread.start();
        }
    }

    public void addWork(byte[] work) {
        workQueue.add(work);
    }

    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(Integer.parseInt(args[0]));
        threadPool.initialize();
        threadPool.startThreads();

        Random r = new Random();

        for (int i = 0; i < 100; ++i) {
            byte[] randomBytes = new byte[8000];
            r.nextBytes(randomBytes);
            threadPool.addWork(randomBytes);
        }
    }

}
