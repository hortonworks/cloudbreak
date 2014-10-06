FROM node:0.10.32
MAINTAINER SequenceIQ

COPY . /uluwatu

RUN npm install --prefix /uaa-registration /uaa-registration

EXPOSE 3000

CMD ["/uaa-registration/start.sh"]
