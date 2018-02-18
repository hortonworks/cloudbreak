FROM alpine

COPY . /go/src/github.com/hortonworks/cb-cli
WORKDIR /go/src/github.com/hortonworks/cb-cli

RUN apk update \
    && apk add --no-cache -t build-deps musl-dev go git \
    && cd /go/src/github.com/hortonworks/cb-cli \
    && export GOPATH=/go \
    && export PATH=$PATH:/$GOPATH/bin \
    && go get github.com/golang/dep/cmd/dep \
    && go build -o /bin/cb-cli  \
    && rm -rf /go \
    && apk del --purge build-deps

ENTRYPOINT ["/bin/cb-cli"]
