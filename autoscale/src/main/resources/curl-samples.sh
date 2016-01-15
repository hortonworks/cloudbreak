TOKEN=$(curl -iX POST -H "accept: application/x-www-form-urlencoded" -d 'credentials={"username":"XX@sequenceiq.com","password":"YY"}' "$UAA/oauth/authorize?response_type=token&client_id=periscope-client&scope.0=openid&source=login&redirect_uri=http://periscope.client"  | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1)
HOST=localhost:8080

# cluster
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"host":"104.154.75.134", "port":"8080", "user":"admin", "pass":"admin"}' $HOST/clusters | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"host":"104.154.75.135", "port":"8080", "user":"admin", "pass":"admin"}' $HOST/clusters/52 | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50 | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50 | jq .

# set cluster state
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"state":"SUSPENDED"}' $HOST/clusters/50/state | jq .

# app movement
curl -w '%{http_code}' -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST -d '{"allowed":"false"}' $HOST/clusters/50/movement | jq .

# set random priority
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" $HOST/applications/50/random |jq .

# list all app
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/applications | jq .

# refresh ambari configuration
curl -X POST -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/configurations | jq .

# set queue newSetup
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"setup":[{"name":"default", "capacity":55}, {"name":"high", "capacity":45}]}' $HOST/clusters/50/configurations/queue | jq .

# set metric alarms
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alertName":"allocatedmemory","description":"Allocated memory","period":3,"alertDefinition":"allocated_memory", "alertState":"CRITICAL"}' $HOST/clusters/50/alerts/metric | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alertName":"allocatedmemory","description":"Allocated memory","period":3,"alertDefinition":"allocated_memory", "alertState":"WARN"}' $HOST/clusters/50/alerts/metric | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alertName":"allocatedmemory","description":"Low free global resource rate","period":5,"alertDefinition":"allocated_memory"}' $HOST/clusters/50/alerts/metric/150 | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alerts/metric | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alerts/metric/150 | jq .

# set time alerts
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alertName":"cron-worktime","description":"Number of nodes during worktime","timeZone":"Europe/Budapest","cron":"0 54 10 ? * MON-FRI"}' $HOST/clusters/50/alerts/time | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alertName":"cron-worktime-modified","description":"Number of nodes during worktime","timeZone":"Europe/Budapest","cron":"0 39 16 ? * MON-FRI"}' $HOST/clusters/50/alerts/time/153 | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alerts/time | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alerts/time/102 | jq .

# set scaling policy for metric alerts
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"upScale","adjustmentType":"NODE_COUNT","scalingAdjustment":2,"hostGroup":"slave_1","alertId":"151"}' $HOST/clusters/50/policies | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"upScaleModified","adjustmentType":"EXACT","scalingAdjustment":2,"hostGroup":"slave_1","alertId":"151"}' $HOST/clusters/50/policies/150 | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/policies | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/policies/150 | jq .

# set scaling configuration
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"minSize":3,"maxSize":10,"cooldown":30}' $HOST/clusters/50/configurations/scaling | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/configurations/scaling | jq .