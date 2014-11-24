FROM node:0.10.32
MAINTAINER SequenceIQ

COPY . /sultans

RUN npm install --prefix /sultans /sultans

EXPOSE 3000

CMD ["/sultans/start.sh"]
