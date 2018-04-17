'use strict';

var writer = require('../utils/writer');

/**
 * delete cluster on a specific stack
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * id Long 
 * withStackDelete Boolean  (optional)
 * deleteDependencies Boolean  (optional)
 * no response value expected for this operation
 **/
exports.deleteCluster = function(id,withStackDelete,deleteDependencies) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete instance resource from stack
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * stackId Long 
 * instanceId String 
 * no response value expected for this operation
 **/
exports.deleteInstanceStack = function(stackId,instanceId) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private stack by name
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * name String 
 * forced Boolean  (optional)
 * deleteDependencies Boolean  (optional)
 * no response value expected for this operation
 **/
exports.deletePrivateStack = function(name,forced,deleteDependencies) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private stack by name
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * name String 
 * forced Boolean  (optional)
 * deleteDependencies Boolean  (optional)
 * no response value expected for this operation
 **/
exports.deletePublicStack = function(name,forced,deleteDependencies) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete stack by id
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * id Long 
 * forced Boolean  (optional)
 * deleteDependencies Boolean  (optional)
 * no response value expected for this operation
 **/
exports.deleteStack = function(id,forced,deleteDependencies) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * failure report
 * Endpoint to report the failed nodes in the given cluster. If recovery mode for the node's hostgroup is AUTO then autorecovery would be started. If recovery mode for the node's hostgroup is MANUAL, the nodes will be marked as unhealthy.
 *
 * id Long 
 * body FailureReport  (optional)
 * no response value expected for this operation
 **/
exports.failureReportCluster = function(id,body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve all stacks
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * returns List
 **/
exports.getAllStack = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "owner" : "owner",
  "password" : "password",
  "ambariServerIp" : "ambariServerIp",
  "created" : 1,
  "stackId" : 0,
  "name" : "name",
  "userName" : "userName",
  "account" : "account",
  "gatewayPort" : 6,
  "clusterStatus" : "REQUESTED",
  "status" : "REQUESTED"
}, {
  "owner" : "owner",
  "password" : "password",
  "ambariServerIp" : "ambariServerIp",
  "created" : 1,
  "stackId" : 0,
  "name" : "name",
  "userName" : "userName",
  "account" : "account",
  "gatewayPort" : 6,
  "clusterStatus" : "REQUESTED",
  "status" : "REQUESTED"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieves the TLS certificate used by the gateway
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * id Long 
 * returns CertificateResponse
 **/
exports.getCertificateStack = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "clientCert" : [ "clientCert", "clientCert" ],
  "serverCert" : [ "serverCert", "serverCert" ],
  "clientKey" : [ "clientKey", "clientKey" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve cluster by stack id
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * id Long 
 * returns ClusterResponse
 **/
exports.getCluster = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : "cluster",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "baseUrl",
    "version" : "version",
    "gpgKeyUrl" : "gpgKeyUrl"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  }, {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  } ],
  "ambariServerIp" : "ambariServerIp",
  "ambariServerUrl" : "ambariServerUrl",
  "proxyName" : "proxyName",
  "description" : "description",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 5,
  "serviceEndPoints" : {
    "key" : "serviceEndPoints"
  },
  "statusReason" : "statusReason",
  "ambariStackDetails" : {
    "enableGplRepo" : false,
    "stack" : {
      "key" : "stack"
    },
    "util" : {
      "key" : "util"
    },
    "mpacks" : [ {
      "name" : "name"
    }, {
      "name" : "name"
    } ],
    "hdpVersion" : "hdpVersion",
    "verify" : false
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "name",
    "propertyValue" : "propertyValue"
  }, {
    "name" : "name",
    "propertyValue" : "propertyValue"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "password",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "name",
    "host" : "host",
    "userName" : "userName"
  },
  "id" : 5,
  "blueprintCustomProperties" : "blueprintCustomProperties",
  "executorType" : "CONTAINER",
  "extendedBlueprintText" : "extendedBlueprintText",
  "customQueue" : "customQueue",
  "userName" : "userName",
  "blueprintId" : 7,
  "ldapConfig" : {
    "adminGroup" : "adminGroup",
    "groupNameAttribute" : "groupNameAttribute",
    "groupMemberAttribute" : "groupMemberAttribute",
    "description" : "description",
    "userNameAttribute" : "userNameAttribute",
    "serverPort" : 5249,
    "serverHost" : "serverHost",
    "directoryType" : "LDAP",
    "bindDn" : "bindDn",
    "protocol" : "protocol",
    "groupSearchBase" : "groupSearchBase",
    "userSearchBase" : "userSearchBase",
    "public" : false,
    "domain" : "domain",
    "name" : "name",
    "id" : 6,
    "userObjectClass" : "userObjectClass",
    "groupObjectClass" : "groupObjectClass"
  },
  "uptime" : 6,
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "e30=",
    "inputs" : [ {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    }, {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    } ],
    "hostGroupCount" : 6,
    "name" : "name",
    "description" : "description",
    "id" : 0,
    "tags" : {
      "key" : "{}"
    },
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  }, {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "definitions"
    }
  },
  "creationFinished" : 1,
  "kerberosResponse" : {
    "krb5Conf" : "krb5Conf",
    "ldapUrl" : "ldapUrl",
    "admin" : "admin",
    "realm" : "realm",
    "containerDn" : "containerDn",
    "descriptor" : "descriptor",
    "adminUrl" : "adminUrl",
    "type" : "CB_MANAGED",
    "url" : "url",
    "tcpAllowed" : false
  },
  "name" : "name",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7, 7 ],
  "gateway" : {
    "path" : "path",
    "topologyName" : "topologyName",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "ssoProvider",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "tokenCert",
    "exposedServices" : [ "exposedServices", "exposedServices" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 2
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * get cluster properties with blueprint outputs
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * id Long 
 * body ConfigsRequest  (optional)
 * returns ConfigsResponse
 **/
exports.getConfigsCluster = function(id,body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "inputs" : [ {
    "name" : "name",
    "propertyValue" : "propertyValue"
  }, {
    "name" : "name",
    "propertyValue" : "propertyValue"
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
 * retrieve cluster by stack id
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * id Long 
 * returns AutoscaleClusterResponse
 **/
exports.getFullCluster = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : "cluster",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "baseUrl",
    "version" : "version",
    "gpgKeyUrl" : "gpgKeyUrl"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  }, {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  } ],
  "ambariServerIp" : "ambariServerIp",
  "ambariServerUrl" : "ambariServerUrl",
  "proxyName" : "proxyName",
  "description" : "description",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 6,
  "serviceEndPoints" : {
    "key" : "serviceEndPoints"
  },
  "password" : "password",
  "statusReason" : "statusReason",
  "ambariStackDetails" : {
    "enableGplRepo" : false,
    "stack" : {
      "key" : "stack"
    },
    "util" : {
      "key" : "util"
    },
    "mpacks" : [ {
      "name" : "name"
    }, {
      "name" : "name"
    } ],
    "hdpVersion" : "hdpVersion",
    "verify" : false
  },
  "ldapConfigId" : 2,
  "blueprintInputs" : [ {
    "name" : "name",
    "propertyValue" : "propertyValue"
  }, {
    "name" : "name",
    "propertyValue" : "propertyValue"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "password",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "name",
    "host" : "host",
    "userName" : "userName"
  },
  "id" : 0,
  "blueprintCustomProperties" : "blueprintCustomProperties",
  "executorType" : "CONTAINER",
  "extendedBlueprintText" : "extendedBlueprintText",
  "customQueue" : "customQueue",
  "userName" : "userName",
  "blueprintId" : 5,
  "ldapConfig" : {
    "adminGroup" : "adminGroup",
    "groupNameAttribute" : "groupNameAttribute",
    "groupMemberAttribute" : "groupMemberAttribute",
    "description" : "description",
    "userNameAttribute" : "userNameAttribute",
    "serverPort" : 5249,
    "serverHost" : "serverHost",
    "directoryType" : "LDAP",
    "bindDn" : "bindDn",
    "protocol" : "protocol",
    "groupSearchBase" : "groupSearchBase",
    "userSearchBase" : "userSearchBase",
    "public" : false,
    "domain" : "domain",
    "name" : "name",
    "id" : 6,
    "userObjectClass" : "userObjectClass",
    "groupObjectClass" : "groupObjectClass"
  },
  "uptime" : 9,
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "e30=",
    "inputs" : [ {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    }, {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    } ],
    "hostGroupCount" : 6,
    "name" : "name",
    "description" : "description",
    "id" : 0,
    "tags" : {
      "key" : "{}"
    },
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  }, {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "definitions"
    }
  },
  "creationFinished" : 7,
  "kerberosResponse" : {
    "krb5Conf" : "krb5Conf",
    "ldapUrl" : "ldapUrl",
    "admin" : "admin",
    "realm" : "realm",
    "containerDn" : "containerDn",
    "descriptor" : "descriptor",
    "adminUrl" : "adminUrl",
    "type" : "CB_MANAGED",
    "url" : "url",
    "tcpAllowed" : false
  },
  "name" : "name",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 5, 5 ],
  "gateway" : {
    "path" : "path",
    "topologyName" : "topologyName",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "ssoProvider",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "tokenCert",
    "exposedServices" : [ "exposedServices", "exposedServices" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 1
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve cluster by stack name (private)
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * name String 
 * returns ClusterResponse
 **/
exports.getPrivateCluster = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : "cluster",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "baseUrl",
    "version" : "version",
    "gpgKeyUrl" : "gpgKeyUrl"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  }, {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  } ],
  "ambariServerIp" : "ambariServerIp",
  "ambariServerUrl" : "ambariServerUrl",
  "proxyName" : "proxyName",
  "description" : "description",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 5,
  "serviceEndPoints" : {
    "key" : "serviceEndPoints"
  },
  "statusReason" : "statusReason",
  "ambariStackDetails" : {
    "enableGplRepo" : false,
    "stack" : {
      "key" : "stack"
    },
    "util" : {
      "key" : "util"
    },
    "mpacks" : [ {
      "name" : "name"
    }, {
      "name" : "name"
    } ],
    "hdpVersion" : "hdpVersion",
    "verify" : false
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "name",
    "propertyValue" : "propertyValue"
  }, {
    "name" : "name",
    "propertyValue" : "propertyValue"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "password",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "name",
    "host" : "host",
    "userName" : "userName"
  },
  "id" : 5,
  "blueprintCustomProperties" : "blueprintCustomProperties",
  "executorType" : "CONTAINER",
  "extendedBlueprintText" : "extendedBlueprintText",
  "customQueue" : "customQueue",
  "userName" : "userName",
  "blueprintId" : 7,
  "ldapConfig" : {
    "adminGroup" : "adminGroup",
    "groupNameAttribute" : "groupNameAttribute",
    "groupMemberAttribute" : "groupMemberAttribute",
    "description" : "description",
    "userNameAttribute" : "userNameAttribute",
    "serverPort" : 5249,
    "serverHost" : "serverHost",
    "directoryType" : "LDAP",
    "bindDn" : "bindDn",
    "protocol" : "protocol",
    "groupSearchBase" : "groupSearchBase",
    "userSearchBase" : "userSearchBase",
    "public" : false,
    "domain" : "domain",
    "name" : "name",
    "id" : 6,
    "userObjectClass" : "userObjectClass",
    "groupObjectClass" : "groupObjectClass"
  },
  "uptime" : 6,
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "e30=",
    "inputs" : [ {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    }, {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    } ],
    "hostGroupCount" : 6,
    "name" : "name",
    "description" : "description",
    "id" : 0,
    "tags" : {
      "key" : "{}"
    },
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  }, {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "definitions"
    }
  },
  "creationFinished" : 1,
  "kerberosResponse" : {
    "krb5Conf" : "krb5Conf",
    "ldapUrl" : "ldapUrl",
    "admin" : "admin",
    "realm" : "realm",
    "containerDn" : "containerDn",
    "descriptor" : "descriptor",
    "adminUrl" : "adminUrl",
    "type" : "CB_MANAGED",
    "url" : "url",
    "tcpAllowed" : false
  },
  "name" : "name",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7, 7 ],
  "gateway" : {
    "path" : "path",
    "topologyName" : "topologyName",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "ssoProvider",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "tokenCert",
    "exposedServices" : [ "exposedServices", "exposedServices" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 2
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private stack by name
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * name String 
 * entry List  (optional)
 * returns StackResponse
 **/
exports.getPrivateStack = function(name,entry) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : {
    "cluster" : "cluster",
    "ambariRepoDetailsJson" : {
      "baseUrl" : "baseUrl",
      "version" : "version",
      "gpgKeyUrl" : "gpgKeyUrl"
    },
    "hostGroups" : [ {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    }, {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    } ],
    "ambariServerIp" : "ambariServerIp",
    "ambariServerUrl" : "ambariServerUrl",
    "proxyName" : "proxyName",
    "description" : "description",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "serviceEndPoints"
    },
    "statusReason" : "statusReason",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "stack"
      },
      "util" : {
        "key" : "util"
      },
      "mpacks" : [ {
        "name" : "name"
      }, {
        "name" : "name"
      } ],
      "hdpVersion" : "hdpVersion",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "name",
      "propertyValue" : "propertyValue"
    }, {
      "name" : "name",
      "propertyValue" : "propertyValue"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "password",
      "port" : 1,
      "vendor" : "POSTGRES",
      "name" : "name",
      "host" : "host",
      "userName" : "userName"
    },
    "id" : 5,
    "blueprintCustomProperties" : "blueprintCustomProperties",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "extendedBlueprintText",
    "customQueue" : "customQueue",
    "userName" : "userName",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "adminGroup",
      "groupNameAttribute" : "groupNameAttribute",
      "groupMemberAttribute" : "groupMemberAttribute",
      "description" : "description",
      "userNameAttribute" : "userNameAttribute",
      "serverPort" : 5249,
      "serverHost" : "serverHost",
      "directoryType" : "LDAP",
      "bindDn" : "bindDn",
      "protocol" : "protocol",
      "groupSearchBase" : "groupSearchBase",
      "userSearchBase" : "userSearchBase",
      "public" : false,
      "domain" : "domain",
      "name" : "name",
      "id" : 6,
      "userObjectClass" : "userObjectClass",
      "groupObjectClass" : "groupObjectClass"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "e30=",
      "inputs" : [ {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      }, {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      } ],
      "hostGroupCount" : 6,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    }, {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "definitions"
      }
    },
    "creationFinished" : 1,
    "kerberosResponse" : {
      "krb5Conf" : "krb5Conf",
      "ldapUrl" : "ldapUrl",
      "admin" : "admin",
      "realm" : "realm",
      "containerDn" : "containerDn",
      "descriptor" : "descriptor",
      "adminUrl" : "adminUrl",
      "type" : "CB_MANAGED",
      "url" : "url",
      "tcpAllowed" : false
    },
    "name" : "name",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 7, 7 ],
    "gateway" : {
      "path" : "path",
      "topologyName" : "topologyName",
      "gatewayType" : "CENTRAL",
      "ssoProvider" : "ssoProvider",
      "enableGateway" : false,
      "ssoType" : "SSO_PROVIDER",
      "tokenCert" : "tokenCert",
      "exposedServices" : [ "exposedServices", "exposedServices" ]
    },
    "status" : "REQUESTED",
    "minutesUp" : 2
  },
  "hdpVersion" : "hdpVersion",
  "cloudbreakEvents" : [ {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  }, {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  } ],
  "platformVariant" : "platformVariant",
  "customHostname" : "customHostname",
  "userDefinedTags" : {
    "key" : "userDefinedTags"
  },
  "flexSubscription" : {
    "owner" : "owner",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 0,
    "usedForController" : false,
    "name" : "name",
    "smartSenseSubscription" : {
      "owner" : "owner",
      "publicInAccount" : false,
      "id" : 1,
      "autoGenerated" : false,
      "subscriptionId" : "subscriptionId",
      "account" : "account"
    },
    "id" : 6,
    "subscriptionId" : "subscriptionId",
    "account" : "account",
    "usedAsDefault" : false
  },
  "availabilityZone" : "availabilityZone",
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "network" : {
    "subnetCIDR" : "subnetCIDR",
    "cloudPlatform" : "cloudPlatform",
    "publicInAccount" : false,
    "topologyId" : 0,
    "name" : "name",
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "statusReason",
  "credential" : {
    "cloudPlatform" : "cloudPlatform",
    "public" : false,
    "name" : "name",
    "topologyId" : 0,
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 6,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 6,
    "id" : 1
  },
  "owner" : "owner",
  "applicationTags" : {
    "key" : "applicationTags"
  },
  "image" : {
    "imageName" : "imageName",
    "imageId" : "imageId",
    "imageCatalogUrl" : "imageCatalogUrl",
    "imageCatalogName" : "imageCatalogName"
  },
  "cloudbreakDetails" : {
    "version" : "version"
  },
  "cloudPlatform" : "cloudPlatform",
  "created" : 2,
  "customDomain" : "customDomain",
  "gatewayPort" : 6,
  "stackAuthentication" : {
    "loginUserName" : "loginUserName",
    "publicKey" : "publicKey",
    "publicKeyId" : "publicKeyId"
  },
  "orchestrator" : {
    "apiEndpoint" : "apiEndpoint",
    "type" : "type",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  }, {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  }, {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  } ],
  "name" : "name",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
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
  } ],
  "region" : "region",
  "ambariVersion" : "ambariVersion",
  "parameters" : {
    "key" : "parameters"
  },
  "account" : "account",
  "status" : "REQUESTED"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private stack
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * returns List
 **/
exports.getPrivatesStack = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "cluster" : {
    "cluster" : "cluster",
    "ambariRepoDetailsJson" : {
      "baseUrl" : "baseUrl",
      "version" : "version",
      "gpgKeyUrl" : "gpgKeyUrl"
    },
    "hostGroups" : [ {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    }, {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    } ],
    "ambariServerIp" : "ambariServerIp",
    "ambariServerUrl" : "ambariServerUrl",
    "proxyName" : "proxyName",
    "description" : "description",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "serviceEndPoints"
    },
    "statusReason" : "statusReason",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "stack"
      },
      "util" : {
        "key" : "util"
      },
      "mpacks" : [ {
        "name" : "name"
      }, {
        "name" : "name"
      } ],
      "hdpVersion" : "hdpVersion",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "name",
      "propertyValue" : "propertyValue"
    }, {
      "name" : "name",
      "propertyValue" : "propertyValue"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "password",
      "port" : 1,
      "vendor" : "POSTGRES",
      "name" : "name",
      "host" : "host",
      "userName" : "userName"
    },
    "id" : 5,
    "blueprintCustomProperties" : "blueprintCustomProperties",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "extendedBlueprintText",
    "customQueue" : "customQueue",
    "userName" : "userName",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "adminGroup",
      "groupNameAttribute" : "groupNameAttribute",
      "groupMemberAttribute" : "groupMemberAttribute",
      "description" : "description",
      "userNameAttribute" : "userNameAttribute",
      "serverPort" : 5249,
      "serverHost" : "serverHost",
      "directoryType" : "LDAP",
      "bindDn" : "bindDn",
      "protocol" : "protocol",
      "groupSearchBase" : "groupSearchBase",
      "userSearchBase" : "userSearchBase",
      "public" : false,
      "domain" : "domain",
      "name" : "name",
      "id" : 6,
      "userObjectClass" : "userObjectClass",
      "groupObjectClass" : "groupObjectClass"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "e30=",
      "inputs" : [ {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      }, {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      } ],
      "hostGroupCount" : 6,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    }, {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "definitions"
      }
    },
    "creationFinished" : 1,
    "kerberosResponse" : {
      "krb5Conf" : "krb5Conf",
      "ldapUrl" : "ldapUrl",
      "admin" : "admin",
      "realm" : "realm",
      "containerDn" : "containerDn",
      "descriptor" : "descriptor",
      "adminUrl" : "adminUrl",
      "type" : "CB_MANAGED",
      "url" : "url",
      "tcpAllowed" : false
    },
    "name" : "name",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 7, 7 ],
    "gateway" : {
      "path" : "path",
      "topologyName" : "topologyName",
      "gatewayType" : "CENTRAL",
      "ssoProvider" : "ssoProvider",
      "enableGateway" : false,
      "ssoType" : "SSO_PROVIDER",
      "tokenCert" : "tokenCert",
      "exposedServices" : [ "exposedServices", "exposedServices" ]
    },
    "status" : "REQUESTED",
    "minutesUp" : 2
  },
  "hdpVersion" : "hdpVersion",
  "cloudbreakEvents" : [ {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  }, {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  } ],
  "platformVariant" : "platformVariant",
  "customHostname" : "customHostname",
  "userDefinedTags" : {
    "key" : "userDefinedTags"
  },
  "flexSubscription" : {
    "owner" : "owner",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 0,
    "usedForController" : false,
    "name" : "name",
    "smartSenseSubscription" : {
      "owner" : "owner",
      "publicInAccount" : false,
      "id" : 1,
      "autoGenerated" : false,
      "subscriptionId" : "subscriptionId",
      "account" : "account"
    },
    "id" : 6,
    "subscriptionId" : "subscriptionId",
    "account" : "account",
    "usedAsDefault" : false
  },
  "availabilityZone" : "availabilityZone",
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "network" : {
    "subnetCIDR" : "subnetCIDR",
    "cloudPlatform" : "cloudPlatform",
    "publicInAccount" : false,
    "topologyId" : 0,
    "name" : "name",
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "statusReason",
  "credential" : {
    "cloudPlatform" : "cloudPlatform",
    "public" : false,
    "name" : "name",
    "topologyId" : 0,
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 6,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 6,
    "id" : 1
  },
  "owner" : "owner",
  "applicationTags" : {
    "key" : "applicationTags"
  },
  "image" : {
    "imageName" : "imageName",
    "imageId" : "imageId",
    "imageCatalogUrl" : "imageCatalogUrl",
    "imageCatalogName" : "imageCatalogName"
  },
  "cloudbreakDetails" : {
    "version" : "version"
  },
  "cloudPlatform" : "cloudPlatform",
  "created" : 2,
  "customDomain" : "customDomain",
  "gatewayPort" : 6,
  "stackAuthentication" : {
    "loginUserName" : "loginUserName",
    "publicKey" : "publicKey",
    "publicKeyId" : "publicKeyId"
  },
  "orchestrator" : {
    "apiEndpoint" : "apiEndpoint",
    "type" : "type",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  }, {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  }, {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  } ],
  "name" : "name",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
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
  } ],
  "region" : "region",
  "ambariVersion" : "ambariVersion",
  "parameters" : {
    "key" : "parameters"
  },
  "account" : "account",
  "status" : "REQUESTED"
}, {
  "cluster" : {
    "cluster" : "cluster",
    "ambariRepoDetailsJson" : {
      "baseUrl" : "baseUrl",
      "version" : "version",
      "gpgKeyUrl" : "gpgKeyUrl"
    },
    "hostGroups" : [ {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    }, {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    } ],
    "ambariServerIp" : "ambariServerIp",
    "ambariServerUrl" : "ambariServerUrl",
    "proxyName" : "proxyName",
    "description" : "description",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "serviceEndPoints"
    },
    "statusReason" : "statusReason",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "stack"
      },
      "util" : {
        "key" : "util"
      },
      "mpacks" : [ {
        "name" : "name"
      }, {
        "name" : "name"
      } ],
      "hdpVersion" : "hdpVersion",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "name",
      "propertyValue" : "propertyValue"
    }, {
      "name" : "name",
      "propertyValue" : "propertyValue"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "password",
      "port" : 1,
      "vendor" : "POSTGRES",
      "name" : "name",
      "host" : "host",
      "userName" : "userName"
    },
    "id" : 5,
    "blueprintCustomProperties" : "blueprintCustomProperties",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "extendedBlueprintText",
    "customQueue" : "customQueue",
    "userName" : "userName",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "adminGroup",
      "groupNameAttribute" : "groupNameAttribute",
      "groupMemberAttribute" : "groupMemberAttribute",
      "description" : "description",
      "userNameAttribute" : "userNameAttribute",
      "serverPort" : 5249,
      "serverHost" : "serverHost",
      "directoryType" : "LDAP",
      "bindDn" : "bindDn",
      "protocol" : "protocol",
      "groupSearchBase" : "groupSearchBase",
      "userSearchBase" : "userSearchBase",
      "public" : false,
      "domain" : "domain",
      "name" : "name",
      "id" : 6,
      "userObjectClass" : "userObjectClass",
      "groupObjectClass" : "groupObjectClass"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "e30=",
      "inputs" : [ {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      }, {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      } ],
      "hostGroupCount" : 6,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    }, {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "definitions"
      }
    },
    "creationFinished" : 1,
    "kerberosResponse" : {
      "krb5Conf" : "krb5Conf",
      "ldapUrl" : "ldapUrl",
      "admin" : "admin",
      "realm" : "realm",
      "containerDn" : "containerDn",
      "descriptor" : "descriptor",
      "adminUrl" : "adminUrl",
      "type" : "CB_MANAGED",
      "url" : "url",
      "tcpAllowed" : false
    },
    "name" : "name",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 7, 7 ],
    "gateway" : {
      "path" : "path",
      "topologyName" : "topologyName",
      "gatewayType" : "CENTRAL",
      "ssoProvider" : "ssoProvider",
      "enableGateway" : false,
      "ssoType" : "SSO_PROVIDER",
      "tokenCert" : "tokenCert",
      "exposedServices" : [ "exposedServices", "exposedServices" ]
    },
    "status" : "REQUESTED",
    "minutesUp" : 2
  },
  "hdpVersion" : "hdpVersion",
  "cloudbreakEvents" : [ {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  }, {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  } ],
  "platformVariant" : "platformVariant",
  "customHostname" : "customHostname",
  "userDefinedTags" : {
    "key" : "userDefinedTags"
  },
  "flexSubscription" : {
    "owner" : "owner",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 0,
    "usedForController" : false,
    "name" : "name",
    "smartSenseSubscription" : {
      "owner" : "owner",
      "publicInAccount" : false,
      "id" : 1,
      "autoGenerated" : false,
      "subscriptionId" : "subscriptionId",
      "account" : "account"
    },
    "id" : 6,
    "subscriptionId" : "subscriptionId",
    "account" : "account",
    "usedAsDefault" : false
  },
  "availabilityZone" : "availabilityZone",
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "network" : {
    "subnetCIDR" : "subnetCIDR",
    "cloudPlatform" : "cloudPlatform",
    "publicInAccount" : false,
    "topologyId" : 0,
    "name" : "name",
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "statusReason",
  "credential" : {
    "cloudPlatform" : "cloudPlatform",
    "public" : false,
    "name" : "name",
    "topologyId" : 0,
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 6,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 6,
    "id" : 1
  },
  "owner" : "owner",
  "applicationTags" : {
    "key" : "applicationTags"
  },
  "image" : {
    "imageName" : "imageName",
    "imageId" : "imageId",
    "imageCatalogUrl" : "imageCatalogUrl",
    "imageCatalogName" : "imageCatalogName"
  },
  "cloudbreakDetails" : {
    "version" : "version"
  },
  "cloudPlatform" : "cloudPlatform",
  "created" : 2,
  "customDomain" : "customDomain",
  "gatewayPort" : 6,
  "stackAuthentication" : {
    "loginUserName" : "loginUserName",
    "publicKey" : "publicKey",
    "publicKeyId" : "publicKeyId"
  },
  "orchestrator" : {
    "apiEndpoint" : "apiEndpoint",
    "type" : "type",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  }, {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  }, {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  } ],
  "name" : "name",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
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
  } ],
  "region" : "region",
  "ambariVersion" : "ambariVersion",
  "parameters" : {
    "key" : "parameters"
  },
  "account" : "account",
  "status" : "REQUESTED"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve cluster by stack name (public)
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * name String 
 * returns ClusterResponse
 **/
exports.getPublicCluster = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : "cluster",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "baseUrl",
    "version" : "version",
    "gpgKeyUrl" : "gpgKeyUrl"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  }, {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  } ],
  "ambariServerIp" : "ambariServerIp",
  "ambariServerUrl" : "ambariServerUrl",
  "proxyName" : "proxyName",
  "description" : "description",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 5,
  "serviceEndPoints" : {
    "key" : "serviceEndPoints"
  },
  "statusReason" : "statusReason",
  "ambariStackDetails" : {
    "enableGplRepo" : false,
    "stack" : {
      "key" : "stack"
    },
    "util" : {
      "key" : "util"
    },
    "mpacks" : [ {
      "name" : "name"
    }, {
      "name" : "name"
    } ],
    "hdpVersion" : "hdpVersion",
    "verify" : false
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "name",
    "propertyValue" : "propertyValue"
  }, {
    "name" : "name",
    "propertyValue" : "propertyValue"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "password",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "name",
    "host" : "host",
    "userName" : "userName"
  },
  "id" : 5,
  "blueprintCustomProperties" : "blueprintCustomProperties",
  "executorType" : "CONTAINER",
  "extendedBlueprintText" : "extendedBlueprintText",
  "customQueue" : "customQueue",
  "userName" : "userName",
  "blueprintId" : 7,
  "ldapConfig" : {
    "adminGroup" : "adminGroup",
    "groupNameAttribute" : "groupNameAttribute",
    "groupMemberAttribute" : "groupMemberAttribute",
    "description" : "description",
    "userNameAttribute" : "userNameAttribute",
    "serverPort" : 5249,
    "serverHost" : "serverHost",
    "directoryType" : "LDAP",
    "bindDn" : "bindDn",
    "protocol" : "protocol",
    "groupSearchBase" : "groupSearchBase",
    "userSearchBase" : "userSearchBase",
    "public" : false,
    "domain" : "domain",
    "name" : "name",
    "id" : 6,
    "userObjectClass" : "userObjectClass",
    "groupObjectClass" : "groupObjectClass"
  },
  "uptime" : 6,
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "e30=",
    "inputs" : [ {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    }, {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    } ],
    "hostGroupCount" : 6,
    "name" : "name",
    "description" : "description",
    "id" : 0,
    "tags" : {
      "key" : "{}"
    },
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  }, {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "definitions"
    }
  },
  "creationFinished" : 1,
  "kerberosResponse" : {
    "krb5Conf" : "krb5Conf",
    "ldapUrl" : "ldapUrl",
    "admin" : "admin",
    "realm" : "realm",
    "containerDn" : "containerDn",
    "descriptor" : "descriptor",
    "adminUrl" : "adminUrl",
    "type" : "CB_MANAGED",
    "url" : "url",
    "tcpAllowed" : false
  },
  "name" : "name",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7, 7 ],
  "gateway" : {
    "path" : "path",
    "topologyName" : "topologyName",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "ssoProvider",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "tokenCert",
    "exposedServices" : [ "exposedServices", "exposedServices" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 2
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) stack by name
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * name String 
 * entry List  (optional)
 * returns StackResponse
 **/
exports.getPublicStack = function(name,entry) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/stacks/openstack.json');
    if (Object.keys(examples).length > 0) {
        switch(name){
            case 'az404':
                reject(writer.respondWithCode(404, JSON.stringify({"message":"Stack 'az404' not found"})));
                break;
            default:
                resolve(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
        }
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) stacks
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * returns List
 **/
exports.getPublicsStack = function() {
  return new Promise(function(resolve, reject) {
    var openstack_data = require('../responses/stacks/openstack.json');
    var aws_data = require('../responses/stacks/aws.json');
    var azure_data = require('../responses/stacks/azure.json');
    var gcp_data = require('../responses/stacks/gcp.json');
    var response_array = [];

    response_array.push(openstack_data,aws_data,azure_data,gcp_data);
    var examples = {};
    examples['application/json'] = response_array;
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve stack by id
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * id Long 
 * entry List  (optional)
 * returns StackResponse
 **/
exports.getStack = function(id,entry) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : {
    "cluster" : "cluster",
    "ambariRepoDetailsJson" : {
      "baseUrl" : "baseUrl",
      "version" : "version",
      "gpgKeyUrl" : "gpgKeyUrl"
    },
    "hostGroups" : [ {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    }, {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    } ],
    "ambariServerIp" : "ambariServerIp",
    "ambariServerUrl" : "ambariServerUrl",
    "proxyName" : "proxyName",
    "description" : "description",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "serviceEndPoints"
    },
    "statusReason" : "statusReason",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "stack"
      },
      "util" : {
        "key" : "util"
      },
      "mpacks" : [ {
        "name" : "name"
      }, {
        "name" : "name"
      } ],
      "hdpVersion" : "hdpVersion",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "name",
      "propertyValue" : "propertyValue"
    }, {
      "name" : "name",
      "propertyValue" : "propertyValue"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "password",
      "port" : 1,
      "vendor" : "POSTGRES",
      "name" : "name",
      "host" : "host",
      "userName" : "userName"
    },
    "id" : 5,
    "blueprintCustomProperties" : "blueprintCustomProperties",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "extendedBlueprintText",
    "customQueue" : "customQueue",
    "userName" : "userName",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "adminGroup",
      "groupNameAttribute" : "groupNameAttribute",
      "groupMemberAttribute" : "groupMemberAttribute",
      "description" : "description",
      "userNameAttribute" : "userNameAttribute",
      "serverPort" : 5249,
      "serverHost" : "serverHost",
      "directoryType" : "LDAP",
      "bindDn" : "bindDn",
      "protocol" : "protocol",
      "groupSearchBase" : "groupSearchBase",
      "userSearchBase" : "userSearchBase",
      "public" : false,
      "domain" : "domain",
      "name" : "name",
      "id" : 6,
      "userObjectClass" : "userObjectClass",
      "groupObjectClass" : "groupObjectClass"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "e30=",
      "inputs" : [ {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      }, {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      } ],
      "hostGroupCount" : 6,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    }, {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "definitions"
      }
    },
    "creationFinished" : 1,
    "kerberosResponse" : {
      "krb5Conf" : "krb5Conf",
      "ldapUrl" : "ldapUrl",
      "admin" : "admin",
      "realm" : "realm",
      "containerDn" : "containerDn",
      "descriptor" : "descriptor",
      "adminUrl" : "adminUrl",
      "type" : "CB_MANAGED",
      "url" : "url",
      "tcpAllowed" : false
    },
    "name" : "name",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 7, 7 ],
    "gateway" : {
      "path" : "path",
      "topologyName" : "topologyName",
      "gatewayType" : "CENTRAL",
      "ssoProvider" : "ssoProvider",
      "enableGateway" : false,
      "ssoType" : "SSO_PROVIDER",
      "tokenCert" : "tokenCert",
      "exposedServices" : [ "exposedServices", "exposedServices" ]
    },
    "status" : "REQUESTED",
    "minutesUp" : 2
  },
  "hdpVersion" : "hdpVersion",
  "cloudbreakEvents" : [ {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  }, {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  } ],
  "platformVariant" : "platformVariant",
  "customHostname" : "customHostname",
  "userDefinedTags" : {
    "key" : "userDefinedTags"
  },
  "flexSubscription" : {
    "owner" : "owner",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 0,
    "usedForController" : false,
    "name" : "name",
    "smartSenseSubscription" : {
      "owner" : "owner",
      "publicInAccount" : false,
      "id" : 1,
      "autoGenerated" : false,
      "subscriptionId" : "subscriptionId",
      "account" : "account"
    },
    "id" : 6,
    "subscriptionId" : "subscriptionId",
    "account" : "account",
    "usedAsDefault" : false
  },
  "availabilityZone" : "availabilityZone",
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "network" : {
    "subnetCIDR" : "subnetCIDR",
    "cloudPlatform" : "cloudPlatform",
    "publicInAccount" : false,
    "topologyId" : 0,
    "name" : "name",
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "statusReason",
  "credential" : {
    "cloudPlatform" : "cloudPlatform",
    "public" : false,
    "name" : "name",
    "topologyId" : 0,
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 6,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 6,
    "id" : 1
  },
  "owner" : "owner",
  "applicationTags" : {
    "key" : "applicationTags"
  },
  "image" : {
    "imageName" : "imageName",
    "imageId" : "imageId",
    "imageCatalogUrl" : "imageCatalogUrl",
    "imageCatalogName" : "imageCatalogName"
  },
  "cloudbreakDetails" : {
    "version" : "version"
  },
  "cloudPlatform" : "cloudPlatform",
  "created" : 2,
  "customDomain" : "customDomain",
  "gatewayPort" : 6,
  "stackAuthentication" : {
    "loginUserName" : "loginUserName",
    "publicKey" : "publicKey",
    "publicKeyId" : "publicKeyId"
  },
  "orchestrator" : {
    "apiEndpoint" : "apiEndpoint",
    "type" : "type",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  }, {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  }, {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  } ],
  "name" : "name",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
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
  } ],
  "region" : "region",
  "ambariVersion" : "ambariVersion",
  "parameters" : {
    "key" : "parameters"
  },
  "account" : "account",
  "status" : "REQUESTED"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve stack by ambari address
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * body AmbariAddress  (optional)
 * returns StackResponse
 **/
exports.getStackForAmbari = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : {
    "cluster" : "cluster",
    "ambariRepoDetailsJson" : {
      "baseUrl" : "baseUrl",
      "version" : "version",
      "gpgKeyUrl" : "gpgKeyUrl"
    },
    "hostGroups" : [ {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    }, {
      "recipes" : [ {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      }, {
        "recipeType" : "PRE_AMBARI_START",
        "public" : false,
        "name" : "name",
        "description" : "description",
        "id" : 0,
        "uri" : "uri",
        "content" : "content"
      } ],
      "metadata" : [ {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      }, {
        "groupName" : "groupName",
        "name" : "name",
        "id" : 4,
        "state" : "state"
      } ],
      "recipeIds" : [ 3, 3 ],
      "recoveryMode" : "MANUAL",
      "name" : "name",
      "constraint" : {
        "hostCount" : 9,
        "constraintTemplateName" : "constraintTemplateName",
        "instanceGroupName" : "instanceGroupName"
      },
      "id" : 2
    } ],
    "ambariServerIp" : "ambariServerIp",
    "ambariServerUrl" : "ambariServerUrl",
    "proxyName" : "proxyName",
    "description" : "description",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "serviceEndPoints"
    },
    "statusReason" : "statusReason",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "stack"
      },
      "util" : {
        "key" : "util"
      },
      "mpacks" : [ {
        "name" : "name"
      }, {
        "name" : "name"
      } ],
      "hdpVersion" : "hdpVersion",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "name",
      "propertyValue" : "propertyValue"
    }, {
      "name" : "name",
      "propertyValue" : "propertyValue"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "password",
      "port" : 1,
      "vendor" : "POSTGRES",
      "name" : "name",
      "host" : "host",
      "userName" : "userName"
    },
    "id" : 5,
    "blueprintCustomProperties" : "blueprintCustomProperties",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "extendedBlueprintText",
    "customQueue" : "customQueue",
    "userName" : "userName",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "adminGroup",
      "groupNameAttribute" : "groupNameAttribute",
      "groupMemberAttribute" : "groupMemberAttribute",
      "description" : "description",
      "userNameAttribute" : "userNameAttribute",
      "serverPort" : 5249,
      "serverHost" : "serverHost",
      "directoryType" : "LDAP",
      "bindDn" : "bindDn",
      "protocol" : "protocol",
      "groupSearchBase" : "groupSearchBase",
      "userSearchBase" : "userSearchBase",
      "public" : false,
      "domain" : "domain",
      "name" : "name",
      "id" : 6,
      "userObjectClass" : "userObjectClass",
      "groupObjectClass" : "groupObjectClass"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "e30=",
      "inputs" : [ {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      }, {
        "referenceConfiguration" : "referenceConfiguration",
        "name" : "name",
        "description" : "description"
      } ],
      "hostGroupCount" : 6,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    }, {
      "connectionDriver" : "connectionDriver",
      "databaseEngineDisplayName" : "databaseEngineDisplayName",
      "clusterNames" : [ "clusterNames", "clusterNames" ],
      "connectorJarUrl" : "connectorJarUrl",
      "publicInAccount" : false,
      "stackVersion" : "stackVersion",
      "name" : "name",
      "connectionURL" : "connectionURL",
      "id" : 0,
      "type" : "type",
      "creationDate" : 6,
      "databaseEngine" : "databaseEngine"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "definitions"
      }
    },
    "creationFinished" : 1,
    "kerberosResponse" : {
      "krb5Conf" : "krb5Conf",
      "ldapUrl" : "ldapUrl",
      "admin" : "admin",
      "realm" : "realm",
      "containerDn" : "containerDn",
      "descriptor" : "descriptor",
      "adminUrl" : "adminUrl",
      "type" : "CB_MANAGED",
      "url" : "url",
      "tcpAllowed" : false
    },
    "name" : "name",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 7, 7 ],
    "gateway" : {
      "path" : "path",
      "topologyName" : "topologyName",
      "gatewayType" : "CENTRAL",
      "ssoProvider" : "ssoProvider",
      "enableGateway" : false,
      "ssoType" : "SSO_PROVIDER",
      "tokenCert" : "tokenCert",
      "exposedServices" : [ "exposedServices", "exposedServices" ]
    },
    "status" : "REQUESTED",
    "minutesUp" : 2
  },
  "hdpVersion" : "hdpVersion",
  "cloudbreakEvents" : [ {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  }, {
    "eventMessage" : "eventMessage",
    "owner" : "owner",
    "blueprintName" : "blueprintName",
    "stackId" : 5,
    "stackName" : "stackName",
    "stackStatus" : "REQUESTED",
    "eventType" : "eventType",
    "clusterId" : 1,
    "instanceGroup" : "instanceGroup",
    "availabilityZone" : "availabilityZone",
    "blueprintId" : 6,
    "cloud" : "cloud",
    "clusterName" : "clusterName",
    "nodeCount" : 5,
    "region" : "region",
    "account" : "account",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 0
  } ],
  "platformVariant" : "platformVariant",
  "customHostname" : "customHostname",
  "userDefinedTags" : {
    "key" : "userDefinedTags"
  },
  "flexSubscription" : {
    "owner" : "owner",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 0,
    "usedForController" : false,
    "name" : "name",
    "smartSenseSubscription" : {
      "owner" : "owner",
      "publicInAccount" : false,
      "id" : 1,
      "autoGenerated" : false,
      "subscriptionId" : "subscriptionId",
      "account" : "account"
    },
    "id" : 6,
    "subscriptionId" : "subscriptionId",
    "account" : "account",
    "usedAsDefault" : false
  },
  "availabilityZone" : "availabilityZone",
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "network" : {
    "subnetCIDR" : "subnetCIDR",
    "cloudPlatform" : "cloudPlatform",
    "publicInAccount" : false,
    "topologyId" : 0,
    "name" : "name",
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "statusReason",
  "credential" : {
    "cloudPlatform" : "cloudPlatform",
    "public" : false,
    "name" : "name",
    "topologyId" : 0,
    "description" : "description",
    "id" : 6,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 6,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 6,
    "id" : 1
  },
  "owner" : "owner",
  "applicationTags" : {
    "key" : "applicationTags"
  },
  "image" : {
    "imageName" : "imageName",
    "imageId" : "imageId",
    "imageCatalogUrl" : "imageCatalogUrl",
    "imageCatalogName" : "imageCatalogName"
  },
  "cloudbreakDetails" : {
    "version" : "version"
  },
  "cloudPlatform" : "cloudPlatform",
  "created" : 2,
  "customDomain" : "customDomain",
  "gatewayPort" : 6,
  "stackAuthentication" : {
    "loginUserName" : "loginUserName",
    "publicKey" : "publicKey",
    "publicKeyId" : "publicKeyId"
  },
  "orchestrator" : {
    "apiEndpoint" : "apiEndpoint",
    "type" : "type",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  }, {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "volumeType",
      "cloudPlatform" : "cloudPlatform",
      "public" : false,
      "instanceType" : "instanceType",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 9
      },
      "topologyId" : 9,
      "name" : "name",
      "description" : "description",
      "volumeCount" : 8,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 9
    },
    "metadata" : [ {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    }, {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 49652,
    "securityGroup" : {
      "securityGroupId" : "securityGroupId",
      "owner" : "owner",
      "cloudPlatform" : "cloudPlatform",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      }, {
        "subnet" : "subnet",
        "protocol" : "protocol",
        "id" : 6,
        "ports" : "ports",
        "modifiable" : false
      } ],
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "account" : "account"
    },
    "id" : 6,
    "templateId" : 7,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "group"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  }, {
    "instanceMetaData" : {
      "sshPort" : 3,
      "instanceId" : "instanceId",
      "ambariServer" : false,
      "privateIp" : "privateIp",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "discoveryFQDN",
      "publicIp" : "publicIp",
      "instanceGroup" : "instanceGroup",
      "instanceStatus" : "REQUESTED"
    },
    "hostMetadata" : {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }
  } ],
  "name" : "name",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
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
  } ],
  "region" : "region",
  "ambariVersion" : "ambariVersion",
  "parameters" : {
    "key" : "parameters"
  },
  "account" : "account",
  "status" : "REQUESTED"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create cluster for stack
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * id Long 
 * body ClusterRequest  (optional)
 * returns ClusterResponse
 **/
exports.postCluster = function(id,body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cluster" : "cluster",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "baseUrl",
    "version" : "version",
    "gpgKeyUrl" : "gpgKeyUrl"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  }, {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    }, {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "name",
      "description" : "description",
      "id" : 0,
      "uri" : "uri",
      "content" : "content"
    } ],
    "metadata" : [ {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    }, {
      "groupName" : "groupName",
      "name" : "name",
      "id" : 4,
      "state" : "state"
    } ],
    "recipeIds" : [ 3, 3 ],
    "recoveryMode" : "MANUAL",
    "name" : "name",
    "constraint" : {
      "hostCount" : 9,
      "constraintTemplateName" : "constraintTemplateName",
      "instanceGroupName" : "instanceGroupName"
    },
    "id" : 2
  } ],
  "ambariServerIp" : "ambariServerIp",
  "ambariServerUrl" : "ambariServerUrl",
  "proxyName" : "proxyName",
  "description" : "description",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 5,
  "serviceEndPoints" : {
    "key" : "serviceEndPoints"
  },
  "statusReason" : "statusReason",
  "ambariStackDetails" : {
    "enableGplRepo" : false,
    "stack" : {
      "key" : "stack"
    },
    "util" : {
      "key" : "util"
    },
    "mpacks" : [ {
      "name" : "name"
    }, {
      "name" : "name"
    } ],
    "hdpVersion" : "hdpVersion",
    "verify" : false
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "name",
    "propertyValue" : "propertyValue"
  }, {
    "name" : "name",
    "propertyValue" : "propertyValue"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "password",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "name",
    "host" : "host",
    "userName" : "userName"
  },
  "id" : 5,
  "blueprintCustomProperties" : "blueprintCustomProperties",
  "executorType" : "CONTAINER",
  "extendedBlueprintText" : "extendedBlueprintText",
  "customQueue" : "customQueue",
  "userName" : "userName",
  "blueprintId" : 7,
  "ldapConfig" : {
    "adminGroup" : "adminGroup",
    "groupNameAttribute" : "groupNameAttribute",
    "groupMemberAttribute" : "groupMemberAttribute",
    "description" : "description",
    "userNameAttribute" : "userNameAttribute",
    "serverPort" : 5249,
    "serverHost" : "serverHost",
    "directoryType" : "LDAP",
    "bindDn" : "bindDn",
    "protocol" : "protocol",
    "groupSearchBase" : "groupSearchBase",
    "userSearchBase" : "userSearchBase",
    "public" : false,
    "domain" : "domain",
    "name" : "name",
    "id" : 6,
    "userObjectClass" : "userObjectClass",
    "groupObjectClass" : "groupObjectClass"
  },
  "uptime" : 6,
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "e30=",
    "inputs" : [ {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    }, {
      "referenceConfiguration" : "referenceConfiguration",
      "name" : "name",
      "description" : "description"
    } ],
    "hostGroupCount" : 6,
    "name" : "name",
    "description" : "description",
    "id" : 0,
    "tags" : {
      "key" : "{}"
    },
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  }, {
    "connectionDriver" : "connectionDriver",
    "databaseEngineDisplayName" : "databaseEngineDisplayName",
    "clusterNames" : [ "clusterNames", "clusterNames" ],
    "connectorJarUrl" : "connectorJarUrl",
    "publicInAccount" : false,
    "stackVersion" : "stackVersion",
    "name" : "name",
    "connectionURL" : "connectionURL",
    "id" : 0,
    "type" : "type",
    "creationDate" : 6,
    "databaseEngine" : "databaseEngine"
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "definitions"
    }
  },
  "creationFinished" : 1,
  "kerberosResponse" : {
    "krb5Conf" : "krb5Conf",
    "ldapUrl" : "ldapUrl",
    "admin" : "admin",
    "realm" : "realm",
    "containerDn" : "containerDn",
    "descriptor" : "descriptor",
    "adminUrl" : "adminUrl",
    "type" : "CB_MANAGED",
    "url" : "url",
    "tcpAllowed" : false
  },
  "name" : "name",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7, 7 ],
  "gateway" : {
    "path" : "path",
    "topologyName" : "topologyName",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "ssoProvider",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "tokenCert",
    "exposedServices" : [ "exposedServices", "exposedServices" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 2
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * update cluster by stack id
 * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
 *
 * id Long 
 * body UpdateCluster  (optional)
 * no response value expected for this operation
 **/
exports.putCluster = function(id,body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * update stack by id
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * id Long 
 * body UpdateStack  (optional)
 * no response value expected for this operation
 **/
exports.putStack = function(id,body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * repair the cluster
 * Removing the failed nodes and starting new nodes to substitute them.
 *
 * id Long 
 * body ClusterRepairRequest  (optional)
 * no response value expected for this operation
 **/
exports.repairCluster = function(id,body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve stack status by stack id
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * id Long 
 * returns Map
 **/
exports.statusStack = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : "{}"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * upgrade the Ambari version
 * Ambari is used to provision the Hadoop clusters.
 *
 * id Long 
 * body AmbariRepoDetails  (optional)
 * no response value expected for this operation
 **/
exports.upgradeCluster = function(id,body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * validate stack
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * body StackValidationRequest  (optional)
 * no response value expected for this operation
 **/
exports.validateStack = function(body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve available platform variants
 * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
 *
 * returns PlatformVariantsJson
 **/
exports.variantsStack = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "platformToVariants" : {
    "key" : [ "platformToVariants", "platformToVariants" ]
  },
  "defaultVariants" : {
    "key" : "defaultVariants"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

