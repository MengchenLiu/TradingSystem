package project;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import org.json.*;

/**
 *
 */
public class Server {

    final static Hashtable<String, Integer> serverAddressTable = new Hashtable<>();
    static {
        serverAddressTable.put("Asia", 1111);
        serverAddressTable.put("AsiaBackup", 1112);
        serverAddressTable.put("Africa", 2222);
        serverAddressTable.put("AfricaBackup", 2223);
        serverAddressTable.put("Europe", 3333);
        serverAddressTable.put("EuropeBackup", 3334);
        serverAddressTable.put("America", 4444);
        serverAddressTable.put("AmericaBackup", 4445);
    }

    private String serverName;  // the name of this Server
    private int portNumber;
    private ServerSocket serverSocket;

    private long startTime;

    private int leftPort;    // the port number of the Server on the left
    private int rightPort;   // the port number of the Server on the right

    private Hashtable<String, Integer> exchangeAddressTable; // <ExchangeName, ExchangeAddress>
    private Hashtable<String, HashSet<String>> exchangeStockTable;   // <ExchangeName, StockNames>
    private HashSet<String> internalExchangeSet;  //

    public Server(String serverName, long startTime) {
        this.serverName = serverName;
        this.startTime = startTime;
        this.exchangeAddressTable = new Hashtable<>();
        this.exchangeStockTable = new Hashtable<>();
        this.internalExchangeSet = new HashSet<>();

        switch (serverName) {
            case "Asia":
            case "AsiaBackup":
                this.portNumber = serverAddressTable.get("Asia");
                this.leftPort = -1;
                this.rightPort = serverAddressTable.get("Africa");
                break;
            case "Africa":
            case "AfricaBackup":
                this.portNumber = serverAddressTable.get("Africa");
                this.leftPort = serverAddressTable.get("Asia");
                this.rightPort = serverAddressTable.get("Europe");
                break;
            case "Europe":
            case "EuropeBackup":
                this.portNumber = serverAddressTable.get("Europe");
                this.leftPort = serverAddressTable.get("Africa");
                this.rightPort = serverAddressTable.get("America");
                break;
            case "America":
            case "AmericaBackup":
                this.portNumber = serverAddressTable.get("America");
                this.leftPort = serverAddressTable.get("Europe");
                this.rightPort = -1;
                break;
        }

        try {
            this.serverSocket = new ServerSocket(serverAddressTable.get(this.serverName));
            System.out.println("Server " + this.serverName + " opened Server Socket on Port " + serverAddressTable.get(this.serverName));

            while (true) {
                Socket socket = serverSocket.accept();

                long beginTime = this.startTime;
                new Thread() {
                    @Override
                    public void run() {
                        try (
                                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter socketWriter = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
                        ) {

                            String input = socketReader.readLine();

                            //action,exchangeName
                            JSONObject obj = new JSONObject(input);
                            String type = obj.getString("Type");

                            // originalPort is the port # of the server that originates a address request to other server(s)
                            // it's used for a server to decide whether to ask left side or right side
                            int originalPort = -1;

                            // handle registration request from exchange
                            if (type.equals("Registration")) {

                                System.out.println(serverName + " : receive registration request");

                                int exchangePort = obj.getInt("Address");
                                String exchangeName = obj.getString("ExchangeName");

                                // get StockSet from obj and turn it back to HashSet
                                JSONArray arr = obj.getJSONArray("StockSet");
                                ArrayList<String> list = new ArrayList<String>();
                                if (arr != null) {
                                    int len = arr.length();
                                    for (int i=0;i<len;i++){
                                        list.add(arr.get(i).toString());
                                    }
                                }
                                HashSet<String> stockTable = new HashSet<String>(list);

                                registerExchange(exchangeName, exchangePort, stockTable);

                                // send the StartTime back to the exchange in registration
                                HashMap<String, Object> hm = new HashMap<>();
                                hm.put("StartTime", beginTime);
                                JSONObject stObj = new JSONObject(hm);
                                socketWriter.println(stObj.toString());
                            }
                            // handle request from exchange asking for address of a stock
                            else if (type.equals("Request")) {
                                String stockName = obj.getString("StockName");
                                String src = obj.getString("src");
                                if (src.equals("server")) {
                                    originalPort = obj.getInt("OriginalPort");
                                }

                                String exchange;
                                int exchangeAddress;
                                // if the stocked is in an exchange under or cached in the server
                                if ((exchange = getExchangeWithStock(stockName)) != null) {
                                    System.out.println(serverName + " : receive routing request, find exchange address at local");

                                    exchangeAddress = getInternalExchangeAddress(exchange);

                                    // generate a message to answer the exchange with an exchange address for the stock
                                    HashMap<String, Object> hm = new HashMap<>();
                                    hm.put("ExchangeAddress", exchangeAddress);
                                    hm.put("ExchangeName", exchange);
                                    JSONObject exAddrObj = new JSONObject(hm);
                                    socketWriter.println(exAddrObj.toString());

//                                    System.out.println(serverName + " : send " + exAddrObj.toString());
                                }
                                // if the stock is outside of this server or never requested by the server
                                else {
                                    System.out.println(serverName + " : receive routing request, ask next server for exchange address");
                                    getExternalExchangeAddress(originalPort, stockName, socket, socketWriter);
                                }
                            }
                            // handle notification from exchange about an exchange can't be connected
                            else if (type.equals("Notify")) {
                                System.out.println(serverName + " : reveice notification, an exchange's down");

                                String source = obj.getString("src");
                                int downExAddr = obj.getInt("ExchangeAddress");
                                String downExName = "";
                                String downStockName = "";
                                if (source.equals("server")) {
                                    originalPort = obj.getInt("OriginalPort");
                                    downExName = obj.getString("ExchangeName");

                                    removeDownExchangeCache(downExName,downExAddr);
                                }
                                else {
                                    downStockName = obj.getString("StockName");

                                    if (getExchangeWithStock(downStockName) != null) {
                                        downExName = getExchangeWithStock(downStockName);

                                        removeDownExchangeCache(downExName,downExAddr);
                                    }
                                }

                                notifyDownExchange(downExAddr, downExName, originalPort);
                            }
                        } catch (IOException e) {
                            System.exit(1);
                        }
                    }
                }.start();
            }

        } catch (IOException e) {
            System.exit(1);
        }

    }

    // add exchange address and exchange stock set to exchangeAddressTable and exchangeStockTable
    private void registerExchange(String exchangeName, int exchangePort, HashSet<String> stockTable) {
        this.exchangeAddressTable.put(exchangeName, exchangePort);
        this.exchangeStockTable.put(exchangeName, stockTable);
        this.internalExchangeSet.add(exchangeName);
    }

    // get address of an exchange that is under this server
    private int getInternalExchangeAddress(String exchangeName) {
        return this.exchangeAddressTable.get(exchangeName);
    }

    // get address of an exchange that is outside of this server
    private void getExternalExchangeAddress(int originalPort, String stockName, Socket socket, PrintWriter socketWriter) {

        String exchange;

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("Type", "Request");
        hm.put("src", "server");
        hm.put("StockName", stockName);

        // if source for address request is server
        if (originalPort != -1) {
            hm.put("OriginalPort", originalPort);
            JSONObject messageObj = new JSONObject(hm);
            String message = messageObj.toString();
            // if the original server's on the right to this server, ask the left server for address; else, ask right server
            if (originalPort > this.portNumber) {
                if (this.leftPort != -1) startNextSocket(originalPort, leftPort, message, stockName, socket);
                else {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("ExchangeAddress", -1);
                    JSONObject noAddrObj = new JSONObject(hashMap);
                    socketWriter.println(noAddrObj.toString());

                    System.out.println(serverName + " : reach left end, send " + noAddrObj.toString());
                }
            }
            else {
                if (this.rightPort != -1) startNextSocket(originalPort, rightPort, message, stockName, socket);
                else {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("ExchangeAddress", -1);
                    JSONObject noAddrObj = new JSONObject(hashMap);
                    socketWriter.println(noAddrObj.toString());

                    System.out.println(serverName + " : reach right end, send " + noAddrObj.toString());
                }
            }
        }
        // if source is exchange, then this server is the one of all servers that originated this address request
        else {
            hm.put("OriginalPort", this.portNumber);
            JSONObject messageObj = new JSONObject(hm);
            String message = messageObj.toString();
            // if this server has two neighboring servers, ask left side first,
            // if left side cannot find the stock, then ask right side, whether right side finds it or not, send back to the exchange
            if (this.leftPort != -1 && this.rightPort != -1) {
                HashMap<String, Object> hashMap = new HashMap<>();
                int leftResult = twoSideStartNextSocket(this.portNumber, leftPort, message, stockName, socket);
                if (leftResult != -1) {
                    hashMap.put("ExchangeAddress", leftResult);
                }
                else {
                    int rightResult = twoSideStartNextSocket(this.portNumber, rightPort, message, stockName, socket);
                    hashMap.put("ExchangeAddress", rightResult);
                }
                JSONObject exAddrObj = new JSONObject(hashMap);
                socketWriter.println(exAddrObj.toString());
                System.out.println(serverName + " : send " + exAddrObj.toString());

            }
            // if this server has only one neighboring server, ask it for address and send the result right back to the exchange (via startNextSocket() method)
            else if (this.leftPort == -1) {
                startNextSocket(this.portNumber, rightPort, message, stockName, socket);
            } else if (this.rightPort == -1) {
                startNextSocket(this.portNumber, leftPort, message, stockName, socket);
            }
        }

    }

    private void removeDownExchangeCache(String downExName, int downExAddr) {

        // if the exchange down is under this server, remove the exchange if it's not back (address not changed)
        if (internalExchangeSet.contains(downExName)) {
            if (exchangeAddressTable.get(downExName) == downExAddr) {
                exchangeAddressTable.remove(downExName);
                exchangeStockTable.remove(downExName);
                internalExchangeSet.remove(downExName);
            }
        }
        else {
            // if this server has the exchange's address in the routing table, delete it
            if (exchangeAddressTable.containsKey(downExName)) {
                exchangeAddressTable.remove(downExName);
                exchangeStockTable.remove(downExName);
            }
        }
    }

    // notify other servers about the down exchange
    private void notifyDownExchange(int exchangeAddr, String exchangeName, int originalPort) {
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("Type", "Notify");
        hm.put("src", "server");
        hm.put("ExchangeName", exchangeName);
        hm.put("ExchangeAddress", exchangeAddr);

        // if source for notification is server
        if (originalPort != -1) {
            hm.put("OriginalPort", originalPort);
            JSONObject messageObj = new JSONObject(hm);
            String message = messageObj.toString();

            // if the original server's on the right to this server, notify the left server
            if (originalPort > this.portNumber) {
                if (this.leftPort != -1) {
                    notifyNextServer(leftPort, message);
                }
            }
            else {
                if (this.rightPort != -1) {
                    notifyNextServer(rightPort, message);
                }
            }
        }
        // if source is exchange, then this server is the one of all servers that originated this notification
        else {
            hm.put("OriginalPort", this.portNumber);
            JSONObject messageObj = new JSONObject(hm);
            String message = messageObj.toString();

            // if this server has two neighboring servers, notify both
            if (this.leftPort != -1 && this.rightPort != -1) {
                notifyNextServer(leftPort, message);
                notifyNextServer(rightPort, message);
            }
            // if this server has only one neighboring server, notify that neighbor
            else if (this.leftPort == -1) {
                notifyNextServer(rightPort, message);
            } else if (this.rightPort == -1) {
                notifyNextServer(leftPort, message);
            }
        }
    }

    // create connection to the next server for notification of the down exchange
    private void notifyNextServer(int nextPort, String message) {

        String serverIP = "127.0.0.1";

        Socket notifySocket;
        PrintWriter out;
        try {
            notifySocket = new Socket(serverIP, nextPort);
            out = new PrintWriter(new BufferedOutputStream(notifySocket.getOutputStream()), true);
            out.println(message);

        } catch (IOException e) {
            System.out.println("On notify: can't connect to server: " + nextPort);
            nextPort = nextPort + 1;

            try {
                notifySocket = new Socket(serverIP, nextPort);
                out = new PrintWriter(new BufferedOutputStream(notifySocket.getOutputStream()), true);
                out.println(message);

                updateNextPort(nextPort);
            } catch (IOException e1) {
                System.out.println("On notify: can't connect to server's backup");
            }

        }
    }

    // check if the stock is in one of the exchanges under this server
    // return exchange name if found it, otherwise return null
    private String getExchangeWithStock(String stockName) {
        for (String exchangeName : exchangeStockTable.keySet()) {
            if (exchangeStockTable.get(exchangeName).contains(stockName)) {
                return exchangeName;
            }
        }
        return null;
    }

    // send address request for the stock to a next server
    // return the exchange address, or -1 if exchange's not found
    private int twoSideStartNextSocket(int originalPort, int nextPort, String message, String stockName, Socket socket) {

        String serverIP = "127.0.0.1";

        Socket nextSocket;
        BufferedReader in;
        PrintWriter out;

        // create connection to the next server, if fails, connect to the backup of next server
        try {
            nextSocket = new Socket(serverIP, nextPort);
            in = new BufferedReader(new InputStreamReader(nextSocket.getInputStream()));
            out = new PrintWriter(new BufferedOutputStream(nextSocket.getOutputStream()), true);

            out.println(message);
        } catch (IOException e) {
            System.out.println("Can't connect to server: "+nextPort);
            nextPort = nextPort + 1;
            try {
                nextSocket = new Socket(serverIP, nextPort);
                in = new BufferedReader(new InputStreamReader(nextSocket.getInputStream()));
                out = new PrintWriter(new BufferedOutputStream(nextSocket.getOutputStream()), true);

                out.println(message);

                updateNextPort(nextPort);
            } catch (IOException e1) {
                System.out.println("Can't connect next server's backup: "+(nextPort+1));
                return -1;
            }
        }

        try {
            String input = "";
            String exchangeName = "";
            int exchangeAddr = -1;
            if ((input = in.readLine()) != null) {

                nextSocket.close();

                JSONObject obj = new JSONObject(input);
                exchangeAddr = obj.getInt("ExchangeAddress");

                // cache the exchange address and stock-exchange relation
                if (exchangeAddr != -1) {

                    exchangeName = obj.getString("ExchangeName");
                    this.exchangeAddressTable.put(exchangeName, exchangeAddr);
                    if (exchangeStockTable.containsKey(exchangeName)) {
                        exchangeStockTable.get(exchangeName).add(stockName);
                    } else {

                        exchangeStockTable.put(exchangeName, new HashSet<>());
                        exchangeStockTable.get(exchangeName).add(stockName);
                    }
                }
            }
            // if readLine() returns null, the connection to next server has broken
            else {
                exchangeAddr = -1;
                updateNextPort(nextPort);
            }
            return exchangeAddr;

        } catch (IOException e) {
            return -1;
        }

    }

    // send address request for the stock to a next server, and send the answer right back to the upper requester
    private void startNextSocket(int originalPort, int nextPort, String message, String stockName, Socket socket) {

        String serverIP = "127.0.0.1";

        Socket nextSocket;
        BufferedReader in;
        PrintWriter out;
        // create connection to the next server
        try {
            nextSocket = new Socket(serverIP, nextPort);

            in = new BufferedReader(new InputStreamReader(nextSocket.getInputStream()));
            out = new PrintWriter(new BufferedOutputStream(nextSocket.getOutputStream()), true);

            out.println(message);
        }
        // if fails, connect to the backup of next server
        catch (IOException e) {
            System.out.println("Can't connect to server: "+nextPort);
            nextPort = nextPort + 1;
            try {
                nextSocket = new Socket(serverIP, nextPort);
                in = new BufferedReader(new InputStreamReader(nextSocket.getInputStream()));
                out = new PrintWriter(new BufferedOutputStream(nextSocket.getOutputStream()), true);

                out.println(message);

                updateNextPort(nextPort);
            }
            // if fails to connect to the back up, inform the upper server/exchange
            catch (IOException e1) {
                System.out.println("Can't connect next server's backup: "+(nextPort+1));

                try {
                    PrintWriter ackOut = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("ExchangeAddress", -1);
                    JSONObject exAddrObj = new JSONObject(hashMap);
//                    System.out.println(serverName + " : " + "back: "+exAddrObj.toString());
                    ackOut.println(exAddrObj.toString());

                } catch (IOException e2) {
                    System.out.println("Connection to the last server/exchange has broken. ");
                }
                return;
            }
        }


        try {
            String input = "";

            if ((input = in.readLine()) != null) {
                PrintWriter ackOut = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
                ackOut.println(input);
                System.out.println(serverName + " : receive " + input);

                nextSocket.close();

                if (originalPort == this.portNumber) {

                    JSONObject obj = new JSONObject(input);
                    int exchangeAddr = obj.getInt("ExchangeAddress");

                    // cache the exchange address and stock-exchange relation
                    if (exchangeAddr != -1) {
                        String exchangeName = obj.getString("ExchangeName");
                        this.exchangeAddressTable.put(exchangeName, exchangeAddr);
                        if (exchangeStockTable.containsKey(exchangeName)) {
                            exchangeStockTable.get(exchangeName).add(stockName);
                        }
                        else {

                            exchangeStockTable.put(exchangeName, new HashSet<>());
                            exchangeStockTable.get(exchangeName).add(stockName);
                        }
                    }
                }
            }
            // if readLine() returns null, the connection to next server has broken
            else {
                updateNextPort(nextPort);
                PrintWriter ackOut = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("ExchangeAddress", -1);
                JSONObject exAddrObj = new JSONObject(hashMap);
                ackOut.println(exAddrObj.toString());
            }

        } catch (IOException e) {
            return;
        }
    }

    // if a next server is down, update the leftPort or rightPort of this server to its backup
    private void updateNextPort(int nextPort) {

        if (nextPort > this.portNumber) {
            if (nextPort % 1111 == 0) {
                rightPort += 1;
            }
            else {
                rightPort -= 1;
            }

        }
        else {
            if (nextPort % 1111 == 0) {
                leftPort += 1;
            }
            else {
                leftPort -= 1;
            }
        }
    }

    /**
     * Usage: java Server --serverName=<Server Name>
     * @param args
     */
    public static void main(String[] args) {
        String serverName = args[0].split("=")[1];
        long startTime = Long.parseLong(args[1].split("=")[1]) * 1000;

        File f = new File(serverName + ".log");
        if (f.exists()) {
            //read time from log
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String value = br.readLine();
                br.close();
                startTime = Long.parseLong(value);
            } catch (IOException e) {
                System.out.println("Can't find log file.");
            }
        }
        else {
            //write start time to log
            try {
                PrintWriter log = new PrintWriter(serverName + ".log");
                log.println(startTime);
                log.flush();
                log.close();
            } catch (FileNotFoundException e) {
                System.out.println("Can't find log file. ");
            }
        }

        startTime += 20000;

        Server server = new Server(serverName, startTime);

    }
}