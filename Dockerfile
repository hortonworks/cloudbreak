FROM gliderlabs/alpine:3.1
MAINTAINER SequenceIQ

ENV ULU_SERVER_PORT 3000
RUN apk-install curl nodejs bash git
EXPOSE 3000
ADD . /uluwatu
RUN cd /uluwatu && VERSION=$(git name-rev --tags --name-only $(git rev-parse HEAD)) && echo $VERSION && sed -i '$s/}/,\n"version":"VERSION"}/' package.json && sed -i s/VERSION/$VERSION/ package.json
RUN rm -rf /uluwatu/.git

RUN cd /uluwatu && npm install
RUN cd /uluwatu/app/static && bower install

CMD ["/uluwatu/start-docker.sh"]
