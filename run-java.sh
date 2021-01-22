sleepInMillis=800
sleepInMillis_value=${1:-$sleepInMillis}

stopCounter=1000
stopCounter_value=${2:-$stopCounter}

serverOnly=no
serverOnly_value=${3:-$serverOnly}

java -jar build/libs/telemetry-udp.jar ${sleepInMillis_value} ${stopCounter_value} ${serverOnly_value}
