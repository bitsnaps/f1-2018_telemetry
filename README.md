# f1-2018_telemetry
A Java app to consume UDP telemetry data from Codemaster's F1 2018 video game. Based on the documentation provided at https://forums.codemasters.com/discussion/136948/f1-2018-udp-specification.

## Changelog:
- Use gradle instead of maven for build.
- Create branch for groovy script wrapper.
- Create branch for a simple UDP java example generating random positive integers.

## Execution steps:
1- Clean & build: `./build-jar.sh`
2- Run groovy script: `groovy -cp build/libs/telemetry-udp.jar run.groovy`
3- Run as jar file: `./run-java.sh`, or with optional arguments:
```
./run-java.sh [sleepInMillis] [stopCounter] [serverOnly] [ipAddress] [port]
# Example:
./run-java.sh 100 2 false 127.0.0.1 20777
```
