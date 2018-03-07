package cs455.scaling.server;

import cs455.scaling.util.StatisticsCollector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

public class Server {
    private Selector selector;
    private ThreadPool threadPool;

    // Ensures we only start the StatisticsCollector once
    private boolean statisticsStarted = false;

    // Shared with the TaskHandler. Allows TaskHandler to track the throughput for each client
    private HashMap<String, Integer> clientThroughput;

    private final boolean DEBUG = false;

    public Server(int threadPoolSize) throws IOException {
        this.selector = Selector.open();
        this.clientThroughput = new HashMap<>();
        this.threadPool = new ThreadPool(threadPoolSize, clientThroughput);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();

        if (DEBUG)
            System.out.println("Accepting incoming connection");

        // Keep track of packet throughput for this client
        String ipPortNumStr = socketChannel.getRemoteAddress().toString();

        /*  This object is shared with the TaskHandler. Need to initialize the throughput for this connection 0.
         *  TaskHandler will increment the throughput for a client every time it processes a message for that client.  */
        synchronized (clientThroughput) {
            clientThroughput.put(ipPortNumStr, 0);
        }

        socketChannel.configureBlocking(false);
        // Register this socket with the intention to read from it since client will be sending us data
        socketChannel.register(this.selector, SelectionKey.OP_READ);

        /*  Can only start the StatisticsCollector once a client has been registered. This is because the
         *  StatisticsCollector is its own thread. The thread will try to calculate the meanClientThroughput
         *  per second as soon as it is started. Had this thread been started in the main server thread, before
         *  any connections came in, then a divide by 0 exception would occur since you need to divide clientThroughput
         *  by the number of clients to find meanClientThroughput  */
        if (!statisticsStarted) {
            statisticsStarted = true;
            (new Thread(new StatisticsCollector(clientThroughput))).start();
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        /*  Keep the server as simple as possible. Let the worker thread do work of reading the information from the client.
         *  All the server needs to do is add the key to the work queue.  */
        key.interestOps(SelectionKey.OP_WRITE);
        this.threadPool.addWork(key);
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