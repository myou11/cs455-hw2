package cs455.scaling.client;


import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

public class Client {
    private Selector selector;
    private int messageRate;
    private ByteBuffer buffer;

    private LinkedList<byte[]> sentHashCodes;

    private final boolean DEBUG = false;

    public Client(int messageRate) throws IOException{
        this.selector = Selector.open();
        this.messageRate = messageRate;
        this.buffer = ByteBuffer.allocate(20);
        this.sentHashCodes = new LinkedList<>();
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();

        System.out.printf("Client listening on port: %d\n", socketChannel.socket().getLocalPort());

        /*  Once connected to the server, the main client thread will only want to read the responses
         *  the server sends back since the client's sender thread will take care of writing messages to the server  */
        key.interestOps(SelectionKey.OP_READ);

        // start a new thread to send messages
        (new Thread(new ClientSenderThread(key, messageRate, sentHashCodes))).start();
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // reset position to 0 for reading into the buffer
        buffer.clear();

        int bytesRead = 0;
        while (buffer.hasRemaining() && bytesRead != -1) {
            bytesRead = socketChannel.read(buffer);
        }

        byte[] hashBytes = buffer.array();

        // Resets the position to 0 to allow us to read (get) the data into a byte[]
        buffer.flip();
        buffer.get(hashBytes);

        // clear the buffer for the next write operation
        buffer.clear();

        // convert the hashed digest into a readable format; used for debugging below
        BigInteger hashInt = new BigInteger(1, hashBytes);
        String hashString = hashInt.toString(16);

        if (DEBUG)
            System.out.printf("Hash code from server: %s\n", hashString);

        if (sentHashCodes.contains(hashBytes)) {
            sentHashCodes.remove(hashBytes);

            if (DEBUG)
                System.out.printf("Hash code %s removed from the list\n", hashString);
        } else {
            if(DEBUG)
                System.out.printf("Hash code %s was not found in the list\n", hashString);
        }
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

                    /*  No need to check for isWritable b/c client has a separate thread that sends
                     *  the server messageRate msgs every second.  */

                    // Remove the key now that we've handled it
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
