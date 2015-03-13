FROM gliderlabs/alpine:3.1
MAINTAINER SequenceIQ

ENV SL_SERVER_PORT 3001
RUN apk-install curl nodejs bash
EXPOSE 3001
ADD . /sultans
CMD rm -rf /sultans/.git
RUN cd /sultans && npm install
CMD ["/sultans/start-docker.sh"]
