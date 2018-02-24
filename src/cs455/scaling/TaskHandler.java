package cs455.scaling;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TaskHandler implements Runnable {
    private WorkQueue workQueue;

    public TaskHandler(WorkQueue workQueue) {
        this.workQueue = workQueue;
    }

    public static String SHA1FromBytes(byte[] data) {
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
            SelectionKey task = workQueue.remove(0);
            // Hash message
            String hashCode = SHA1FromBytes((byte[]) task.attachment());

            ByteBuffer buffer = ByteBuffer.wrap(hashCode.getBytes());

            SocketChannel clientChannel = (SocketChannel) task.channel();
            try {
                clientChannel.write(buffer);
                System.out.printf("Sending hash code %s back to client %s\n", hashCode, clientChannel.socket().getInetAddress().getHostAddress());

                // after sending hash back to client, this channel will be
                // wanting to read incoming messages again
                task.interestOps(SelectionKey.OP_READ);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
