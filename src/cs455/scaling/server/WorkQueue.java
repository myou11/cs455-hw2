package cs455.scaling.server;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

public class WorkQueue {
    private ArrayList<SelectionKey> workQueue;

    public WorkQueue() {
        this.workQueue = new ArrayList<>();
    }

    public synchronized boolean isEmpty() {
        return workQueue.isEmpty();
    }

    public synchronized void add(SelectionKey task) {
        workQueue.add(task);
        notifyAll();
    }

    public synchronized SelectionKey remove(int index) {
        while (true) {
            if(workQueue.isEmpty()) {
                try {
                    wait();
                } catch(InterruptedException ie) {
                    ie.printStackTrace();
                }
            } else
                return workQueue.remove(index);
        }
    }
}
