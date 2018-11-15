FROM hortonworks/hwx_openjdk:10-jdk-slim
MAINTAINER info@hortonworks.com

# install the cloudbreak app
COPY build/libs/mock-caas.jar /caas-mock/mock-caas.jar

CMD java -jar /caas-mock/mock-caas.jar