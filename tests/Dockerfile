FROM ubuntu
MAINTAINER Hortonworks

RUN apt-get update
RUN apt-get install -y software-properties-common
RUN apt-add-repository -y ppa:rael-gc/rvm
RUN apt-get update
RUN apt-get install -y rvm
RUN /bin/bash -c "source /usr/share/rvm/scripts/rvm;rvm install 2.4.1;rvm list;gem install rspec;gem install aruba;gem install aruba-rspec;gem install json;gem install oauth;gem install rspec-json_expectations;gem install rspec_junit_formatter;gem install rest-client"

WORKDIR /tmp/

COPY /scripts/aruba-docker.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENV HOME=/tmp

RUN chmod -Rf 777 .

ENTRYPOINT ["/entrypoint.sh"]