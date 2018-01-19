'use strict';

exports.deleteByCloudbreakCluster = function(args, res, next) {
  /**
   * delete cluster
   * Ambari cluster.
   *
   * cbClusterId Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.disableAutoscaleStateByCloudbreakCluster = function(args, res, next) {
  /**
   * set cluster's autoscale feature state
   * Ambari cluster.
   *
   * cbClusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : 0,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.enableAutoscaleStateByCloudbreakCluster = function(args, res, next) {
  /**
   * set cluster's autoscale feature state
   * Ambari cluster.
   *
   * cbClusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : 0,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getByCloudbreakCluster = function(args, res, next) {
  /**
   * retrieve cluster
   * Ambari cluster.
   *
   * cbClusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : args.cbClusterId.value,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    switch(args.cbClusterId.value){
      case 1:
        var responseJson = {
          "host":null,
          "port":"9443",
          "user":"admin",
          "stackId":1,
          "id":1,
          "state":"CREATE_FAILED",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {  
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      case 2:
        var responseJson = {
          "host":"52.16.174.228",
          "port":"9443",
          "user":"admin",
          "stackId":2,
          "id":2,
          "state":"UPDATE_IN_PROGRESS",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {  
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      case 3:
        var responseJson = {
          "host":null,
          "port":"9443",
          "user":"admin",
          "stackId":3,
          "id":3,
          "state":"CREATE_IN_PROGRESS",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {  
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      case 4:
        var responseJson = {
          "host":"35.187.4.62",
          "port":"9443",
          "user":"admin",
          "stackId":4,
          "id":4,
          "state":"RUNNING",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      default:
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    }
  } else {
    res.end();
  }
}

exports.modifyByCloudbreakCluster = function(args, res, next) {
  /**
   * modify cluster
   * Ambari cluster.
   *
   * cbClusterId Long 
   * body AutoscaleClusterRequest  (optional)
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : 0,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.runByCloudbreakCluster = function(args, res, next) {
  /**
   * set cluster state
   * Ambari cluster.
   *
   * cbClusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : 0,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.suspendByCloudbreakCluster = function(args, res, next) {
  /**
   * set cluster state
   * Ambari cluster.
   *
   * cbClusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : 0,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

