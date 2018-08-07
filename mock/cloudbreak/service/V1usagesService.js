'use strict';


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
exports.getAccountUsage = function(since,filterenddate,user,cloud,zone) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "owner" : "owner",
  "costs" : 3.353193347011243,
  "instanceNum" : 7,
  "blueprintName" : "blueprintName",
  "stackId" : 6,
  "instanceType" : "instanceType",
  "instanceHours" : 5,
  "stackName" : "stackName",
  "peak" : 0,
  "instanceGroup" : "instanceGroup",
  "availabilityZone" : "availabilityZone",
  "blueprintId" : 3,
  "duration" : "duration",
  "provider" : "provider",
  "flexId" : "flexId",
  "region" : "region",
  "day" : "day",
  "stackUuid" : "stackUuid",
  "account" : "account",
  "username" : "username"
}, {
  "owner" : "owner",
  "costs" : 3.353193347011243,
  "instanceNum" : 7,
  "blueprintName" : "blueprintName",
  "stackId" : 6,
  "instanceType" : "instanceType",
  "instanceHours" : 5,
  "stackName" : "stackName",
  "peak" : 0,
  "instanceGroup" : "instanceGroup",
  "availabilityZone" : "availabilityZone",
  "blueprintId" : 3,
  "duration" : "duration",
  "provider" : "provider",
  "flexId" : "flexId",
  "region" : "region",
  "day" : "day",
  "stackUuid" : "stackUuid",
  "account" : "account",
  "username" : "username"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve Flex related daily usages
 * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
 *
 * returns CloudbreakFlexUsage
 **/
exports.getDailyFlexUsage = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "controller" : {
    "instanceId" : "instanceId",
    "provider" : "provider",
    "smartSenseId" : "smartSenseId",
    "guid" : "guid",
    "region" : "region",
    "userName" : "userName"
  },
  "products" : [ {
    "components" : [ {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    }, {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    } ],
    "productId" : "productId"
  }, {
    "components" : [ {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    }, {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    } ],
    "productId" : "productId"
  } ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


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
exports.getDeployerUsage = function(since,filterenddate,user,account,cloud,zone) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "owner" : "owner",
  "costs" : 3.353193347011243,
  "instanceNum" : 7,
  "blueprintName" : "blueprintName",
  "stackId" : 6,
  "instanceType" : "instanceType",
  "instanceHours" : 5,
  "stackName" : "stackName",
  "peak" : 0,
  "instanceGroup" : "instanceGroup",
  "availabilityZone" : "availabilityZone",
  "blueprintId" : 3,
  "duration" : "duration",
  "provider" : "provider",
  "flexId" : "flexId",
  "region" : "region",
  "day" : "day",
  "stackUuid" : "stackUuid",
  "account" : "account",
  "username" : "username"
}, {
  "owner" : "owner",
  "costs" : 3.353193347011243,
  "instanceNum" : 7,
  "blueprintName" : "blueprintName",
  "stackId" : 6,
  "instanceType" : "instanceType",
  "instanceHours" : 5,
  "stackName" : "stackName",
  "peak" : 0,
  "instanceGroup" : "instanceGroup",
  "availabilityZone" : "availabilityZone",
  "blueprintId" : 3,
  "duration" : "duration",
  "provider" : "provider",
  "flexId" : "flexId",
  "region" : "region",
  "day" : "day",
  "stackUuid" : "stackUuid",
  "account" : "account",
  "username" : "username"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve Flex related latest usages, usages for the given day
 * Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc)
 *
 * returns CloudbreakFlexUsage
 **/
exports.getLatestFlexUsage = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "controller" : {
    "instanceId" : "instanceId",
    "provider" : "provider",
    "smartSenseId" : "smartSenseId",
    "guid" : "guid",
    "region" : "region",
    "userName" : "userName"
  },
  "products" : [ {
    "components" : [ {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    }, {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    } ],
    "productId" : "productId"
  }, {
    "components" : [ {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    }, {
      "componentId" : "componentId",
      "instances" : [ {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      }, {
        "usageDate" : "usageDate",
        "creationTime" : "creationTime",
        "provider" : "provider",
        "guid" : "guid",
        "flexSubscriptionId" : "flexSubscriptionId",
        "region" : "region",
        "peakUsage" : 0
      } ]
    } ],
    "productId" : "productId"
  } ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


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
exports.getUserUsage = function(since,filterenddate,cloud,zone) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "owner" : "owner",
  "costs" : 3.353193347011243,
  "instanceNum" : 7,
  "blueprintName" : "blueprintName",
  "stackId" : 6,
  "instanceType" : "instanceType",
  "instanceHours" : 5,
  "stackName" : "stackName",
  "peak" : 0,
  "instanceGroup" : "instanceGroup",
  "availabilityZone" : "availabilityZone",
  "blueprintId" : 3,
  "duration" : "duration",
  "provider" : "provider",
  "flexId" : "flexId",
  "region" : "region",
  "day" : "day",
  "stackUuid" : "stackUuid",
  "account" : "account",
  "username" : "username"
}, {
  "owner" : "owner",
  "costs" : 3.353193347011243,
  "instanceNum" : 7,
  "blueprintName" : "blueprintName",
  "stackId" : 6,
  "instanceType" : "instanceType",
  "instanceHours" : 5,
  "stackName" : "stackName",
  "peak" : 0,
  "instanceGroup" : "instanceGroup",
  "availabilityZone" : "availabilityZone",
  "blueprintId" : 3,
  "duration" : "duration",
  "provider" : "provider",
  "flexId" : "flexId",
  "region" : "region",
  "day" : "day",
  "stackUuid" : "stackUuid",
  "account" : "account",
  "username" : "username"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

