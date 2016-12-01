FROM gliderlabs/alpine:3.1
MAINTAINER Hortonworks

ENV ULU_SERVER_PORT 3000
RUN apk-install curl nodejs bash git
EXPOSE 3000
ADD . /uluwatu
RUN cd /uluwatu && npm install && npm install -g bower
RUN for i in {1..10}; do cd /uluwatu && npm install && npm install -g bower && break || sleep 5; done
RUN for i in {1..10}; do cd /uluwatu/app/static && bower install --allow-root && break || sleep 5; done

CMD ["/uluwatu/start-docker.sh"]

