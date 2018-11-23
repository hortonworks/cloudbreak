FROM golang:1.11
LABEL maintainer=Hortonworks

COPY . /go/src/github.com/hortonworks/cb-cli
WORKDIR /go/src/github.com/hortonworks/cb-cli

ENV GO111MODULE on

RUN make build-linux

FROM alpine

COPY --from=0 /go/src/github.com/hortonworks/cb-cli/build/Linux/dp /usr/local/bin

ENTRYPOINT ["dp"]
