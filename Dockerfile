FROM alpine

COPY . /go/src/github.com/hortonworks/cb-cli
WORKDIR /go/src/github.com/hortonworks/cb-cli

RUN apk update \
    && apk add -t build-deps go git \
    && cd /go/src/github.com/hortonworks/cb-cli \
    && export GOPATH=/go \
    && export PATH=$PATH:/$GOPATH/bin \
    && go get github.com/tools/godep \
    && godep restore \
    && go build -o /bin/cb-cli  \
    && rm -rf /go \
    && apk del --purge build-deps

