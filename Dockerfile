FROM node:0.10.32
MAINTAINER SequenceIQ

ENV ULU_SERVER_PORT 3000

COPY . /uluwatu

RUN npm install --prefix /uluwatu /uluwatu

EXPOSE 3000

CMD ["/uluwatu/start.sh"]
