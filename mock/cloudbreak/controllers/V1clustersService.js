'use strict';

exports.deleteCluster = function(args, res, next) {
  /**
   * delete cluster on a specific stack
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * id Long 
   * withStackDelete Boolean  (optional)
   * deleteDependencies Boolean  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.failureReportCluster = function(args, res, next) {
  /**
   * failure report
   * Endpoint to report the failed nodes in the given cluster. If recovery mode for the node's hostgroup is AUTO then autorecovery would be started. If recovery mode for the node's hostgroup is MANUAL, the nodes will be marked as unhealthy.
   *
   * id Long 
   * body FailureReport  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.getCluster = function(args, res, next) {
  /**
   * retrieve cluster by stack id
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * id Long 
   * returns ClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : "aeiou",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "aeiou",
    "version" : "aeiou",
    "gpgKeyUrl" : "aeiou"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 2,
      "uri" : "aeiou",
      "content" : "aeiou"
    } ],
    "metadata" : [ {
      "groupName" : "aeiou",
      "name" : "aeiou",
      "id" : 4,
      "state" : "aeiou"
    } ],
    "recipeIds" : [ 9 ],
    "recoveryMode" : "MANUAL",
    "name" : "aeiou",
    "constraint" : {
      "hostCount" : 7,
      "constraintTemplateName" : "aeiou",
      "instanceGroupName" : "aeiou"
    },
    "id" : 3
  } ],
  "ambariServerIp" : "aeiou",
  "ambariServerUrl" : "aeiou",
  "description" : "aeiou",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 6,
  "serviceEndPoints" : {
    "key" : "aeiou"
  },
  "statusReason" : "aeiou",
  "ambariStackDetails" : {
    "stack" : {
      "key" : "aeiou"
    },
    "util" : {
      "key" : "aeiou"
    },
    "hdpVersion" : "aeiou",
    "verify" : false,
    "knox" : {
      "key" : "aeiou"
    }
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "aeiou",
    "propertyValue" : "aeiou"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "aeiou",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "aeiou",
    "host" : "aeiou",
    "userName" : "aeiou"
  },
  "id" : 0,
  "blueprintCustomProperties" : "aeiou",
  "executorType" : "CONTAINER",
  "customQueue" : "aeiou",
  "userName" : "aeiou",
  "blueprintId" : 5,
  "ldapConfig" : {
    "adminGroup" : "aeiou",
    "groupNameAttribute" : "aeiou",
    "groupMemberAttribute" : "aeiou",
    "description" : "aeiou",
    "userNameAttribute" : "aeiou",
    "serverPort" : 44871,
    "serverHost" : "aeiou",
    "directoryType" : "LDAP",
    "bindDn" : "aeiou",
    "protocol" : "aeiou",
    "groupSearchBase" : "aeiou",
    "userSearchBase" : "aeiou",
    "public" : false,
    "domain" : "aeiou",
    "name" : "aeiou",
    "id" : 7,
    "userObjectClass" : "aeiou",
    "groupObjectClass" : "aeiou"
  },
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "aeiou",
    "inputs" : [ {
      "referenceConfiguration" : "aeiou",
      "name" : "aeiou",
      "description" : "aeiou"
    } ],
    "hostGroupCount" : 2,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 5,
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "databaseType" : "POSTGRES",
    "clusterNames" : [ "aeiou" ],
    "validated" : false,
    "publicInAccount" : false,
    "hdpVersion" : "aeiou",
    "name" : "aeiou",
    "connectionURL" : "aeiou",
    "id" : 1,
    "type" : "HIVE",
    "creationDate" : 1,
    "properties" : [ {
      "name" : "aeiou",
      "value" : "aeiou"
    } ]
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "aeiou"
    }
  },
  "creationFinished" : 4,
  "name" : "aeiou",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7 ],
  "gateway" : {
    "path" : "aeiou",
    "topologyName" : "aeiou",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "aeiou",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "aeiou",
    "exposedServices" : [ "aeiou" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 1
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getConfigsCluster = function(args, res, next) {
  /**
   * get cluster properties with blueprint outputs
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * id Long 
   * body ConfigsRequest  (optional)
   * returns ConfigsResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "inputs" : [ {
    "name" : "aeiou",
    "propertyValue" : "aeiou"
  } ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getFullCluster = function(args, res, next) {
  /**
   * retrieve cluster by stack id
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * id Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : "aeiou",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "aeiou",
    "version" : "aeiou",
    "gpgKeyUrl" : "aeiou"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 2,
      "uri" : "aeiou",
      "content" : "aeiou"
    } ],
    "metadata" : [ {
      "groupName" : "aeiou",
      "name" : "aeiou",
      "id" : 4,
      "state" : "aeiou"
    } ],
    "recipeIds" : [ 9 ],
    "recoveryMode" : "MANUAL",
    "name" : "aeiou",
    "constraint" : {
      "hostCount" : 7,
      "constraintTemplateName" : "aeiou",
      "instanceGroupName" : "aeiou"
    },
    "id" : 3
  } ],
  "ambariServerIp" : "aeiou",
  "ambariServerUrl" : "aeiou",
  "description" : "aeiou",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 6,
  "serviceEndPoints" : {
    "key" : "aeiou"
  },
  "password" : "aeiou",
  "statusReason" : "aeiou",
  "ambariStackDetails" : {
    "stack" : {
      "key" : "aeiou"
    },
    "util" : {
      "key" : "aeiou"
    },
    "hdpVersion" : "aeiou",
    "verify" : false,
    "knox" : {
      "key" : "aeiou"
    }
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "aeiou",
    "propertyValue" : "aeiou"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "aeiou",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "aeiou",
    "host" : "aeiou",
    "userName" : "aeiou"
  },
  "id" : 0,
  "blueprintCustomProperties" : "aeiou",
  "executorType" : "CONTAINER",
  "customQueue" : "aeiou",
  "userName" : "aeiou",
  "blueprintId" : 5,
  "ldapConfig" : {
    "adminGroup" : "aeiou",
    "groupNameAttribute" : "aeiou",
    "groupMemberAttribute" : "aeiou",
    "description" : "aeiou",
    "userNameAttribute" : "aeiou",
    "serverPort" : 44871,
    "serverHost" : "aeiou",
    "directoryType" : "LDAP",
    "bindDn" : "aeiou",
    "protocol" : "aeiou",
    "groupSearchBase" : "aeiou",
    "userSearchBase" : "aeiou",
    "public" : false,
    "domain" : "aeiou",
    "name" : "aeiou",
    "id" : 7,
    "userObjectClass" : "aeiou",
    "groupObjectClass" : "aeiou"
  },
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "aeiou",
    "inputs" : [ {
      "referenceConfiguration" : "aeiou",
      "name" : "aeiou",
      "description" : "aeiou"
    } ],
    "hostGroupCount" : 2,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 5,
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "databaseType" : "POSTGRES",
    "clusterNames" : [ "aeiou" ],
    "validated" : false,
    "publicInAccount" : false,
    "hdpVersion" : "aeiou",
    "name" : "aeiou",
    "connectionURL" : "aeiou",
    "id" : 1,
    "type" : "HIVE",
    "creationDate" : 1,
    "properties" : [ {
      "name" : "aeiou",
      "value" : "aeiou"
    } ]
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "aeiou"
    }
  },
  "creationFinished" : 4,
  "name" : "aeiou",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7 ],
  "gateway" : {
    "path" : "aeiou",
    "topologyName" : "aeiou",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "aeiou",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "aeiou",
    "exposedServices" : [ "aeiou" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 1
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateCluster = function(args, res, next) {
  /**
   * retrieve cluster by stack name (private)
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * name String 
   * returns ClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : "aeiou",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "aeiou",
    "version" : "aeiou",
    "gpgKeyUrl" : "aeiou"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 2,
      "uri" : "aeiou",
      "content" : "aeiou"
    } ],
    "metadata" : [ {
      "groupName" : "aeiou",
      "name" : "aeiou",
      "id" : 4,
      "state" : "aeiou"
    } ],
    "recipeIds" : [ 9 ],
    "recoveryMode" : "MANUAL",
    "name" : "aeiou",
    "constraint" : {
      "hostCount" : 7,
      "constraintTemplateName" : "aeiou",
      "instanceGroupName" : "aeiou"
    },
    "id" : 3
  } ],
  "ambariServerIp" : "aeiou",
  "ambariServerUrl" : "aeiou",
  "description" : "aeiou",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 6,
  "serviceEndPoints" : {
    "key" : "aeiou"
  },
  "statusReason" : "aeiou",
  "ambariStackDetails" : {
    "stack" : {
      "key" : "aeiou"
    },
    "util" : {
      "key" : "aeiou"
    },
    "hdpVersion" : "aeiou",
    "verify" : false,
    "knox" : {
      "key" : "aeiou"
    }
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "aeiou",
    "propertyValue" : "aeiou"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "aeiou",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "aeiou",
    "host" : "aeiou",
    "userName" : "aeiou"
  },
  "id" : 0,
  "blueprintCustomProperties" : "aeiou",
  "executorType" : "CONTAINER",
  "customQueue" : "aeiou",
  "userName" : "aeiou",
  "blueprintId" : 5,
  "ldapConfig" : {
    "adminGroup" : "aeiou",
    "groupNameAttribute" : "aeiou",
    "groupMemberAttribute" : "aeiou",
    "description" : "aeiou",
    "userNameAttribute" : "aeiou",
    "serverPort" : 44871,
    "serverHost" : "aeiou",
    "directoryType" : "LDAP",
    "bindDn" : "aeiou",
    "protocol" : "aeiou",
    "groupSearchBase" : "aeiou",
    "userSearchBase" : "aeiou",
    "public" : false,
    "domain" : "aeiou",
    "name" : "aeiou",
    "id" : 7,
    "userObjectClass" : "aeiou",
    "groupObjectClass" : "aeiou"
  },
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "aeiou",
    "inputs" : [ {
      "referenceConfiguration" : "aeiou",
      "name" : "aeiou",
      "description" : "aeiou"
    } ],
    "hostGroupCount" : 2,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 5,
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "databaseType" : "POSTGRES",
    "clusterNames" : [ "aeiou" ],
    "validated" : false,
    "publicInAccount" : false,
    "hdpVersion" : "aeiou",
    "name" : "aeiou",
    "connectionURL" : "aeiou",
    "id" : 1,
    "type" : "HIVE",
    "creationDate" : 1,
    "properties" : [ {
      "name" : "aeiou",
      "value" : "aeiou"
    } ]
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "aeiou"
    }
  },
  "creationFinished" : 4,
  "name" : "aeiou",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7 ],
  "gateway" : {
    "path" : "aeiou",
    "topologyName" : "aeiou",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "aeiou",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "aeiou",
    "exposedServices" : [ "aeiou" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 1
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicCluster = function(args, res, next) {
  /**
   * retrieve cluster by stack name (public)
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * name String 
   * returns ClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : "aeiou",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "aeiou",
    "version" : "aeiou",
    "gpgKeyUrl" : "aeiou"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 2,
      "uri" : "aeiou",
      "content" : "aeiou"
    } ],
    "metadata" : [ {
      "groupName" : "aeiou",
      "name" : "aeiou",
      "id" : 4,
      "state" : "aeiou"
    } ],
    "recipeIds" : [ 9 ],
    "recoveryMode" : "MANUAL",
    "name" : "aeiou",
    "constraint" : {
      "hostCount" : 7,
      "constraintTemplateName" : "aeiou",
      "instanceGroupName" : "aeiou"
    },
    "id" : 3
  } ],
  "ambariServerIp" : "aeiou",
  "ambariServerUrl" : "aeiou",
  "description" : "aeiou",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 6,
  "serviceEndPoints" : {
    "key" : "aeiou"
  },
  "statusReason" : "aeiou",
  "ambariStackDetails" : {
    "stack" : {
      "key" : "aeiou"
    },
    "util" : {
      "key" : "aeiou"
    },
    "hdpVersion" : "aeiou",
    "verify" : false,
    "knox" : {
      "key" : "aeiou"
    }
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "aeiou",
    "propertyValue" : "aeiou"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "aeiou",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "aeiou",
    "host" : "aeiou",
    "userName" : "aeiou"
  },
  "id" : 0,
  "blueprintCustomProperties" : "aeiou",
  "executorType" : "CONTAINER",
  "customQueue" : "aeiou",
  "userName" : "aeiou",
  "blueprintId" : 5,
  "ldapConfig" : {
    "adminGroup" : "aeiou",
    "groupNameAttribute" : "aeiou",
    "groupMemberAttribute" : "aeiou",
    "description" : "aeiou",
    "userNameAttribute" : "aeiou",
    "serverPort" : 44871,
    "serverHost" : "aeiou",
    "directoryType" : "LDAP",
    "bindDn" : "aeiou",
    "protocol" : "aeiou",
    "groupSearchBase" : "aeiou",
    "userSearchBase" : "aeiou",
    "public" : false,
    "domain" : "aeiou",
    "name" : "aeiou",
    "id" : 7,
    "userObjectClass" : "aeiou",
    "groupObjectClass" : "aeiou"
  },
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "aeiou",
    "inputs" : [ {
      "referenceConfiguration" : "aeiou",
      "name" : "aeiou",
      "description" : "aeiou"
    } ],
    "hostGroupCount" : 2,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 5,
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "databaseType" : "POSTGRES",
    "clusterNames" : [ "aeiou" ],
    "validated" : false,
    "publicInAccount" : false,
    "hdpVersion" : "aeiou",
    "name" : "aeiou",
    "connectionURL" : "aeiou",
    "id" : 1,
    "type" : "HIVE",
    "creationDate" : 1,
    "properties" : [ {
      "name" : "aeiou",
      "value" : "aeiou"
    } ]
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "aeiou"
    }
  },
  "creationFinished" : 4,
  "name" : "aeiou",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7 ],
  "gateway" : {
    "path" : "aeiou",
    "topologyName" : "aeiou",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "aeiou",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "aeiou",
    "exposedServices" : [ "aeiou" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 1
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postCluster = function(args, res, next) {
  /**
   * create cluster for stack
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * id Long 
   * body ClusterRequest  (optional)
   * returns ClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : "aeiou",
  "ambariRepoDetailsJson" : {
    "baseUrl" : "aeiou",
    "version" : "aeiou",
    "gpgKeyUrl" : "aeiou"
  },
  "hostGroups" : [ {
    "recipes" : [ {
      "recipeType" : "PRE_AMBARI_START",
      "public" : false,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 2,
      "uri" : "aeiou",
      "content" : "aeiou"
    } ],
    "metadata" : [ {
      "groupName" : "aeiou",
      "name" : "aeiou",
      "id" : 4,
      "state" : "aeiou"
    } ],
    "recipeIds" : [ 9 ],
    "recoveryMode" : "MANUAL",
    "name" : "aeiou",
    "constraint" : {
      "hostCount" : 7,
      "constraintTemplateName" : "aeiou",
      "instanceGroupName" : "aeiou"
    },
    "id" : 3
  } ],
  "ambariServerIp" : "aeiou",
  "ambariServerUrl" : "aeiou",
  "description" : "aeiou",
  "secure" : false,
  "configStrategy" : "NEVER_APPLY",
  "hoursUp" : 6,
  "serviceEndPoints" : {
    "key" : "aeiou"
  },
  "statusReason" : "aeiou",
  "ambariStackDetails" : {
    "stack" : {
      "key" : "aeiou"
    },
    "util" : {
      "key" : "aeiou"
    },
    "hdpVersion" : "aeiou",
    "verify" : false,
    "knox" : {
      "key" : "aeiou"
    }
  },
  "ldapConfigId" : 1,
  "blueprintInputs" : [ {
    "name" : "aeiou",
    "propertyValue" : "aeiou"
  } ],
  "ambariDatabaseDetails" : {
    "password" : "aeiou",
    "port" : 1,
    "vendor" : "POSTGRES",
    "name" : "aeiou",
    "host" : "aeiou",
    "userName" : "aeiou"
  },
  "id" : 0,
  "blueprintCustomProperties" : "aeiou",
  "executorType" : "CONTAINER",
  "customQueue" : "aeiou",
  "userName" : "aeiou",
  "blueprintId" : 5,
  "ldapConfig" : {
    "adminGroup" : "aeiou",
    "groupNameAttribute" : "aeiou",
    "groupMemberAttribute" : "aeiou",
    "description" : "aeiou",
    "userNameAttribute" : "aeiou",
    "serverPort" : 44871,
    "serverHost" : "aeiou",
    "directoryType" : "LDAP",
    "bindDn" : "aeiou",
    "protocol" : "aeiou",
    "groupSearchBase" : "aeiou",
    "userSearchBase" : "aeiou",
    "public" : false,
    "domain" : "aeiou",
    "name" : "aeiou",
    "id" : 7,
    "userObjectClass" : "aeiou",
    "groupObjectClass" : "aeiou"
  },
  "blueprint" : {
    "public" : false,
    "ambariBlueprint" : "aeiou",
    "inputs" : [ {
      "referenceConfiguration" : "aeiou",
      "name" : "aeiou",
      "description" : "aeiou"
    } ],
    "hostGroupCount" : 2,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 5,
    "status" : "DEFAULT"
  },
  "rdsConfigs" : [ {
    "databaseType" : "POSTGRES",
    "clusterNames" : [ "aeiou" ],
    "validated" : false,
    "publicInAccount" : false,
    "hdpVersion" : "aeiou",
    "name" : "aeiou",
    "connectionURL" : "aeiou",
    "id" : 1,
    "type" : "HIVE",
    "creationDate" : 1,
    "properties" : [ {
      "name" : "aeiou",
      "value" : "aeiou"
    } ]
  } ],
  "customContainers" : {
    "definitions" : {
      "key" : "aeiou"
    }
  },
  "creationFinished" : 4,
  "name" : "aeiou",
  "attributes" : {
    "key" : "{}"
  },
  "rdsConfigIds" : [ 7 ],
  "gateway" : {
    "path" : "aeiou",
    "topologyName" : "aeiou",
    "gatewayType" : "CENTRAL",
    "ssoProvider" : "aeiou",
    "enableGateway" : false,
    "ssoType" : "SSO_PROVIDER",
    "tokenCert" : "aeiou",
    "exposedServices" : [ "aeiou" ]
  },
  "status" : "REQUESTED",
  "minutesUp" : 1
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.putCluster = function(args, res, next) {
  /**
   * update cluster by stack id
   * Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack. Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
   *
   * id Long 
   * body UpdateCluster  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.repairCluster = function(args, res, next) {
  /**
   * repair the cluster
   * Removing the failed nodes and starting new nodes to substitute them.
   *
   * id Long 
   * body ClusterRepairRequest  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.upgradeCluster = function(args, res, next) {
  /**
   * upgrade the Ambari version
   * Ambari is used to provision the Hadoop clusters.
   *
   * id Long 
   * body AmbariRepoDetails  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

