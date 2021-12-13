To build locally:
	
	gradle clean build


To start this on Windows:

java -DSPRING_PROFILES_ACTIVE=development -DSPRING_CLOUD_CONFIG_URI=http://localhost:8888/ -DSPRING_CONFIG_IMPORT=optional:configserver:http://localhost:8888/ -DEUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka -jar .\build\libs\resume-tailor-service-1.0.jar

build docker image

	docker build -t resumetailor/resume-tailor-service:1.0 .

run docker image - 8001 is debug port

	docker run -d -p 8080:8080 -p 8001:8001 resumetailor/resume-tailor-service:1.0
	
run docker image with access to docker host (localhost)

	docker run -d -p 8080:8080 -p 8001:8001 --add-host=host.docker.internal:host-gateway resumetailor/resume-tailor-service:1.0

From within running image, curl a running config svc on the docker host:  (running in docker or otherwise)

	curl http://host.docker.internal:8888/resume-tailor-service/development/	
	
	