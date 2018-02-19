FROM golang:1.9.2
LABEL maintainer=Hortonworks

COPY . /go/src/github.com/hortonworks/cb-cli
WORKDIR /go/src/github.com/hortonworks/cb-cli

RUN make build-linux

FROM alpine

COPY --from=0 /go/src/github.com/hortonworks/cb-cli/build/Linux/cb /usr/local/bin

ENTRYPOINT ["cb"]
