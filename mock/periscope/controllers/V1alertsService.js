'use strict';

exports.createMetricAlerts = function(args, res, next) {
  /**
   * create alert which metric based
   * Auto-scaling supports two Alert types: metric and time based. Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. 
   *
   * clusterId Long 
   * body MetricAlertRequest  (optional)
   * returns MetricAlertResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/alerts/test-alert.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.createPrometheusAlert = function(args, res, next) {
  /**
   * create alert which prometheus based
   * Prometheus based alerts are using Prometheus under the hood. 
   *
   * clusterId Long 
   * body PrometheusAlertRequest  (optional)
   * returns PrometheusAlertResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "scalingPolicy" : {
    "adjustmentType" : "NODE_COUNT",
    "name" : "aeiou",
    "scalingAdjustment" : 5,
    "alertId" : 2,
    "hostGroup" : "aeiou"
  },
  "scalingPolicyId" : 5,
  "alertRuleName" : "aeiou",
  "period" : 6,
  "alertName" : "aeiou",
  "description" : "aeiou",
  "threshold" : 1.4658129805029452,
  "id" : 0,
  "alertState" : "OK",
  "alertOperator" : "LESS_THAN"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.createTimeAlert = function(args, res, next) {
  /**
   * create alert which time based
   * Auto-scaling supports two Alert types: metric and time based. Time based alerts are based on cron expressions and allow alerts to be triggered based on time.
   *
   * clusterId Long 
   * body TimeAlertRequest  (optional)
   * returns TimeAlertResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cron" : "aeiou",
  "scalingPolicy" : {
    "adjustmentType" : "NODE_COUNT",
    "name" : "aeiou",
    "scalingAdjustment" : 1,
    "alertId" : 5,
    "hostGroup" : "aeiou"
  },
  "scalingPolicyId" : 6,
  "alertName" : "aeiou",
  "description" : "aeiou",
  "timeZone" : "aeiou",
  "id" : 0
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.deleteMetricAlarm = function(args, res, next) {
  /**
   * delete alert which metric based
   * Auto-scaling supports two Alert types: metric and time based. Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. 
   *
   * clusterId Long 
   * alertId Long 
   * no response value expected for this operation
   **/
  var examples = {};
    examples['application/json'] = require('../responses/alerts/test-alert.json');
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.deletePrometheusAlarm = function(args, res, next) {
  /**
   * delete alert which prometheus based
   * Prometheus based alerts are using Prometheus under the hood. 
   *
   * clusterId Long 
   * alertId Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteTimeAlert = function(args, res, next) {
  /**
   * delete alert which time based
   * Auto-scaling supports two Alert types: metric and time based. Time based alerts are based on cron expressions and allow alerts to be triggered based on time.
   *
   * clusterId Long 
   * alertId Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getAlertDefinitions = function(args, res, next) {
  /**
   * retrieve alert definitions
   * Auto-scaling supports two Alert types: metric and time based. Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. 
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/alerts/qa-definitions.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getMetricAlerts = function(args, res, next) {
  /**
   * retrieve alert which metric based
   * Auto-scaling supports two Alert types: metric and time based. Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. 
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/alerts/metric-alerts.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrometheusAlerts = function(args, res, next) {
  /**
   * retrieve alert which prometheus based
   * Prometheus based alerts are using Prometheus under the hood. 
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "scalingPolicy" : {
    "adjustmentType" : "NODE_COUNT",
    "name" : "aeiou",
    "scalingAdjustment" : 5,
    "alertId" : 2,
    "hostGroup" : "aeiou"
  },
  "scalingPolicyId" : 5,
  "alertRuleName" : "aeiou",
  "period" : 6,
  "alertName" : "aeiou",
  "description" : "aeiou",
  "threshold" : 1.4658129805029452,
  "id" : 0,
  "alertState" : "OK",
  "alertOperator" : "LESS_THAN"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrometheusDefinitions = function(args, res, next) {
  /**
   * retrieve alert definitions
   * retrieve Prometheus alert rule definitions
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "name" : "aeiou",
  "label" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getTimeAlerts = function(args, res, next) {
  /**
   * retrieve alert which time based
   * Auto-scaling supports two Alert types: metric and time based. Time based alerts are based on cron expressions and allow alerts to be triggered based on time.
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/alerts/time-alerts.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.updateMetricAlerts = function(args, res, next) {
  /**
   * modify alert which metric based
   * Auto-scaling supports two Alert types: metric and time based. Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. 
   *
   * clusterId Long 
   * alertId Long 
   * body MetricAlertRequest  (optional)
   * returns MetricAlertResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/alerts/test-alert.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.updatePrometheusAlert = function(args, res, next) {
  /**
   * modify alert which prometheus based
   * Prometheus based alerts are using Prometheus under the hood. 
   *
   * clusterId Long 
   * alertId Long 
   * body PrometheusAlertRequest  (optional)
   * returns PrometheusAlertResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "scalingPolicy" : {
    "adjustmentType" : "NODE_COUNT",
    "name" : "aeiou",
    "scalingAdjustment" : 5,
    "alertId" : 2,
    "hostGroup" : "aeiou"
  },
  "scalingPolicyId" : 5,
  "alertRuleName" : "aeiou",
  "period" : 6,
  "alertName" : "aeiou",
  "description" : "aeiou",
  "threshold" : 1.4658129805029452,
  "id" : 0,
  "alertState" : "OK",
  "alertOperator" : "LESS_THAN"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.updateTimeAlert = function(args, res, next) {
  /**
   * modify alert which time based
   * Auto-scaling supports two Alert types: metric and time based. Time based alerts are based on cron expressions and allow alerts to be triggered based on time.
   *
   * clusterId Long 
   * alertId Long 
   * body TimeAlertRequest  (optional)
   * returns TimeAlertResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cron" : "aeiou",
  "scalingPolicy" : {
    "adjustmentType" : "NODE_COUNT",
    "name" : "aeiou",
    "scalingAdjustment" : 1,
    "alertId" : 5,
    "hostGroup" : "aeiou"
  },
  "scalingPolicyId" : 6,
  "alertName" : "aeiou",
  "description" : "aeiou",
  "timeZone" : "aeiou",
  "id" : 0
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.validateCronExpression = function(args, res, next) {
  /**
   * cron expression validation
   * Auto-scaling supports two Alert types: metric and time based. Time based alerts are based on cron expressions and allow alerts to be triggered based on time.
   *
   * clusterId Long 
   * body TimeAlertValidationRequest  (optional)
   * returns Boolean
   **/
  var examples = {};
  examples['application/json'] = true;
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

