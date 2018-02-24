package cs455.scaling;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class TaskHandler implements Runnable {
    private WorkQueue workQueue;

    public TaskHandler(WorkQueue workQueue) {
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
        while(true) {
            // Read message
            byte[] data = workQueue.remove(0);
            // Hash message
            String hashCode = SHA1FromBytes(data);

            // TODO: send this back to client, printing it for now
            System.out.printf("hashCode: %s\n", hashCode);
        }
    }
}
