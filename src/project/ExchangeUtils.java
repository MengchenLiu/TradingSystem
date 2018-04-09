package project;

class ExchangeUtils {

    private String currencySymbol;
    private double currencyToUSD;
    private int port;

    ExchangeUtils(String exchangeName) {
        switch (exchangeName) {
            case "Bombay":
                this.currencySymbol = "INR";
                this.currencyToUSD = 0.015;
                this.port = 10000;
                break;
            case "Brussels":
                this.currencySymbol = "EUR";
                this.currencyToUSD = 1.11;
                this.port = 10001;
                break;
            case "EuronextParis":
                this.currencySymbol = "EUR";
                this.currencyToUSD = 1.11;
                this.port = 10002;
                break;
            case "Frankfurt":
                this.currencySymbol = "EUR";
                this.currencyToUSD = 1.11;
                this.port = 10003;
                break;
            case "HongKong":
                this.currencySymbol = "HKD";
                this.currencyToUSD = 0.13;
                this.port = 10004;
                break;
            case "Johannesburg":
                this.currencySymbol = "ZAR";
                this.currencyToUSD = 0.077;
                this.port = 10005;
                break;
            case "Lisbon":
                this.currencySymbol = "EUR";
                this.currencyToUSD = 1.11;
                this.port = 10006;
                break;
            case "London":
                this.currencySymbol = "GBP";
                this.currencyToUSD = 1.28;
                this.port = 10007;
                break;
            case "NewYorkStockExchange":
                this.currencySymbol = "USD";
                this.currencyToUSD = 1.0;
                this.port = 10008;
                break;
            case "SaoPaulo":
                this.currencySymbol = "BRL";
                this.currencyToUSD = 0.31;
                this.port = 10009;
                break;
            case "Seoul":
                this.currencySymbol = "KRW";
                this.currencyToUSD = 0.00089;
                this.port = 10010;
                break;
            case "Shanghai":
                this.currencySymbol = "CNY";
                this.currencyToUSD = 0.15;
                this.port = 10011;
                break;
            case "Shenzhen":
                this.currencySymbol = "CNY";
                this.currencyToUSD = 0.15;
                this.port = 10012;
                break;
            case "Sydney":
                this.currencySymbol = "AUD";
                this.currencyToUSD = 0.74;
                this.port = 10013;
                break;
            case "Tokyo":
                this.currencySymbol = "JPY";
                this.currencyToUSD = 0.0090;
                this.port = 10014;
                break;
            case "Toronto":
                this.currencySymbol = "CAD";
                this.currencyToUSD = 0.74;
                this.port = 10015;
                break;
            case "Zurich":
                this.currencySymbol = "EUR";
                this.currencyToUSD = 1.11;
                this.port = 10016;
                break;
        }
    }

    String getCurrencySymbol() {
        return currencySymbol;
    }

    double getCurrencyToUSD() {
        return currencyToUSD;
    }

    int getPort() {
        return port;
    }
}
