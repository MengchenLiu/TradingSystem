package project;

import java.util.ArrayList;

/**
 *
 */
public class ClientTest {

    final static int RANDOM = 1;
    final static int FIXED = 2;

    private int scenarioIndex;
    private int numClient;
    private int clientType;  // RANDOM or FIXED

    private int round;

    private ArrayList<String> actions; // a list contains the actions for each client if FIXED client


    private ClientTest(int scenarioIndex) {

        this.scenarioIndex = scenarioIndex;
        this.actions = new ArrayList<>();

        switch (scenarioIndex) {
            case 1:
                this.numClient = 2;
                this.clientType = FIXED;

                actions.add("exchange=Shenzhen, action=B, stock=JINGGONGSCIENCE, qty=100, delay=0, period=2000");
                actions.add("exchange=Shenzhen, action=S, stock=JINGGONGSCIENCE, qty=100, delay=0, period=2000");
                break;
            case 2:
                this.numClient = 1;
                this.clientType = FIXED;

                // (action=R means random action)
                actions.add("exchange=London, action=R, stock=NHU, qty=100, delay=0, period=2000");
                break;
            case 3:
                this.numClient = 2;
                this.clientType = FIXED;

                actions.add("exchange=London, action=S, stock=MIRACLELOGISTICS, qty=100, delay=0, period=2000");
                actions.add("exchange=Shenzhen, action=S, stock=MIRACLELOGISTICS, qty=100, delay=1000, period=2000");
                break;
            case 4:
                this.numClient = 2;
                this.clientType = FIXED;

                actions.add("exchange=London, action=B, stock=EASTCOMPEACE, qty=400, delay=0, period=2000");
                actions.add("exchange=Shenzhen, action=B, stock=EASTCOMPEACE, qty=400, delay=0, period=2000");
                break;
            case 5:
                this.numClient = 10;
                this.clientType = RANDOM;

                this.round = Integer.MAX_VALUE;

                break;
            case 6:
                this.numClient = 1000;
                this.clientType = RANDOM;

                this.round = 2;

                break;
            case 7:
                this.numClient = 1;
                this.clientType = FIXED;

                // reused Scenario 2
                actions.add("exchange=London, action=R, stock=JSQH, qty=100, delay=0, period=2000");
                break;
            case 8:
                this.numClient = 1;
                this.clientType = FIXED;

                actions.add("exchange=Shenzhen, action=B, stock=Mutual_Fund_Energy, qty=1000, delay=0, period=2000");
                break;
            default:
                System.out.println("Invalid Scenario number.");
                System.exit(1);
        }

        // spawns "numClient" number of threads for handling request for each Client
        for (int i = 1; i <= this.numClient; i++) {
            new Thread(new ClientRunnable(i)).start();
        }
    }

    private class ClientRunnable implements Runnable {

        private int clientID;
        private Client client;
        private String transaction;

        ClientRunnable(int clientID) {
            this.clientID = clientID;
        }

        @Override
        public void run() {
            if (clientType == RANDOM) {
                this.client = new Client(this.clientID, round);
            }

            else if (clientType == FIXED) {

                this.transaction = actions.get(clientID - 1);
                String exchange = transaction.split(", ")[0].split("=")[1].trim();
                String action = transaction.split(", ")[1].split("=")[1].trim();
                String stock = transaction.split(", ")[2].split("=")[1].trim();
                int qty = Integer.parseInt(transaction.split(", ")[3].split("=")[1].trim());
                int delay = Integer.parseInt(transaction.split(", ")[4].split("=")[1].trim());
                int period = Integer.parseInt(transaction.split(", ")[5].split("=")[1].trim());

                this.client = new Client(clientID, exchange, action, stock, qty, delay, period);
            }
        }
    }

    public static void main(String[] args) {

        int scenario = Integer.parseInt(args[0].split("=")[1]);
        new ClientTest(scenario);
    }
}
