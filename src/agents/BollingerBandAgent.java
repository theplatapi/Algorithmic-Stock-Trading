package agents;

import common.BasicStatistics;
import common.Stock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * User: allen
 * Date: 11/5/13
 * Time: 9:45 PM
 */
public class BollingerBandAgent extends MultipleStockTraderAgent {
    private int movingAverageSampleSize;
    private int bandWidth;
    private HashMap<String, ArrayBlockingQueue<Stock>> stockQueues;
    private HashMap<String, BasicStatistics> basicStatistics;

    public BollingerBandAgent(double capital) {
        super(capital);
        initValues(40, 2);
    }

    private void initValues(int movingAverageSampleSize, int bandWidth) {
        this.movingAverageSampleSize = movingAverageSampleSize;
        this.bandWidth = bandWidth;
        stockQueues = new HashMap<String, ArrayBlockingQueue<Stock>>(stockSymbolsToTrade.size());
        basicStatistics = new HashMap<String, BasicStatistics>(stockSymbolsToTrade.size());

        for (int i = 0; i < stockSymbolsToTrade.size(); i++) {
            stockQueues.put(stockSymbolsToTrade.get(i), new ArrayBlockingQueue<Stock>(movingAverageSampleSize));
            basicStatistics.put(stockSymbolsToTrade.get(i), new BasicStatistics(movingAverageSampleSize));
        }
    }

    @Override
    public void trade(Stock stock) {
        if (stockSymbolsToTrade.contains(stock.getSymbol())) {
            lastValues.put(stock.getSymbol(), stock.getValue());
            if (stockQueues.get(stock.getSymbol()).size() < movingAverageSampleSize) {
                stockQueues.get(stock.getSymbol()).add(stock);
                basicStatistics.get(stock.getSymbol()).add(stock.getValue());
            } else if (stockQueues.get(stock.getSymbol()).size() == movingAverageSampleSize) {
                doTrade(stock);
                refreshQAndStats(stock);
                doTrade(stock);
            } else {
                refreshQAndStats(stock);
                doTrade(stock);
            }
        }
    }

    @Override
    public String getAgentName() {
        return "Bollinger Band Agent";
    }

    private void doTrade(Stock stock) {
        double mean = basicStatistics.get(stock.getSymbol()).getMean();
        double standardDeviation = basicStatistics.get(stock.getSymbol()).getStandardDeviation();
        double lowerBound = mean - standardDeviation * bandWidth;
        double upperBound = mean + standardDeviation * bandWidth;

        //undervalued
        if (stock.getValue() < lowerBound) {
            //buy
            super.buy(stock);
        }
        //overvalued
        else if (stock.getValue() > upperBound) {
            //sell
            super.sell(stock);
        }
    }

    private void refreshQAndStats(Stock newValue) {
        stockQueues.get(newValue.getSymbol()).remove();
        stockQueues.get(newValue.getSymbol()).add(newValue);
        basicStatistics.get(newValue.getSymbol()).removeOldestValue();
        basicStatistics.get(newValue.getSymbol()).add(newValue.getValue());
    }
}
