package cs455.scaling;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskHandler implements Runnable {
    private ConcurrentLinkedQueue<byte[]> workQueue;

    public TaskHandler(ConcurrentLinkedQueue workQueue) {
        this.workQueue = workQueue;
    }

    public String SHA1FromBytes(byte[] data) {
        BigInteger hashInt = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            byte[] hash = digest.digest(data);
            hashInt = new BigInteger(1, hash);
        } catch (NoSuchAlgorithmException noAlgo) {
            System.out.println("Not a valid encryption algorithm");
            System.exit(1);
        }

        return hashInt.toString(16);
    }

    public void run() {
        while (true) {
            // Read message
            // Remove task from head of the queue
            // null if empty
            byte[] data = workQueue.poll();
            if (data != null) {
                // Hash message
                String hashCode = SHA1FromBytes(data);

                // TODO: send this back to client, printing it for now
                System.out.printf("hashCode: %s\n", hashCode);
            }
        }
    }
}
