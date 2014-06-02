#!/bin/sh

curl -u user@seq.com:test123 -X GET -H "Content-Type:application/json"  http://localhost:8080/me | jq .

curl -u user@seq.com:test123 -X PUT -H "Content-Type:application/json" -d '{"statusRequest": "START"}' http://localhost:8080/stack/50 | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","parameters":{"subscriptionId":"a8b8dac0-2b84-41da-a08b-1e45297e0d2b", "jksPassword": "tet123"}}' http://localhost:8080/credential | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"name":"smap123","url":"https://gist.githubusercontent.com/doktoric/11eb550f45416e54fe60/raw/1a7d302c162ee5fb91866766ec1a1fd18fd98ed3/gistfile1.txt"}' http://localhost:8080/blueprint | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","clusterSize":2, "templateId":"51", "name":"my azure", "credentialId":"51"}' http://localhost:8080/stack | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","name":"my-azure-cluster","parameters":{"location":"NORTH_EUROPE","description":"my-azure-cluster description","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","deploymentSlot":"production","imageName":"c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6","username":"ricsiuser","password":"Password!@#$","disableSshPasswordAuthentication":"true","vmType":"SMALL"}}' http://localhost:8080/template | jq .

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","name":"my-azure-cluster","parameters":{"location":"NORTH_EUROPE","description":"my-azure-cluster description","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","deploymentSlot":"production","imageName":"c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6","username":"ricsiuser","password":"Password!@#$","disableSshPasswordAuthentication":"true","vmType":"SMALL","ports":[{"protocol":"TCP","port":"8080","name":"local","localPort":"8080"}]}}' http://localhost:8080/template | jq .


# create AWS stack
curl -u user@seq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","parameters":{"roleArn":"arn:aws:iam::755047402263:role/seq-self-cf","instanceProfileRoleArn":"arn:aws:iam::755047402263:instance-profile/readonly-role"}}' http://localhost:8080/credential | jq .
curl -u user@seq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","name":"awsinstancestemplate","parameters":{"region":"EU_WEST_1", "amiId":"ami-2f39f458", "keyName":"sequence-eu","sshLocation":"0.0.0.0/0","instanceType":"M1Small"}}' http://localhost:8080/template | jq .
curl -u user@seq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"nodeCount":3, "templateId":"52", "name":"sundaywork2", "credentialId":"52"}' http://localhost:8080/stack | jq .

curl -u user@seq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/stack/51 | jq .
curl -u user@seq.com:test123 -sX DELETE -H "Content-Type:application/json" http://localhost:8080/stack/51 | jq .
