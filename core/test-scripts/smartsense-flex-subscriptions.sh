
#SmartSense subscriptions
curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/smartsensesubscriptions/account
curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/smartsensesubscriptions/user

curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/smartsensesubscriptions/2

curl -k -X POST -H "Content-type: Application/json" -H "Authorization: Bearer $TOKEN" -d '{"subscriptionId": "A-99907550-C-47402263"}' $HOST/cb/api/v1/smartsensesubscriptions/user
curl -k -X POST -H "Content-type: Application/json" -H "Authorization: Bearer $TOKEN" -d '{"subscriptionId": "A-99907550-C-47402263"}' $HOST/cb/api/v1/smartsensesubscriptions/account

curl -k -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/smartsensesubscriptions/1

#Flex subscriptions
curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/account
curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/user

curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/2

curl -k -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/2

curl -k -X POST -H "Content-type: Application/json" -H "Authorization: Bearer $TOKEN" -d '{"name": "defaultSubscription","subscriptionId": "FLEX-9990755047402263", "smartSenseSubscriptionId": 2}' $HOST/cb/api/v1/flexsubscriptions/user
curl -k -X POST -H "Content-type: Application/json" -H "Authorization: Bearer $TOKEN" -d '{"name": "defaultSubscription2","subscriptionId": "FLEX-9990755047402264", "smartSenseSubscriptionId": 2}' $HOST/cb/api/v1/flexsubscriptions/account
curl -k -X POST -H "Content-type: Application/json" -H "Authorization: Bearer $TOKEN" -d '{"name": "full-defaultSubscription","subscriptionId": "FLEX-9990755048", "smartSenseSubscriptionId": 3, "usedForController": true, "usedAsDefault": true}' $HOST/cb/api/v1/flexsubscriptions/account

curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/user/defaultSubscription
curl -k -X GET -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/account/defaultSubscription2

curl -k -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/user/defaultSubscription
curl -k -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/cb/api/v1/flexsubscriptions/account/defaultSubscription2