##Quickstart and installation

##Running Cloudbreak API using Docker

###Database
The only dependency that Cloudbreak needs is a postgresql database. The easiest way to spin up a postgresql is of course Docker. Run it first with this line:
```
docker run -d --name="postgresql" -p 5432:5432 -v /tmp/data:/data -e USER="seqadmin" -e DB="cloudbreak" -e PASS="seq123_" paintedfox/postgresql
```
###Cloudbreak REST API
After postgresql is running, Cloudbreak can be started locally in a Docker container with the following command. By linking the database container, the necessary environment variables for the connection are set. The postgresql address can be set explicitly through the environment variable: DB_PORT_5432_TCP_ADDR.
```
VERSION=0.1-20140623140412

docker run -d --name cloudbreak \
-e "VERSION=$VERSION" \
-e "AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" \
-e "AWS_SECRET_KEY=$AWS_SECRET_KEY" \
-e "HBM2DDL_STRATEGY=create" \
-e "MAIL_SENDER_USERNAME=$MAIL_SENDER_USERNAME" \
-e "MAIL_SENDER_PASSWORD=$MAIL_SENDER_PASSWORD" \
-e "MAIL_SENDER_HOST=$MAIL_SENDER_HOST" \
-e "MAIL_SENDER_PORT=$MAIL_SENDER_PORT" \
-e "MAIL_SENDER_FROM=$MAIL_SENDER_FROM" \
-e "HOST_ADDR=$HOST_ADDR" \
--link postgresql:db -p 8080:8080 \
dockerfile/java bash \
-c 'curl -o /tmp/cloudbreak-$VERSION.jar https://s3-eu-west-1.amazonaws.com/seq-repo/releases/com/sequenceiq/cloudbreak/$VERSION/cloudbreak-$VERSION.jar && java -jar /tmp/cloudbreak-$VERSION.jar'

```

Note: The system properties prefixed with MAIL_SENDER_ are the SNMP settings required to send emails.  

##Running Cloudbreak API on the host

If you'd like to run Cloudbreak outside of a Docker container - directly on the host - we provide you an installation shell script.

After building the application _(./gradlew clean build)_ please run the following script from the project root:
```
./run_cloudbreak.sh <db-user> <db-pass> <db-host> <db-port> <host-address>
```
The arguments are as follows:

`db-user` - your database user

`db-pass` - your password for the database

`db-host` - the address of the machine hosting your database

`db-port` - the port where you can connect to the database

`host-address` - the ngrok generated address to receive SNS notifications


##Configuration 

###Development 

In order to be able to receive Amazon push notifications on localhost, you will need to install a secure introspectable tunnel to localhost.

###Install and configure ngrok
Cloudbreak uses SNS to receive notifications. On OSX you can do the following:

```
brew update && brew install ngrok
ngrok 8080
```
_Note: In the terminal window you'll find displayed a value - this is the last argument `host-address` of the `run_cloudbreak.sh` script_

###Production

TBD - add properties list !!!
