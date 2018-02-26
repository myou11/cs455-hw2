package cs455.scaling;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StatisticsCollector {
    HashMap<String, Integer> clientThroughput;

    public StatisticsCollector(HashMap<String, Integer> clientThroughput) {
        this.clientThroughput = clientThroughput;
    }

    private double stdDev(double mean) {
        double sumSq = 0;
        for (Map.Entry<String, Integer> entry : clientThroughput.entrySet()) {
            sumSq += Math.pow(Math.abs(entry.getValue() - mean), 2);

            // TODO: clear the throughputs in the map for the next rounds of statistics printing
            // each 20s should only have stats for the last 20s, not the past 40, 60, etc...
            clientThroughput.put(entry.getKey(), 0);
        }
        sumSq /= clientThroughput.size();
        return Math.sqrt(sumSq);
    }

    public void printStatistics(long currTime) {
        Date date = new Date(currTime);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        int serverThroughput = 0;
        int activeClientConnections = clientThroughput.size();

        for (int throughput : clientThroughput.values()) {
            serverThroughput += throughput;
        }

        double meanPerClientThroughput = serverThroughput / activeClientConnections;
        double stdDevPerClientThroughput = stdDev(meanPerClientThroughput);

        System.out.printf("[%s] Server Throughput: %d, Active Client Connection: %d, Mean Per-client Throughput: %.3f, Std. Dev. of Per-client Throughput: %.3f\n",
                        sdf.format(date), serverThroughput, activeClientConnections, meanPerClientThroughput, stdDevPerClientThroughput);
    }
}