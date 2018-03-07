package cs455.scaling.client;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientStateThread implements Runnable {
    private AtomicInteger numMessagesSent;
    private LinkedList<String> sentHashCodes;

    public ClientStateThread(AtomicInteger numMessagesSent, LinkedList<String> sentHashCodes) {
        this.numMessagesSent = numMessagesSent;
        this.sentHashCodes = sentHashCodes;
    }

    public void run() {
        while(true) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            System.out.printf("Messages sent: %d, Size of sentHashCodes: %d\n", this.numMessagesSent.get(), this.sentHashCodes.size());

            synchronized (numMessagesSent) {
                // reset the count so we can track the number of msgs sent for the next 20s
                this.numMessagesSent.set(0);
            }
        }
    }
}
