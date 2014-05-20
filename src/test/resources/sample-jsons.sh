#!/bin/sh

curl -u user@seq.com:test123 -X GET -H "Content-Type:application/json"  http://localhost:8080/me | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","parameters":{"subscriptionId":"asd","jksPassword":"pw123"}}' http://localhost:8080/infra

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","clusterSize":2, "infraId":"1"}' http://localhost:8080/cloud

curl -u user@seq.com:test123 -sX POST -H "Content-Type: application/json" -d '{"cloudPlatform":"AWS","clusterName":"my-infra-1","parameters":{"region":"eu-west-1","keyName":"sequence-eu","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","instanceType":"M3.XLARGE"}}' http://localhost:8080/infra | jq .
