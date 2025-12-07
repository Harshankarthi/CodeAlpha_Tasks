import java.util.*;
class UserPortfolio {
    private HashMap<String, Integer> holdings = new HashMap<>();
    private ArrayList<Transaction> history = new ArrayList<>();
    private double balance = 10000.0; // starting balance

    public double getBalance() {
        return balance;
    }

    public void buyStock(Stock stock, int qty) {
        double cost = stock.getPrice() * qty;
        if (cost > balance) {
            System.out.println("❌ Not enough balance!");
            return;
        }
        balance -= cost;

        holdings.put(stock.getSymbol(), holdings.getOrDefault(stock.getSymbol(), 0) + qty);
        history.add(new Transaction(stock.getSymbol(), qty, stock.getPrice(), "BUY"));
        System.out.println("✔ Bought " + qty + " shares of " + stock.getSymbol());
    }

    public void sellStock(Stock stock, int qty) {
        if (!holdings.containsKey(stock.getSymbol()) || holdings.get(stock.getSymbol()) < qty) {
            System.out.println("❌ Not enough shares!");
            return;
        }
        double revenue = stock.getPrice() * qty;
        balance += revenue;

        holdings.put(stock.getSymbol(), holdings.get(stock.getSymbol()) - qty);
        history.add(new Transaction(stock.getSymbol(), qty, stock.getPrice(), "SELL"));
        System.out.println("✔ Sold " + qty + " shares of " + stock.getSymbol());
    }

    public void displayPortfolio(StockMarket market) {
        System.out.println("\n--- Portfolio Summary ---");
        double totalValue = balance;

        for (String symbol : holdings.keySet()) {
            int qty = holdings.get(symbol);
            double price = market.getStock(symbol).getPrice();
            double value = qty * price;

            System.out.println(symbol + " | Qty: " + qty + " | Price: $" + price + " | Value: $" + value);
            totalValue += value;
        }

        System.out.println("Cash Balance: $" + balance);
        System.out.println("Total Portfolio Value: $" + totalValue);
    }

    public void displayTransactions() {
        System.out.println("\n--- Transaction History ---");
        for (Transaction t : history) {
            System.out.println(t);
        }
    }
}
