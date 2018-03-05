package cs455.scaling.client;

import cs455.scaling.server.TaskHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Random;

public class ClientSenderThread implements Runnable {
    private SocketChannel serverChannel;
    private int messageRate;
    private LinkedList<String> sentHashCodes;
    private ByteBuffer buffer = ByteBuffer.allocate(8192);

    private final boolean DEBUG = true;

    public ClientSenderThread(SelectionKey serverKey, int messageRate, LinkedList<String> sentHashCodes) {
        this.serverChannel = (SocketChannel) serverKey.channel();
        this.messageRate = messageRate;
        this.sentHashCodes = sentHashCodes;
    }

    public void run() {
        while (true) {
            Random r = new Random();
            r.nextBytes(buffer.array());

            sentHashCodes.addLast(TaskHandler.SHA1FromBytes(buffer.array()));

            int bytesWritten = 0;
            // write until everything in the buffer has been written
            while (buffer.hasRemaining() && bytesWritten != -1) {
                try {
                    bytesWritten = serverChannel.write(buffer);
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }

            // clear the buffer for subsequent writes
            buffer.clear();

            if (DEBUG)
                System.out.printf("Bytes written: %d\n", bytesWritten);

            // sleep so we can fix the rate at which client sends messages
            try {
                Thread.sleep(1000 / messageRate);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
