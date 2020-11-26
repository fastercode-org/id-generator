# sudo docker build --no-cache -t id-generator-app:v1 -f ./Dockerfile .

FROM maven:3.6.3-openjdk-8
MAINTAINER huyaolong <mail@yaolong.net>

VOLUME /tmp
EXPOSE 8080

RUN mkdir -p /opt/id-generator-app/src/main/java/org/fastercode/idgenerator
ADD maven-settings.xml /usr/share/maven/conf/settings.xml
ADD SpringApp.java /opt/id-generator-app/src/main/java/org/fastercode/idgenerator/
ADD pom.xml /opt/id-generator-app/
RUN cd /opt/id-generator-app && mvn clean install -DskipTests -U -e && mv /opt/id-generator-app/target/id-generator-app.jar /

CMD ["java", "-Xms32m", "-Xmx32m", "-jar", "id-generator-app.jar", "--server.port=8080", "--id-generator.serverLists=127.0.0.1:2181", "--id-generator.namespace=id_generator", "--id-generator.workersBackUpFile=/tmp/id_generator.json", "--id-generator.workersBackUpInterval=60"]
