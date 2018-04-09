package project;
import java.util.Hashtable;

/**
 * Stock: for storing all necessary information of stocks in this project.
 */
public class Stock {

    // a Stock object will have the following attributes:
    private String stockName;                       // the name of this Stock in the given csv file, e.g. "ACCOR"
    private String exchangeName;                    // the name of the Exchange where this Stock is listed

    private Hashtable<Integer, Double> priceTable;  // stores <timeIndex, price> of this Stock
    private Hashtable<Integer, Integer> qtyTable;   // stores <timeIndex, quantity> of this Stock

    private int currentQty;                         // the current quantity of this Stock

    private ExchangeUtils exchangeUtils;            // for dealing with currency of the price of this Stock
    private double currencyToUSD;                   // the current price of this Stock in USD
    private String currencySymbol;                  // local currency symbol of this Stock e.g. "EUR"

    /**
     * Instantiates a new Stock.
     *
     * @param stockName    the name of this Stock
     * @param exchangeName the exchange name of where this Stock is listed
     */
    Stock(String stockName, String exchangeName) {
        this.stockName = stockName;
        this.exchangeName = exchangeName;

        this.priceTable = new Hashtable<>();
        this.qtyTable = new Hashtable<>();

        this.currentQty = 0;

        this.exchangeUtils = new ExchangeUtils(this.exchangeName);
        this.currencyToUSD = exchangeUtils.getCurrencyToUSD();
        this.currencySymbol = exchangeUtils.getCurrencySymbol();
    }


    @Override
    public String toString() {
        return String.valueOf(this.currentQty);
    }

    public String currentPriceToString(int time) {
        double localPrice = getPrice(time);
        return localPrice + " " + currencySymbol + " (= " + String.format("%1$,.2f", (localPrice * currencyToUSD)) + " USD)";
    }

    // A list of getters and setters for a Stock object:
    String getName() {
        return stockName;
    }

    String getExchangeName() {
        return exchangeName;
    }


    void setPrice(int time, double price) {
        this.priceTable.put(time, price);
    }


    void setQuantity(int time, int quantity) {
        this.qtyTable.put(time, quantity);
    }


    double getPrice(int time) {
        return this.priceTable.get(time);
    }

    int getQty(int time) {
        return this.qtyTable.get(time);
    }

    int getCurrentQty() {
        return this.currentQty;
    }

    void addCurrentQty(int qty) {
        this.currentQty += qty;
    }

    boolean deCurrentQty(int qty) {
        if(this.currentQty < qty){
            return false;
        }else{
            this.currentQty -= qty;
            return true;
        }
    }

    void printPriceTable() {
        System.out.println(this.priceTable);
    }


    void printQtyTable() {
        System.out.println(this.qtyTable);
    }

}

