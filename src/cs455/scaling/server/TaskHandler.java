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

    public static byte[] SHA1FromBytes(byte[] data) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            hash = digest.digest(data);
        } catch (NoSuchAlgorithmException noAlgo) {
            System.out.println("Not a valid encryption algorithm");
            System.exit(1);
        }

        return hash;
    }

    public void run() {
        while(true) {
            /*  Guaranteed to not be null by the time it is used past the synchronized block.
             *  After the synchronized block, a task will have been retrieved from the workQueue.  */
            SelectionKey task = null;

            // TaskHandler will pull work from the queue as the server adds it
            synchronized (workQueue) {
                while (workQueue.isEmpty()) {
                    try {
                        workQueue.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                // Retrieve the key. Key's channel contains the bytes we will need to perform work on
                task = workQueue.removeFirst();
            }

            SocketChannel clientChannel = (SocketChannel) task.channel();

            // Client send random 8KB of data for us to hash
            ByteBuffer buffer = ByteBuffer.allocate(8192);

            /*  Read until everything from the channel has been read (because Java NIO might not read full contents
             *  of channel from only 1 read)  */
            int bytesRead = 0;
            while (buffer.hasRemaining() && bytesRead != -1) {
                try {
                    bytesRead = clientChannel.read(buffer);
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }

            // Hash message
            byte[] hashBytes = SHA1FromBytes(buffer.array());

            // Create another buffer to send the hash back to client
            ByteBuffer hashBuffer = ByteBuffer.wrap(hashBytes);

            try {
                /*  Write until everything from the buffer has been written to the channel
                 *  (Java NIO does not guarantee full contents of buffer to be written for a write)  */
                int bytesWritten = 0;
                while (hashBuffer.hasRemaining() && bytesWritten != -1) {
                    bytesWritten = clientChannel.write(hashBuffer);
                }

                String ipPortNumStr = clientChannel.getRemoteAddress().toString();

                /*  Sync access to compound operation on this shared map.
                 *  Get and increment the throughput for this client.  */
                synchronized (clientThroughput) {
                    int throughput = clientThroughput.get(ipPortNumStr);
                    clientThroughput.put(ipPortNumStr, throughput + 1);
                }

                if (DEBUG) {
                    // convert the hashed digest into a readable format; used for debugging below
                    BigInteger hashInt = new BigInteger(1, hashBytes);
                    String hashString = hashInt.toString(16);
                    System.out.printf("Sending hash code %s back to client %s\n", hashString, clientChannel.socket().getInetAddress().getHostAddress());
                }

                /*  after sending hash back to client, this channel will be
                 *  wanting to read incoming messages again  */
                task.interestOps(SelectionKey.OP_READ);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }


        }
    }
}
