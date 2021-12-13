# example build command
# docker build -t resumetailor/resume-tailor-service:1.0 .

# example run and connect
# docker run --rm -ti resumetailor/resume-tailor-service:1.0 /bin/sh

# example run as detached
# docker run -d resumetailor/resume-tailor-service:1.0

# example run with port exposed to localhost (docker host)
# docker run -d -p 8080:8080 -p 8001:8001 --add-host=host.docker.internal:host-gateway resumetailor/resume-tailor-service:1.0

FROM adoptopenjdk/openjdk16:latest

WORKDIR /java-apps/

# enables the use of netstat and curl commands to determine use of ports
RUN apt-get update && apt-get install -y net-tools

COPY ./build/libs/resume-tailor-service-1.0.jar /java-apps/resume-tailor-service-1.0.jar

# add wait script to make container wait in docker compose
COPY ./src/main/resources/wait-for-it.sh /java-apps/wait-for-it.sh

# the *:8001 part of the following line is critical for docker debugging 
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8001

# provide default environment entries that allow the container to run separately and connect to services on localhost
# see https://medium.com/@TimvanBaarsen/how-to-connect-to-the-docker-host-from-inside-a-docker-container-112b4c71bc66
ENV SPRING_PROFILES_ACTIVE development
ENV SPRING_CLOUD_CONFIG_URI http://host.docker.internal:8888/
ENV SPRING_CONFIG_IMPORT optional:configserver:http://host.docker.internal:8888/
ENV EUREKA_CLIENT_SERVICEURL_DEFAULTZONE http://host.docker.internal:8761/eureka

CMD ["./wait-for-it.sh","neverfindthishostname:8888","-t", "10","--", "java", "-jar", "resume-tailor-service-1.0.jar"]

# run the java apps at container startup
# CMD ["java", "-jar", "resume-tailor-service-1.0.jar"]

EXPOSE 8080
EXPOSE 8001

