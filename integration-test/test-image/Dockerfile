FROM hortonworks/cloudbreak:1.16.6

ADD cloudbreak.jar /

RUN rm -rf /schema \
    && unzip -o cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ \
    && mv /tmp/BOOT-INF/classes/schema/ /schema/
