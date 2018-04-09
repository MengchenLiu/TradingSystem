package project;
import java.util.Hashtable;

/**
 * The type Mutual fund.
 */
class MutualFund {

    private String mutualFundName;
    private Hashtable<String, Double> stockShareTable;

    /**
     * Instantiates a new Mutual fund.
     *
     * @param mutualFundName the mutual fund name
     */
    MutualFund(String mutualFundName) {
        this.mutualFundName = mutualFundName;
        this.stockShareTable = new Hashtable<>();

        switch (mutualFundName) {
            case "Mutual_Fund_Banking":
                stockShareTable.put("DeutscheBank", 0.2);
                stockShareTable.put("CREDITAGRICOLE", 0.2);
                stockShareTable.put("SOCIETEGENERALE", 0.1);
                stockShareTable.put("AmericanExpress", 0.2);
                stockShareTable.put("GoldmanSachs", 0.1);
                stockShareTable.put("JPMorganChase", 0.15);
                stockShareTable.put("NomuraHoldingsInc", 0.05);
                break;
            case "Mutual_Fund_Energy":
                stockShareTable.put("Petrobras", 0.15);
                stockShareTable.put("BPPLC", 0.15);
                stockShareTable.put("TOTAL", 0.4);
                stockShareTable.put("ExxonMobil", 0.3);
                break;
            case "Mutual_Fund_Diversified":
                stockShareTable.put("SwirePacificLimited", 0.15);
                stockShareTable.put("SoftbankCorp", 0.35);
                stockShareTable.put("SkyPLC", 0.4);
                stockShareTable.put("DeutscheLufthansa", 0.1);
                break;
            default:
                break;
        }
    }

    /**
     * Gets stock share table.
     *
     * @return the stock share table
     */
    Hashtable<String, Double> getStockShareTable() {
        return stockShareTable;
    }
}