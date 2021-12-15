FROM maven:3.6-openjdk-8 as build

RUN apt-get install -y git

# Install dependencies

# Install Nunaliit
RUN git clone https://github.com/GCRC/nunaliit \
    && cd nunaliit \
    && git checkout nunaliit2-2.2.7 \
    && mvn clean compile \
    && mvn clean install

# Install sensordb
COPY . /opt/sensorDb

RUN cd /opt/sensorDb \
    && mvn clean compile \
    && mvn clean install

# Second stage
FROM openjdk:8-jre as deploy

# TODO: extract this automatically with "$(grep -m 1 -o '<version>.*</version>' pom.xml | sed 's-<\/\?version>--g')"
ENV SDB_VER="0.0.2-SNAPSHOT"  

COPY --from=build /opt/sensorDb /opt/sensorDb

# add path variables
RUN cd "/opt/sensorDb/sensorDb-command/target/" \
    && tar zxvf "sensorDb-command-${SDB_VER}-sensorDb.tar.gz" \
    && chmod -R o+r,o+x "sensorDb-command-${SDB_VER}-sensorDb.tar.gz"

ENV PATH="${PATH}:/opt/sensorDb/sensorDb-command/target/sensorDb-command-${SDB_VER}/bin"
RUN apt-get install -y bash

# Create default configuration
RUN cd / && ln -s /opt/sensorDb/config config
WORKDIR "/opt/sensorDb/config"
COPY example-server/ .

ENTRYPOINT ["sensorDb"]
CMD ["run"]