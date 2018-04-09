package project;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.json.*;

public class Client {

    private int clientID;
    private int clientType;

    private String exchangeName;
    private String exchangeIP;
    private int exchangePort;
    private ExchangeUtils exchangeUtils;

    private Socket socket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;

    private String transaction;

    private Timer timer;
    private int round;

    private ArrayList<String> stocks;
    private ArrayList<String> exchanges;


    public Client(int clientID, String exchange, String action, String stock, int qty, int delay, int period) {
        this.clientID = clientID;
        this.clientType = ClientTest.FIXED;
        this.exchangeName = exchange;
        this.exchangeUtils = new ExchangeUtils(this.exchangeName);
        this.exchangeIP = "localhost";
        this.exchangePort = this.exchangeUtils.getPort();

        this.transaction = action + " " + stock + " " + qty;

        this.connectToExchange();

        this.timer = new Timer();
        this.round = Integer.MAX_VALUE;
        this.timer.scheduleAtFixedRate(new TransactionTask(), delay, period);
    }

    public Client(int clientID, int round) {
        this.clientID = clientID;
        this.clientType = ClientTest.RANDOM;
        this.readTable();

        this.exchangeName = generateRandomExchange();

        this.exchangeUtils = new ExchangeUtils(this.exchangeName);
        this.exchangeIP = "localhost";
        this.exchangePort = this.exchangeUtils.getPort();

        this.connectToExchange();

        this.timer = new Timer();
        this.round = round;
        int period = ThreadLocalRandom.current().nextInt(2000, 5000);
        this.timer.scheduleAtFixedRate(new TransactionTask(), 0, period);
    }

    private void connectToExchange() {
        try {
            socket = new Socket(exchangeIP, exchangePort);
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socketWriter = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e) {
            System.out.println("Client " + clientID + ": Failed to connect with Exchange " + exchangeName);
        }
    }

    private String generateRandomExchange() {

        // e.g. "Shenzhen"
        int randomNum = ThreadLocalRandom.current().nextInt(0, exchanges.size());
        return exchanges.get(randomNum);
    }

    private String generateRandomTransaction() {

        // e.g. "B EASTCOMPEACE 100"

        int randomNum = ThreadLocalRandom.current().nextInt(0, stocks.size());
        String stock = stocks.get(randomNum);

        randomNum = ThreadLocalRandom.current().nextInt(0, 2);
        String action;
        if (randomNum > 0) {
            action = "B";
        } else {
            action = "S";
        }

        int qty = ThreadLocalRandom.current().nextInt(1, 401);
        return action + " " + stock + " " + qty;
    }


    private class TransactionTask extends TimerTask {

        String currentTransaction;

        int count = 0;

        @Override
        public void run() {

            if (clientType == ClientTest.FIXED) {
                currentTransaction = transaction;

                if (transaction.startsWith("R")) {
                    int randomNum = ThreadLocalRandom.current().nextInt(0, 2);
                    if (randomNum > 0) {
                        currentTransaction = transaction.replaceFirst("R", "B");
                    } else {
                        currentTransaction = transaction.replaceFirst("R", "S");
                    }
                }
            } else if (clientType == ClientTest.RANDOM){
                currentTransaction = generateRandomTransaction();
            }

            count++;
            if (count < round) {
                String msg = format(currentTransaction);
                // Sends the request to the Exchange
                socketWriter.println(msg);

                // Tries to receive the response from the Exchange
                try {
                    String response = socketReader.readLine();
                    if (response == null) {
                        System.out.println("Client " + clientID + ": Exchange " + exchangeName + " did not respond. Transaction failed. Retrying connecting with Exchange...");
                        connectToExchange();
                    } else {
                        JSONObject responseObj = new JSONObject(response);
                        System.out.println("Client " + clientID + " requested " + currentTransaction + " " + responseObj.getString("result"));
                    }
                } catch (IOException e) {
                    System.out.println("Client " + clientID + ": Failed to send request to Exchange. Transaction failed. Retrying connecting with Exchange...");
                    connectToExchange();
                }
            } else {
                cancel();
                timer.cancel();
                timer.purge();
            }
        }
    }

    private String format(String line) {
        String[] command = line.split(" ");

        HashMap<String, Object> hm = new HashMap<>();

        hm.put("clientName", this.clientID);
        hm.put("src", "client");
        hm.put("action", command[0]);
        hm.put("qty", Integer.parseInt(command[2]));
        hm.put("stock", command[1]);

        JSONObject obj = new JSONObject(hm);
        return obj.toString();
    }

    /**
     * Reads the "price_stocks.csv" file for helping with getting all stocks and exchanges (for generating random transaction)
     * @return
     */
    private void readTable() {

        this.stocks = new ArrayList<>();
        this.exchanges = new ArrayList<>();

        try {
            CSVReader csvReader = new CSVReader("price_stocks.csv");

            while (csvReader.hasNextLine()) {
                List<String> line = csvReader.readLine();

                if (csvReader.getCurrentLineIndex() == 3) {
                    for (int i = 3; i < line.size(); i++) {
                        String s = line.get(i).replaceAll("[^A-Za-z]+", "");
                        if (!exchanges.contains(s)) {
                            exchanges.add(s);
                        }
                    }

                } else if (csvReader.getCurrentLineIndex() == 4) {
                    for (int i = 3; i < line.size(); i++) {
                        String s = line.get(i).replaceAll("[^A-Za-z]+", "");
                        if (!stocks.contains(s)) {
                            stocks.add(s);
                        }
                    }
                    break;
                }
            }

            csvReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("Cannot find csv file.");
        } catch (IOException e) {
            System.out.println("Failed to read next line.");
        }
    }
}
