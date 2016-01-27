FROM gliderlabs/alpine:3.1
MAINTAINER SequenceIQ

ENV SL_SERVER_PORT 3001
RUN apk-install curl nodejs bash git
EXPOSE 3001
ADD . /sultans
RUN cd /sultans && npm install
RUN cp -R /sultans/schema /

CMD ["/sultans/start-docker.sh"]
