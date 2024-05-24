FROM amazoncorretto:11
ENV SPRING_PROFILES_ACTIVE=docker
COPY wait-for-it.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/wait-for-it.sh
COPY target/*.jar app.jar
ENTRYPOINT ["wait-for-it.sh", "stats-server", "9090", "java", "-jar", "/app.jar"]