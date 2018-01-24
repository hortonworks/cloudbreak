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
        "hdpVersion" : "aeiou",
        "platformVariant" : "aeiou",
        "customHostname" : "aeiou",
        "userDefinedTags" : {
            "key" : "aeiou"
        },
        "availabilityZone" : "aeiou",
        "defaultTags" : {
            "key" : "aeiou"
        },
        "network" : {
            "subnetCIDR" : "aeiou",
            "parameters" : {
                "key" : "{}"
            }
        },
        "hostgroupNameAsHostname" : false,
        "onFailureAction" : "ROLLBACK",
        "clusterNameAsSubdomain" : false,
        "flexId" : 7,
        "failurePolicy" : {
            "adjustmentType" : "EXACT",
            "threshold" : 2
        },
        "applicationTags" : {
            "key" : "aeiou"
        },
        "imageId" : "aeiou",
        "customDomain" : "aeiou",
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
        "imageCatalog" : "aeiou",
        "name" : args.name.value,
        "clusterRequest" : {
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
            "rdsConfigJsons" : [ {
                "databaseType" : "POSTGRES",
                "connectionUserName" : "aeiou",
                "validated" : false,
                "hdpVersion" : "aeiou",
                "name" : "aeiou",
                "connectionURL" : "aeiou",
                "connectionPassword" : "aeiou",
                "type" : "HIVE",
                "properties" : [ {
                    "name" : "aeiou",
                    "value" : "aeiou"
                } ]
            } ],
            "ldapConfigName" : "aeiou",
            "ambariRequest" : {
                "ambariRepoDetailsJson" : {
                    "baseUrl" : "aeiou",
                    "version" : "aeiou",
                    "gpgKeyUrl" : "aeiou"
                },
                "blueprintCustomProperties" : "aeiou",
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
                "blueprintId" : 3,
                "connectedCluster" : {
                    "sourceClusterName" : "aeiou",
                    "sourceClusterId" : 4
                },
                "configStrategy" : "NEVER_APPLY",
                "enableSecurity" : false,
                "password" : "aeiou",
                "validateBlueprint" : false,
                "ambariStackDetails" : {
                    "utilsBaseURL" : "aeiou",
                    "stack" : "aeiou",
                    "os" : "aeiou",
                    "stackRepoId" : "aeiou",
                    "utilsRepoId" : "aeiou",
                    "verify" : false,
                    "mpackUrl" : "aeiou",
                    "version" : "aeiou",
                    "stackBaseURL" : "aeiou",
                    "versionDefinitionFileUrl" : "aeiou",
                    "repositoryVersion" : "aeiou"
                },
                "blueprintInputs" : [ {
                    "name" : "aeiou",
                    "propertyValue" : "aeiou"
                } ],
                "ambariDatabaseDetails" : {
                    "password" : "aeiou",
                    "port" : 2,
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
            "emailTo" : "aeiou",
            "rdsConfigIds" : [ 9 ]
        },
        "region" : "aeiou",
        "credentialName" : "aeiou",
        "ambariVersion" : "aeiou",
        "parameters" : {
            "key" : "aeiou"
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
            "description" : "aeiou",
            "secure" : false,
            "configStrategy" : "NEVER_APPLY",
            "hoursUp" : 5,
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
                "port" : 9,
                "vendor" : "POSTGRES",
                "name" : "aeiou",
                "host" : "aeiou",
                "userName" : "aeiou"
            },
            "id" : 5,
            "blueprintCustomProperties" : "aeiou",
            "executorType" : "CONTAINER",
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
                "id" : 6,
                "type" : "HIVE",
                "creationDate" : 7,
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
            "creationFinished" : 9,
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
            "stackId" : 0,
            "stackName" : "aeiou",
            "stackStatus" : "REQUESTED",
            "eventType" : "aeiou",
            "clusterId" : 4,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 3,
            "cloud" : "aeiou",
            "clusterName" : "aeiou",
            "nodeCount" : 6,
            "region" : "aeiou",
            "account" : "aeiou",
            "clusterStatus" : "REQUESTED",
            "eventTimestamp" : 5
        } ],
        "platformVariant" : "aeiou",
        "customHostname" : "aeiou",
        "userDefinedTags" : {
            "key" : "aeiou"
        },
        "flexSubscription" : {
            "owner" : "aeiou",
            "default" : false,
            "publicInAccount" : false,
            "smartSenseSubscriptionId" : 7,
            "usedForController" : false,
            "name" : "aeiou",
            "smartSenseSubscription" : {
                "owner" : "aeiou",
                "publicInAccount" : false,
                "id" : 3,
                "autoGenerated" : false,
                "subscriptionId" : "aeiou",
                "account" : "aeiou"
            },
            "id" : 3,
            "subscriptionId" : "aeiou",
            "account" : "aeiou"
        },
        "availabilityZone" : "aeiou",
        "defaultTags" : {
            "key" : "aeiou"
        },
        "network" : {
            "subnetCIDR" : "aeiou",
            "cloudPlatform" : "aeiou",
            "publicInAccount" : false,
            "topologyId" : 9,
            "name" : "aeiou",
            "description" : "aeiou",
            "id" : 6,
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
            "topologyId" : 6,
            "description" : "aeiou",
            "id" : 8,
            "parameters" : {
                "key" : "{}"
            }
        },
        "public" : false,
        "networkId" : 6,
        "nodeCount" : 7,
        "clusterNameAsSubdomain" : false,
        "id" : 1,
        "failurePolicy" : {
            "adjustmentType" : "EXACT",
            "threshold" : 6,
            "id" : 0
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
        "created" : 4,
        "customDomain" : "aeiou",
        "gatewayPort" : 8,
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
            "securityGroupId" : 6,
            "template" : {
                "volumeType" : "aeiou",
                "cloudPlatform" : "aeiou",
                "public" : false,
                "instanceType" : "aeiou",
                "customInstanceType" : {
                    "memory" : 2,
                    "cpus" : 6
                },
                "topologyId" : 6,
                "name" : "aeiou",
                "description" : "aeiou",
                "volumeCount" : 6,
                "id" : 5,
                "parameters" : {
                    "key" : "{}"
                },
                "volumeSize" : 3
            },
            "metadata" : [ {
                "sshPort" : 7,
                "instanceId" : "aeiou",
                "ambariServer" : false,
                "privateIp" : "aeiou",
                "instanceType" : "GATEWAY",
                "discoveryFQDN" : "aeiou",
                "publicIp" : "aeiou",
                "instanceGroup" : "aeiou",
                "instanceStatus" : "REQUESTED"
            } ],
            "nodeCount" : 12846,
            "securityGroup" : {
                "securityGroupId" : "aeiou",
                "owner" : "aeiou",
                "cloudPlatform" : "aeiou",
                "publicInAccount" : false,
                "securityRules" : [ {
                    "subnet" : "aeiou",
                    "protocol" : "aeiou",
                    "id" : 7,
                    "ports" : "aeiou",
                    "modifiable" : false
                } ],
                "name" : "aeiou",
                "description" : "aeiou",
                "id" : 3,
                "account" : "aeiou"
            },
            "id" : 0,
            "templateId" : 3,
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
            "costs" : 4.678947989005849,
            "instanceNum" : 9,
            "blueprintName" : "aeiou",
            "stackId" : 1,
            "instanceType" : "aeiou",
            "instanceHours" : 4,
            "stackName" : "aeiou",
            "peak" : 0,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 7,
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
            "description" : "aeiou",
            "secure" : false,
            "configStrategy" : "NEVER_APPLY",
            "hoursUp" : 5,
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
                "port" : 9,
                "vendor" : "POSTGRES",
                "name" : "aeiou",
                "host" : "aeiou",
                "userName" : "aeiou"
            },
            "id" : 5,
            "blueprintCustomProperties" : "aeiou",
            "executorType" : "CONTAINER",
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
                "id" : 6,
                "type" : "HIVE",
                "creationDate" : 7,
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
            "creationFinished" : 9,
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
            "stackId" : 0,
            "stackName" : "aeiou",
            "stackStatus" : "REQUESTED",
            "eventType" : "aeiou",
            "clusterId" : 4,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 3,
            "cloud" : "aeiou",
            "clusterName" : "aeiou",
            "nodeCount" : 6,
            "region" : "aeiou",
            "account" : "aeiou",
            "clusterStatus" : "REQUESTED",
            "eventTimestamp" : 5
        } ],
        "platformVariant" : "aeiou",
        "customHostname" : "aeiou",
        "userDefinedTags" : {
            "key" : "aeiou"
        },
        "flexSubscription" : {
            "owner" : "aeiou",
            "default" : false,
            "publicInAccount" : false,
            "smartSenseSubscriptionId" : 7,
            "usedForController" : false,
            "name" : "aeiou",
            "smartSenseSubscription" : {
                "owner" : "aeiou",
                "publicInAccount" : false,
                "id" : 3,
                "autoGenerated" : false,
                "subscriptionId" : "aeiou",
                "account" : "aeiou"
            },
            "id" : 3,
            "subscriptionId" : "aeiou",
            "account" : "aeiou"
        },
        "availabilityZone" : "aeiou",
        "defaultTags" : {
            "key" : "aeiou"
        },
        "network" : {
            "subnetCIDR" : "aeiou",
            "cloudPlatform" : "aeiou",
            "publicInAccount" : false,
            "topologyId" : 9,
            "name" : "aeiou",
            "description" : "aeiou",
            "id" : 6,
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
            "topologyId" : 6,
            "description" : "aeiou",
            "id" : 8,
            "parameters" : {
                "key" : "{}"
            }
        },
        "public" : false,
        "networkId" : 6,
        "nodeCount" : 7,
        "clusterNameAsSubdomain" : false,
        "id" : 1,
        "failurePolicy" : {
            "adjustmentType" : "EXACT",
            "threshold" : 6,
            "id" : 0
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
        "created" : 4,
        "customDomain" : "aeiou",
        "gatewayPort" : 8,
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
            "securityGroupId" : 6,
            "template" : {
                "volumeType" : "aeiou",
                "cloudPlatform" : "aeiou",
                "public" : false,
                "instanceType" : "aeiou",
                "customInstanceType" : {
                    "memory" : 2,
                    "cpus" : 6
                },
                "topologyId" : 6,
                "name" : "aeiou",
                "description" : "aeiou",
                "volumeCount" : 6,
                "id" : 5,
                "parameters" : {
                    "key" : "{}"
                },
                "volumeSize" : 3
            },
            "metadata" : [ {
                "sshPort" : 7,
                "instanceId" : "aeiou",
                "ambariServer" : false,
                "privateIp" : "aeiou",
                "instanceType" : "GATEWAY",
                "discoveryFQDN" : "aeiou",
                "publicIp" : "aeiou",
                "instanceGroup" : "aeiou",
                "instanceStatus" : "REQUESTED"
            } ],
            "nodeCount" : 12846,
            "securityGroup" : {
                "securityGroupId" : "aeiou",
                "owner" : "aeiou",
                "cloudPlatform" : "aeiou",
                "publicInAccount" : false,
                "securityRules" : [ {
                    "subnet" : "aeiou",
                    "protocol" : "aeiou",
                    "id" : 7,
                    "ports" : "aeiou",
                    "modifiable" : false
                } ],
                "name" : "aeiou",
                "description" : "aeiou",
                "id" : 3,
                "account" : "aeiou"
            },
            "id" : 0,
            "templateId" : 3,
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
            "costs" : 4.678947989005849,
            "instanceNum" : 9,
            "blueprintName" : "aeiou",
            "stackId" : 1,
            "instanceType" : "aeiou",
            "instanceHours" : 4,
            "stackName" : "aeiou",
            "peak" : 0,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 7,
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
            "description" : "aeiou",
            "secure" : false,
            "configStrategy" : "NEVER_APPLY",
            "hoursUp" : 5,
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
                "port" : 9,
                "vendor" : "POSTGRES",
                "name" : "aeiou",
                "host" : "aeiou",
                "userName" : "aeiou"
            },
            "id" : 5,
            "blueprintCustomProperties" : "aeiou",
            "executorType" : "CONTAINER",
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
                "id" : 6,
                "type" : "HIVE",
                "creationDate" : 7,
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
            "creationFinished" : 9,
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
            "stackId" : 0,
            "stackName" : "aeiou",
            "stackStatus" : "REQUESTED",
            "eventType" : "aeiou",
            "clusterId" : 4,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 3,
            "cloud" : "aeiou",
            "clusterName" : "aeiou",
            "nodeCount" : 6,
            "region" : "aeiou",
            "account" : "aeiou",
            "clusterStatus" : "REQUESTED",
            "eventTimestamp" : 5
        } ],
        "platformVariant" : "aeiou",
        "customHostname" : "aeiou",
        "userDefinedTags" : {
            "key" : "aeiou"
        },
        "flexSubscription" : {
            "owner" : "aeiou",
            "default" : false,
            "publicInAccount" : false,
            "smartSenseSubscriptionId" : 7,
            "usedForController" : false,
            "name" : "aeiou",
            "smartSenseSubscription" : {
                "owner" : "aeiou",
                "publicInAccount" : false,
                "id" : 3,
                "autoGenerated" : false,
                "subscriptionId" : "aeiou",
                "account" : "aeiou"
            },
            "id" : 3,
            "subscriptionId" : "aeiou",
            "account" : "aeiou"
        },
        "availabilityZone" : "aeiou",
        "defaultTags" : {
            "key" : "aeiou"
        },
        "network" : {
            "subnetCIDR" : "aeiou",
            "cloudPlatform" : "aeiou",
            "publicInAccount" : false,
            "topologyId" : 9,
            "name" : "aeiou",
            "description" : "aeiou",
            "id" : 6,
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
            "topologyId" : 6,
            "description" : "aeiou",
            "id" : 8,
            "parameters" : {
                "key" : "{}"
            }
        },
        "public" : false,
        "networkId" : 6,
        "nodeCount" : 7,
        "clusterNameAsSubdomain" : false,
        "id" : 1,
        "failurePolicy" : {
            "adjustmentType" : "EXACT",
            "threshold" : 6,
            "id" : 0
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
        "created" : 4,
        "customDomain" : "aeiou",
        "gatewayPort" : 8,
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
            "securityGroupId" : 6,
            "template" : {
                "volumeType" : "aeiou",
                "cloudPlatform" : "aeiou",
                "public" : false,
                "instanceType" : "aeiou",
                "customInstanceType" : {
                    "memory" : 2,
                    "cpus" : 6
                },
                "topologyId" : 6,
                "name" : "aeiou",
                "description" : "aeiou",
                "volumeCount" : 6,
                "id" : 5,
                "parameters" : {
                    "key" : "{}"
                },
                "volumeSize" : 3
            },
            "metadata" : [ {
                "sshPort" : 7,
                "instanceId" : "aeiou",
                "ambariServer" : false,
                "privateIp" : "aeiou",
                "instanceType" : "GATEWAY",
                "discoveryFQDN" : "aeiou",
                "publicIp" : "aeiou",
                "instanceGroup" : "aeiou",
                "instanceStatus" : "REQUESTED"
            } ],
            "nodeCount" : 12846,
            "securityGroup" : {
                "securityGroupId" : "aeiou",
                "owner" : "aeiou",
                "cloudPlatform" : "aeiou",
                "publicInAccount" : false,
                "securityRules" : [ {
                    "subnet" : "aeiou",
                    "protocol" : "aeiou",
                    "id" : 7,
                    "ports" : "aeiou",
                    "modifiable" : false
                } ],
                "name" : "aeiou",
                "description" : "aeiou",
                "id" : 3,
                "account" : "aeiou"
            },
            "id" : 0,
            "templateId" : 3,
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
            "costs" : 4.678947989005849,
            "instanceNum" : 9,
            "blueprintName" : "aeiou",
            "stackId" : 1,
            "instanceType" : "aeiou",
            "instanceHours" : 4,
            "stackName" : "aeiou",
            "peak" : 0,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 7,
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
            "description" : "aeiou",
            "secure" : false,
            "configStrategy" : "NEVER_APPLY",
            "hoursUp" : 5,
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
                "port" : 9,
                "vendor" : "POSTGRES",
                "name" : "aeiou",
                "host" : "aeiou",
                "userName" : "aeiou"
            },
            "id" : 5,
            "blueprintCustomProperties" : "aeiou",
            "executorType" : "CONTAINER",
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
                "id" : 6,
                "type" : "HIVE",
                "creationDate" : 7,
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
            "creationFinished" : 9,
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
            "stackId" : 0,
            "stackName" : "aeiou",
            "stackStatus" : "REQUESTED",
            "eventType" : "aeiou",
            "clusterId" : 4,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 3,
            "cloud" : "aeiou",
            "clusterName" : "aeiou",
            "nodeCount" : 6,
            "region" : "aeiou",
            "account" : "aeiou",
            "clusterStatus" : "REQUESTED",
            "eventTimestamp" : 5
        } ],
        "platformVariant" : "aeiou",
        "customHostname" : "aeiou",
        "userDefinedTags" : {
            "key" : "aeiou"
        },
        "flexSubscription" : {
            "owner" : "aeiou",
            "default" : false,
            "publicInAccount" : false,
            "smartSenseSubscriptionId" : 7,
            "usedForController" : false,
            "name" : "aeiou",
            "smartSenseSubscription" : {
                "owner" : "aeiou",
                "publicInAccount" : false,
                "id" : 3,
                "autoGenerated" : false,
                "subscriptionId" : "aeiou",
                "account" : "aeiou"
            },
            "id" : 3,
            "subscriptionId" : "aeiou",
            "account" : "aeiou"
        },
        "availabilityZone" : "aeiou",
        "defaultTags" : {
            "key" : "aeiou"
        },
        "network" : {
            "subnetCIDR" : "aeiou",
            "cloudPlatform" : "aeiou",
            "publicInAccount" : false,
            "topologyId" : 9,
            "name" : "aeiou",
            "description" : "aeiou",
            "id" : 6,
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
            "topologyId" : 6,
            "description" : "aeiou",
            "id" : 8,
            "parameters" : {
                "key" : "{}"
            }
        },
        "public" : false,
        "networkId" : 6,
        "nodeCount" : 7,
        "clusterNameAsSubdomain" : false,
        "id" : 1,
        "failurePolicy" : {
            "adjustmentType" : "EXACT",
            "threshold" : 6,
            "id" : 0
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
        "created" : 4,
        "customDomain" : "aeiou",
        "gatewayPort" : 8,
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
            "securityGroupId" : 6,
            "template" : {
                "volumeType" : "aeiou",
                "cloudPlatform" : "aeiou",
                "public" : false,
                "instanceType" : "aeiou",
                "customInstanceType" : {
                    "memory" : 2,
                    "cpus" : 6
                },
                "topologyId" : 6,
                "name" : "aeiou",
                "description" : "aeiou",
                "volumeCount" : 6,
                "id" : 5,
                "parameters" : {
                    "key" : "{}"
                },
                "volumeSize" : 3
            },
            "metadata" : [ {
                "sshPort" : 7,
                "instanceId" : "aeiou",
                "ambariServer" : false,
                "privateIp" : "aeiou",
                "instanceType" : "GATEWAY",
                "discoveryFQDN" : "aeiou",
                "publicIp" : "aeiou",
                "instanceGroup" : "aeiou",
                "instanceStatus" : "REQUESTED"
            } ],
            "nodeCount" : 12846,
            "securityGroup" : {
                "securityGroupId" : "aeiou",
                "owner" : "aeiou",
                "cloudPlatform" : "aeiou",
                "publicInAccount" : false,
                "securityRules" : [ {
                    "subnet" : "aeiou",
                    "protocol" : "aeiou",
                    "id" : 7,
                    "ports" : "aeiou",
                    "modifiable" : false
                } ],
                "name" : "aeiou",
                "description" : "aeiou",
                "id" : 3,
                "account" : "aeiou"
            },
            "id" : 0,
            "templateId" : 3,
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
            "costs" : 4.678947989005849,
            "instanceNum" : 9,
            "blueprintName" : "aeiou",
            "stackId" : 1,
            "instanceType" : "aeiou",
            "instanceHours" : 4,
            "stackName" : "aeiou",
            "peak" : 0,
            "instanceGroup" : "aeiou",
            "availabilityZone" : "aeiou",
            "blueprintId" : 7,
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

