package cs455.scaling;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Client {
    private Selector selector;
    private int messageRate;
    private ByteBuffer buffer;

    private ArrayList<String> sentHashCodes;

    private final boolean DEBUG = true;

    public Client(int messageRate) throws IOException{
        this.selector = Selector.open();
        this.messageRate = messageRate;
        this.buffer = ByteBuffer.allocate(8000);
        this.sentHashCodes = new ArrayList<>();
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();

        if (DEBUG)
            System.out.printf("Client listening on port: %d\n", socketChannel.socket().getLocalPort());

        // now that we have finished connecting to the server,
        // next time the selector scans the channels for activity,
        // let selector know that this socketChannel is interested in
        // sending data to the server next time
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // reset position to 0 for reading into the buffer
        // TODO: think about using compact
        buffer.clear();

        int numBytesRead = socketChannel.read(buffer);
        byte[] hashBytes = new byte[numBytesRead];

        // after reading response from the server, set channel to write again
        key.interestOps(SelectionKey.OP_WRITE);
        // resets the position to 0 to allow us to read the data into a byte[]
        buffer.flip();
        buffer.get(hashBytes);

        // clear the buffer for the next write operation
        buffer.clear();

        String hashCode = new String(hashBytes).trim();
        System.out.printf("Hash code from server: %s\n", hashCode);

        if (sentHashCodes.contains(hashCode)) {
            sentHashCodes.remove(hashCode);
            System.out.printf("Hash code %s removed from the list\n", hashCode);
        } else
            System.out.printf("Hash code %s was not found in the list\n", hashCode);
    }

    private void write(SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        Random r = new Random();
        r.nextBytes(buffer.array());

        // Keep track of hash code to verify with hash code server sends back
        sentHashCodes.add(TaskHandler.SHA1FromBytes(buffer.array()));

        int numBytesWritten = socketChannel.write(buffer);

        // after sending random 8KB to the server, this channel
        // will be expecting a response so get it ready to read next time around
        // TODO: might need to set interests to write and read since we want to send
        // at a rate of R per second. Might need to remove the else ifs below too
        key.interestOps(/*SelectionKey.OP_WRITE | */SelectionKey.OP_READ);

        // TODO: going to have to modify this to handle cases when not all 8000 bytes are written
        /*if (numBytesWritten == 8000) {
            Thread.sleep(1000 / messageRate);
        }*/

        if (DEBUG)
            System.out.printf("Bytes written: %d\n", numBytesWritten);

        Thread.sleep(1000/messageRate);
    }

    private void startClient(String host, int serverPortNum) throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(host, serverPortNum));

        while (true) {
            int channelsReady = this.selector.select();

            if (channelsReady > 0) {
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();

                    if (key.isConnectable()) {
                        this.connect(key);
                    }

                    else if (key.isReadable()) {
                        this.read(key);
                    }

                    else if (key.isWritable()) {
                        this.write(key);
                    }

                    keys.remove();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client(Integer.parseInt(args[2]));
            client.startClient(args[0], Integer.parseInt(args[1]));
        } catch (IOException | InterruptedException excep) {
            excep.printStackTrace();
        }
    }
}
