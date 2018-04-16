'use strict';

exports.deleteInstanceStackV2 = function(args, res, next) {
  /**
   * delete instance resource from stack
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * stackId Long 
   * instanceId String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePrivateStackV2 = function(args, res, next) {
  /**
   * delete private stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * forced Boolean  (optional)
   * deleteDependencies Boolean  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicStackV2 = function(args, res, next) {
  /**
   * delete public (owned) or private stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * forced Boolean  (optional)
   * deleteDependencies Boolean  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteStackV2 = function(args, res, next) {
  /**
   * delete stack by id
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * id Long 
   * forced Boolean  (optional)
   * deleteDependencies Boolean  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.getAllStackV2 = function(args, res, next) {
  /**
   * retrieve all stacks
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "owner" : "aeiou",
  "password" : "aeiou",
  "ambariServerIp" : "aeiou",
  "created" : 1,
  "stackId" : 0,
  "name" : "aeiou",
  "userName" : "aeiou",
  "account" : "aeiou",
  "gatewayPort" : 6,
  "clusterStatus" : "REQUESTED",
  "status" : "REQUESTED"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getCertificateStackV2 = function(args, res, next) {
  /**
   * retrieves the TLS certificate used by the gateway
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * id Long 
   * returns CertificateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "clientCert" : [ "aeiou" ],
  "serverCert" : [ "aeiou" ],
  "clientKey" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getClusterRequestFromName = function(args, res, next) {
  /**
   * retrieve stack request by stack name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * returns StackV2Request
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : {
    "fileSystem" : {
      "name" : "aeiou",
      "defaultFs" : false,
      "type" : "WASB_INTEGRATED",
      "properties" : {
        "key" : "aeiou"
      }
    },
    "executorType" : "CONTAINER",
    "emailNeeded" : false,
    "proxyName" : "aeiou",
    "sharedService" : {
      "sharedCluster" : "aeiou"
    },
    "ambari" : {
      "ambariRepoDetailsJson" : {
        "baseUrl" : "aeiou",
        "version" : "aeiou",
        "gpgKeyUrl" : "aeiou"
      },
      "blueprintCustomProperties" : "aeiou",
      "ambariSecurityMasterKey" : "aeiou",
      "blueprintName" : "aeiou",
      "kerberos" : {
        "principal" : "aeiou",
        "password" : "aeiou",
        "krb5Conf" : "aeiou",
        "masterKey" : "aeiou",
        "ldapUrl" : "aeiou",
        "admin" : "aeiou",
        "realm" : "aeiou",
        "containerDn" : "aeiou",
        "descriptor" : "aeiou",
        "adminUrl" : "aeiou",
        "url" : "aeiou",
        "tcpAllowed" : false
      },
      "userName" : "aeiou",
      "blueprintId" : 9,
      "connectedCluster" : {
        "sourceClusterName" : "aeiou",
        "sourceClusterId" : 2
      },
      "configStrategy" : "NEVER_APPLY",
      "enableSecurity" : false,
      "password" : "aeiou",
      "validateBlueprint" : false,
      "ambariStackDetails" : {
        "enableGplRepo" : false,
        "stack" : "aeiou",
        "os" : "aeiou",
        "stackRepoId" : "aeiou",
        "utilsRepoId" : "aeiou",
        "mpackUrl" : "aeiou",
        "version" : "aeiou",
        "stackBaseURL" : "aeiou",
        "versionDefinitionFileUrl" : "aeiou",
        "repositoryVersion" : "aeiou",
        "utilsBaseURL" : "aeiou",
        "mpacks" : [ {
          "preInstalled" : false,
          "name" : "aeiou",
          "description" : "aeiou",
          "purge" : false,
          "force" : false,
          "stackDefault" : false,
          "mpackUrl" : "aeiou",
          "purgeList" : [ "aeiou" ]
        } ],
        "verify" : false
      },
      "blueprintInputs" : [ {
        "name" : "aeiou",
        "propertyValue" : "aeiou"
      } ],
      "ambariDatabaseDetails" : {
        "password" : "aeiou",
        "port" : 3,
        "vendor" : "POSTGRES",
        "name" : "aeiou",
        "host" : "aeiou",
        "userName" : "aeiou"
      },
      "gateway" : {
        "path" : "aeiou",
        "topologyName" : "aeiou",
        "gatewayType" : "CENTRAL",
        "ssoProvider" : "aeiou",
        "enableGateway" : false,
        "ssoType" : "SSO_PROVIDER",
        "tokenCert" : "aeiou",
        "exposedServices" : [ "aeiou" ]
      }
    },
    "ldapConfigName" : "aeiou",
    "rdsConfigNames" : [ "aeiou" ],
    "emailTo" : "aeiou"
  },
  "hdpVersion" : "aeiou",
  "platformVariant" : "aeiou",
  "imageSettings" : {
    "imageId" : "aeiou",
    "imageCatalog" : "aeiou"
  },
  "customDomain" : {
    "hostgroupNameAsHostname" : false,
    "customHostname" : "aeiou",
    "clusterNameAsSubdomain" : false,
    "customDomain" : "aeiou"
  },
  "stackAuthentication" : {
    "loginUserName" : "aeiou",
    "publicKey" : "aeiou",
    "publicKeyId" : "aeiou"
  },
  "tags" : {
    "applicationTags" : {
      "key" : "aeiou"
    },
    "userDefinedTags" : {
      "key" : "aeiou"
    },
    "defaultTags" : {
      "key" : "aeiou"
    }
  },
  "network" : {
    "subnetCIDR" : "aeiou",
    "parameters" : {
      "key" : "{}"
    }
  },
  "general" : {
    "name" : args.name.value,
    "credentialName" : "aeiou"
  },
  "instanceGroups" : [ {
    "template" : {
      "volumeType" : "aeiou",
      "instanceType" : "aeiou",
      "customInstanceType" : {
        "memory" : 5,
        "cpus" : 5
      },
      "volumeCount" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 1
    },
    "recipeNames" : [ "aeiou" ],
    "recoveryMode" : "MANUAL",
    "nodeCount" : 8008,
    "securityGroup" : {
      "securityGroupId" : "aeiou",
      "securityRules" : [ {
        "subnet" : "aeiou",
        "protocol" : "aeiou",
        "ports" : "aeiou",
        "modifiable" : false
      } ]
    },
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "aeiou"
  } ],
  "placement" : {
    "region" : "aeiou",
    "availabilityZone" : "aeiou"
  },
  "flexId" : 7,
  "ambariVersion" : "aeiou",
  "parameters" : {
    "key" : "{}"
  },
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 2
  }
};
  if (Object.keys(examples).length > 0) {
      res.setHeader('Content-Type', 'application/json');
      switch(args.name.value){
          case 'openstack-cluster':
              var responseJson = require('../responses/stacks/openstack.json');
              res.end(JSON.stringify(responseJson));
              break;
          case 'aws-cluster':
              var responseJson = require('../responses/stacks/aws.json');
              res.end(JSON.stringify(responseJson));
              break;
          case 'azure-cluster':
              var responseJson = require('../responses/stacks/azure.json');
              res.end(JSON.stringify(responseJson));
              break;
          case 'gcp-cluster':
              var responseJson = require('../responses/stacks/gcp.json');
              res.end(JSON.stringify(responseJson));
              break;
          default:
              res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
      }
  } else {
      res.end();
  }
}

exports.getPrivateStackV2 = function(args, res, next) {
  /**
   * retrieve a private stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * entry List  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : {
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
        "id" : 1,
        "uri" : "aeiou",
        "content" : "aeiou"
      } ],
      "metadata" : [ {
        "groupName" : "aeiou",
        "name" : "aeiou",
        "id" : 1,
        "state" : "aeiou"
      } ],
      "recipeIds" : [ 4 ],
      "recoveryMode" : "MANUAL",
      "name" : "aeiou",
      "constraint" : {
        "hostCount" : 2,
        "constraintTemplateName" : "aeiou",
        "instanceGroupName" : "aeiou"
      },
      "id" : 7
    } ],
    "ambariServerIp" : "aeiou",
    "ambariServerUrl" : "aeiou",
    "proxyName" : "aeiou",
    "description" : "aeiou",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "aeiou"
    },
    "statusReason" : "aeiou",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "aeiou"
      },
      "util" : {
        "key" : "aeiou"
      },
      "mpacks" : [ {
        "preInstalled" : false,
        "name" : "aeiou",
        "description" : "aeiou",
        "purge" : false,
        "force" : false,
        "stackDefault" : false,
        "mpackUrl" : "aeiou",
        "purgeList" : [ "aeiou" ]
      } ],
      "hdpVersion" : "aeiou",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "aeiou",
      "propertyValue" : "aeiou"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "aeiou",
      "port" : 9,
      "vendor" : "POSTGRES",
      "name" : "aeiou",
      "host" : "aeiou",
      "userName" : "aeiou"
    },
    "id" : 5,
    "blueprintCustomProperties" : "aeiou",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "aeiou",
    "customQueue" : "aeiou",
    "userName" : "aeiou",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "aeiou",
      "groupNameAttribute" : "aeiou",
      "groupMemberAttribute" : "aeiou",
      "description" : "aeiou",
      "userNameAttribute" : "aeiou",
      "serverPort" : 32540,
      "serverHost" : "aeiou",
      "directoryType" : "LDAP",
      "bindDn" : "aeiou",
      "protocol" : "aeiou",
      "groupSearchBase" : "aeiou",
      "userSearchBase" : "aeiou",
      "public" : false,
      "domain" : "aeiou",
      "name" : "aeiou",
      "id" : 5,
      "userObjectClass" : "aeiou",
      "groupObjectClass" : "aeiou"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "aeiou",
      "inputs" : [ {
        "referenceConfiguration" : "aeiou",
        "name" : "aeiou",
        "description" : "aeiou"
      } ],
      "hostGroupCount" : 3,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 9,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "aeiou",
      "databaseEngineDisplayName" : "aeiou",
      "clusterNames" : [ "aeiou" ],
      "connectorJarUrl" : "aeiou",
      "publicInAccount" : false,
      "stackVersion" : "aeiou",
      "name" : "aeiou",
      "connectionURL" : "aeiou",
      "id" : 6,
      "type" : "aeiou",
      "creationDate" : 7,
      "databaseEngine" : "aeiou"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "aeiou"
      }
    },
    "creationFinished" : 9,
    "kerberosResponse" : {
      "krb5Conf" : "aeiou",
      "ldapUrl" : "aeiou",
      "admin" : "aeiou",
      "realm" : "aeiou",
      "containerDn" : "aeiou",
      "descriptor" : "aeiou",
      "adminUrl" : "aeiou",
      "type" : "CB_MANAGED",
      "url" : "aeiou",
      "tcpAllowed" : false
    },
    "name" : "aeiou",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 1 ],
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
    "minutesUp" : 2
  },
  "hdpVersion" : "aeiou",
  "cloudbreakEvents" : [ {
    "eventMessage" : "aeiou",
    "owner" : "aeiou",
    "blueprintName" : "aeiou",
    "stackId" : 6,
    "stackName" : "aeiou",
    "stackStatus" : "REQUESTED",
    "eventType" : "aeiou",
    "clusterId" : 0,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 4,
    "cloud" : "aeiou",
    "clusterName" : "aeiou",
    "nodeCount" : 4,
    "region" : "aeiou",
    "account" : "aeiou",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 3
  } ],
  "platformVariant" : "aeiou",
  "customHostname" : "aeiou",
  "userDefinedTags" : {
    "key" : "aeiou"
  },
  "flexSubscription" : {
    "owner" : "aeiou",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 3,
    "usedForController" : false,
    "name" : "aeiou",
    "smartSenseSubscription" : {
      "owner" : "aeiou",
      "publicInAccount" : false,
      "id" : 7,
      "autoGenerated" : false,
      "subscriptionId" : "aeiou",
      "account" : "aeiou"
    },
    "id" : 3,
    "subscriptionId" : "aeiou",
    "account" : "aeiou",
    "usedAsDefault" : false
  },
  "availabilityZone" : "aeiou",
  "defaultTags" : {
    "key" : "aeiou"
  },
  "network" : {
    "subnetCIDR" : "aeiou",
    "cloudPlatform" : "aeiou",
    "publicInAccount" : false,
    "topologyId" : 6,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 3,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "aeiou",
  "credential" : {
    "cloudPlatform" : "aeiou",
    "public" : false,
    "name" : "aeiou",
    "topologyId" : 8,
    "description" : "aeiou",
    "id" : 9,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 5,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 0,
    "id" : 4
  },
  "owner" : "aeiou",
  "applicationTags" : {
    "key" : "aeiou"
  },
  "image" : {
    "imageName" : "aeiou",
    "imageId" : "aeiou",
    "imageCatalogUrl" : "aeiou",
    "imageCatalogName" : "aeiou"
  },
  "cloudbreakDetails" : {
    "version" : "aeiou"
  },
  "cloudPlatform" : "aeiou",
  "created" : 8,
  "customDomain" : "aeiou",
  "gatewayPort" : 7,
  "stackAuthentication" : {
    "loginUserName" : "aeiou",
    "publicKey" : "aeiou",
    "publicKeyId" : "aeiou"
  },
  "orchestrator" : {
    "apiEndpoint" : "aeiou",
    "type" : "aeiou",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "aeiou",
      "cloudPlatform" : "aeiou",
      "public" : false,
      "instanceType" : "aeiou",
      "customInstanceType" : {
        "memory" : 6,
        "cpus" : 6
      },
      "topologyId" : 5,
      "name" : "aeiou",
      "description" : "aeiou",
      "volumeCount" : 3,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 3
    },
    "metadata" : [ {
      "sshPort" : 6,
      "instanceId" : "aeiou",
      "ambariServer" : false,
      "privateIp" : "aeiou",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "aeiou",
      "publicIp" : "aeiou",
      "instanceGroup" : "aeiou",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 28841,
    "securityGroup" : {
      "securityGroupId" : "aeiou",
      "owner" : "aeiou",
      "cloudPlatform" : "aeiou",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "aeiou",
        "protocol" : "aeiou",
        "id" : 0,
        "ports" : "aeiou",
        "modifiable" : false
      } ],
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 7,
      "account" : "aeiou"
    },
    "id" : 7,
    "templateId" : 6,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "aeiou"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : "",
    "hostMetadata" : ""
  } ],
  "name" : "aeiou",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
    "owner" : "aeiou",
    "costs" : 7.260521264802104,
    "instanceNum" : 0,
    "blueprintName" : "aeiou",
    "stackId" : 4,
    "instanceType" : "aeiou",
    "instanceHours" : 1,
    "stackName" : "aeiou",
    "peak" : 9,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 9,
    "duration" : "aeiou",
    "provider" : "aeiou",
    "flexId" : "aeiou",
    "region" : "aeiou",
    "day" : "aeiou",
    "stackUuid" : "aeiou",
    "account" : "aeiou",
    "username" : "aeiou"
  } ],
  "region" : "aeiou",
  "ambariVersion" : "aeiou",
  "parameters" : {
    "key" : "aeiou"
  },
  "account" : "aeiou",
  "status" : "REQUESTED"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesStackV2 = function(args, res, next) {
  /**
   * retrieve private stack
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "cluster" : {
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
        "id" : 1,
        "uri" : "aeiou",
        "content" : "aeiou"
      } ],
      "metadata" : [ {
        "groupName" : "aeiou",
        "name" : "aeiou",
        "id" : 1,
        "state" : "aeiou"
      } ],
      "recipeIds" : [ 4 ],
      "recoveryMode" : "MANUAL",
      "name" : "aeiou",
      "constraint" : {
        "hostCount" : 2,
        "constraintTemplateName" : "aeiou",
        "instanceGroupName" : "aeiou"
      },
      "id" : 7
    } ],
    "ambariServerIp" : "aeiou",
    "ambariServerUrl" : "aeiou",
    "proxyName" : "aeiou",
    "description" : "aeiou",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "aeiou"
    },
    "statusReason" : "aeiou",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "aeiou"
      },
      "util" : {
        "key" : "aeiou"
      },
      "mpacks" : [ {
        "preInstalled" : false,
        "name" : "aeiou",
        "description" : "aeiou",
        "purge" : false,
        "force" : false,
        "stackDefault" : false,
        "mpackUrl" : "aeiou",
        "purgeList" : [ "aeiou" ]
      } ],
      "hdpVersion" : "aeiou",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "aeiou",
      "propertyValue" : "aeiou"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "aeiou",
      "port" : 9,
      "vendor" : "POSTGRES",
      "name" : "aeiou",
      "host" : "aeiou",
      "userName" : "aeiou"
    },
    "id" : 5,
    "blueprintCustomProperties" : "aeiou",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "aeiou",
    "customQueue" : "aeiou",
    "userName" : "aeiou",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "aeiou",
      "groupNameAttribute" : "aeiou",
      "groupMemberAttribute" : "aeiou",
      "description" : "aeiou",
      "userNameAttribute" : "aeiou",
      "serverPort" : 32540,
      "serverHost" : "aeiou",
      "directoryType" : "LDAP",
      "bindDn" : "aeiou",
      "protocol" : "aeiou",
      "groupSearchBase" : "aeiou",
      "userSearchBase" : "aeiou",
      "public" : false,
      "domain" : "aeiou",
      "name" : "aeiou",
      "id" : 5,
      "userObjectClass" : "aeiou",
      "groupObjectClass" : "aeiou"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "aeiou",
      "inputs" : [ {
        "referenceConfiguration" : "aeiou",
        "name" : "aeiou",
        "description" : "aeiou"
      } ],
      "hostGroupCount" : 3,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 9,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "aeiou",
      "databaseEngineDisplayName" : "aeiou",
      "clusterNames" : [ "aeiou" ],
      "connectorJarUrl" : "aeiou",
      "publicInAccount" : false,
      "stackVersion" : "aeiou",
      "name" : "aeiou",
      "connectionURL" : "aeiou",
      "id" : 6,
      "type" : "aeiou",
      "creationDate" : 7,
      "databaseEngine" : "aeiou"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "aeiou"
      }
    },
    "creationFinished" : 9,
    "kerberosResponse" : {
      "krb5Conf" : "aeiou",
      "ldapUrl" : "aeiou",
      "admin" : "aeiou",
      "realm" : "aeiou",
      "containerDn" : "aeiou",
      "descriptor" : "aeiou",
      "adminUrl" : "aeiou",
      "type" : "CB_MANAGED",
      "url" : "aeiou",
      "tcpAllowed" : false
    },
    "name" : "aeiou",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 1 ],
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
    "minutesUp" : 2
  },
  "hdpVersion" : "aeiou",
  "cloudbreakEvents" : [ {
    "eventMessage" : "aeiou",
    "owner" : "aeiou",
    "blueprintName" : "aeiou",
    "stackId" : 6,
    "stackName" : "aeiou",
    "stackStatus" : "REQUESTED",
    "eventType" : "aeiou",
    "clusterId" : 0,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 4,
    "cloud" : "aeiou",
    "clusterName" : "aeiou",
    "nodeCount" : 4,
    "region" : "aeiou",
    "account" : "aeiou",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 3
  } ],
  "platformVariant" : "aeiou",
  "customHostname" : "aeiou",
  "userDefinedTags" : {
    "key" : "aeiou"
  },
  "flexSubscription" : {
    "owner" : "aeiou",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 3,
    "usedForController" : false,
    "name" : "aeiou",
    "smartSenseSubscription" : {
      "owner" : "aeiou",
      "publicInAccount" : false,
      "id" : 7,
      "autoGenerated" : false,
      "subscriptionId" : "aeiou",
      "account" : "aeiou"
    },
    "id" : 3,
    "subscriptionId" : "aeiou",
    "account" : "aeiou",
    "usedAsDefault" : false
  },
  "availabilityZone" : "aeiou",
  "defaultTags" : {
    "key" : "aeiou"
  },
  "network" : {
    "subnetCIDR" : "aeiou",
    "cloudPlatform" : "aeiou",
    "publicInAccount" : false,
    "topologyId" : 6,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 3,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "aeiou",
  "credential" : {
    "cloudPlatform" : "aeiou",
    "public" : false,
    "name" : "aeiou",
    "topologyId" : 8,
    "description" : "aeiou",
    "id" : 9,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 5,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 0,
    "id" : 4
  },
  "owner" : "aeiou",
  "applicationTags" : {
    "key" : "aeiou"
  },
  "image" : {
    "imageName" : "aeiou",
    "imageId" : "aeiou",
    "imageCatalogUrl" : "aeiou",
    "imageCatalogName" : "aeiou"
  },
  "cloudbreakDetails" : {
    "version" : "aeiou"
  },
  "cloudPlatform" : "aeiou",
  "created" : 8,
  "customDomain" : "aeiou",
  "gatewayPort" : 7,
  "stackAuthentication" : {
    "loginUserName" : "aeiou",
    "publicKey" : "aeiou",
    "publicKeyId" : "aeiou"
  },
  "orchestrator" : {
    "apiEndpoint" : "aeiou",
    "type" : "aeiou",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "aeiou",
      "cloudPlatform" : "aeiou",
      "public" : false,
      "instanceType" : "aeiou",
      "customInstanceType" : {
        "memory" : 6,
        "cpus" : 6
      },
      "topologyId" : 5,
      "name" : "aeiou",
      "description" : "aeiou",
      "volumeCount" : 3,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 3
    },
    "metadata" : [ {
      "sshPort" : 6,
      "instanceId" : "aeiou",
      "ambariServer" : false,
      "privateIp" : "aeiou",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "aeiou",
      "publicIp" : "aeiou",
      "instanceGroup" : "aeiou",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 28841,
    "securityGroup" : {
      "securityGroupId" : "aeiou",
      "owner" : "aeiou",
      "cloudPlatform" : "aeiou",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "aeiou",
        "protocol" : "aeiou",
        "id" : 0,
        "ports" : "aeiou",
        "modifiable" : false
      } ],
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 7,
      "account" : "aeiou"
    },
    "id" : 7,
    "templateId" : 6,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "aeiou"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : "",
    "hostMetadata" : ""
  } ],
  "name" : "aeiou",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
    "owner" : "aeiou",
    "costs" : 7.260521264802104,
    "instanceNum" : 0,
    "blueprintName" : "aeiou",
    "stackId" : 4,
    "instanceType" : "aeiou",
    "instanceHours" : 1,
    "stackName" : "aeiou",
    "peak" : 9,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 9,
    "duration" : "aeiou",
    "provider" : "aeiou",
    "flexId" : "aeiou",
    "region" : "aeiou",
    "day" : "aeiou",
    "stackUuid" : "aeiou",
    "account" : "aeiou",
    "username" : "aeiou"
  } ],
  "region" : "aeiou",
  "ambariVersion" : "aeiou",
  "parameters" : {
    "key" : "aeiou"
  },
  "account" : "aeiou",
  "status" : "REQUESTED"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicStackV2 = function(args, res, next) {
  /**
   * retrieve a public or private (owned) stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * entry List  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/stacks/openstack.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    switch(args.name.value){
      case 'az404':
        res.statusCode=404;
        res.end(JSON.stringify({"message":"Stack 'az404' not found"}));
        break;
      default:
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    }
  } else {
    res.end();
  }
}

exports.getPublicsStackV2 = function(args, res, next) {
  /**
   * retrieve public and private (owned) stacks
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * returns List
   **/
  var openstack_data = require('../responses/stacks/openstack.json');
  var aws_data = require('../responses/stacks/aws.json');
  var azure_data = require('../responses/stacks/azure.json');
  var gcp_data = require('../responses/stacks/gcp.json');
  var response_array = [];

  response_array.push(openstack_data,aws_data,azure_data,gcp_data);

  var examples = {};
  examples['application/json'] = response_array;
  if (Object.keys(examples).length > 0) {
      res.setHeader('Content-Type', 'application/json');
      res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
      res.end();
  }
}

exports.getStackForAmbariV2 = function(args, res, next) {
  /**
   * retrieve stack by ambari address
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * body AmbariAddress  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : {
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
        "id" : 1,
        "uri" : "aeiou",
        "content" : "aeiou"
      } ],
      "metadata" : [ {
        "groupName" : "aeiou",
        "name" : "aeiou",
        "id" : 1,
        "state" : "aeiou"
      } ],
      "recipeIds" : [ 4 ],
      "recoveryMode" : "MANUAL",
      "name" : "aeiou",
      "constraint" : {
        "hostCount" : 2,
        "constraintTemplateName" : "aeiou",
        "instanceGroupName" : "aeiou"
      },
      "id" : 7
    } ],
    "ambariServerIp" : "aeiou",
    "ambariServerUrl" : "aeiou",
    "proxyName" : "aeiou",
    "description" : "aeiou",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "aeiou"
    },
    "statusReason" : "aeiou",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "aeiou"
      },
      "util" : {
        "key" : "aeiou"
      },
      "mpacks" : [ {
        "preInstalled" : false,
        "name" : "aeiou",
        "description" : "aeiou",
        "purge" : false,
        "force" : false,
        "stackDefault" : false,
        "mpackUrl" : "aeiou",
        "purgeList" : [ "aeiou" ]
      } ],
      "hdpVersion" : "aeiou",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "aeiou",
      "propertyValue" : "aeiou"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "aeiou",
      "port" : 9,
      "vendor" : "POSTGRES",
      "name" : "aeiou",
      "host" : "aeiou",
      "userName" : "aeiou"
    },
    "id" : 5,
    "blueprintCustomProperties" : "aeiou",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "aeiou",
    "customQueue" : "aeiou",
    "userName" : "aeiou",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "aeiou",
      "groupNameAttribute" : "aeiou",
      "groupMemberAttribute" : "aeiou",
      "description" : "aeiou",
      "userNameAttribute" : "aeiou",
      "serverPort" : 32540,
      "serverHost" : "aeiou",
      "directoryType" : "LDAP",
      "bindDn" : "aeiou",
      "protocol" : "aeiou",
      "groupSearchBase" : "aeiou",
      "userSearchBase" : "aeiou",
      "public" : false,
      "domain" : "aeiou",
      "name" : "aeiou",
      "id" : 5,
      "userObjectClass" : "aeiou",
      "groupObjectClass" : "aeiou"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "aeiou",
      "inputs" : [ {
        "referenceConfiguration" : "aeiou",
        "name" : "aeiou",
        "description" : "aeiou"
      } ],
      "hostGroupCount" : 3,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 9,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "aeiou",
      "databaseEngineDisplayName" : "aeiou",
      "clusterNames" : [ "aeiou" ],
      "connectorJarUrl" : "aeiou",
      "publicInAccount" : false,
      "stackVersion" : "aeiou",
      "name" : "aeiou",
      "connectionURL" : "aeiou",
      "id" : 6,
      "type" : "aeiou",
      "creationDate" : 7,
      "databaseEngine" : "aeiou"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "aeiou"
      }
    },
    "creationFinished" : 9,
    "kerberosResponse" : {
      "krb5Conf" : "aeiou",
      "ldapUrl" : "aeiou",
      "admin" : "aeiou",
      "realm" : "aeiou",
      "containerDn" : "aeiou",
      "descriptor" : "aeiou",
      "adminUrl" : "aeiou",
      "type" : "CB_MANAGED",
      "url" : "aeiou",
      "tcpAllowed" : false
    },
    "name" : "aeiou",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 1 ],
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
    "minutesUp" : 2
  },
  "hdpVersion" : "aeiou",
  "cloudbreakEvents" : [ {
    "eventMessage" : "aeiou",
    "owner" : "aeiou",
    "blueprintName" : "aeiou",
    "stackId" : 6,
    "stackName" : "aeiou",
    "stackStatus" : "REQUESTED",
    "eventType" : "aeiou",
    "clusterId" : 0,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 4,
    "cloud" : "aeiou",
    "clusterName" : "aeiou",
    "nodeCount" : 4,
    "region" : "aeiou",
    "account" : "aeiou",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 3
  } ],
  "platformVariant" : "aeiou",
  "customHostname" : "aeiou",
  "userDefinedTags" : {
    "key" : "aeiou"
  },
  "flexSubscription" : {
    "owner" : "aeiou",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 3,
    "usedForController" : false,
    "name" : "aeiou",
    "smartSenseSubscription" : {
      "owner" : "aeiou",
      "publicInAccount" : false,
      "id" : 7,
      "autoGenerated" : false,
      "subscriptionId" : "aeiou",
      "account" : "aeiou"
    },
    "id" : 3,
    "subscriptionId" : "aeiou",
    "account" : "aeiou",
    "usedAsDefault" : false
  },
  "availabilityZone" : "aeiou",
  "defaultTags" : {
    "key" : "aeiou"
  },
  "network" : {
    "subnetCIDR" : "aeiou",
    "cloudPlatform" : "aeiou",
    "publicInAccount" : false,
    "topologyId" : 6,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 3,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "aeiou",
  "credential" : {
    "cloudPlatform" : "aeiou",
    "public" : false,
    "name" : "aeiou",
    "topologyId" : 8,
    "description" : "aeiou",
    "id" : 9,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 5,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 0,
    "id" : 4
  },
  "owner" : "aeiou",
  "applicationTags" : {
    "key" : "aeiou"
  },
  "image" : {
    "imageName" : "aeiou",
    "imageId" : "aeiou",
    "imageCatalogUrl" : "aeiou",
    "imageCatalogName" : "aeiou"
  },
  "cloudbreakDetails" : {
    "version" : "aeiou"
  },
  "cloudPlatform" : "aeiou",
  "created" : 8,
  "customDomain" : "aeiou",
  "gatewayPort" : 7,
  "stackAuthentication" : {
    "loginUserName" : "aeiou",
    "publicKey" : "aeiou",
    "publicKeyId" : "aeiou"
  },
  "orchestrator" : {
    "apiEndpoint" : "aeiou",
    "type" : "aeiou",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "aeiou",
      "cloudPlatform" : "aeiou",
      "public" : false,
      "instanceType" : "aeiou",
      "customInstanceType" : {
        "memory" : 6,
        "cpus" : 6
      },
      "topologyId" : 5,
      "name" : "aeiou",
      "description" : "aeiou",
      "volumeCount" : 3,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 3
    },
    "metadata" : [ {
      "sshPort" : 6,
      "instanceId" : "aeiou",
      "ambariServer" : false,
      "privateIp" : "aeiou",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "aeiou",
      "publicIp" : "aeiou",
      "instanceGroup" : "aeiou",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 28841,
    "securityGroup" : {
      "securityGroupId" : "aeiou",
      "owner" : "aeiou",
      "cloudPlatform" : "aeiou",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "aeiou",
        "protocol" : "aeiou",
        "id" : 0,
        "ports" : "aeiou",
        "modifiable" : false
      } ],
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 7,
      "account" : "aeiou"
    },
    "id" : 7,
    "templateId" : 6,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "aeiou"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : "",
    "hostMetadata" : ""
  } ],
  "name" : "aeiou",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
    "owner" : "aeiou",
    "costs" : 7.260521264802104,
    "instanceNum" : 0,
    "blueprintName" : "aeiou",
    "stackId" : 4,
    "instanceType" : "aeiou",
    "instanceHours" : 1,
    "stackName" : "aeiou",
    "peak" : 9,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 9,
    "duration" : "aeiou",
    "provider" : "aeiou",
    "flexId" : "aeiou",
    "region" : "aeiou",
    "day" : "aeiou",
    "stackUuid" : "aeiou",
    "account" : "aeiou",
    "username" : "aeiou"
  } ],
  "region" : "aeiou",
  "ambariVersion" : "aeiou",
  "parameters" : {
    "key" : "aeiou"
  },
  "account" : "aeiou",
  "status" : "REQUESTED"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getStackV2 = function(args, res, next) {
  /**
   * retrieve stack by id
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * id Long 
   * entry List  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/stacks/openstack.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateStackV2 = function(args, res, next) {
  /**
   * create stack as private resource
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * body StackV2Request  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/stacks/openstack.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicStackV2 = function(args, res, next) {
  /**
   * create stack as public resource
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * body StackV2Request  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cluster" : {
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
        "id" : 1,
        "uri" : "aeiou",
        "content" : "aeiou"
      } ],
      "metadata" : [ {
        "groupName" : "aeiou",
        "name" : "aeiou",
        "id" : 1,
        "state" : "aeiou"
      } ],
      "recipeIds" : [ 4 ],
      "recoveryMode" : "MANUAL",
      "name" : "aeiou",
      "constraint" : {
        "hostCount" : 2,
        "constraintTemplateName" : "aeiou",
        "instanceGroupName" : "aeiou"
      },
      "id" : 7
    } ],
    "ambariServerIp" : "aeiou",
    "ambariServerUrl" : "aeiou",
    "proxyName" : "aeiou",
    "description" : "aeiou",
    "secure" : false,
    "configStrategy" : "NEVER_APPLY",
    "hoursUp" : 5,
    "serviceEndPoints" : {
      "key" : "aeiou"
    },
    "statusReason" : "aeiou",
    "ambariStackDetails" : {
      "enableGplRepo" : false,
      "stack" : {
        "key" : "aeiou"
      },
      "util" : {
        "key" : "aeiou"
      },
      "mpacks" : [ {
        "preInstalled" : false,
        "name" : "aeiou",
        "description" : "aeiou",
        "purge" : false,
        "force" : false,
        "stackDefault" : false,
        "mpackUrl" : "aeiou",
        "purgeList" : [ "aeiou" ]
      } ],
      "hdpVersion" : "aeiou",
      "verify" : false
    },
    "ldapConfigId" : 1,
    "blueprintInputs" : [ {
      "name" : "aeiou",
      "propertyValue" : "aeiou"
    } ],
    "ambariDatabaseDetails" : {
      "password" : "aeiou",
      "port" : 9,
      "vendor" : "POSTGRES",
      "name" : "aeiou",
      "host" : "aeiou",
      "userName" : "aeiou"
    },
    "id" : 5,
    "blueprintCustomProperties" : "aeiou",
    "executorType" : "CONTAINER",
    "extendedBlueprintText" : "aeiou",
    "customQueue" : "aeiou",
    "userName" : "aeiou",
    "blueprintId" : 7,
    "ldapConfig" : {
      "adminGroup" : "aeiou",
      "groupNameAttribute" : "aeiou",
      "groupMemberAttribute" : "aeiou",
      "description" : "aeiou",
      "userNameAttribute" : "aeiou",
      "serverPort" : 32540,
      "serverHost" : "aeiou",
      "directoryType" : "LDAP",
      "bindDn" : "aeiou",
      "protocol" : "aeiou",
      "groupSearchBase" : "aeiou",
      "userSearchBase" : "aeiou",
      "public" : false,
      "domain" : "aeiou",
      "name" : "aeiou",
      "id" : 5,
      "userObjectClass" : "aeiou",
      "groupObjectClass" : "aeiou"
    },
    "uptime" : 6,
    "blueprint" : {
      "public" : false,
      "ambariBlueprint" : "aeiou",
      "inputs" : [ {
        "referenceConfiguration" : "aeiou",
        "name" : "aeiou",
        "description" : "aeiou"
      } ],
      "hostGroupCount" : 3,
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 9,
      "tags" : {
        "key" : "{}"
      },
      "status" : "DEFAULT"
    },
    "rdsConfigs" : [ {
      "connectionDriver" : "aeiou",
      "databaseEngineDisplayName" : "aeiou",
      "clusterNames" : [ "aeiou" ],
      "connectorJarUrl" : "aeiou",
      "publicInAccount" : false,
      "stackVersion" : "aeiou",
      "name" : "aeiou",
      "connectionURL" : "aeiou",
      "id" : 6,
      "type" : "aeiou",
      "creationDate" : 7,
      "databaseEngine" : "aeiou"
    } ],
    "customContainers" : {
      "definitions" : {
        "key" : "aeiou"
      }
    },
    "creationFinished" : 9,
    "kerberosResponse" : {
      "krb5Conf" : "aeiou",
      "ldapUrl" : "aeiou",
      "admin" : "aeiou",
      "realm" : "aeiou",
      "containerDn" : "aeiou",
      "descriptor" : "aeiou",
      "adminUrl" : "aeiou",
      "type" : "CB_MANAGED",
      "url" : "aeiou",
      "tcpAllowed" : false
    },
    "name" : "aeiou",
    "attributes" : {
      "key" : "{}"
    },
    "rdsConfigIds" : [ 1 ],
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
    "minutesUp" : 2
  },
  "hdpVersion" : "aeiou",
  "cloudbreakEvents" : [ {
    "eventMessage" : "aeiou",
    "owner" : "aeiou",
    "blueprintName" : "aeiou",
    "stackId" : 6,
    "stackName" : "aeiou",
    "stackStatus" : "REQUESTED",
    "eventType" : "aeiou",
    "clusterId" : 0,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 4,
    "cloud" : "aeiou",
    "clusterName" : "aeiou",
    "nodeCount" : 4,
    "region" : "aeiou",
    "account" : "aeiou",
    "clusterStatus" : "REQUESTED",
    "eventTimestamp" : 3
  } ],
  "platformVariant" : "aeiou",
  "customHostname" : "aeiou",
  "userDefinedTags" : {
    "key" : "aeiou"
  },
  "flexSubscription" : {
    "owner" : "aeiou",
    "publicInAccount" : false,
    "smartSenseSubscriptionId" : 3,
    "usedForController" : false,
    "name" : "aeiou",
    "smartSenseSubscription" : {
      "owner" : "aeiou",
      "publicInAccount" : false,
      "id" : 7,
      "autoGenerated" : false,
      "subscriptionId" : "aeiou",
      "account" : "aeiou"
    },
    "id" : 3,
    "subscriptionId" : "aeiou",
    "account" : "aeiou",
    "usedAsDefault" : false
  },
  "availabilityZone" : "aeiou",
  "defaultTags" : {
    "key" : "aeiou"
  },
  "network" : {
    "subnetCIDR" : "aeiou",
    "cloudPlatform" : "aeiou",
    "publicInAccount" : false,
    "topologyId" : 6,
    "name" : "aeiou",
    "description" : "aeiou",
    "id" : 3,
    "parameters" : {
      "key" : "{}"
    }
  },
  "hostgroupNameAsHostname" : false,
  "onFailureAction" : "ROLLBACK",
  "statusReason" : "aeiou",
  "credential" : {
    "cloudPlatform" : "aeiou",
    "public" : false,
    "name" : "aeiou",
    "topologyId" : 8,
    "description" : "aeiou",
    "id" : 9,
    "parameters" : {
      "key" : "{}"
    }
  },
  "public" : false,
  "networkId" : 6,
  "nodeCount" : 5,
  "clusterNameAsSubdomain" : false,
  "id" : 1,
  "failurePolicy" : {
    "adjustmentType" : "EXACT",
    "threshold" : 0,
    "id" : 4
  },
  "owner" : "aeiou",
  "applicationTags" : {
    "key" : "aeiou"
  },
  "image" : {
    "imageName" : "aeiou",
    "imageId" : "aeiou",
    "imageCatalogUrl" : "aeiou",
    "imageCatalogName" : "aeiou"
  },
  "cloudbreakDetails" : {
    "version" : "aeiou"
  },
  "cloudPlatform" : "aeiou",
  "created" : 8,
  "customDomain" : "aeiou",
  "gatewayPort" : 7,
  "stackAuthentication" : {
    "loginUserName" : "aeiou",
    "publicKey" : "aeiou",
    "publicKeyId" : "aeiou"
  },
  "orchestrator" : {
    "apiEndpoint" : "aeiou",
    "type" : "aeiou",
    "parameters" : {
      "key" : "{}"
    }
  },
  "instanceGroups" : [ {
    "securityGroupId" : 1,
    "template" : {
      "volumeType" : "aeiou",
      "cloudPlatform" : "aeiou",
      "public" : false,
      "instanceType" : "aeiou",
      "customInstanceType" : {
        "memory" : 6,
        "cpus" : 6
      },
      "topologyId" : 5,
      "name" : "aeiou",
      "description" : "aeiou",
      "volumeCount" : 3,
      "id" : 6,
      "parameters" : {
        "key" : "{}"
      },
      "volumeSize" : 3
    },
    "metadata" : [ {
      "sshPort" : 6,
      "instanceId" : "aeiou",
      "ambariServer" : false,
      "privateIp" : "aeiou",
      "instanceType" : "GATEWAY",
      "discoveryFQDN" : "aeiou",
      "publicIp" : "aeiou",
      "instanceGroup" : "aeiou",
      "instanceStatus" : "REQUESTED"
    } ],
    "nodeCount" : 28841,
    "securityGroup" : {
      "securityGroupId" : "aeiou",
      "owner" : "aeiou",
      "cloudPlatform" : "aeiou",
      "publicInAccount" : false,
      "securityRules" : [ {
        "subnet" : "aeiou",
        "protocol" : "aeiou",
        "id" : 0,
        "ports" : "aeiou",
        "modifiable" : false
      } ],
      "name" : "aeiou",
      "description" : "aeiou",
      "id" : 7,
      "account" : "aeiou"
    },
    "id" : 7,
    "templateId" : 6,
    "type" : "GATEWAY",
    "parameters" : {
      "key" : "{}"
    },
    "group" : "aeiou"
  } ],
  "hardwareInfos" : [ {
    "instanceMetaData" : "",
    "hostMetadata" : ""
  } ],
  "name" : "aeiou",
  "credentialId" : 0,
  "cloudbreakUsages" : [ {
    "owner" : "aeiou",
    "costs" : 7.260521264802104,
    "instanceNum" : 0,
    "blueprintName" : "aeiou",
    "stackId" : 4,
    "instanceType" : "aeiou",
    "instanceHours" : 1,
    "stackName" : "aeiou",
    "peak" : 9,
    "instanceGroup" : "aeiou",
    "availabilityZone" : "aeiou",
    "blueprintId" : 9,
    "duration" : "aeiou",
    "provider" : "aeiou",
    "flexId" : "aeiou",
    "region" : "aeiou",
    "day" : "aeiou",
    "stackUuid" : "aeiou",
    "account" : "aeiou",
    "username" : "aeiou"
  } ],
  "region" : "aeiou",
  "ambariVersion" : "aeiou",
  "parameters" : {
    "key" : "aeiou"
  },
  "account" : "aeiou",
  "status" : "REQUESTED"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicStackV2ForBlueprint = function(args, res, next) {
  /**
   * create stack as public resource for blueprint
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * body StackV2Request  (optional)
   * returns GeneratedBlueprintResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "ambariBlueprint" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.putpasswordStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * body UserNamePassword  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.putreinstallStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * body ReinstallRequestV2  (optional)
   * no response value expected for this operation
   **/
    if (args.name.value === 'aaaaa') {
        res.statusCode=404
        res.end(JSON.stringify({"message":"Stack 'aaaaa' not found"}));
    } else {
        res.end();
    }
}

exports.putrepairStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * no response value expected for this operation
   **/
    if (args.name.value === 'azstatus') {
        res.statusCode=404
        res.end(JSON.stringify({"message":"Stack 'azstatus' not found"}));
    } else {
        res.end();
    }
}

exports.putscalingStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * body StackScaleRequestV2  (optional)
   * no response value expected for this operation
   **/
    if (args.name.value === 'azstatus') {
        res.statusCode=404
        res.end(JSON.stringify({"message":"Stack 'azstatus' not found"}));
    } else {
        res.end();
    }
}

exports.putstartStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * no response value expected for this operation
   **/
    if (args.name.value === 'azstatus') {
        res.statusCode=404
        res.end(JSON.stringify({"message":"Stack 'azstatus' not found"}));
    } else {
        res.end();
    }
}

exports.putstopStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * no response value expected for this operation
   **/
    if (args.name.value === 'azstatus') {
        res.statusCode=404
        res.end(JSON.stringify({"message":"Stack 'azstatus' not found"}));
    } else {
        res.end();
    }
}

exports.putsyncStackV2 = function(args, res, next) {
  /**
   * update stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String 
   * no response value expected for this operation
   **/
    if (args.name.value === 'azstatus') {
        res.statusCode=404
        res.end(JSON.stringify({"message":"Stack 'azstatus' not found"}));
    } else {
        res.end();
    }
}

exports.statusStackV2 = function(args, res, next) {
  /**
   * retrieve stack status by stack id
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * id Long 
   * returns Map
   **/
  var examples = {};
  examples['application/json'] = {
  "key" : "{}"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.validateStackV2 = function(args, res, next) {
  /**
   * validate stack
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * body StackValidationRequest  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.variantsStackV2 = function(args, res, next) {
  /**
   * retrieve available platform variants
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * returns PlatformVariantsJson
   **/
  var examples = {};
  examples['application/json'] = {
  "platformToVariants" : {
    "key" : [ "aeiou" ]
  },
  "defaultVariants" : {
    "key" : "aeiou"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

