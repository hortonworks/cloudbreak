FROM hortonworks/cloudbreak:2.4.2

ADD cloudbreak.jar /

RUN rm -rf /schema \
    && unzip -o cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ \
    && mv /tmp/BOOT-INF/classes/schema/ /schema/
