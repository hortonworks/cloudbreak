#!/bin/sh

curl -u user@seq.com:test123 -X GET -H "Content-Type:application/json"  http://localhost:8080/me | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","parameters":{"roleArn":"a8b8dac0-2b84-41da-a08b-1e45297e0d2b"}}' http://localhost:8080/credential | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","parameters":{"subscriptionId":"a8b8dac0-2b84-41da-a08b-1e45297e0d2b", "jksPassword": "tet123"}}' http://localhost:8080/credential | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"name":"smap123","url":"https://gist.githubusercontent.com/lalyos/53eeda2d3f6287656d84/raw/install-mac.sh"}' http://localhost:8080/blueprint | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","clusterSize":2, "templateId":"51", "name":"my azure", "credentialId":"51"}' http://localhost:8080/stack | jq .

curl -u user@seq.com:test123 -sX POST -H "Content-Type: application/json" -d '{"cloudPlatform":"AWS","clusterName":"my-infra-1","parameters":{"region":"EU_WEST_1", "amiId":"1234", "keyName":"sequence-eu","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","instanceType":"M3Xlarge"}}' http://localhost:8080/template | jq .

curl -u user@seq.com:test123 -sX POST -H "Content-Type: application/json" -d '{"cloudPlatform":"AWS","clusterName":"my-infra-1","parameters":{"region":"eu-west-1","keyName":"sequence-eu","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","instanceType":"M3.XLARGE"}}' http://localhost:8080/template | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","clusterName":"my-azure-cluster","parameters":{"location":"NORTH_EUROPE","description":"my-azure-cluster description","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","deploymentSlot":"production","imageName":"c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6","username":"ricsiuser","password":"Password!@#$","disableSshPasswordAuthentication":"true","vmType":"SMALL"}}' http://localhost:8080/template | jq .