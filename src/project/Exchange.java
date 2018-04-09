package project;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import org.json.*;

/**
 * Usage: java Exchange --exchangeName=<exchangeName> --serverName=<serverName>
 */
public class Exchange {

    // An Exchange object will have the following attributes:
    private String exchangeName;                  // it's name given in the csv file, e.g. "Shenzhen"
    private int exchangePort;                     // a port number it will listen on
    private ServerSocket serverSocket;            // a server socket it will open for accepting incoming connections

    private Hashtable<String, Stock> stockTable;  // stores <stock name, stock object> for this Exchange

    private String serverName;                    // the name of the continent Server for this Exchange, e.g. "Asia"
    private String serverIP;                      // the IP address of the continent Server, e.g. "localhost"
    private int serverPort;                       // the port number the continent Server listens on

    private String backupServerName;              // the name of the backup continent Server for this Exchange, e.g. "AsiaBackup"
    private String backupServerIP;                // the IP address of the backup continent Server, e.g. "localhost"
    private int backupServerPort;                 // the port number the backup continent Server listens on

    private Timer timer;                          // a timer used to start the "time" of this Exchange
    private long startTime;                       // the time when the system should start, sent from Server after registration
    private int delay;                            // for scheduling the timer task
    private int period;                           // for scheduling the timer task
    private int timeIndex;                        // e.g. timeIndex 1 corresponds to the 1st timestamp in the csv file
    private Hashtable<Integer, String> timeIndexTable; // stores the timeIndex and its corresponding timestamp

    private PrintWriter logWriter;                // for writing log file for this Exchange for recovering in case of failure


    /**
     * Constructor: Initiates an exchange by its name and its corresponding Continent Server name
     *
     * @param exchangeName the exchange name
     * @param serverName   the server name
     */
    private Exchange(String exchangeName, String serverName) {

        try {
            this.exchangeName = exchangeName;
            this.exchangePort = new ExchangeUtils(exchangeName).getPort();
            this.serverSocket = new ServerSocket(this.exchangePort);

            this.stockTable = new Hashtable<>();

            this.serverName = serverName;
            this.serverIP = "127.0.0.1";
            this.serverPort = Server.serverAddressTable.get(this.serverName);

            this.backupServerName = serverName + "Backup";
            this.backupServerIP = "127.0.0.1";
            this.backupServerPort = Server.serverAddressTable.get(backupServerName);

            this.timer = new Timer();
            this.period = 1000;
            this.timeIndexTable = new Hashtable<>();

            System.out.println(this.exchangeName + ": Opened Server Socket on Port " + this.exchangePort);

        } catch (IOException e) {
            System.out.println(this.exchangeName + ": Failed to open Server Socket on Port " + this.exchangePort);
        }
    }


    /**
     * Loads stock information for this Exchange by reading the price and quantity table.
     */
    private void loadStock() {

        System.out.println(this.exchangeName + ": Loading stocks...");

        // the price and quantity files to be read
        String csvPriceFile = "price_stocks.csv";
        String csvQtyFile = "qty_stocks.csv";

        final int CONTINENT_LINE = 1,
                COUNTRY_LINE = 2,
                EXCHANGE_LINE = 3,
                STOCK_LINE = 4;

        final int DATE_COL = 0,
                TIME_COL = 1;

        try {

            CSVReader csvPriceReader = new CSVReader(csvPriceFile);
            CSVReader csvQtyReader = new CSVReader(csvQtyFile);

            // columnIndices correspond to the indices of the columns in the csv file for stocks in this Exchange
            ArrayList<Integer> columnIndices = new ArrayList<>();

            // stockIndexTable keeps track to the column index and stock name
            Hashtable<Integer, Stock> stockIndexTable = new Hashtable<>();

            while (csvPriceReader.hasNextLine()) {

                csvQtyReader.hasNextLine(); // just for reading a line away in the Quantity Reader

                List<String> line = csvPriceReader.readLine();
                // records the column indices corresponding to this exchange when reading the EXCHANGE_LINE
                if (csvPriceReader.getCurrentLineIndex() == EXCHANGE_LINE) {
                    for (int i = 0; i < line.size(); i++) {
                        if (line.get(i).replaceAll("[^A-Za-z]+", "").equals(this.exchangeName)) {
                            columnIndices.add(i);
                        }
                    }
                    continue;
                }

                // records the stock names in this exchange and corresponding indices when reading the STOCK_LINE
                if (csvPriceReader.getCurrentLineIndex() == STOCK_LINE) {
                    for (Integer columnIndex : columnIndices) {
                        // stockName: keep only letters
                        String stockName = line.get(columnIndex).replaceAll("[^A-Za-z]+", "");
                        Stock stock = new Stock(stockName, this.exchangeName);
                        stockIndexTable.put(columnIndex, stock);
                    }
                    continue;
                }

                // records the prices at different time for each stock at this exchange when reading the rest of the table
                if (csvPriceReader.getCurrentLineIndex() > STOCK_LINE) {

                    String currentDateTime = line.get(DATE_COL) + " " + line.get(TIME_COL);
                    int currentSecond = csvPriceReader.getCurrentLineIndex() - STOCK_LINE;
                    this.timeIndexTable.put(currentSecond, currentDateTime); // i.e. the 1st datetime in the table corresponds to timeIndex 1

                    for (Integer columnIndex : columnIndices) {

                        Stock stock = stockIndexTable.get(columnIndex);
                        stock.setPrice(currentSecond, Double.parseDouble(line.get(columnIndex)));

                        try {
                            stock.setQuantity(currentSecond, Integer.parseInt(csvQtyReader.readLine().get(columnIndex)));
                        } catch (NumberFormatException e) {
                            stock.setQuantity(currentSecond, 0);
                        }

                        this.stockTable.put(stock.getName(), stock);
                    }
                }
            }

            System.out.println(this.exchangeName + ": Finished loading stocks.");
            csvPriceReader.close();
            csvQtyReader.close();

            // this.printStocks();  // for testing whether this Exchange has loaded all its stocks

        } catch (FileNotFoundException e) {
            System.out.println(this.exchangeName + ": Cannot find csv file.");
            System.exit(1);
        } catch (IOException e) {
            System.out.println(this.exchangeName + ": Failed to read next line.");
            System.exit(1);
        }
    }

    /**
     * Print stocks after loading, for debugging purposes
     */
    private void printStocks() {
        for (Stock stock : stockTable.values()) {
            System.out.println(stock.getName() + " " + stock.getExchangeName());
            stock.printPriceTable();
            stock.printQtyTable();
        }
        System.out.println("Available stock number is " + this.stockTable.size());
    }

    /**
     * Register this exchange with its continent server and backup server by sending its name, address and stock set.
     * Then waits for Server responding the startTime of the system.
     * Question: what if Server down during registration?
     */
    private void register() {

        try {

            // Opens a TCP socket to the Continent Server and sends the registration message.
            Socket socketToServer = new Socket(this.serverIP, this.serverPort);

            BufferedReader socketReader = new BufferedReader(new InputStreamReader(socketToServer.getInputStream()));
            PrintWriter socketWriter = new PrintWriter(socketToServer.getOutputStream(), true);

            System.out.println(this.exchangeName + ": Connected with Continent Server " + this.serverName);

            HashMap<String, Object> registrationMsg = new HashMap<>();
            registrationMsg.put("Type", "Registration");
            registrationMsg.put("ExchangeName", this.exchangeName);
            registrationMsg.put("Address", this.exchangePort);
            registrationMsg.put("StockSet", this.stockTable.keySet());

            // Sends the registration message in JSON
            socketWriter.println(new JSONObject(registrationMsg));
            // System.out.println(registrationMsg);

            System.out.println(this.exchangeName + ": Finished Registration with Continent Server " + this.serverName);


            // Opens a TCP socket to the Backup Server and sends the registration message.
            Socket socketToBackupServer = new Socket(this.backupServerIP, this.backupServerPort);
            PrintWriter socketWriterBackup = new PrintWriter(socketToBackupServer.getOutputStream(), true);
            System.out.println(this.exchangeName + ": Connected with Backup Server " + this.backupServerName);
            socketWriterBackup.println(new JSONObject(registrationMsg));
            System.out.println(this.exchangeName + ": Finished Registration with Backup Server " + this.backupServerName);


            // Reads Server response for the start time
            String response = socketReader.readLine();
            JSONObject responseObj = new JSONObject(response);
            this.startTime = responseObj.getLong("StartTime");
            // System.out.println("Start time: " + this.startTime);
            long currentTime = System.currentTimeMillis();
            // System.out.println("Current time: " + currentTime);


            // Determines when to start the timer task by checking the start time sent by server and the current time
            if (this.startTime <= currentTime) {

                System.out.println(this.exchangeName + ": The whole system already started.");

                int msPassed = Math.toIntExact(currentTime - this.startTime); // milliseconds passed since starting
                this.timeIndex = (int) Math.ceil(msPassed / 1000.0) + 1;
                this.delay = 1000 - msPassed % 1000;

            } else {

                this.timeIndex = 1;
                this.delay = Math.toIntExact(this.startTime - currentTime);
                System.out.println(this.exchangeName + ": The whole system will start in about " + String.format("%1$,.0f",(delay / 1000.0)) + " seconds. Please wait...");
            }

            // If there exists a log file for this Exchange when it started, it means that it probably went down previously.
            // Therefore, this exchange needs to read the log file and recover the amount of stocks it had available
            // before failure.
            File logFile = new File(this.exchangeName + ".log");
            if (logFile.exists()) {
                // read the log file and set quantity
                BufferedReader logReader = new BufferedReader(new FileReader(logFile));
                String line;
                String lastLine = "";
                while ((line = logReader.readLine()) != null) {
                    lastLine = line;
                }
                logReader.close();

                if (lastLine.length() > 0) {
                    lastLine = lastLine.substring(1, lastLine.length() - 1);
                    String[] keyValuePairs = lastLine.split(",");
                    for (String pair : keyValuePairs) {
                        String[] entry = pair.split("=");
                        Stock stock = this.stockTable.get(entry[0].trim());
                        int num = Integer.parseInt(entry[1].trim());
                        stock.addCurrentQty(num);
                    }
                }
            }

            // for writing the log file
            this.logWriter = new PrintWriter(exchangeName + ".log");

        } catch (IOException e) {
            System.out.println(this.exchangeName + ": Failed to register with Server " + this.serverName + " or " + this.backupServerName);
        }
    }



    /**
     * Update time (make sure that the Exchange is up to current time)
     */
    private void updateTime() {
        this.timer.scheduleAtFixedRate(new TimeUpdateTask(), this.delay, this.period);
    }

    /**
     * A TimerTask used for updating the time of this Exchange.
     * In our test, we have hardcoded period to be 1000, i.e. a second in the test corresponds to a timestamp in the
     * csv files.
     */
    private class TimeUpdateTask extends TimerTask {

        int round = 0; // for printing a message indicating that the system started

        @Override
        public void run() {

            for (Stock stock : stockTable.values()) {
                // add the quantity of each stock from the quantity csv file at each timestamp
                if (stock.getQty(timeIndex) > 0) {
                    stock.addCurrentQty(stock.getQty(timeIndex));
                    logWriter.println(stockTable);
                    logWriter.flush();
                }
            }

            if (round == 0) {
                System.out.println(exchangeName + ": Clock started. Client can connect and trade now.");
            }

            round++;
            timeIndex++;

            //System.out.println(timeIndexTable.get(timeIndex));
        }
    }

    /**
     * Print current quantity of each stock. (For testing time consistency)
     */
    private void printCurrentQty(int num) {

        switch (this.exchangeName) {
            case "Sao Paulo":
                System.out.println(stockTable.get("Petrobras").getCurrentQty());
                break;
            case "London":
                System.out.println(stockTable.get("BPPLC").getCurrentQty());
                break;
            case "Euronext Paris":
                System.out.println(stockTable.get("TOTAL").getCurrentQty());
                break;
            case "New York Stock Exchange":
                System.out.println(stockTable.get("ExxonMobil").getCurrentQty());
                break;
        }

        int count = 0;
        for (Stock stock : stockTable.values()) {
            System.out.println(stock.getName() + " " + stock.getCurrentQty());
            count++;
            if (count > num) {
                break;
            }
        }
    }

    /**
     * Listens for incoming connections in an infinite loop.
     */
    private void handleRequest() {
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                new Thread(new ConnectionHandler(socket)).start();

            } catch (IOException e) {
                System.out.println(exchangeName + ": Failed to accept a new request");
            }
        }
    }


    /**
     * For handling all incoming connections
     * (can be from clients or other Exchanges requesting to buy or sell a certain amount of a stock or a mutual fund)
     */
    private class ConnectionHandler implements Runnable {

        Socket socket;
        BufferedReader socketIn;
        PrintWriter socketOut;

        ConnectionHandler(Socket socket) {
            this.socket = socket;
            try {
                this.socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.socketOut = new PrintWriter(socket.getOutputStream(), true);
                // System.out.println("Received incoming connection, waiting for request...");
            } catch (IOException e) {
                System.out.println(exchangeName + ": Failed to open reader and writer on the newly accepted connection");
            }
        }

        /**
         * Handles the request in this run method
         */
        @Override
        public void run() {

            try {
                // Parses the message received from this connection:
                // Question: for handling requests from other Exchanges, need "bye" as well? (yes maybe easier)
                String message;
                while ((message = socketIn.readLine()) != null) {

                    // Parses the (src, action, stock, qty) elements of the message.
                    JSONObject obj = new JSONObject(message);
                    String src = obj.getString("src");
                    String action = obj.getString("action");
                    String stock = obj.getString("stock");
                    int qty = obj.getInt("qty");
                    // System.out.println("Received request : " + message + " from " + src);

                    // This exchange will respond with (result, action, qty, stock)
                    HashMap<String, Object> responseMsg = new HashMap<>();
                    responseMsg.put("action", action);
                    responseMsg.put("qty", qty);
                    responseMsg.put("stock", stock);
                    String result; // indicating whether this transaction succeeded or not

                    // If the request came from a Client, then the requested stock could be for either a stock listed
                    // in this Exchange or some stock listed in other Exchanges. Need to handle both kinds of transactions.
                    String price;
                    if (src.equals("client")) {

                        // For processing Mutual Funds
                        if (stock.startsWith("Mutual_Fund")) {
                            price = processMutualFund(action, stock, qty);
                        }

                        // For processing individual Stocks
                        else {
                            // For processing stocks listed in this Exchange
                            if (stockTable.containsKey(stock)) {
                                price = processInternalTransaction(action, qty, stock);
                            }

                            // For processing stocks listed in other Exchanges
                            else {
                                int port = askAddress(stock);
                                price = processExternalTransaction("127.0.0.1", port, action, qty, stock);
                            }
                        }

                        if (price.equals("Failed")) {
                            result = "Failed";
                        } else {
                            result = "Succeeded";
                        }

                        String actionString = "";
                        if (action.equals("B")) {
                            actionString = " requested to buy ";
                        } else if (action.equals("S")) {
                            actionString = " requested to sell ";
                        }

                        String priceString = price;
                        if (priceString.equals("Failed")) {
                            priceString = "";
                        }
                        System.out.println(exchangeName + " " + timeIndexTable.get(timeIndex) + ": Client " + obj.getInt("clientName") + actionString
                                + qty + " " + stock + " " + priceString + " " + result);

                        responseMsg.put("result", result);
                    }

                    // If a request came from other Exchanges, then it must be a request for stocks listed in this
                    // Exchange.
                    else {

                        price = processInternalTransaction(action, qty, stock);
                        responseMsg.put("result", price);
                    }

                    socketOut.println(new JSONObject(responseMsg));
                    // System.out.println("Sent Response: " + responseMsg);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Processes buy or sell orders for stocks listed in this Exchange
         * @param action    "B" or "S" (representing Buy or Sell)
         * @param qty       quantity to be bought or sold
         * @param stockName the name of the stock requested
         * @return a String representing the current price of the requested stock.
         *         Will return "Failed" if the request cannot be processed (e.g. not enough quantity left to be sold)
         */
        private String processInternalTransaction(String action, int qty, String stockName) {

            Stock stock = stockTable.get(stockName);
            String timeStamp = timeIndexTable.get(timeIndex);

            boolean result;

            System.out.println(exchangeName + " " + timeStamp + ": " + stockName + " Qty (before): " + stock.getCurrentQty());

            if (action.equals("B")) {
                result = stock.deCurrentQty(qty);
            } else {
                result = true;
                stock.addCurrentQty(qty);
            }

            logWriter.println(stockTable);
            logWriter.flush();

            String price;
            if (result) {
                price = stock.currentPriceToString(timeIndex);
            } else {
                price = "Failed";
            }
            System.out.println(exchangeName + " " + timeStamp + ": " + stockName + " Price: " + price);
            System.out.println(exchangeName + " " + timeStamp + ": " + stockName + " (after): " + stock.getCurrentQty());
            return price;
        }

        /**
         * Processes buy or sell orders from client for stocks not listed in this Exchange
         * @param host       the host name of the Exchange where the requested stock is listed
         * @param port       the port number of the Exchange where the requested stock is listed
         * @param action     "B" or "S" (representing Buy or Sell)
         * @param qty        quantity to be bought or sold
         * @param stockName  the name of the stock requested
         * @return a String representing the current price of the requested stock.
         *         Will return "Failed" if the request cannot be processed (e.g. not enough quantity left to be sold)
         */
        private String processExternalTransaction(String host, int port, String action, int qty, String stockName) {

            if (port == -1) {
                return "Failed";
            }

            try {

                // Initiates connection with the Exchange where the requested stock is listed
                Socket socketToExchange = new Socket(host, port);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socketToExchange.getInputStream()));
                PrintWriter socketWriter = new PrintWriter(socketToExchange.getOutputStream(), true);

                // Sends the buy or sell request to that Exchange
                HashMap<String, Object> request = new HashMap<>();
                request.put("src", "exchange");
                request.put("action", action);
                request.put("qty", qty);
                request.put("stock", stockName);
                socketWriter.println(new JSONObject(request));

                // Reads the response from that Exchange
                String response = socketReader.readLine();
                // System.out.println("Received response from other Exchange: " + response);
                JSONObject responseObj = new JSONObject(response);

                return responseObj.getString("result");

            } catch (IOException e) {
                System.out.println(exchangeName + ": Failed to send request to other Exchange. Need to notify Server of possible Exchange failure." );

                Socket notifySocket;
                try {
                    // Initiates a connection with the Continent Server to notify that this Exchange just failed to
                    // connect with another Exchange based on the address returned by the Server. That Exchange might
                    // be down or might have changed address. Need to notify Server to verify and update its cache.
                    notifySocket = new Socket(serverIP, serverPort);
                } catch (IOException e1) {
                    System.out.println(exchangeName + ": Failed to notify Server. Try notifying Backup Server...");
                    try {
                        notifySocket = new Socket(backupServerIP, backupServerPort);
                    } catch (IOException e2) {
                        System.out.println(exchangeName + ": Failed to notify with both Server and Backup Server, this transaction will fail.");
                        // in such case, will just return a "Failed" message
                        return "Failed";
                    }
                }

                // If the connection to either Server or Backup Server succeeded, sends the notification message
                try {
                    PrintWriter notifyWriter = new PrintWriter(notifySocket.getOutputStream(), true);
                    HashMap<String, Object> notifyMsg = new HashMap<>();
                    notifyMsg.put("Type", "Notify");
                    notifyMsg.put("src", "Exchange");
                    notifyMsg.put("ExchangeAddress", port);
                    notifyMsg.put("StockName", stockName);
                    notifyWriter.println(new JSONObject(notifyMsg));

                } catch (IOException e3) {
                    System.out.println(exchangeName + ": Failed to send notify message to Server or Backup Server. This transaction will fail.");
                }
                return "Failed";
            }
        }

        /**
         * Processes buy or sell request for a Mutual Fund using 2PC algorithm
         * @param action          "B" or "S" (representing Buy or Sell)
         * @param mutualFundName  the name of the requested mutual fund
         * @param qty             the requested quantity (must be divisible by 100)
         * @return a String representing the current price of the requested mutual fund.
         *         Will return "Failed" if the request cannot be processed (e.g. not enough quantity left to be sold)
         */
        private String processMutualFund(String action, String mutualFundName, int qty) {

            MutualFund mutualFund = new MutualFund(mutualFundName);

            // Stores the result of the buy or sell request for each stock in this mutual fund
            Hashtable<String, String> stockQuery = new Hashtable<>();
            // Stores the percentage of shares of each stock in this mutual fund
            Hashtable<String, Double> stockShares = mutualFund.getStockShareTable();

            // iterate through the stocks in this mutual fund, send the Buy or Sell action and update stockQuery table
            // (succeed or not)
            for (String stock : stockShares.keySet()) {

                // calculate the number of stocks to buy or sell
                int share = (int) (qty * stockShares.get(stock));

                String result;
                // determine whether to process an Internal or External transaction
                if (stockTable.contains(stock)) {
                    result = processInternalTransaction(action, share, stock);
                } else {
                    int port = askAddress(stock);
                    result = processExternalTransaction("127.0.0.1", port, action, share, stock);
                }

                // update the stockQuery table with the request result for this stock
                stockQuery.put(stock, result);

                // if the buy or sell request for a Stock in this mutual fund failed, just stop further requesting
                if (result.equals("Failed")) {
                    break;
                }
            }

            // check whether rollback is needed by checking whether there's any "false" in the stockQuery table
            boolean rollBack = false;

            for (String result : stockQuery.values()) {
                if (result.equals("Failed")) {
                    rollBack = true;
                    break;
                }
            }

            // if rollback needed, sell back those transactions that succeeded previously, and return that this mutual
            // fund transaction "failed"
            if (rollBack) {
                System.out.println("Roll back");
                for (String stock : stockQuery.keySet()) {
                    if (!stockQuery.get(stock).equals("Failed")) {

                        int share = (int) (qty * stockShares.get(stock));
                        if (stockTable.contains(stock)) {
                            processInternalTransaction("S", share, stock);
                        } else {
                            int port = askAddress(stock);
                            processExternalTransaction("127.0.0.1", port, "S", share, stock);
                        }
                    }
                }
                return "Failed";
            }

            // otherwise, return that this mutual fund transaction succeed by returning the string representation of the
            // price
            else {
                return stockQuery.keySet().toString();
            }
        }

        /**
         * Asks the Continent Server for address of another Exchange for processing External transactions
         * @param stock the name of the requested stock that is not listed in this Exchange
         * @return the address of the found Exchange. Will return -1 if failed to find an address
         */
        private int askAddress(String stock){

            // Initiates connection to Continent Server
            Socket socketToServer;
            try {
                socketToServer = new Socket(serverIP, serverPort);
            } catch (IOException e) {
                System.out.println(exchangeName + ": Failed to ask address from Server. Will try to connect with Backup Server.");
                try {
                    socketToServer = new Socket(backupServerIP, backupServerPort);
                } catch (IOException e1) {
                    System.out.println(exchangeName + ": Failed to ask address from Backup Server as well. This transaction will fail.");
                    return -1;
                }
            }

            try {
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socketToServer.getInputStream()));
                PrintWriter socketWriter = new PrintWriter(socketToServer.getOutputStream(), true);

                // Sends the naming request to the Server
                HashMap<String, String> queryMsg = new HashMap<>();
                queryMsg.put("src", "Exchange");
                queryMsg.put("Type", "Request");
                queryMsg.put("StockName", stock);

                // Sends the query message to the Continent Server
                socketWriter.println(new JSONObject(queryMsg));

                // Reads response from Server
                String response = socketReader.readLine();

                if (response == null) {
                    return -1;
                }

                JSONObject responseObj = new JSONObject(response);
                return responseObj.getInt("ExchangeAddress");

            } catch (IOException e) {
                System.out.println(exchangeName + ": Failed to ask address from both either Server or Backup Server. This transaction will fail.");
                return -1;
            }
        }
    }


    /**
     * Usage: java Exchange --exchangeName=<exchangeName> --serverName=<serverName>
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        String exchangeName = args[0].split("=")[1];
        String serverName = args[1].split("=")[1];

        Exchange exchange = new Exchange(exchangeName, serverName);

        exchange.loadStock();
        exchange.register();
        exchange.updateTime();
        exchange.handleRequest();
    }
}
