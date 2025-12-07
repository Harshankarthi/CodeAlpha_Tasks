import java.util.*;

class StockMarket {
    private HashMap<String, Stock> stocks = new HashMap<>();

    public StockMarket() {
        stocks.put("AAPL", new Stock("AAPL", 150.0));
        stocks.put("GOOG", new Stock("GOOG", 2800.0));
        stocks.put("TSLA", new Stock("TSLA", 700.0));
        stocks.put("AMZN", new Stock("AMZN", 3300.0));
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }

    public void displayMarket() {
        System.out.println("\n--- Market Prices ---");
        for (Stock stock : stocks.values()) {
            System.out.println(stock.getSymbol() + " → $" + stock.getPrice());
        }
    }
}
