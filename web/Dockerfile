FROM gliderlabs/alpine:3.1
MAINTAINER Hortonworks

ENV ULU_SERVER_PORT 3000
RUN apk-install curl nodejs bash git
EXPOSE 3000
ADD . /uluwatu
RUN cd /uluwatu && for i in {1..10}; do npm install && npm install -g bower && break || sleep 5; done
RUN cd /uluwatu/app/static && for i in {1..10}; do bower install --allow-root && break || sleep 5; done

CMD ["/uluwatu/start-docker.sh"]

