FROM openjdk:8u162-jre-stretch
COPY target /opt/edammap
WORKDIR /var/lib/edammap
EXPOSE 8080
CMD java -jar /opt/edammap/edammap-server-0.2-SNAPSHOT.jar -b http://0.0.0.0:8080 -p edammap -e EDAM_1.20.owl -f files --fetching true --db biotools.db --idf biotools.idf --idf-stemmed biotools.stemmed.idf --log /var/log/edammap
