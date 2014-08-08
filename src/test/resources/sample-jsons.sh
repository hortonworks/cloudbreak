#!/bin/sh

curl -u cbuser@sequenceiq.com:test123 -X GET -H "Content-Type:application/json"  http://localhost:8080/me | jq .

curl -u cbuser@sequenceiq.com:test123 -X PUT -H "Content-Type:application/json" -d '{"statusRequest": "START"}' http://localhost:8080/stacks/50 | jq .

curl -u cbuser@sequenceiq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","parameters":{"subscriptionId":"a8b8dac0-2b84-41da-a08b-1e45297e0d2b", "jksPassword": "tet123"}}' http://localhost:8080/credentials | jq .

curl -u cbuser@sequenceiq.com:test123 -X POST -H "Content-Type:application/json" -d '{"name":"smap123","url":"https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn"}' http://localhost:8080/blueprints | jq .

curl -u cbuser@sequenceiq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","clusterSize":2, "templateId":"51", "name":"my azure", "credentialId":"51"}' http://localhost:8080/stacks | jq .

curl -u cbuser@sequenceiq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","name":"my-azure-cluster","parameters":{"location":"NORTH_EUROPE","description":"my-azure-cluster description","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","deploymentSlot":"production","imageName":"c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6","username":"ricsiuser","password":"Password!@#$","disableSshPasswordAuthentication":"true","vmType":"SMALL"}}' http://localhost:8080/templates | jq .

curl -u cbuser@sequenceiq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","name":"my-azure-cluster","parameters":{"location":"NORTH_EUROPE","description":"my-azure-cluster description","addressPrefix":"172.16.0.0/16","subnetAddressPrefix":"172.16.0.0/24","deploymentSlot":"production","imageName":"c290a6b031d841e09f2da759bbabe71f__Oracle-Linux-6","username":"ricsiuser","password":"Password!@#$","disableSshPasswordAuthentication":"true","vmType":"SMALL","ports":[{"protocol":"TCP","port":"8080","name":"local","localPort":"8080"}]}}' http://localhost:8080/templates | jq .


# create AWS stack & cluster - happy flow
curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","name":"mykred","parameters":{"roleArn":"arn:aws:iam::755047402263:role/seq-self-cf","ssKeyName":"sequence-eu"}}' http://localhost:8080/credentials | jq .
curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AWS","name":"awsinstancstemplate2","parameters":{"region":"EU_WEST_1", "amiId":"ami-7778af00","sshLocation":"0.0.0.0/0","instanceType":"T2Small", "volumeCount":2, "volumeSize":40, "volumeType":"Gp2", "spotPriced": true}}' http://localhost:8080/templates | jq .  
curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"nodeCount":3, "templateId":"52", "name":"sundaywork2", "credentialId":"52"}' http://localhost:8080/stacks | jq .
curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"url":"https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn"}' http://localhost:8080/blueprints | jq .
curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d '{"clusterName":"full-cloudbreak-api","blueprintId":52}' http://localhost:8080/stacks/51/cluster | jq .

curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/credentials/51 | jq .
curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/templates/51 | jq .
curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/stacks/51 | jq .
curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/blueprints/52 | jq .
curl -u cbuser@sequenceiq.com:test123 -sX GET -H "Content-Type:application/json" http://localhost:8080/stacks/51/cluster | jq .

curl -u cbuser@sequenceiq.com:test123 -sX DELETE -H "Content-Type:application/json" http://localhost:8080/stacks/51 | jq .
