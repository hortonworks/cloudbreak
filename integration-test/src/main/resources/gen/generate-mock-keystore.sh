#!/usr/bin/env bash

docker run --rm -v $(pwd):/certs ehazlett/certm -d /certs bundle generate --host localhost --host 127.0.0.1 --host *.cluster.local --host test --host mockserver.default.svc.cluster.local --host qa-mockserver.default.svc.cluster.local --host eng-mockserver.default.svc.cluster.local -o=gateway
openssl pkcs12 -export -out server.pkcs12 -in server.pem -inkey server-key.pem -passout pass:secret

keytool -genkey -noprompt -dname "CN=mock_server" -keyalg RSA \
  -alias keystore_mock -keystore keystore_mock -storepass secret -keypass secret

keytool -delete -alias keystore_mock -keystore keystore_mock -storepass secret

keytool -importkeystore \
        -deststorepass secret -destkeypass secret -destkeystore keystore_mock \
        -srckeystore server.pkcs12 -srcstoretype PKCS12 -srcstorepass secret \
        -alias 1


cp server.pem ../cluster.pem
cp keystore_mock ../keystore_server
