package common;

import agents.*;

import java.sql.Date;
import java.sql.SQLException;
import java.util.*;

/**
 * User: Tag
 * Date: 10/31/13
 * Time: 6:47 AM
 */
public class Market {
    private ArrayList<Stock> stocks;
    private final int millisecondsInADay = 86400000;
    private ArrayList<Agent> agents;
    private int walletInUSDollars = 5000;

    public Market() throws SQLException {
        DatabaseConnection databaseConnection = new DatabaseConnection();

        databaseConnection.connect();
        stocks = databaseConnection.getStocksByDay(new Date(System.currentTimeMillis()));
        databaseConnection.disconnect();

        /**
         * refer to here for an explanation of this trick:
         * http://stackoverflow.com/questions/924285/efficiency-of-java-double-brace-initialization
         *
         * Add an agent to the list so it gets invoked.
         */
        agents = new ArrayList<Agent>()
        {{
                add(new BollingerBandAgent(walletInUSDollars));
                add(new AR1Agent(walletInUSDollars));
                add(new TrendAgent(walletInUSDollars));
                add(new DumbTrendAgent(walletInUSDollars));
                add(new BuyLowSellHighAgent(walletInUSDollars));
                add(new DiceRollAgent(walletInUSDollars));
        }};
    }

    public void executeTrades() {
        int i = 0;
        for (Stock currentStock : stocks) {
            for (Agent agent : agents)
                agent.trade(currentStock);
            if (i++ % 1000 == 0)
                printResults();
        }
    }

    public void printResults() {
        for (Agent agent : agents) {
            agent.printResults();
            System.out.println();
        }

        Stack<Agent> results = new Stack<Agent>();
        results.addAll(agents);

        Collections.sort(results, new Comparator<Agent>() {
            @Override
            public int compare(Agent agent, Agent agent2) {
                return new Double(agent2.getNetWorth()).compareTo(agent.getNetWorth());
            }
        });

        System.out.println("Agent Ranking:");
        for (int i = 1; i < results.size() + 1; i++)
            System.out.println(i + ". " + results.get(i - 1).getAgentName());
    }
}
