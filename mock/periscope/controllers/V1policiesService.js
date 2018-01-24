'use strict';

exports.addScalingPolicy = function(args, res, next) {
  /**
   * create policy
   * Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application based on an alert. When scaling policies are used, the capacity is automatically increased or decreased according to the conditions defined. Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it executes the associated policy. We scaling granularity is at the hostgroup level - thus you have the option to scale services or components only, not the whole cluster.
   *
   * clusterId Long 
   * body ScalingPolicyRequest  (optional)
   * returns ScalingPolicyResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/policies/test-policy.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.deleteScalingPolicy = function(args, res, next) {
  /**
   * delete policy
   * Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application based on an alert. When scaling policies are used, the capacity is automatically increased or decreased according to the conditions defined. Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it executes the associated policy. We scaling granularity is at the hostgroup level - thus you have the option to scale services or components only, not the whole cluster.
   *
   * clusterId Long 
   * policyId Long 
   * no response value expected for this operation
   **/
  var examples = {};
    examples['application/json'] = require('../responses/policies/test-policy.json');
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getScalingPolicies = function(args, res, next) {
  /**
   * retrieve policy
   * Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application based on an alert. When scaling policies are used, the capacity is automatically increased or decreased according to the conditions defined. Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it executes the associated policy. We scaling granularity is at the hostgroup level - thus you have the option to scale services or components only, not the whole cluster.
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/policies/policies.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.updateScalingPolicy = function(args, res, next) {
  /**
   * modify policy
   * Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application based on an alert. When scaling policies are used, the capacity is automatically increased or decreased according to the conditions defined. Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it executes the associated policy. We scaling granularity is at the hostgroup level - thus you have the option to scale services or components only, not the whole cluster.
   *
   * clusterId Long 
   * policyId Long 
   * body ScalingPolicyRequest  (optional)
   * returns ScalingPolicyResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "adjustmentType" : "NODE_COUNT",
  "name" : "aeiou",
  "scalingAdjustment" : 0,
  "alertId" : 6,
  "hostGroup" : "aeiou",
  "id" : 1
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

