'use strict';

exports.getAccountUsage = function(args, res, next) {
  /**
   * retrieve public and private (owned) usages by filter parameters
   * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
   *
   * since Long  (optional)
   * filterenddate Long  (optional)
   * user String  (optional)
   * cloud String  (optional)
   * zone String  (optional)
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "owner" : "aeiou",
  "costs" : 1.4658129805029452,
  "instanceNum" : 5,
  "blueprintName" : "aeiou",
  "stackId" : 6,
  "instanceType" : "aeiou",
  "instanceHours" : 0,
  "stackName" : "aeiou",
  "peak" : 2,
  "instanceGroup" : "aeiou",
  "availabilityZone" : "aeiou",
  "blueprintId" : 5,
  "duration" : "aeiou",
  "provider" : "aeiou",
  "flexId" : "aeiou",
  "region" : "aeiou",
  "day" : "aeiou",
  "stackUuid" : "aeiou",
  "account" : "aeiou",
  "username" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getDailyFlexUsage = function(args, res, next) {
  /**
   * retrieve Flex related daily usages
   * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
   *
   * returns CloudbreakFlexUsage
   **/
  var examples = {};
  examples['application/json'] = {
  "controller" : {
    "instanceId" : "aeiou",
    "provider" : "aeiou",
    "smartSenseId" : "aeiou",
    "guid" : "aeiou",
    "region" : "aeiou",
    "userName" : "aeiou"
  },
  "products" : [ {
    "components" : [ {
      "componentId" : "aeiou",
      "instances" : [ {
        "usageDate" : "aeiou",
        "creationTime" : "aeiou",
        "provider" : "aeiou",
        "guid" : "aeiou",
        "flexSubscriptionId" : "aeiou",
        "region" : "aeiou",
        "peakUsage" : 0
      } ]
    } ],
    "productId" : "aeiou"
  } ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getDeployerUsage = function(args, res, next) {
  /**
   * retrieve usages by filter parameters
   * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
   *
   * since Long  (optional)
   * filterenddate Long  (optional)
   * user String  (optional)
   * account String  (optional)
   * cloud String  (optional)
   * zone String  (optional)
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "owner" : "aeiou",
  "costs" : 1.4658129805029452,
  "instanceNum" : 5,
  "blueprintName" : "aeiou",
  "stackId" : 6,
  "instanceType" : "aeiou",
  "instanceHours" : 0,
  "stackName" : "aeiou",
  "peak" : 2,
  "instanceGroup" : "aeiou",
  "availabilityZone" : "aeiou",
  "blueprintId" : 5,
  "duration" : "aeiou",
  "provider" : "aeiou",
  "flexId" : "aeiou",
  "region" : "aeiou",
  "day" : "aeiou",
  "stackUuid" : "aeiou",
  "account" : "aeiou",
  "username" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getLatestFlexUsage = function(args, res, next) {
  /**
   * retrieve Flex related latest usages, usages for the given day
   * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
   *
   * returns CloudbreakFlexUsage
   **/
  var examples = {};
  examples['application/json'] = {
  "controller" : {
    "instanceId" : "aeiou",
    "provider" : "aeiou",
    "smartSenseId" : "aeiou",
    "guid" : "aeiou",
    "region" : "aeiou",
    "userName" : "aeiou"
  },
  "products" : [ {
    "components" : [ {
      "componentId" : "aeiou",
      "instances" : [ {
        "usageDate" : "aeiou",
        "creationTime" : "aeiou",
        "provider" : "aeiou",
        "guid" : "aeiou",
        "flexSubscriptionId" : "aeiou",
        "region" : "aeiou",
        "peakUsage" : 0
      } ]
    } ],
    "productId" : "aeiou"
  } ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getUserUsage = function(args, res, next) {
  /**
   * retrieve private usages by filter parameters
   * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
   *
   * since Long  (optional)
   * filterenddate Long  (optional)
   * cloud String  (optional)
   * zone String  (optional)
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "owner" : "aeiou",
  "costs" : 1.4658129805029452,
  "instanceNum" : 5,
  "blueprintName" : "aeiou",
  "stackId" : 6,
  "instanceType" : "aeiou",
  "instanceHours" : 0,
  "stackName" : "aeiou",
  "peak" : 2,
  "instanceGroup" : "aeiou",
  "availabilityZone" : "aeiou",
  "blueprintId" : 5,
  "duration" : "aeiou",
  "provider" : "aeiou",
  "flexId" : "aeiou",
  "region" : "aeiou",
  "day" : "aeiou",
  "stackUuid" : "aeiou",
  "account" : "aeiou",
  "username" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

