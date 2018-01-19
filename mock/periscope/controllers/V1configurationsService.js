'use strict';

exports.getScalingConfiguration = function(args, res, next) {
  /**
   * retrieve configuration
   * An SLA scaling policy can contain multiple alerts. When an alert is triggered a scaling adjustment is applied, however to keep the cluster size within boundaries a cluster size min. and cluster size max. is attached to the cluster - thus a scaling policy can never over or undersize a cluster. Also in order to avoid stressing the cluster we have introduced a cooldown time period (minutes) - though an alert is raised and there is an associated scaling policy, the system will not apply the policy within the configured timeframe. In an SLA scaling policy the triggered rules are applied in order.
   *
   * clusterId Long 
   * returns ScalingConfiguration
   **/
  var examples = {};
  examples['application/json'] = {
      "minSize": 3,
      "maxSize": 10,
      "cooldown": 30
  };
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.setScalingConfiguration = function(args, res, next) {
  /**
   * create configuration
   * An SLA scaling policy can contain multiple alerts. When an alert is triggered a scaling adjustment is applied, however to keep the cluster size within boundaries a cluster size min. and cluster size max. is attached to the cluster - thus a scaling policy can never over or undersize a cluster. Also in order to avoid stressing the cluster we have introduced a cooldown time period (minutes) - though an alert is raised and there is an associated scaling policy, the system will not apply the policy within the configured timeframe. In an SLA scaling policy the triggered rules are applied in order.
   *
   * clusterId Long 
   * body ScalingConfiguration  (optional)
   * returns ScalingConfiguration
   **/
  var examples = {};
  examples['application/json'] = {
      "minSize": 3,
      "maxSize": 10,
      "cooldown": 30
  };
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

