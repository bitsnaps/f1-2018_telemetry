if ! command -v gradle &> /dev/null
then
    ./gradlew clean build uberJar
    exit
else
  gradle clean build uberJar
fi
