package com.palliser.nztides;

/**
 * Utility class for creating ASCII art tide graphs
 */
public class TideGraphGenerator {
    private static final int GRAPH_ROWS = 10;
    private static final int GRAPH_COLS = 40;
    
    /**
     * Generate ASCII art tide graph
     */
    public static String generateTideGraph(float previousHeight, float nextHeight, 
                                         int currentTimeSeconds, int previousTideTime, 
                                         int nextTideTime) {
        char[][] graph = new char[GRAPH_ROWS][GRAPH_COLS + 1];
        initialiseGraph(graph);
        createTideGraph(graph, previousHeight, nextHeight);
        markCurrentPosition(graph, currentTimeSeconds, previousTideTime, nextTideTime);
        
        return graphToString(graph);
    }
    
    private static void initialiseGraph(char[][] graph) {
        for (int row = 0; row < GRAPH_ROWS; row++) {
            for (int col = 0; col < GRAPH_COLS; col++) {
                graph[row][col] = ' ';
            }
            graph[row][GRAPH_COLS] = '\n';
        }
    }
    
    private static void createTideGraph(char[][] graph, float previousHeight, float nextHeight) {
        for (int k = 0; k < GRAPH_COLS; k++) {
            double x = (1.0 + (previousHeight > nextHeight ? -1 : 1) * 
                       Math.sin(k * 2 * Math.PI / (GRAPH_COLS - 1))) / 2.0;
            x = ((GRAPH_ROWS - 1) * x + 0.5);
            graph[(int) x][k] = '*';
        }
    }
    
    /**
     * Parameters of cosine wave used to interpolate between tides
     * We assume that the tides vary cosinusoidally
     * between the last tide and the next one
     * See NZ Nautical almanac for more details
     */
    private static void markCurrentPosition(char[][] graph, int currentTimeSeconds, 
                                          int previousTideTime, int nextTideTime) {
        double omega = 2 * Math.PI / ((nextTideTime - previousTideTime) * 2);
        double phase = omega * (currentTimeSeconds - previousTideTime);
        double x = (phase + Math.PI / 2) / (2.0 * Math.PI);
        x = ((GRAPH_COLS - 1) * x + 0.5);
        
        for (int j = 0; j < GRAPH_ROWS; j++) {
            graph[j][(int) x] = '|';
        }
    }
    
    private static String graphToString(char[][] graph) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < GRAPH_ROWS; row++) {
            for (int col = 0; col < GRAPH_COLS + 1; col++) {
                result.append(graph[row][col]);
            }
        }
        result.append("\n");
        return result.toString();
    }
}
