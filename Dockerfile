FROM gliderlabs/alpine:3.1
MAINTAINER SequenceIQ

ENV ULU_SERVER_PORT 3000
RUN apk-install curl nodejs
EXPOSE 3000
ADD . /uluwatu
CMD rm -rf /uluwatu/.git

RUN cd /uluwatu && npm install

CMD ["/uluwatu/start-docker.sh"]
