'use strict';

exports.addCluster = function(args, res, next) {
  /**
   * create cluster
   * Ambari cluster.
   *
   * body AutoscaleClusterRequest  (optional)
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/clusters/1.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.deleteCluster = function(args, res, next) {
  /**
   * delete cluster
   * Ambari cluster.
   *
   * clusterId Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getCluster = function(args, res, next) {
  /**
   * retrieve cluster
   * Ambari cluster.
   *
   * clusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : args.clusterId.value,
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

exports.getClusters = function(args, res, next) {
  /**
   * retrieve all cluster
   * Ambari cluster.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "port" : "aeiou",
  "stackId" : args.clusterId.value,
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
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.modifyCluster = function(args, res, next) {
  /**
   * modify cluster
   * Ambari cluster.
   *
   * clusterId Long 
   * body AutoscaleClusterRequest  (optional)
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : args.clusterId.value,
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

exports.setAutoscaleState = function(args, res, next) {
  /**
   * set cluster's autoscale feature state
   * Ambari cluster.
   *
   * clusterId Long 
   * body AutoscaleClusterState  (optional)
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : args.clusterId.value,
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

exports.setState = function(args, res, next) {
  /**
   * set cluster state
   * Ambari cluster.
   *
   * clusterId Long 
   * body ClusterState  (optional)
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
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

