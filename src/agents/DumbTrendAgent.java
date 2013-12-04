package agents;

import common.Market;
import common.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * User: Tag
 * Date: 10/31/13
 * Time: 7:00 AM
 */
public class DumbTrendAgent extends Agent{
    private ArrayList<String> stockSymbolsToTrade;
    private HashMap<String, Integer> numberOfShares;
    private HashMap<String, Double> lastValues;
    private HashMap<String, Boolean> first;
    public DumbTrendAgent(double capital){
        super(capital);
        stockSymbolsToTrade = new ArrayList<String>() {{
            add("TWTR");
            add("VZ");
            add("KR");
            add("BKW");
            add("GOOG");
            add("MSFT");
            add("OLN");
            add("BA");
            add("MSI");
            add("TDC");
        }};
        numberOfShares = new HashMap<String, Integer>(stockSymbolsToTrade.size());
        lastValues = new HashMap<String, Double>(stockSymbolsToTrade.size());
        first = new HashMap<String,Boolean>(stockSymbolsToTrade.size());
        for (int i = 0; i < stockSymbolsToTrade.size(); i++) {
            numberOfShares.put(stockSymbolsToTrade.get(i), 0);
            first.put(stockSymbolsToTrade.get(i), true);
        }
    }

    public void trade(Stock stock){
        if (stockSymbolsToTrade.contains(stock.getSymbol())) {
            if(first.get(stock.getSymbol())){
                lastValues.put(stock.getSymbol(), stock.getValue());
                first.put(stock.getSymbol(),false);
            }
            else{
                int currentNumberOfShares = numberOfShares.get(stock.getSymbol());

                if (stock.getValue()> lastValues.get(stock.getSymbol())) {
                    //increasing, buy 1 share
                    if (wallet > stock.getValue()) {
                        wallet -= stock.getValue();
                        numberOfShares.put(stock.getSymbol(), currentNumberOfShares + 1);
                        //System.out.println("buying a share at $" + curValue[counter]);
                    }
                }
                else if (stock.getValue()< lastValues.get(stock.getSymbol())) {
                    //decreasing, sell 1 share
                    if (currentNumberOfShares > 0) {
                        numberOfShares.put(stock.getSymbol(), currentNumberOfShares - 1);
                        wallet += stock.getValue();;
                        //System.out.println("selling a share at $" + curValue[counter]);
                    }
                }
                lastValues.put(stock.getSymbol(), stock.getValue());
            }
        }

    }
    public String getAgentName(){
        return "Dumb Trend Agent";
    }
    public int getTotalNumberOfStocks(){
        int sum = 0;

        for (int i = 0; i < numberOfShares.size(); i++)
            sum += numberOfShares.get(stockSymbolsToTrade.get(i));
        return sum;

    }
    public double getNetWorth(){
        int portfolioWorth = 0;

        for (int i = 0; i < numberOfShares.size(); i++)
            portfolioWorth += numberOfShares.get(stockSymbolsToTrade.get(i)) * lastValues.get(stockSymbolsToTrade.get(i));
        return wallet + portfolioWorth;
    }

}
