FROM node:0.10.32
MAINTAINER SequenceIQ

ENV ULU_SERVER_PORT 3000
RUN apt-get update
RUN apt-get install -y curl unzip
EXPOSE 3000
ADD . /uluwatu
CMD rm -rf /uluwatu/.git

RUN cd /uluwatu && npm install

CMD ["/uluwatu/start-docker.sh"]
