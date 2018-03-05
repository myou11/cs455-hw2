package cs455.scaling.server;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;

public class TaskHandler implements Runnable {
    private LinkedList<SelectionKey> workQueue;
    private HashMap<String, Integer> clientThroughput;

    private final boolean DEBUG = false;

    public TaskHandler(LinkedList<SelectionKey> workQueue, HashMap<String, Integer> clientThroughput) {
        this.workQueue = workQueue;
        this.clientThroughput = clientThroughput;
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
            synchronized (workQueue) {
                while (workQueue.isEmpty()) {
                    try {
                        workQueue.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                // Read message
                SelectionKey task = workQueue.removeFirst();
                SocketChannel clientChannel = (SocketChannel) task.channel();
                // TODO: NEED TO CHANGE THIS TO ACCOUNT FOR DIFFERENT SHA HASH LENGHTS
                ByteBuffer buffer = ByteBuffer.allocate(8192);

                int bytesRead = 0;
                while (buffer.hasRemaining() && bytesRead != -1) {
                    try {
                        bytesRead = clientChannel.read(buffer);
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }

                // Hash message
                String hashCode = SHA1FromBytes(buffer.array());

                ByteBuffer hashBuffer = ByteBuffer.wrap(hashCode.getBytes());

                try {
                    int bytesWritten = 0;
                    while (hashBuffer.hasRemaining() && bytesWritten != -1) {
                        bytesWritten = clientChannel.write(hashBuffer);
                    }

                    String ipPortNumStr = clientChannel.getRemoteAddress().toString();

                    // TODO: MIGHT NEED TO SYNC ACCESS TO clientThroughput HERE AND IN SERVER ACCEPT
                    int throughput = clientThroughput.get(ipPortNumStr);
                    clientThroughput.put(ipPortNumStr, throughput + 1);

                    if (DEBUG)
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
}
