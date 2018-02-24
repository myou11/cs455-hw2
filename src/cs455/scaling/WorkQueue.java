package cs455.scaling;

import java.util.ArrayList;

public class WorkQueue {
    private ArrayList<byte[]> workQueue;

    public WorkQueue() {
        this.workQueue = new ArrayList<>();
    }

    public synchronized boolean isEmpty() {
        return workQueue.isEmpty();
    }

    public synchronized void add(byte[] data) {
        workQueue.add(data);
        notifyAll();
    }

    public synchronized byte[] remove(int index) {
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
