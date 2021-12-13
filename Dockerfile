FROM maven:3.6-openjdk-8

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
FROM openjdk:8-jre

# TODO: extract this automatically
ENV SDB_VER="0.0.2-SNAPSHOT"  

COPY --from=0 /opt/sensorDb /opt/sensorDb

# add sensorDb to path
RUN cd "/opt/sensorDb/sensorDb-command/target/" \
    && tar zxvf "sensorDb-command-${SDB_VER}-sensorDb.tar.gz" \
    && chmod -R o+r,o+x "sensorDb-command-${SDB_VER}-sensorDb.tar.gz"

# ENV PATH="${PATH}:/usr/local/nunaliit_VERSION_DATE_BUILD/bin"
ENV PATH="${PATH}:/opt/sensorDb/sensorDb-command/target/sensorDb-command-${SDB_VER}/bin"
RUN apt-get install -y bash

WORKDIR "/opt/sensorDb/config"
ENTRYPOINT ["sensorDb"]
CMD ["run"]