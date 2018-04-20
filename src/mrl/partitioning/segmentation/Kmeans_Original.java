package mrl.partitioning.segmentation;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author Selcuk Orhan DEMIREL
 */

public class Kmeans_Original implements KmeansI {

    private Double[][] data;         // data to cluster
    private int numClusters;    // number of clusters
    private Double[][] clusterCenters;   // cluster centers
    private int dataSize;               // size of the data
    private int dataDim;                // dimension of the data
    private ArrayList<Double[]>[] clusters;     // calculated clusters
    private Double[] clusterVars;        // cluster variances

    private Double epsilon;

    public Kmeans_Original(Double[][] data, int numClusters, Double[][] clusterCenters) {
        dataSize = data.length;
        dataDim = data[0].length;

        this.data = data;

        this.numClusters = numClusters;

        this.clusterCenters = clusterCenters;

        clusters = new ArrayList[numClusters];
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new ArrayList();
        }
        clusterVars = new Double[numClusters];

        epsilon = 0.01;
    }

    public Kmeans_Original(Double[][] data, int numClusters) {
        this(data, numClusters, true);
    }

    public Kmeans_Original(Double[][] data, int numClusters, boolean randomizeCenters) {
        dataSize = data.length;
        dataDim = data[0].length;

        this.data = data;

        this.numClusters = numClusters;

        this.clusterCenters = new Double[numClusters][dataDim];

        clusters = new ArrayList[numClusters];
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new ArrayList();
        }
        clusterVars = new Double[numClusters];

        epsilon = 0.01;

        if (randomizeCenters) {
            randomizeCenters(numClusters, data);
        }
    }

    private void randomizeCenters(int numClusters, Double[][] data) {
        Random r = new Random();
        int[] check = new int[numClusters];
        for (int i = 0; i < numClusters; i++) {
            int rand = r.nextInt(dataSize);
            if (check[i] == 0) {
                this.clusterCenters[i] = data[rand].clone();
                check[i] = 1;
            } else {
                i--;
            }
        }
    }

    private void calculateClusterCenters() {
        for (int i = 0; i < numClusters; i++) {
            int clustSize = clusters[i].size();

            for (int k = 0; k < dataDim; k++) {

                Double sum = 0d;
                for (int j = 0; j < clustSize; j++) {
                    Double[] elem = (Double[]) clusters[i].get(j);
                    sum += elem[k];
                }

                clusterCenters[i][k] = sum / clustSize;
            }
        }
    }

    private void calculateClusterVars() {
        for (int i = 0; i < numClusters; i++) {
            int clustSize = clusters[i].size();
            Double sum = 0d;

            for (int j = 0; j < clustSize; j++) {

                Double[] elem = (Double[]) clusters[i].get(j);

                for (int k = 0; k < dataDim; k++) {
                    sum += Math.pow((Double) elem[k] - getClusterCenters()[i][k], 2);
                }
            }

            clusterVars[i] = sum / clustSize;
        }
    }

    @Override
    public Double getTotalVar() {
        Double total = 0d;
        for (int i = 0; i < numClusters; i++) {
            total += clusterVars[i];
        }

        return total;
    }

    @Override
    public Double[] getClusterVars() {
        return clusterVars;
    }

    @Override
    public ArrayList[] getClusters() {
        return clusters;
    }

    private void assignData() {
        for (int k = 0; k < numClusters; k++) {
            clusters[k].clear();
        }

        for (int i = 0; i < dataSize; i++) {

            int clust = 0;
            double dist = Double.MAX_VALUE;
            double newdist = 0;

            for (int j = 0; j < numClusters; j++) {
                newdist = distToCenter(data[i], j);
                if (newdist <= dist) {
                    clust = j;
                    dist = newdist;
                }
            }

            clusters[clust].add(data[i]);
        }

    }

    private Double distToCenter(Double[] datum, int j) {
        Double sum = 0d;
        for (int i = 0; i < dataDim; i++) {
            sum += Math.pow((datum[i] - getClusterCenters()[j][i]), 2);
        }

        return Math.sqrt(sum);
    }

    @Override
    public void calculateClusters() {

        Double var1 = Double.MAX_VALUE;
        Double var2;
        Double delta;

        do {
            calculateClusterCenters();
            assignData();
            calculateClusterVars();
            var2 = getTotalVar();
            if (Double.isNaN(var2))    // if this happens, there must be some empty clusters
            {
                delta = Double.MAX_VALUE;
                randomizeCenters(numClusters, data);
                assignData();
                calculateClusterCenters();
                calculateClusterVars();
            } else {
                delta = Math.abs(var1 - var2);
                var1 = var2;
            }

        } while (delta > epsilon);
    }

    @Override
    public void setEpsilon(Double epsilon) {
        if (epsilon > 0) {
            this.epsilon = epsilon;
        }
    }

    /**
     * @return the clusterCenters
     */
    @Override
    public Double[][] getClusterCenters() {
        return clusterCenters;
    }
}