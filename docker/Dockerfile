FROM maven:3.6.3-openjdk-8
MAINTAINER huyaolong <mail@yaolong.net>

VOLUME /tmp
EXPOSE 8080

RUN mkdir -p /opt/id-generator-app/src/main/java/org/fastercode/idgenerator && \
    cd /opt/id-generator-app && mvn clean install -DskipTests -U -e

