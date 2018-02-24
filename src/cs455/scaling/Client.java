package cs455.scaling;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;

public class Client {
    private Selector selector;
    private int messageRate;
    private ByteBuffer buffer;

    public Client(int messageRate) throws IOException{
        this.selector = Selector.open();
        this.messageRate = messageRate;
        this.buffer = ByteBuffer.allocate(8000);
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();

        // now that we have finished connecting to the server,
        // next time the selector scans the channels for activity,
        // let selector know that this socketChannel is interested in
        // sending data to the server next time
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public void write(SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        Random r = new Random();
        r.nextBytes(buffer.array());

        int numBytesWritten = socketChannel.write(buffer);
        // TODO: going to have to modify this to handle cases when not all 8000 bytes are written
        if (numBytesWritten == 8000) {
            Thread.sleep(1000 / messageRate);
        }
        System.out.printf("Bytes written: %d\n", numBytesWritten);
    }

    public void startClient(String host, int portNum) throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(host, portNum));

        while (true) {
            int channelsReady = this.selector.select();

            if (channelsReady > 0) {
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();

                    if (key.isConnectable()) {
                        this.connect(key);
                    }

                    else if (key.isWritable()) {
                        this.write(key);
                    }
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
