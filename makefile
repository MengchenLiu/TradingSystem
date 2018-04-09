JCC = javac

JFLAGS = -g
CLASSPATH = -classpath src:lib/json-20160810.jar
TIME =$(shell date +%s)

default:
	@echo "make build: compile src file."
	@echo "make clean: clear classes generated."
	@echo "make rebuild: clean log files and recompile src files"
	@echo "make clean_log: clean log files"
	@echo "make kill: kill all java processes"
	@echo "make run_server: run all servers and backup servers at the same time"
	@echo "make run_shenzhen: run Shenzhen exchange for testing"
	@echo "make run_london: run London exchange for testing"
	@echo "make run_exchange: run all exchanges at the same time"
	@echo "make run_test: run a given test scenario"
	@echo "\t Usage: make run_test scenario=<scenario_id>"
	@echo "\t \t scenario_id can be 1 ... 7"
	@echo "\t \t e.g. make run_test scenario=1"


build:
	$(JCC) $(CLASSPATH) src/project/*.java

clean:
	$(RM) src/project/*.class

clean_log:
	$(RM) *.log

rebuild: clean build clean_log

kill:
	ps -ef | grep 'java '| grep -v grep | awk '{print $$2}' | xargs kill -9

run_server:
	java $(CLASSPATH) project.Server --serverName=Asia --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=Africa --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=Europe --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=America --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=AsiaBackup --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=AfricaBackup --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=EuropeBackup --startTime=$(TIME) & \
	java $(CLASSPATH) project.Server --serverName=AmericaBackup --startTime=$(TIME)

run_test:
	java $(CLASSPATH) project.ClientTest --scenario=$(scenario)

run_shenzhen:
	java $(CLASSPATH) project.Exchange --exchangeName=Shenzhen --serverName=Asia

run_london:
	java $(CLASSPATH) project.Exchange --exchangeName=London --serverName=Europe

run_exchange:
	java $(CLASSPATH) project.Exchange --exchangeName=Bombay --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Brussels --serverName=Europe & \
	java $(CLASSPATH) project.Exchange --exchangeName=EuronextParis --serverName=Europe & \
	java $(CLASSPATH) project.Exchange --exchangeName=Frankfurt --serverName=Europe & \
	java $(CLASSPATH) project.Exchange --exchangeName=HongKong --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Johannesburg --serverName=Africa & \
	java $(CLASSPATH) project.Exchange --exchangeName=Lisbon --serverName=Europe & \
	java $(CLASSPATH) project.Exchange --exchangeName=London --serverName=Europe & \
	java $(CLASSPATH) project.Exchange --exchangeName=NewYorkStockExchange --serverName=America & \
	java $(CLASSPATH) project.Exchange --exchangeName=SaoPaulo --serverName=America & \
	java $(CLASSPATH) project.Exchange --exchangeName=Seoul --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Shanghai --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Shenzhen --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Sydney --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Tokyo --serverName=Asia & \
	java $(CLASSPATH) project.Exchange --exchangeName=Toronto --serverName=America & \
	java $(CLASSPATH) project.Exchange --exchangeName=Zurich --serverName=Europe