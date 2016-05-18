FROM sequenceiq/cloudbreak:1.3.0-dev.77

ADD cloudbreak.jar /

RUN rm -rf /schema \
    && unzip -o cloudbreak.jar schema/* -d /
