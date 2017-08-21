FROM node:8.4.0-alpine
MAINTAINER SequenceIQ

ENV SL_SERVER_PORT 3001
RUN apk add --no-cache curl bash git
EXPOSE 3001
ADD . /sultans
RUN cd /sultans && npm install
RUN cp -R /sultans/schema /

CMD ["/sultans/start-docker.sh"]
