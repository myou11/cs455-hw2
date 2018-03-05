package cs455.scaling.server;

import cs455.scaling.util.StatisticsCollector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

public class Server {
    private Selector selector;
    private ThreadPool threadPool;
    //private ByteBuffer buffer;

    private HashMap<String, Integer> clientThroughput;

    private long time;

    private final boolean DEBUG = true;

    public Server(int threadPoolSize) throws IOException {
        this.selector = Selector.open();
        //this.buffer = ByteBuffer.allocate(8000);
        this.clientThroughput = new HashMap<>();
        this.threadPool = new ThreadPool(threadPoolSize, clientThroughput);
        this.time = System.currentTimeMillis();
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();

        if (DEBUG)
            System.out.println("Accepting incoming connection");

        // Keep track of packet throughput for this client
        String ipPortNumStr = socketChannel.getRemoteAddress().toString();
        //System.out.printf("local addr of client: %s\n", ipPortNumStr);
        clientThroughput.put(ipPortNumStr, 0);
        // TODO: Cant increment the throughput per processed task since that happens in the TaskHandler thread
        // would have to pass a ref of the clientThroughput map into TaskHandler
        // Could avoid that by figuring out a way to give the threads a task and then have them indicate they are
        // ready to write, then server could just incrment the throughputs here?

        socketChannel.configureBlocking(false);
        // Register this socket with the intention to read from it
        // since client will be sending us data
        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        /*// clear buffer before we read into it, just in case it was written to before this
        buffer.clear();

        int numBytesRead = socketChannel.read(buffer);
        if (DEBUG)
            System.out.printf("Bytes read: %d\n", numBytesRead);

        // attach the byte[] to the key so the TaskHandler threads can retrieve the messages
        // and write the hashed message back to the client thru the SelectionKey's channel
        key.attach(buffer.array());
        this.threadPool.addWork(key);

        // now that we've read some data, this channel will want to write after
        // it hashes the message and want to send it back to the client
        key.interestOps(SelectionKey.OP_WRITE);*/
        ByteBuffer buffer = ByteBuffer.allocate(8000);

        int bytesRead = 0;
        while (buffer.hasRemaining() && bytesRead != -1) {
            bytesRead = socketChannel.read(buffer);
        }

        key.attach(buffer.array());
        this.threadPool.addWork(key);

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void startServer(int portNum) throws IOException {
        // get the host addr and create a socket address to bind the ServerSocket to
        InetAddress host = InetAddress.getLocalHost();
        InetSocketAddress isa = new InetSocketAddress(host.getHostAddress(), portNum);
        // create the ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(isa);

        System.out.printf("Server listening on port %d...\n", portNum);

        serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        StatisticsCollector statsCollector = new StatisticsCollector(clientThroughput);

        while (true) {
            long currTime = System.currentTimeMillis();
            if (currTime - this.time > 20000) {
                statsCollector.printStatistics(currTime);
                this.time = currTime;
            }

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