# Example of running:
#./run-java.sh [sleepInMillis] [stopCounter] [serverOnly] [ipAddress] [port]
#./run-java.sh 100 2 false 127.0.0.1 20777

sleepInMillis=800
sleepInMillis_value=${1:-$sleepInMillis}

stopCounter=1000
stopCounter_value=${2:-$stopCounter}

serverOnly=false
serverOnly_value=${3:-$serverOnly}

ipAddress="127.0.0.1"
ipAddress_value=${4:-$ipAddress}

port=20777
port_value=${5:-$port}

java -jar build/libs/telemetry-udp.jar ${sleepInMillis_value} ${stopCounter_value} ${serverOnly_value} ${ipAddress_value} ${port_value}
