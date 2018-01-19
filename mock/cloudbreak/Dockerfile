FROM node:8.4.0-alpine

RUN apk update
RUN apk add bash
ADD . /cbm
WORKDIR /cbm
RUN npm install
ENTRYPOINT ["npm","start"]
