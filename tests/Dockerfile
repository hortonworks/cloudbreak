FROM ubuntu 
MAINTAINER mhalmy@hortonworks.com

RUN apt-get update 
RUN apt-get install -y software-properties-common
RUN apt-add-repository -y ppa:rael-gc/rvm
RUN apt-get update
RUN apt-get install -y rvm
RUN /bin/bash -c "source /usr/share/rvm/scripts/rvm;rvm install 2.4.1;rvm list;gem install rspec;gem install aruba;gem install aruba-rspec;gem install json;gem install oauth;gem install rspec-json_expectations;gem install rspec_junit_formatter;"
CMD ["echo","Image created"]
