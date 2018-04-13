'use strict';

exports.getHistory = function(args, res, next) {
  /**
   * retrieve full history
   * Get Auto-scaling history on a specific cluster
   *
   * clusterId Long 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "scalingStatus" : "FAILED",
  "cbStackId" : args.clusterId.value,
  "statusReason" : "aeiou",
  "alertType" : "METRIC",
  "adjustmentType" : "NODE_COUNT",
  "originalNodeCount" : 5,
  "adjustment" : 5,
  "id" : 0,
  "clusterId" : 6,
  "hostGroup" : "aeiou",
  "properties" : {
    "key" : "aeiou"
  },
  "timestamp" : 2
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getHistoryById = function(args, res, next) {
  /**
   * retrieve a specific history
   * Get Auto-scaling history on a specific cluster
   *
   * clusterId Long 
   * historyId Long 
   * returns HistoryResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "scalingStatus" : "FAILED",
  "cbStackId" : args.clusterId.value,
  "statusReason" : "aeiou",
  "alertType" : "METRIC",
  "adjustmentType" : "NODE_COUNT",
  "originalNodeCount" : 5,
  "adjustment" : 5,
  "id" : 0,
  "clusterId" : 6,
  "hostGroup" : "aeiou",
  "properties" : {
    "key" : "aeiou"
  },
  "timestamp" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

