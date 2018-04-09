# Trading System
**MPCS 52040 Final Project**

**Video Demonstration: https://drive.google.com/open?id=0B1c45m8bFtfmTE9jWXdTWlRaMFU**


### Introduction

The project is a real-time distributed trading system, supporting 17 stock exchanges connected with 4 continent servers. For
details about the supported stock exchanges and stocks, please see the price_stocks.csv and qty_stocks.csv files.

### Scenario Testing

#### Scenario 1:
*Client 1 and Client 2 individually buy and sell the same amount of quantity of a stock for several times at the same
time. The quantity of this stock should not be changed after these transactions.*

Use command `make build` to compile. <br>
Use command `make run_server` to start the 4 continent servers and their backup servers. <br>
Use command `make run_shenzhen` to start Shenzhen stock exchange, in a separate terminal. <br>
Use command `make run_test scenario=1` to start testing this scenario, in a separate terminal.

In this test, two clients will be connected with Shenzhen stock exchange. Client 1 will keep buying 100 shares of JINGGONGSCIENCE,
Client 2 will keep selling 100 shares of JINGGONGSCIENCE at the same time. You should see in the terminal of Shenzhen
exchange that the quantity of this stock will stay the same.

To stop this test, `Ctrl+c` in the test terminal. 


#### Scenario 2:

*Client 1 repeatedly buy and sell a reasonable quantity of a stock, which is located in a server. At the same time,
kill the server and see how the whole system behaves.*

Use command `make run_london` to start London stock exchange, in a separate terminal. <br>
(Note that you should now have a terminal running all Servers, a terminal running Shenzhen stock exchange, and another terminal
running London stock exchange.)
Use command `make run_test scenario=2` to start testing this scenario, in a separate terminal.

In this test, a client will be connected with London stock exchange, repeatedly selling and buying 100 shares of NHU, which
is listed under Shenzhen stock exchange. This test also demonstrates how our system is scalable, i.e. adding new stock exchange
to the system. It also shows how the continent server helps resolving the address of Shenzhen exchange for the client who
is connected with London stock exchange.

To test how the system respond when an exchange went down, please `Ctrl+c` to kill Shenzhen exchange. You should notice that
the Client's request would not be fulfilled anymore. please use commnd `make run_shenzhen` to start Shenzhen exchange again.
You should notice that the amount of NHU stayed the same before Shenzhen exchange went down, and the Client's request will be
processed accordingly.

To stop this test, `Ctrl+c` in the test terminal.

#### Scenario 3:
*Client A and Client B both try to sell the same stock. The transaction server should maintain a record of all the
transactions properly.*

Use command `make run_test scenario=3` to start testing this scenario.

In this test, Client 1 will be connected to London stock exchange, Client 2 will be connected to Shenzhen stock exchange.
They will repeatedly sell 100 shares of MIRACLELOGISTICS which is listed under Shenzhen stock exchange. You should notice
that the amount of MIRACLELOGISTICS will be changed to reflect the transaction record properly.

To stop this test, `Ctrl+c` in the test terminal.

#### Scenario 4:
*Client A and Client B both try to buy the same stock, but there is not enough for both. Either client A or Client B
should receive a well formatted Error message.*

Use command `make run_test scenario=4` to start testing this scenario.

In this test, Client 1 will be connected to London stock exchange, Client 2 will be connected to Shenzhen stock exchange.
They will repeatedly buy 400 shares of EASTCOMPEACE at the same time. Since the initial amount of EASTCOMPEACE is 3610, 
eventually, there will not be enough shares for both clients requesting to buy 400 shares. You will notice that one of
the requests will fail when there are not enough for both.

To stop this test, `Ctrl+c` in the test terminal.

#### Scenario 5:
*Spin up 10 clients and have half buy stock and half sell stock for 20 minutes. Make sure there are no memory leaks.
Which software will you be using for memory leaks?*

Since our system keeps track of time, and will only run until the end of the timestamp provided in the price_stocks.csv
file. To ensure that the system can run for 20 minutes, use command `make kill` to kill all processes, and use command
`make clean_log` to clean previous log files so that we can restart the whole system.

Use command `make run_server` to start all continent servers and their backup servers.<br>
Use command `make run_exchange` to start all 17 stock exchanges, in another terminal. <br>
Use command `make run_test scenario=5` to start testing this scenario.

In this test, 10 clients will be connected with 10 random stock exchanges, repeatedly requesting to buy or sell a random
share of a random stock. By keeping the system running for 20 minutes, you should not notice any memory leak.

To stop this test, `Ctrl+c` in the test terminal. Use command `make kill` to stop all processes.


#### Scenario 6:

*Spin Up a large number of clients (i.e. 1000) and sell some stock <br>
Spin Up a large number of clients (i.e. 1000) and buy some stock*

Use command `make run_server` to start all continent servers and their backup servers.<br>
Use command `make run_exchange` to start all 17 stock exchanges, in another terminal. <br>
Use command `make run_test scenario=6` to start testing this scenario.

In this test, we will start 1000 clients, each will be randomly connected with a stock exchange. They will randomly sell
or buy a random share of a random stock.

To stop this test, `Ctrl+c` in the test terminal. Use command `make kill` to stop all processes.

#### Scenario 7:

*Show a recovery feature of your system.*

Use command `make run_server` to start all continent servers and their backup servers.<br>
Use command `make run_shenzhen` to start Shenzhen stock exchange, in a separate terminal. <br>
Use command `make run_london` to start London stock exchange, in a separate terminal. <br>
Use command `make run_test scenario=7` to start testing this scenario.

In this test, we will test how the system respond after the whole system crashes. Use command `make kill` to kill all processes
to simulate a whole system crash. Then repeat commands `make run_server`, `make run_shenzhen`, `make run_london`, `make run_test scenario=7`
to restart this system again. You should notice that the amount of the stocks remained unchanged before the crash. The system
also maintained synchronized clocks.

To stop this test, `Ctrl+c` in the test terminal. Use command `make kill` to stop all processes.

#### Scenario 8:

*Use 2 phase commit algorithm for mutual fund transaction.*

Use command `make run_server` to start all continent servers and their backup servers.<br>
Use command `make run_exchange` to start all 16 stock exchanges.
Use command `make run_test scenario=8` to start testing this scenario.

In this test, a client will repeatedly request to buy Mutual_Fund_Energy, which contains 4 stocks listed under 4 different
stock exchanges. When there are not enough shares for a stock in this mutual fund, the transaction will fail.

To stop this test, `Ctrl+c` in the test terminal. Use command `make kill` to stop all processes.
