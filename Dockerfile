FROM nginx
MAINTAINER SequenceIQ

ADD . /usr/local/nginx/html
CMD nginx
