FROM sapmachine/jdk11
#RUN apt-get update && apt-get install netcat
#VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar","/app.jar"]