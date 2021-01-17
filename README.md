# f1-2018_telemetry
A Java app to consume UDP telemetry data from Codemaster's F1 2018 video game. Based on the documentation provided at https://forums.codemasters.com/discussion/136948/f1-2018-udp-specification.

## Changelog:
- Use gradle instead of maven for build.
- Create branch for groovy script wrapper.
- Create branch for a simple UDP java example generating random positive integers.

## execution setps:
1- Clean & build: `gradlew clean build uberJar`
2- Run groovy script: `groovy -cp build/libs/telemetry-udp.jar run.groovy`
