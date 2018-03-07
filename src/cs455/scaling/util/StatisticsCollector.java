package cs455.scaling.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class StatisticsCollector implements Runnable {
    private HashMap<String, Integer> clientThroughputList;

    /*  Allows us to keep track of when 20s has passed.
     *  Reset to current time every 20s to determine when to print the throughput statistics again.  */
    private long roundStartTime;

    /*  Allows us to keep track of when 1s has passed.
    /*  Reset to current time every second to determine when to copy info from the clientThroughputList.  */
    private long time;

    public StatisticsCollector(HashMap<String, Integer> clientThroughputList) {
        this.clientThroughputList = clientThroughputList;
        this.roundStartTime = System.currentTimeMillis();
        this.time = System.currentTimeMillis();
    }

    // Standard deviation
    private double stdDev(LinkedList<Double> meanThroughputList, double mean) {
        double sumSq = 0;
        for (double msgsPerS : meanThroughputList) {
            sumSq += Math.pow(Math.abs(msgsPerS - mean), 2);
        }
        sumSq /= meanThroughputList.size();
        return Math.sqrt(sumSq);
    }

    public void printStatistics(int activeClientConnections, LinkedList<Integer> serverThroughputList, LinkedList<Double> meanThroughputList) {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        double serverThroughput = 0;

        for (int msgsPerS : serverThroughputList) {
            serverThroughput += msgsPerS;
        }
        // meanThroughputList has the mean client throughput for the last 20s (msgs/20s), so divide it by 20 to get msgs/s.
        serverThroughput /= 20;

        double meanPerClientThroughput = serverThroughput / activeClientConnections;
        double stdDevPerClientThroughput = stdDev(meanThroughputList, meanPerClientThroughput);

        System.out.printf("[%s] Server Throughput: %.3f, Active Client Connections: %d, Mean Per-client Throughput: %.3f, Std. Dev. of Per-client Throughput: %.3f\n",
                        sdf.format(date), serverThroughput, activeClientConnections, meanPerClientThroughput, stdDevPerClientThroughput);
    }

    public void run() {
        /*  List of throughput (msgs/s) for each client for last 20s.
         *  This will allow us to find the throughput for the server.  */
        LinkedList<Integer> serverThroughputList = new LinkedList<>();

        /*  Mean throughput ((msgs/s) / num clients) for each client for last 20s.
         *  We will find the standard deviation of this list to see how much the server throughput
         *  varied in the last 20s.  */
        LinkedList<Double> meanThroughputList = new LinkedList<>();
        while (true) {
            long currTime = System.currentTimeMillis();

            // Copies throughput information from the clientThroughputList every second (1000ms)
            if (currTime - this.time > 1000) {
                synchronized (clientThroughputList) {
                    // server throughput for last second
                    double meanClientThroughput = 0;
                    for (Map.Entry<String, Integer> entry : clientThroughputList.entrySet()) {
                        serverThroughputList.addLast(entry.getValue());
                        meanClientThroughput += entry.getValue();
                        /*  Reset the throughput of the client.
                         *  Each 20s should only have stats for the last 20s, not the past 40, 60, etc...  */
                        clientThroughputList.put(entry.getKey(), 0);
                    }
                    meanClientThroughput /= clientThroughputList.size();
                    meanThroughputList.addLast(meanClientThroughput);
                }
                // reset the time so we know when next 20s happens
                this.time = currTime;
            }

            // Prints the throughput statistics, from the past 20s, every 20s (20000ms)
            if (currTime - this.roundStartTime > 20000) {
                int activeClientConnections = clientThroughputList.size();
                printStatistics(activeClientConnections, serverThroughputList, meanThroughputList);

                // Clear the lists because we only want statistics from the past 20s
                serverThroughputList.clear();
                meanThroughputList.clear();

                // Reset the roundStartTime so we can collect statistics when the next 20s rolls around
                this.roundStartTime = currTime;
            }
        }
    }
}