if [ $# -eq 0 ]; then
    echo "No arguments provided."
    #echo "You must provide the SensorDb version (e.g. 0.0.2-SNAPSHOT)."
    #exit 1
    SDB_VER="$(grep -m 1 -o '<version>.*</version>' pom.xml | sed 's-<\/\?version>--g')"
    echo "Detected version: $SDB_VER"
else
    SDB_VER=$1

fi

mvn clean compile
mvn clean install
ln -sf "/opt/sensorDb/sensorDb-command/target/sensorDb-command-"$SDB_VER /opt/sensorDb/latest
cd sensorDb-command/target/
tar zxvf "sensorDb-command-"$SDB_VER"-sensorDb.tar.gz"
chmod -R o+r,o+x "sensorDb-command-"$SDB_VER"-sensorDb.tar.gz"

