UAA=XX
USERNAME=XX
PASSWORD=XX
TOKEN=$(curl -iX POST -H "accept: application/x-www-form-urlencoded" -d 'credentials={"username":"$USERNAME","password":"$PASSWORD"}' "$UAA/oauth/authorize?response_type=token&client_id=periscope-client&scope.0=openid&source=login&redirect_uri=http://periscope.client"  | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1)
HOST=localhost:8080

# cluster
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"host":"127.0.0.1", "port":"8080", "user":"admin", "pass":"admin"}' $HOST/clusters | jq .
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
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alarms":[{"alarmName":"pendingContainerHigh","description":"Number of pending containers is high","metric":"PENDING_CONTAINERS","threshold":10,"comparisonOperator":"GREATER_THAN","period":1},{"alarmName":"freeGlobalResourcesRateLow","description":"Low free global resource rate","metric":"GLOBAL_RESOURCES","threshold":1,"comparisonOperator":"EQUALS","period":1,"notifications":[{"target":["krisztian.horvath@sequenceiq.com"],"notificationType":"EMAIL"}]}]}' $HOST/clusters/50/alarms/metric | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alarmName":"unhealthyNodesHigh","description":"Number of unhealthy nodes is high","metric":"UNHEALTHY_NODES","threshold":5,"comparisonOperator":"GREATER_OR_EQUAL_THAN","period":5}' $HOST/clusters/50/alarms/metric | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alarms/metric | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alarms/metric/150 | jq .

# set time alarms
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"alarms":[{"alarmName":"cron_worktime","description":"Number of nodes during worktime","timeZone":"Europe/Budapest","cron":"0 49 19 ? * MON-FRI"}]}' $HOST/clusters/50/alarms/time | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alarms/time | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/alarms/time/102 | jq .

# set scaling policy for metric alarms
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"minSize":1,"maxSize":10,"cooldown":30,"scalingPolicies":[{"name":"downScaleWhenHighResource","adjustmentType":"EXACT","scalingAdjustment":5,"hostGroup":"slave_1","alarmId":"101"},{"name":"upScaleWhenHighPendingContainers","adjustmentType":"PERCENTAGE","scalingAdjustment":40,"hostGroup":"slave_1","alarmId":"100"}]}' $HOST/clusters/50/policies | jq .
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"upScaleWhenHighUnhealthyNodes","adjustmentType":"NODE_COUNT","scalingAdjustment":5,"hostGroup":"slave_1","alarmId":"102"}' $HOST/clusters/50/policies | jq .
curl -X GET -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/policies | jq .
curl -X DELETE -H "Authorization: Bearer $TOKEN" $HOST/clusters/50/policies/150 | jq .

# set scaling policy for time alarms
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"minSize":1,"maxSize":10,"cooldown":30,"scalingPolicies":[{"name":"downScaleOnWeekend","adjustmentType":"EXACT","scalingAdjustment":3,"hostGroup":"slave_1","alarmId":"150"}]}' $HOST/clusters/50/policies | jq .
