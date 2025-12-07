import java.util.*;
class StockTradingSimulator {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        StockMarket market = new StockMarket();
        UserPortfolio portfolio = new UserPortfolio();

        while (true) {
            System.out.println("\n===== STOCK TRADING SIMULATOR =====");
            System.out.println("1. View Market Prices");
            System.out.println("2. Buy Stock");
            System.out.println("3. Sell Stock");
            System.out.println("4. View Portfolio");
            System.out.println("5. View Transaction History");
            System.out.println("6. Exit");
            System.out.print("Choose option: ");

            int choice = sc.nextInt();

            switch (choice) {
                case 1:
                    market.displayMarket();
                    break;

                case 2:
                    System.out.print("Enter stock symbol: ");
                    String buySymbol = sc.next().toUpperCase();
                    Stock buyStock = market.getStock(buySymbol);
                    if (buyStock == null) {
                        System.out.println(" Invalid stock symbol!");
                        break;
                    }
                    System.out.print("Enter quantity: ");
                    int buyQty = sc.nextInt();
                    portfolio.buyStock(buyStock, buyQty);
                    break;

                case 3:
                    System.out.print("Enter stock symbol: ");
                    String sellSymbol = sc.next().toUpperCase();
                    Stock sellStock = market.getStock(sellSymbol);
                    if (sellStock == null) {
                        System.out.println("❌ Invalid stock symbol!");
                        break;
                    }
                    System.out.print("Enter quantity: ");
                    int sellQty = sc.nextInt();
                    portfolio.sellStock(sellStock, sellQty);
                    break;

                case 4:
                    portfolio.displayPortfolio(market);
                    break;

                case 5:
                    portfolio.displayTransactions();
                    break;

                case 6:
                    System.out.println("Exiting... Goodbye!");
                    System.exit(0);

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
}
