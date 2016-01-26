FROM gliderlabs/alpine:3.1
MAINTAINER SequenceIQ

ENV SL_SERVER_PORT 3001
RUN apk-install curl nodejs bash git
EXPOSE 3001
ADD . /sultans
RUN cd /sultans && VERSION=$(git name-rev --tags --name-only $(git rev-parse HEAD)) && echo $VERSION && sed -i '$s/}/,\n"version":"VERSION"}/' package.json && sed -i s/VERSION/$VERSION/ package.json
RUN rm -rf /sultans/.git
RUN cd /sultans && npm install
RUN cp -R /sultans/schema /


CMD ["/sultans/start-docker.sh"]
