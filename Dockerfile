FROM nginx
MAINTAINER SequenceIQ

ADD . /usr/local/nginx/html
ADD start.sh /

CMD /start.sh
