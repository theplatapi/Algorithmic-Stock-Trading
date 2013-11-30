package agents;

import common.Market;
import common.StockValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * User: allen
 * Date: 11/5/13
 * Time: 9:45 PM
 */
public class BollingerBandAgent {
    private final int millisecondsInADay = 86400000;
    private Market market = new Market(new java.sql.Date(System.currentTimeMillis() - millisecondsInADay));
    private StockValue stockValue;
    private ArrayList<String> stockSymbolsToTrade;
    private double wallet;
    private HashMap<String, Integer> numberOfShares;
    private HashMap<String, Double> lastValues;

    private int movingAverageSampleSize;
    private int bandWidth;
    private HashMap<String, ArrayBlockingQueue<StockValue>> stockValueQueues;
    private HashMap<String, BasicStatistics> basicStatistics;

    public BollingerBandAgent(ArrayList<String> stockSymbolsToTrade, int wallet, int movingAverageSampleSize, int bandWidth) {
        this.stockSymbolsToTrade = stockSymbolsToTrade;
        this.wallet = wallet;
        this.movingAverageSampleSize = movingAverageSampleSize;
        this.bandWidth = bandWidth;
        numberOfShares = new HashMap<String, Integer>(stockSymbolsToTrade.size());
        lastValues = new HashMap<String, Double>(stockSymbolsToTrade.size());
        stockValueQueues = new HashMap<String, ArrayBlockingQueue<StockValue>>(stockSymbolsToTrade.size());
        basicStatistics = new HashMap<String, BasicStatistics>(stockSymbolsToTrade.size());

        for (int i = 0; i < stockSymbolsToTrade.size(); i++) {
            stockValueQueues.put(stockSymbolsToTrade.get(i), new ArrayBlockingQueue<StockValue>(movingAverageSampleSize));
            basicStatistics.put(stockSymbolsToTrade.get(i), new BasicStatistics(movingAverageSampleSize));
            numberOfShares.put(stockSymbolsToTrade.get(i), 0);
        }

        System.out.println("Initial wallet is $" + wallet);
    }

    public void startTrading() {
        while (market.hasNextValue() && wallet >= 0) {
            stockValue = market.getNextValue();
            //only trade the stocks specified
            if (!stockSymbolsToTrade.contains(stockValue.getSymbol()))
                continue;
            //set last values
            lastValues.put(stockValue.getSymbol(), stockValue.getValue());
            //initialize q with 1st movingAverageSampleSize stocks. Temporary until market method is created to do this.
            if (stockValueQueues.get(stockValue.getSymbol()).size() < movingAverageSampleSize) {
                stockValueQueues.get(stockValue.getSymbol()).add(stockValue);
                basicStatistics.get(stockValue.getSymbol()).add(stockValue.getValue());
            }
            else if (stockValueQueues.get(stockValue.getSymbol()).size() == movingAverageSampleSize) {
                doTrade(stockValue);
                refreshQAndStats(stockValue);
                doTrade(stockValue);
            }
            else {
                refreshQAndStats(stockValue);
                doTrade(stockValue);
            }
        }
    }

    private void doTrade(StockValue stockValue) {
        double mean = basicStatistics.get(stockValue.getSymbol()).getMean();
        double standardDeviation = basicStatistics.get(stockValue.getSymbol()).getStandardDeviation();
        double lowerBound = mean - standardDeviation * bandWidth;
        double upperBound = mean + standardDeviation * bandWidth;
        int currentNumberOfShares = numberOfShares.get(stockValue.getSymbol());

        //undervalued
        if (stockValue.getValue() < lowerBound) {
            //buy
            if (wallet - stockValue.getValue() > 0) {
                wallet -= stockValue.getValue();
                numberOfShares.put(stockValue.getSymbol(), currentNumberOfShares + 1);
            }
        }
        //overvalued
        else if (stockValue.getValue() > upperBound) {
            //sell
            if (currentNumberOfShares > 0) {
                numberOfShares.put(stockValue.getSymbol(), currentNumberOfShares - 1);
                wallet += stockValue.getValue();
            }
        }
    }

    private void refreshQAndStats(StockValue newValue) {
        stockValueQueues.get(newValue.getSymbol()).remove();
        stockValueQueues.get(newValue.getSymbol()).add(newValue);
        basicStatistics.get(newValue.getSymbol()).removeOldestValue();
        basicStatistics.get(newValue.getSymbol()).add(newValue.getValue());
    }

    public double getFinalWallet() {
        return wallet;
    }

    public int getFinalStockCounts() {
        int sum = 0;

        for (int i = 0; i < numberOfShares.size(); i++)
            sum += numberOfShares.get(stockSymbolsToTrade.get(i));
        return sum;
    }

    public void printStockNameAndFrequencyOutput() {
        String leftAlignFormat = "| %-8s | %-9d |%n";

        System.out.format("+----------+------------+%n");
        System.out.printf("| Stock    | Frequency  |%n");
        System.out.format("+----------+------------+%n");
        for (int i = 0; i < stockSymbolsToTrade.size(); i++) {
            System.out.format(leftAlignFormat, stockSymbolsToTrade.get(i), numberOfShares.get(stockSymbolsToTrade.get(i)));
        }
        System.out.format("+----------+------------+%n");
    }

    public double getNetWorth() {
        int portfolioWorth = 0;

        for (int i = 0; i < numberOfShares.size(); i++) {
           portfolioWorth +=  numberOfShares.get(stockSymbolsToTrade.get(i)) * lastValues.get(stockSymbolsToTrade.get(i));
        }
        return wallet + portfolioWorth;
    }

    private class BasicStatistics {
        private ArrayBlockingQueue<Double> sample;
        private double mean;
        private double standardDeviation;

        public BasicStatistics(int sampleSize) {
            sample = new ArrayBlockingQueue<Double>(sampleSize);
        }

        public void add(double sampleValue) {
            int oldSampleSize = sample.size();
            sample.add(sampleValue);
            int newSampleSize = sample.size();

            //update mean
            double sum = mean * oldSampleSize;
            mean = (sum + sampleValue) / newSampleSize;
        }

        public void removeOldestValue() {
            int oldSampleSize = sample.size();
            double removedItem = sample.remove();
            int newSampleSize = sample.size();

            //update mean
            double sum = mean * oldSampleSize;
            mean = (sum - removedItem) / newSampleSize;
        }

        public double getMean() {
            return mean;
        }

        public double getStandardDeviation() {
            Iterator<Double> iterator = sample.iterator();
            double mean = getMean();
            double temp = 0;

            while (iterator.hasNext()) {
                double next = iterator.next();
                temp += (mean - next) * (mean - next);
            }

            return Math.sqrt(temp / sample.size());
        }
    }

}