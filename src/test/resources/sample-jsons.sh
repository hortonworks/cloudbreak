curl -u user@seq.com:test123 -X GET -H "Content-Type:application/json"  http://localhost:8080/me

curl -u user@seq.com:test123 -X POST -H "Content-Type:application/json" -d '{"cloudPlatform":"AZURE","parameters":{"subscriptionId":"asd","jksPassword":"pw123"}}' http://localhost:8080/infra