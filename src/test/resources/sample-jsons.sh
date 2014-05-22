#!/bin/sh

curl -u user@seq.com:test123 -X GET -H "Content-Type:application/json"  http://localhost:8080/me | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","parameters":{"subscriptionId":"a8b8dac0-2b84-41da-a08b-1e45297e0d2b","jksPassword":"test123"}}' http://localhost:8080/credentials

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","clusterSize":2, "infraId":"1"}' http://localhost:8080/cloud

curl -u user@seq.com:test123 -sX POST -H "Content-Type: application/json" -d '{"cloudPlatform":"AWS","clusterName":"my-infra-1","parameters":{"region":"eu-west-1","keyName":"sequence-eu","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","instanceType":"M3.XLARGE"}}' http://localhost:8080/infra | jq .

curl -u user@seq.com:test123 -sX POST -H "Content-Type: application/json" -d '{"cloudPlatform":"AZURE","clusterName":"ricsicluster","parameters":{"location":"NORTH_EUROPE","description":"ricsi cluster description","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","deploymentSlot":"production", "imageName":"c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6", "username": "ricsiuser", "password":"Password!@#$", "disableSshPasswordAuthentication": "true", "vmType": "Small"}}' http://localhost:8080/infra | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"clusterSize":52, "infraId":"1"}' http://localhost:8080/cloud