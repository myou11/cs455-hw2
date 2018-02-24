package cs455.scaling;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {
    private Selector selector;
    private ThreadPool threadPool;
    private ByteBuffer buffer;

    private final boolean DEBUG = true;

    public Server(int threadPoolSize) throws IOException {
        this.selector = Selector.open();
        this.threadPool = new ThreadPool(threadPoolSize);
        this.buffer = ByteBuffer.allocate(8000);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();

        System.out.println("Accepting incoming connection");
        socketChannel.configureBlocking(false);
        // Register this socket with the intention to read from it
        // since client will be sending us data
        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        // clear buffer before we read into it, just in case it was written to before this
        buffer.clear();

        int numBytesRead = socketChannel.read(buffer);
        System.out.printf("Bytes read: %d\n", numBytesRead);
        this.threadPool.addWork(buffer.array());
    }

    public void startServer(int portNum) throws IOException {
        if (DEBUG)
            System.out.println("Server starting...");

        // setup thread pool
        this.threadPool.initialize();
        this.threadPool.startThreads();

        // create the ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress("localhost", portNum));

        serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        while (true) {
            int channelsReady = this.selector.select();

            if (channelsReady > 0) {
                Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();

                    if (key.isAcceptable()) {
                        this.accept(key);
                    }

                    else if (key.isReadable()) {
                        this.read(key);
                    }

                    keys.remove();
                }
            }

        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(Integer.parseInt(args[1]));
            server.startServer(Integer.parseInt(args[0]));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}