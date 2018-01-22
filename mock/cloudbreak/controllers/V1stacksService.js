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

exports.deleteInstanceStack = function(args, res, next) {
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

exports.deletePrivateStack = function(args, res, next) {
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

exports.deletePublicStack = function(args, res, next) {
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

exports.deleteStack = function(args, res, next) {
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

exports.getAllStack = function(args, res, next) {
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

exports.getCertificateStack = function(args, res, next) {
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

exports.getPrivateStack = function(args, res, next) {
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

exports.getPrivatesStack = function(args, res, next) {
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

exports.getPublicStack = function(args, res, next) {
  /**
   * retrieve a public or private (owned) stack by name
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * name String
   * entry List  (optional)
   * returns StackResponse
   **/
  var examples = {};
  examples['application/json'] =
    {
      "name":args.name.value,
      "availabilityZone":"nova",
      "region":"RegionOne",
      "platformVariant":"HEAT",
      "credentialId":1,
      "onFailureAction":"DO_NOTHING",
      "networkId":1,
      "ambariVersion":"2.6.0.0",
      "hdpVersion":"2.6.3.0-235",
      "parameters":{},
      "customDomain":null,
      "customHostname":null,
      "clusterNameAsSubdomain":false,
      "hostgroupNameAsHostname":false,
      "applicationTags":{},
      "userDefinedTags":
      {
        "kisnyul":"pityuka"
      },
      "defaultTags":{},
      "id":1,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "status":"CREATE_FAILED",
      "cluster":
      {
        "id":1,
        "name":"openstack-cluster",
        "status":"REQUESTED",
        "hoursUp":0,
        "minutesUp":0,
        "cluster":null,
        "blueprintId":7,
        "blueprint":
        {
          "ambariBlueprint":"eyJCbHVlcHJpbnRzIjp7ImJsdWVwcmludF9uYW1lIjoiaGRwMjYtZWR3LWFuYWx5dGljcyIsInN0YWNrX25hbWUiOiJIRFAiLCJzdGFja192ZXJzaW9uIjoiMi42In0sInNldHRpbmdzIjpbeyJyZWNvdmVyeV9zZXR0aW5ncyI6W119LHsic2VydmljZV9zZXR0aW5ncyI6W3sibmFtZSI6IkhJVkUiLCJjcmVkZW50aWFsX3N0b3JlX2VuYWJsZWQiOiJmYWxzZSJ9XX0seyJjb21wb25lbnRfc2V0dGluZ3MiOltdfV0sImNvbmZpZ3VyYXRpb25zIjpbeyJoaXZlLWludGVyYWN0aXZlLWVudiI6eyJlbmFibGVfaGl2ZV9pbnRlcmFjdGl2ZSI6InRydWUifX0seyJjb3JlLXNpdGUiOnsiZnMudHJhc2guaW50ZXJ2YWwiOiI0MzIwIn19LHsiaGRmcy1zaXRlIjp7ImRmcy5uYW1lbm9kZS5zYWZlbW9kZS50aHJlc2hvbGQtcGN0IjoiMC45OSJ9fSx7ImhpdmUtaW50ZXJhY3RpdmUtc2l0ZSI6eyJoaXZlLmV4ZWMub3JjLnNwbGl0LnN0cmF0ZWd5IjoiQkkiLCJoaXZlLnN0YXRzLmZldGNoLmJpdHZlY3RvciI6InRydWUiLCJoaXZlLm1ldGFzdG9yZS5yYXdzdG9yZS5pbXBsIjoib3JnLmFwYWNoZS5oYWRvb3AuaGl2ZS5tZXRhc3RvcmUuY2FjaGUuQ2FjaGVkU3RvcmUifX0seyJoaXZlLXNpdGUiOnsiaGl2ZS5leGVjLmNvbXByZXNzLm91dHB1dCI6InRydWUiLCJoaXZlLm1lcmdlLm1hcGZpbGVzIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50ZXouaW5pdGlhbGl6ZS5kZWZhdWx0LnNlc3Npb25zIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50cmFuc3BvcnQubW9kZSI6Imh0dHAifX0seyJtYXByZWQtc2l0ZSI6eyJtYXByZWR1Y2Uuam9iLnJlZHVjZS5zbG93c3RhcnQuY29tcGxldGVkbWFwcyI6IjAuNyIsIm1hcHJlZHVjZS5tYXAub3V0cHV0LmNvbXByZXNzIjoidHJ1ZSIsIm1hcHJlZHVjZS5vdXRwdXQuZmlsZW91dHB1dGZvcm1hdC5jb21wcmVzcyI6InRydWUifX0seyJ0ZXotc2l0ZSI6eyJ0ZXoucnVudGltZS5zaHVmZmxlLnBhcmFsbGVsLmNvcGllcyI6IjQiLCJ0ZXoucnVudGltZS5lbmFibGUuZmluYWwtbWVyZ2UuaW4ub3V0cHV0IjoiZmFsc2UiLCJ0ZXouYW0uYW0tcm0uaGVhcnRiZWF0LmludGVydmFsLW1zLm1heCI6IjIwMDAifX0seyJ5YXJuLXNpdGUiOnsieWFybi5hY2wuZW5hYmxlIjoidHJ1ZSJ9fV0sImhvc3RfZ3JvdXBzIjpbeyJuYW1lIjoibWFzdGVyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiQVBQX1RJTUVMSU5FX1NFUlZFUiJ9LHsibmFtZSI6IkhDQVQifSx7Im5hbWUiOiJIREZTX0NMSUVOVCJ9LHsibmFtZSI6IkhJU1RPUllTRVJWRVIifSx7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IkhJVkVfTUVUQVNUT1JFIn0seyJuYW1lIjoiSElWRV9TRVJWRVIifSx7Im5hbWUiOiJISVZFX1NFUlZFUl9JTlRFUkFDVElWRSJ9LHsibmFtZSI6IkpPVVJOQUxOT0RFIn0seyJuYW1lIjoiTElWWV9TRVJWRVIifSx7Im5hbWUiOiJNQVBSRURVQ0UyX0NMSUVOVCJ9LHsibmFtZSI6Ik1FVFJJQ1NfQ09MTEVDVE9SIn0seyJuYW1lIjoiTUVUUklDU19NT05JVE9SIn0seyJuYW1lIjoiTVlTUUxfU0VSVkVSIn0seyJuYW1lIjoiTkFNRU5PREUifSx7Im5hbWUiOiJQSUcifSx7Im5hbWUiOiJSRVNPVVJDRU1BTkFHRVIifSx7Im5hbWUiOiJTRUNPTkRBUllfTkFNRU5PREUifSx7Im5hbWUiOiJTTElERVIifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJTUEFSS19KT0JISVNUT1JZU0VSVkVSIn0seyJuYW1lIjoiVEVaX0NMSUVOVCJ9LHsibmFtZSI6IldFQkhDQVRfU0VSVkVSIn0seyJuYW1lIjoiWUFSTl9DTElFTlQifSx7Im5hbWUiOiJaRVBQRUxJTl9NQVNURVIifSx7Im5hbWUiOiJaT09LRUVQRVJfQ0xJRU5UIn0seyJuYW1lIjoiWk9PS0VFUEVSX1NFUlZFUiJ9XSwiY2FyZGluYWxpdHkiOiIxIn0seyJuYW1lIjoid29ya2VyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiSElWRV9DTElFTlQifSx7Im5hbWUiOiJURVpfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiREFUQU5PREUifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9LHsibmFtZSI6ImNvbXB1dGUiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9XX0=",
          "description":"Useful for EDW analytics using Hive LLAP",
          "inputs":[],
          "name":"EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0",
          "id":7,
          "hostGroupCount":3,
          "status":"DEFAULT","public":true
        },
        "description":"",
        "statusReason":null,
        "ambariServerIp":null,
        "ambariServerUrl":null,
        "userName":"admin",
        "secure":false,
        "hostGroups":
        [
          {
            "name":"worker",
            "constraint":
            {
              "instanceGroupName":"worker",
              "constraintTemplateName":null,
              "hostCount":1
            },
            "recipeIds":[1],
            "recoveryMode":"AUTO",
            "id":1,
            "recipes":
            [
              {
                "name":"pre-start",
                "description":"",
                "recipeType":"PRE_AMBARI_START",
                "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                "id":1,
                "public":true
              }
            ],
            "metadata":[]
          },{
            "name":"compute",
            "constraint":
            {
              "instanceGroupName":"compute",
              "constraintTemplateName":null,
              "hostCount":0
            },
            "recipeIds":[1],
            "recoveryMode":"AUTO",
            "id":2,
            "recipes":
            [
              {
                "name":"pre-start",
                "description":"",
                "recipeType":"PRE_AMBARI_START",
                "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                "id":1,
                "public":true
              }
            ],
            "metadata":[]
          },{
            "name":"master",
            "constraint":
            {
              "instanceGroupName":"master",
              "constraintTemplateName":null,
              "hostCount":1
            },
            "recipeIds":[1],
            "recoveryMode":"AUTO",
            "id":3,
            "recipes":
            [
              {
                "name":"pre-start",
                "description":"",
                "recipeType":"PRE_AMBARI_START",
                "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                "id":1,
                "public":true
              }
            ],
            "metadata":[]
          }
        ],
        "rdsConfigIds":[],
        "rdsConfigs":[],
        "serviceEndPoints":{},
        "configStrategy":"ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES",
        "ldapConfigId":null,
        "ldapConfig":null,
        "attributes":{},
        "blueprintInputs":[],
        "blueprintCustomProperties":null,
        "executorType":"DEFAULT",
        "gateway":
        {
          "enableGateway":false,
          "path":"openstack-cluster",
          "topologyName":"services",
          "exposedServices":[],
          "ssoProvider":"/openstack-cluster/knoxsso/api/v1/websso",
          "tokenCert":null,
          "gatewayType":"INDIVIDUAL",
          "ssoType":"NONE"
        },
        "customContainers":
        {
          "definitions":{}
        },
        "ambariStackDetails":
        {
          "stack":
          {
            "repoid":"HDP",
            "repository-version":"2.6.3.0-235",
            "redhat7":null,
            "vdf-url":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
          },
          "util":
          {
            "repoid":null,
            "redhat7":null
          },
          "knox":null,
          "verify":false,
          "hdpVersion":"2.6"
        },
        "ambariRepoDetailsJson":{
          "version":"2.6.0.0",
          "baseUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
          "gpgKeyUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
        },
        "ambariDatabaseDetails":
        {
          "vendor":null,
          "name":"postgres",
          "host":"localhost",
          "port":5432,
          "userName":null,
          "password":null
        },
        "customQueue":"default",
        "creationFinished":null
      },
      "statusReason":"Failed to create the stack for CloudContext{id=3937, name='openstack-cluster', platform='StringType{value='OPENSTACK'}', owner='a0b9ae1b-8dc5-45ad-bada-567170cac68f'} due to: Resource CREATE failed: ConnectionError: resources.ambari_master_0: ('Connection aborted.', BadStatusLine(\"''\",))",
      "credential":
      {
        "name":"openstack",
        "cloudPlatform":"OPENSTACK",
        "parameters":
        {
          "facing":"internal",
          "endpoint":"http://openstack.eng.com:3000/v2.0",
          "selector":"cb-keystone-v2",
          "keystoneVersion":"cb-keystone-v2",
          "userName":"cloudbreak",
          "tenantName":"cloudbreak"
        },
        "description":"",
        "topologyId":null,
        "id":1,
        "public":false
      },
      "network":
      {
        "description":null,
        "subnetCIDR":null,
        "cloudPlatform":"OPENSTACK",
        "parameters":
        {
          "publicNetId":null,
          "routerId":null,
          "internetGatewayId":null,
          "subnetId":"0404bf21-db5f-4987-8576-e65a4a99b14e",
          "networkingOption":"self-service",
          "networkId":"a5ad7a1d-d3a6-4180-8d61-07a23f6fb449"
        },
        "topologyId":null,
        "name":"ncc1f02de-0ff6-404b-ad4e-526cfb620d87",
        "id":1,
        "publicInAccount":false
      },
      "instanceGroups":
      [
        {
          "templateId":1,
          "securityGroupId":1,
          "nodeCount":1,
          "group":"worker",
          "type":"CORE",
          "parameters":{},
          "template":
          {
            "cloudPlatform":"OPENSTACK",
            "parameters":
            {
              "encrypted":false
            },
            "description":"",
            "volumeType":"HDD",
            "instanceType":"cloudbreak",
            "customInstanceType":null,
            "topologyId":null,
            "name":"taef3eebf-c93c-49c9-af5d-37d1ec97d160",
            "id":1,
            "volumeCount":0,
            "volumeSize":100,
            "public":false
          },
          "securityGroup":
          {
            "description":null,
            "securityGroupId":null,
            "cloudPlatform":"OPENSTACK",
            "name":"sga9cd44e7-a324-4828-8101-0a273d7fe4a2",
            "id":1,
            "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "securityRules":
            [
              {
                "subnet":"0.0.0.0/0",
                "ports":"22",
                "protocol":"tcp",
                "modifiable":false,
                "id":1
              }
            ],
            "publicInAccount":false
          },
          "id":1,
          "metadata":
          [
            {
              "privateIp":null,
              "publicIp":null,
              "sshPort":null,
              "instanceId":null,
              "ambariServer":null,
              "discoveryFQDN":null,
              "instanceGroup":"worker",
              "instanceStatus":"REQUESTED",
              "instanceType":null
            }
          ]
        },{
          "templateId":1,
          "securityGroupId":1,
          "nodeCount":0,
          "group":"compute",
          "type":"CORE",
          "parameters":{},
          "template":
          {
            "cloudPlatform":"OPENSTACK",
            "parameters":
            {
              "encrypted":false
            },
            "description":"",
            "volumeType":"HDD",
            "instanceType":"cloudbreak",
            "customInstanceType":null,
            "topologyId":null,
            "name":"t2655eeae-a4e5-43f4-9af7-3365ed493776",
            "id":1,
            "volumeCount":0,
            "volumeSize":100,
            "public":false
          },
          "securityGroup":
          {
            "description":null,
            "securityGroupId":null,
            "cloudPlatform":"OPENSTACK",
            "name":"sgb4c28e84-fa47-4ce9-bf85-f1fabcb9c774",
            "id":1,
            "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "securityRules":
            [
              {
                "subnet":"0.0.0.0/0",
                "ports":"22",
                "protocol":"tcp",
                "modifiable":false,
                "id":1
              }
            ],
            "publicInAccount":false
          },
          "id":2,
          "metadata":[]
        },{
          "templateId":1,
          "securityGroupId":2,
          "nodeCount":1,
          "group":"master",
          "type":"GATEWAY",
          "parameters":{},
          "template":
          {
            "cloudPlatform":"OPENSTACK",
            "parameters":
            {
              "encrypted":false
            },
            "description":"",
            "volumeType":"HDD",
            "instanceType":"cloudbreak",
            "customInstanceType":null,
            "topologyId":null,
            "name":"t6445f510-d972-4914-9192-9dfe9d841ec9",
            "id":1,
            "volumeCount":0,
            "volumeSize":100,
            "public":false
          },
          "securityGroup":
          {
            "description":null,
            "securityGroupId":null,
            "cloudPlatform":"OPENSTACK",
            "name":"sgcc9c4d11-0926-4e5e-b6c3-8206fbca2c5a",
            "id":2,
            "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "securityRules":
            [
              {
              "subnet":"0.0.0.0/0",
              "ports":"443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4200
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4199
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4198
            },{
              "subnet":"0.0.0.0/0",
              "ports":"9443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4197
            }
          ],
          "publicInAccount":false
        },
        "id":3,
        "metadata":
        [
          {
            "privateIp":null,
            "publicIp":null,
            "sshPort":null,
            "instanceId":null,
            "ambariServer":null,
            "discoveryFQDN":null,
            "instanceGroup":"master",
            "instanceStatus":"REQUESTED",
            "instanceType":null
          }
        ]
      }
    ],
    "failurePolicy":null,
    "orchestrator":
    {
      "parameters":{},
      "apiEndpoint":null,
      "type":"SALT"
    },
    "created":1515151198060,
    "gatewayPort":9443,
    "image":
    {
      "imageName":"hdc-hdp--1711101841"
    },
    "cloudbreakDetails":
    {
      "version":"2.4.0-dev.1"
    },
    "flexSubscription":null,
    "stackAuthentication":
    {
      "publicKey":null,
      "publicKeyId":"seq-master",
      "loginUserName":"cloudbreak"
    },
    "nodeCount":2,
    "hardwareInfos":[],
    "cloudbreakEvents":[],
    "cloudbreakUsages":[],
    "cloudPlatform":"OPENSTACK",
    "public":false
  };
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

exports.getPublicsStack = function(args, res, next) {
  /**
   * retrieve public and private (owned) stacks
   * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] =
  [
    {
      "name":"openstack-cluster",
      "availabilityZone":"nova",
      "region":"RegionOne",
      "platformVariant":"HEAT",
      "credentialId":1,
      "onFailureAction":"DO_NOTHING",
      "networkId":1,
      "ambariVersion":"2.6.0.0",
      "hdpVersion":"2.6.3.0-235",
      "parameters":{},
      "customDomain":null,
      "customHostname":null,
      "clusterNameAsSubdomain":false,
      "hostgroupNameAsHostname":false,
      "applicationTags":{},
      "userDefinedTags":
      {
        "kisnyul":"pityuka"
      },
      "defaultTags":{},
      "id":1,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "status":"CREATE_FAILED",
      "cluster":
      {
        "id":1,
        "name":"openstack-cluster",
        "status":"REQUESTED",
        "hoursUp":0,
        "minutesUp":0,
        "cluster":null,
        "blueprintId":7,
        "blueprint":
        {
          "ambariBlueprint":"eyJCbHVlcHJpbnRzIjp7ImJsdWVwcmludF9uYW1lIjoiaGRwMjYtZWR3LWFuYWx5dGljcyIsInN0YWNrX25hbWUiOiJIRFAiLCJzdGFja192ZXJzaW9uIjoiMi42In0sInNldHRpbmdzIjpbeyJyZWNvdmVyeV9zZXR0aW5ncyI6W119LHsic2VydmljZV9zZXR0aW5ncyI6W3sibmFtZSI6IkhJVkUiLCJjcmVkZW50aWFsX3N0b3JlX2VuYWJsZWQiOiJmYWxzZSJ9XX0seyJjb21wb25lbnRfc2V0dGluZ3MiOltdfV0sImNvbmZpZ3VyYXRpb25zIjpbeyJoaXZlLWludGVyYWN0aXZlLWVudiI6eyJlbmFibGVfaGl2ZV9pbnRlcmFjdGl2ZSI6InRydWUifX0seyJjb3JlLXNpdGUiOnsiZnMudHJhc2guaW50ZXJ2YWwiOiI0MzIwIn19LHsiaGRmcy1zaXRlIjp7ImRmcy5uYW1lbm9kZS5zYWZlbW9kZS50aHJlc2hvbGQtcGN0IjoiMC45OSJ9fSx7ImhpdmUtaW50ZXJhY3RpdmUtc2l0ZSI6eyJoaXZlLmV4ZWMub3JjLnNwbGl0LnN0cmF0ZWd5IjoiQkkiLCJoaXZlLnN0YXRzLmZldGNoLmJpdHZlY3RvciI6InRydWUiLCJoaXZlLm1ldGFzdG9yZS5yYXdzdG9yZS5pbXBsIjoib3JnLmFwYWNoZS5oYWRvb3AuaGl2ZS5tZXRhc3RvcmUuY2FjaGUuQ2FjaGVkU3RvcmUifX0seyJoaXZlLXNpdGUiOnsiaGl2ZS5leGVjLmNvbXByZXNzLm91dHB1dCI6InRydWUiLCJoaXZlLm1lcmdlLm1hcGZpbGVzIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50ZXouaW5pdGlhbGl6ZS5kZWZhdWx0LnNlc3Npb25zIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50cmFuc3BvcnQubW9kZSI6Imh0dHAifX0seyJtYXByZWQtc2l0ZSI6eyJtYXByZWR1Y2Uuam9iLnJlZHVjZS5zbG93c3RhcnQuY29tcGxldGVkbWFwcyI6IjAuNyIsIm1hcHJlZHVjZS5tYXAub3V0cHV0LmNvbXByZXNzIjoidHJ1ZSIsIm1hcHJlZHVjZS5vdXRwdXQuZmlsZW91dHB1dGZvcm1hdC5jb21wcmVzcyI6InRydWUifX0seyJ0ZXotc2l0ZSI6eyJ0ZXoucnVudGltZS5zaHVmZmxlLnBhcmFsbGVsLmNvcGllcyI6IjQiLCJ0ZXoucnVudGltZS5lbmFibGUuZmluYWwtbWVyZ2UuaW4ub3V0cHV0IjoiZmFsc2UiLCJ0ZXouYW0uYW0tcm0uaGVhcnRiZWF0LmludGVydmFsLW1zLm1heCI6IjIwMDAifX0seyJ5YXJuLXNpdGUiOnsieWFybi5hY2wuZW5hYmxlIjoidHJ1ZSJ9fV0sImhvc3RfZ3JvdXBzIjpbeyJuYW1lIjoibWFzdGVyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiQVBQX1RJTUVMSU5FX1NFUlZFUiJ9LHsibmFtZSI6IkhDQVQifSx7Im5hbWUiOiJIREZTX0NMSUVOVCJ9LHsibmFtZSI6IkhJU1RPUllTRVJWRVIifSx7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IkhJVkVfTUVUQVNUT1JFIn0seyJuYW1lIjoiSElWRV9TRVJWRVIifSx7Im5hbWUiOiJISVZFX1NFUlZFUl9JTlRFUkFDVElWRSJ9LHsibmFtZSI6IkpPVVJOQUxOT0RFIn0seyJuYW1lIjoiTElWWV9TRVJWRVIifSx7Im5hbWUiOiJNQVBSRURVQ0UyX0NMSUVOVCJ9LHsibmFtZSI6Ik1FVFJJQ1NfQ09MTEVDVE9SIn0seyJuYW1lIjoiTUVUUklDU19NT05JVE9SIn0seyJuYW1lIjoiTVlTUUxfU0VSVkVSIn0seyJuYW1lIjoiTkFNRU5PREUifSx7Im5hbWUiOiJQSUcifSx7Im5hbWUiOiJSRVNPVVJDRU1BTkFHRVIifSx7Im5hbWUiOiJTRUNPTkRBUllfTkFNRU5PREUifSx7Im5hbWUiOiJTTElERVIifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJTUEFSS19KT0JISVNUT1JZU0VSVkVSIn0seyJuYW1lIjoiVEVaX0NMSUVOVCJ9LHsibmFtZSI6IldFQkhDQVRfU0VSVkVSIn0seyJuYW1lIjoiWUFSTl9DTElFTlQifSx7Im5hbWUiOiJaRVBQRUxJTl9NQVNURVIifSx7Im5hbWUiOiJaT09LRUVQRVJfQ0xJRU5UIn0seyJuYW1lIjoiWk9PS0VFUEVSX1NFUlZFUiJ9XSwiY2FyZGluYWxpdHkiOiIxIn0seyJuYW1lIjoid29ya2VyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiSElWRV9DTElFTlQifSx7Im5hbWUiOiJURVpfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiREFUQU5PREUifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9LHsibmFtZSI6ImNvbXB1dGUiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9XX0=",
          "description":"Useful for EDW analytics using Hive LLAP",
          "inputs":[],
          "name":"EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0",
          "id":7,
          "hostGroupCount":3,
          "status":"DEFAULT","public":true
        },
        "description":"",
        "statusReason":null,
        "ambariServerIp":null,
        "ambariServerUrl":null,
        "userName":"admin",
        "secure":false,
        "hostGroups":
        [
          {
            "name":"worker",
            "constraint":
            {
              "instanceGroupName":"worker",
              "constraintTemplateName":null,
              "hostCount":1
            },
            "recipeIds":[1],
            "recoveryMode":"AUTO",
            "id":1,
            "recipes":
            [
              {
                "name":"pre-start",
                "description":"",
                "recipeType":"PRE_AMBARI_START",
                "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                "id":1,
                "public":true
              }
            ],
            "metadata":[]
          },{
            "name":"compute",
            "constraint":
            {
              "instanceGroupName":"compute",
              "constraintTemplateName":null,
              "hostCount":0
            },
            "recipeIds":[1],
            "recoveryMode":"AUTO",
            "id":2,
            "recipes":
            [
              {
                "name":"pre-start",
                "description":"",
                "recipeType":"PRE_AMBARI_START",
                "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                "id":1,
                "public":true
              }
            ],
            "metadata":[]
          },{
            "name":"master",
            "constraint":
            {
              "instanceGroupName":"master",
              "constraintTemplateName":null,
              "hostCount":1
            },
            "recipeIds":[1],
            "recoveryMode":"AUTO",
            "id":3,
            "recipes":
            [
              {
                "name":"pre-start",
                "description":"",
                "recipeType":"PRE_AMBARI_START",
                "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                "id":1,
                "public":true
              }
            ],
            "metadata":[]
          }
        ],
        "rdsConfigIds":[],
        "rdsConfigs":[],
        "serviceEndPoints":{},
        "configStrategy":"ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES",
        "ldapConfigId":null,
        "ldapConfig":null,
        "attributes":{},
        "blueprintInputs":[],
        "blueprintCustomProperties":null,
        "executorType":"DEFAULT",
        "gateway":
        {
          "enableGateway":false,
          "path":"openstack-cluster",
          "topologyName":"services",
          "exposedServices":[],
          "ssoProvider":"/openstack-cluster/knoxsso/api/v1/websso",
          "tokenCert":null,
          "gatewayType":"INDIVIDUAL",
          "ssoType":"NONE"
        },
        "customContainers":
        {
          "definitions":{}
        },
        "ambariStackDetails":
        {
          "stack":
          {
            "repoid":"HDP",
            "repository-version":"2.6.3.0-235",
            "redhat7":null,
            "vdf-url":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
          },
          "util":
          {
            "repoid":null,
            "redhat7":null
          },
          "knox":null,
          "verify":false,
          "hdpVersion":"2.6"
        },
        "ambariRepoDetailsJson":{
          "version":"2.6.0.0",
          "baseUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
          "gpgKeyUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
        },
        "ambariDatabaseDetails":
        {
          "vendor":null,
          "name":"postgres",
          "host":"localhost",
          "port":5432,
          "userName":null,
          "password":null
        },
        "customQueue":"default",
        "creationFinished":null
      },
      "statusReason":"Failed to create the stack for CloudContext{id=3937, name='openstack-cluster', platform='StringType{value='OPENSTACK'}', owner='a0b9ae1b-8dc5-45ad-bada-567170cac68f'} due to: Resource CREATE failed: ConnectionError: resources.ambari_master_0: ('Connection aborted.', BadStatusLine(\"''\",))",
      "credential":
      {
        "name":"openstack",
        "cloudPlatform":"OPENSTACK",
        "parameters":
        {
          "facing":"internal",
          "endpoint":"http://openstack.eng.com:3000/v2.0",
          "selector":"cb-keystone-v2",
          "keystoneVersion":"cb-keystone-v2",
          "userName":"cloudbreak",
          "tenantName":"cloudbreak"
        },
        "description":"",
        "topologyId":null,
        "id":1,
        "public":false
      },
      "network":
      {
        "description":null,
        "subnetCIDR":null,
        "cloudPlatform":"OPENSTACK",
        "parameters":
        {
          "publicNetId":null,
          "routerId":null,
          "internetGatewayId":null,
          "subnetId":"0404bf21-db5f-4987-8576-e65a4a99b14e",
          "networkingOption":"self-service",
          "networkId":"a5ad7a1d-d3a6-4180-8d61-07a23f6fb449"
        },
        "topologyId":null,
        "name":"ncc1f02de-0ff6-404b-ad4e-526cfb620d87",
        "id":1,
        "publicInAccount":false
      },
      "instanceGroups":
      [
        {
          "templateId":1,
          "securityGroupId":1,
          "nodeCount":1,
          "group":"worker",
          "type":"CORE",
          "parameters":{},
          "template":
          {
            "cloudPlatform":"OPENSTACK",
            "parameters":
            {
              "encrypted":false
            },
            "description":"",
            "volumeType":"HDD",
            "instanceType":"cloudbreak",
            "customInstanceType":null,
            "topologyId":null,
            "name":"taef3eebf-c93c-49c9-af5d-37d1ec97d160",
            "id":1,
            "volumeCount":0,
            "volumeSize":100,
            "public":false
          },
          "securityGroup":
          {
            "description":null,
            "securityGroupId":null,
            "cloudPlatform":"OPENSTACK",
            "name":"sga9cd44e7-a324-4828-8101-0a273d7fe4a2",
            "id":1,
            "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "securityRules":
            [
              {
                "subnet":"0.0.0.0/0",
                "ports":"22",
                "protocol":"tcp",
                "modifiable":false,
                "id":1
              }
            ],
            "publicInAccount":false
          },
          "id":1,
          "metadata":
          [
            {
              "privateIp":null,
              "publicIp":null,
              "sshPort":null,
              "instanceId":null,
              "ambariServer":null,
              "discoveryFQDN":null,
              "instanceGroup":"worker",
              "instanceStatus":"REQUESTED",
              "instanceType":null
            }
          ]
        },{
          "templateId":1,
          "securityGroupId":1,
          "nodeCount":0,
          "group":"compute",
          "type":"CORE",
          "parameters":{},
          "template":
          {
            "cloudPlatform":"OPENSTACK",
            "parameters":
            {
              "encrypted":false
            },
            "description":"",
            "volumeType":"HDD",
            "instanceType":"cloudbreak",
            "customInstanceType":null,
            "topologyId":null,
            "name":"t2655eeae-a4e5-43f4-9af7-3365ed493776",
            "id":1,
            "volumeCount":0,
            "volumeSize":100,
            "public":false
          },
          "securityGroup":
          {
            "description":null,
            "securityGroupId":null,
            "cloudPlatform":"OPENSTACK",
            "name":"sgb4c28e84-fa47-4ce9-bf85-f1fabcb9c774",
            "id":1,
            "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "securityRules":
            [
              {
                "subnet":"0.0.0.0/0",
                "ports":"22",
                "protocol":"tcp",
                "modifiable":false,
                "id":1
              }
            ],
            "publicInAccount":false
          },
          "id":2,
          "metadata":[]
        },{
          "templateId":1,
          "securityGroupId":2,
          "nodeCount":1,
          "group":"master",
          "type":"GATEWAY",
          "parameters":{},
          "template":
          {
            "cloudPlatform":"OPENSTACK",
            "parameters":
            {
              "encrypted":false
            },
            "description":"",
            "volumeType":"HDD",
            "instanceType":"cloudbreak",
            "customInstanceType":null,
            "topologyId":null,
            "name":"t6445f510-d972-4914-9192-9dfe9d841ec9",
            "id":1,
            "volumeCount":0,
            "volumeSize":100,
            "public":false
          },
          "securityGroup":
          {
            "description":null,
            "securityGroupId":null,
            "cloudPlatform":"OPENSTACK",
            "name":"sgcc9c4d11-0926-4e5e-b6c3-8206fbca2c5a",
            "id":2,
            "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
            "securityRules":
            [
              {
              "subnet":"0.0.0.0/0",
              "ports":"443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4200
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4199
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4198
            },{
              "subnet":"0.0.0.0/0",
              "ports":"9443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4197
            }
          ],
          "publicInAccount":false
        },
        "id":3,
        "metadata":
        [
          {
            "privateIp":null,
            "publicIp":null,
            "sshPort":null,
            "instanceId":null,
            "ambariServer":null,
            "discoveryFQDN":null,
            "instanceGroup":"master",
            "instanceStatus":"REQUESTED",
            "instanceType":null
          }
        ]
      }
    ],
    "failurePolicy":null,
    "orchestrator":
    {
      "parameters":{},
      "apiEndpoint":null,
      "type":"SALT"
    },
    "created":1515151198060,
    "gatewayPort":9443,
    "image":
    {
      "imageName":"hdc-hdp--1711101841"
    },
    "cloudbreakDetails":
    {
      "version":"2.4.0-dev.1"
    },
    "flexSubscription":null,
    "stackAuthentication":
    {
      "publicKey":null,
      "publicKeyId":"seq-master",
      "loginUserName":"cloudbreak"
    },
    "nodeCount":2,
    "hardwareInfos":[],
    "cloudbreakEvents":[],
    "cloudbreakUsages":[],
    "cloudPlatform":"OPENSTACK",
    "public":false
  },{
    "name":"aws-cluster",
    "availabilityZone":"eu-west-1a",
    "region":"eu-west-1",
    "platformVariant":"AWS",
    "credentialId":4,
    "onFailureAction":"DO_NOTHING",
    "networkId":2,
    "ambariVersion":"2.6.0.0",
    "hdpVersion":"2.6.3.0-235",
    "parameters":
    {
      "instanceProfileStrategy":"CREATE",
      "AWS_SUBNET":"subnet-445d0c1f",
      "AWS_S3_ROLE":"arn:aws:iam::755047402263:role/aws-cluster-3938-S3AccessRole-1WONDIIEGB2GR",
      "AWS_VPC":"vpc-3c0ae35a"
    },
    "customDomain":null,
    "customHostname":null,
    "clusterNameAsSubdomain":false,
    "hostgroupNameAsHostname":false,
    "applicationTags":{},
    "userDefinedTags":
    {
      "kisnyul":"pityuka"
    },
    "defaultTags":{},
    "id":2,
    "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
    "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
    "status":"UPDATE_IN_PROGRESS",
    "cluster":
    {
      "id":2,
      "name":"aws-cluster",
      "status":"REQUESTED",
      "hoursUp":0,
      "minutesUp":0,
      "cluster":null,
      "blueprintId":7,
      "blueprint":
      {
        "ambariBlueprint":"eyJCbHVlcHJpbnRzIjp7ImJsdWVwcmludF9uYW1lIjoiaGRwMjYtZWR3LWFuYWx5dGljcyIsInN0YWNrX25hbWUiOiJIRFAiLCJzdGFja192ZXJzaW9uIjoiMi42In0sInNldHRpbmdzIjpbeyJyZWNvdmVyeV9zZXR0aW5ncyI6W119LHsic2VydmljZV9zZXR0aW5ncyI6W3sibmFtZSI6IkhJVkUiLCJjcmVkZW50aWFsX3N0b3JlX2VuYWJsZWQiOiJmYWxzZSJ9XX0seyJjb21wb25lbnRfc2V0dGluZ3MiOltdfV0sImNvbmZpZ3VyYXRpb25zIjpbeyJoaXZlLWludGVyYWN0aXZlLWVudiI6eyJlbmFibGVfaGl2ZV9pbnRlcmFjdGl2ZSI6InRydWUifX0seyJjb3JlLXNpdGUiOnsiZnMudHJhc2guaW50ZXJ2YWwiOiI0MzIwIn19LHsiaGRmcy1zaXRlIjp7ImRmcy5uYW1lbm9kZS5zYWZlbW9kZS50aHJlc2hvbGQtcGN0IjoiMC45OSJ9fSx7ImhpdmUtaW50ZXJhY3RpdmUtc2l0ZSI6eyJoaXZlLmV4ZWMub3JjLnNwbGl0LnN0cmF0ZWd5IjoiQkkiLCJoaXZlLnN0YXRzLmZldGNoLmJpdHZlY3RvciI6InRydWUiLCJoaXZlLm1ldGFzdG9yZS5yYXdzdG9yZS5pbXBsIjoib3JnLmFwYWNoZS5oYWRvb3AuaGl2ZS5tZXRhc3RvcmUuY2FjaGUuQ2FjaGVkU3RvcmUifX0seyJoaXZlLXNpdGUiOnsiaGl2ZS5leGVjLmNvbXByZXNzLm91dHB1dCI6InRydWUiLCJoaXZlLm1lcmdlLm1hcGZpbGVzIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50ZXouaW5pdGlhbGl6ZS5kZWZhdWx0LnNlc3Npb25zIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50cmFuc3BvcnQubW9kZSI6Imh0dHAifX0seyJtYXByZWQtc2l0ZSI6eyJtYXByZWR1Y2Uuam9iLnJlZHVjZS5zbG93c3RhcnQuY29tcGxldGVkbWFwcyI6IjAuNyIsIm1hcHJlZHVjZS5tYXAub3V0cHV0LmNvbXByZXNzIjoidHJ1ZSIsIm1hcHJlZHVjZS5vdXRwdXQuZmlsZW91dHB1dGZvcm1hdC5jb21wcmVzcyI6InRydWUifX0seyJ0ZXotc2l0ZSI6eyJ0ZXoucnVudGltZS5zaHVmZmxlLnBhcmFsbGVsLmNvcGllcyI6IjQiLCJ0ZXoucnVudGltZS5lbmFibGUuZmluYWwtbWVyZ2UuaW4ub3V0cHV0IjoiZmFsc2UiLCJ0ZXouYW0uYW0tcm0uaGVhcnRiZWF0LmludGVydmFsLW1zLm1heCI6IjIwMDAifX0seyJ5YXJuLXNpdGUiOnsieWFybi5hY2wuZW5hYmxlIjoidHJ1ZSJ9fV0sImhvc3RfZ3JvdXBzIjpbeyJuYW1lIjoibWFzdGVyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiQVBQX1RJTUVMSU5FX1NFUlZFUiJ9LHsibmFtZSI6IkhDQVQifSx7Im5hbWUiOiJIREZTX0NMSUVOVCJ9LHsibmFtZSI6IkhJU1RPUllTRVJWRVIifSx7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IkhJVkVfTUVUQVNUT1JFIn0seyJuYW1lIjoiSElWRV9TRVJWRVIifSx7Im5hbWUiOiJISVZFX1NFUlZFUl9JTlRFUkFDVElWRSJ9LHsibmFtZSI6IkpPVVJOQUxOT0RFIn0seyJuYW1lIjoiTElWWV9TRVJWRVIifSx7Im5hbWUiOiJNQVBSRURVQ0UyX0NMSUVOVCJ9LHsibmFtZSI6Ik1FVFJJQ1NfQ09MTEVDVE9SIn0seyJuYW1lIjoiTUVUUklDU19NT05JVE9SIn0seyJuYW1lIjoiTVlTUUxfU0VSVkVSIn0seyJuYW1lIjoiTkFNRU5PREUifSx7Im5hbWUiOiJQSUcifSx7Im5hbWUiOiJSRVNPVVJDRU1BTkFHRVIifSx7Im5hbWUiOiJTRUNPTkRBUllfTkFNRU5PREUifSx7Im5hbWUiOiJTTElERVIifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJTUEFSS19KT0JISVNUT1JZU0VSVkVSIn0seyJuYW1lIjoiVEVaX0NMSUVOVCJ9LHsibmFtZSI6IldFQkhDQVRfU0VSVkVSIn0seyJuYW1lIjoiWUFSTl9DTElFTlQifSx7Im5hbWUiOiJaRVBQRUxJTl9NQVNURVIifSx7Im5hbWUiOiJaT09LRUVQRVJfQ0xJRU5UIn0seyJuYW1lIjoiWk9PS0VFUEVSX1NFUlZFUiJ9XSwiY2FyZGluYWxpdHkiOiIxIn0seyJuYW1lIjoid29ya2VyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiSElWRV9DTElFTlQifSx7Im5hbWUiOiJURVpfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiREFUQU5PREUifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9LHsibmFtZSI6ImNvbXB1dGUiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9XX0=",
        "description":"Useful for EDW analytics using Hive LLAP",
        "inputs":[],
        "name":"EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0",
        "id":7,
        "hostGroupCount":3,
        "status":"DEFAULT",
        "public":true
      },
      "description":"",
      "statusReason":null,
      "ambariServerIp":"52.16.174.228",
      "ambariServerUrl":"https://52.16.174.228/ambari/",
      "userName":"admin",
      "secure":false,
      "hostGroups":
      [
        {
          "name":"worker",
          "constraint":
          {
            "instanceGroupName":"worker",
            "constraintTemplateName":null,
            "hostCount":1
          },
          "recipeIds":[1],
          "recoveryMode":"AUTO",
          "id":4,
          "recipes":
          [
            {
              "name":"pre-start",
              "description":"",
              "recipeType":"PRE_AMBARI_START",
              "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
              "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
              "id":1,
              "public":true
            }
          ],
          "metadata":[]
        },{
          "name":"compute",
          "constraint":
          {
            "instanceGroupName":"compute",
            "constraintTemplateName":null,
            "hostCount":0
          },
          "recipeIds":[1],
          "recoveryMode":"AUTO",
          "id":5,
          "recipes":
          [
            {
              "name":"pre-start",
              "description":"",
              "recipeType":"PRE_AMBARI_START",
              "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
              "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
              "id":1,
              "public":true
            }
          ],
          "metadata":[]
        },{
          "name":"master",
          "constraint":
          {
            "instanceGroupName":"master",
            "constraintTemplateName":null,
            "hostCount":1
          },
          "recipeIds":[1],
          "recoveryMode":"AUTO",
          "id":6,
          "recipes":
          [
            {
              "name":"pre-start",
              "description":"",
              "recipeType":"PRE_AMBARI_START",
              "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
              "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
              "id":1,
              "public":true
            }
          ],
          "metadata":[]
        }
      ],
      "rdsConfigIds":[],
      "rdsConfigs":[],
      "serviceEndPoints":
      {
        "Hive Server":"http://52.16.174.228:10000",
        "Zeppelin UI":"http://52.16.174.228:9995",
        "Job History Server":"http://52.16.174.228:19888",
        "Name Node":"http://52.16.174.228:50070",
        "Zeppelin Web Socket":"http://52.16.174.228:9996",
        "Resource Manager":"http://52.16.174.228:8088",
        "Spark History Server":"http://52.16.174.228:18080"
      },
      "configStrategy":"ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES",
      "ldapConfigId":null,
      "ldapConfig":null,
      "attributes":{},
      "blueprintInputs":[],
      "blueprintCustomProperties":null,
      "executorType":"DEFAULT",
      "gateway":
      {
        "enableGateway":false,
        "path":"aws-cluster",
        "topologyName":"services",
        "exposedServices":[],
        "ssoProvider":"/aws-cluster/knoxsso/api/v1/websso",
        "tokenCert":null,
        "gatewayType":"INDIVIDUAL",
        "ssoType":"NONE"
      },
      "customContainers":
      {
        "definitions":{}
      },
      "ambariStackDetails":
      {
        "stack":
        {
          "repoid":"HDP",
          "repository-version":"2.6.3.0-235",
          "redhat6":null,
          "vdf-url":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
        },
        "util":
        {
          "repoid":null,
          "redhat6":null
        },
        "knox":null,
        "verify":false,
        "hdpVersion":"2.6"
      },
      "ambariRepoDetailsJson":
      {
        "version":"2.6.0.0",
        "baseUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
        "gpgKeyUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
      },
      "ambariDatabaseDetails":
      {
        "vendor":null,
        "name":"postgres",
        "host":"localhost",
        "port":5432,
        "userName":null,
        "password":null
      },
      "customQueue":"default",
      "creationFinished":null
    },
    "statusReason":"Running cluster services.",
    "credential":
    {
      "name":"amazon",
      "cloudPlatform":"AWS",
      "parameters":
      {
        "smartSenseId":"null",
        "selector":"role-based"
      },
      "description":"",
      "topologyId":null,
      "id":4,
      "public":false
    },
    "network":
    {
      "description":null,
      "subnetCIDR":"10.0.0.0/16",
      "cloudPlatform":"AWS",
      "parameters":
      {
        "publicNetId":null,
        "routerId":null,
        "internetGatewayId":null,
        "networkId":null,
        "vpcId":null
      },
      "topologyId":null,
      "name":"n7c98594f-1afa-4e79-8c77-7697a7d4346f",
      "id":2,
      "publicInAccount":false
    },
    "instanceGroups":
    [
      {
        "templateId":2,
        "securityGroupId":3,
        "nodeCount":0,
        "group":"compute",
        "type":"CORE",
        "parameters":{},
        "template":
        {
          "cloudPlatform":"AWS",
          "parameters":
          {
            "encrypted":false
          },
          "description":"",
          "volumeType":"standard",
          "instanceType":"m4.xlarge",
          "customInstanceType":null,
          "topologyId":null,
          "name":"tb9d8d756-81e4-471f-a7a9-a188fa1ce85b",
          "id":2,
          "volumeCount":1,
          "volumeSize":100,
          "public":false
        },
        "securityGroup":
        {
          "description":null,
          "securityGroupId":null,
          "cloudPlatform":"AWS",
          "name":"sge019005d-5be5-46aa-b181-4f76ab7677a8",
          "id":3,
          "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "securityRules":
          [
            {
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4206
            }
          ],
          "publicInAccount":false
        },
        "id":4,
        "metadata":[]
      },{
        "templateId":3,
        "securityGroupId":4,
        "nodeCount":1,
        "group":"master",
        "type":"GATEWAY",
        "parameters":{},
        "template":
        {
          "cloudPlatform":"AWS",
          "parameters":
          {
            "encrypted":false
          },
          "description":"",
          "volumeType":"standard",
          "instanceType":"m4.4xlarge",
          "customInstanceType":null,
          "topologyId":null,
          "name":"t7b01713b-522e-41b6-bbfc-5c60da63d050",
          "id":3,
          "volumeCount":1,
          "volumeSize":100,
          "public":false
        },
        "securityGroup":
        {
          "description":null,
          "securityGroupId":null,
          "cloudPlatform":"AWS",
          "name":"sg8cb22ed9-d776-4fc1-8b75-65e0fb6d5616",
          "id":4,
          "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "securityRules":
          [
            {
              "subnet":"0.0.0.0/0",
              "ports":"9443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4202
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4203
            },{
              "subnet":"0.0.0.0/0",
              "ports":"443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4205
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4204
            }
          ],
          "publicInAccount":false
        },
        "id":5,
        "metadata":
        [
          {
            "privateIp":"10.0.252.128",
            "publicIp":"52.16.174.228",
            "sshPort":22,
            "instanceId":"i-0f66703ef1de45e84",
            "ambariServer":true,
            "discoveryFQDN":"ip-10-0-252-128.eu-west-1.compute.internal",
            "instanceGroup":"master",
            "instanceStatus":"CREATED",
            "instanceType":"GATEWAY_PRIMARY"
          }
        ]
      },{
        "templateId":2,
        "securityGroupId":3,
        "nodeCount":1,
        "group":"worker",
        "type":"CORE",
        "parameters":{},
        "template":
        {
          "cloudPlatform":"AWS",
          "parameters":
          {
            "encrypted":false
          },
          "description":"",
          "volumeType":"standard",
          "instanceType":"m4.xlarge",
          "customInstanceType":null,
          "topologyId":null,
          "name":"tb9d8d756-81e4-471f-a7a9-a188fa1ce85b",
          "id":2,
          "volumeCount":1,
          "volumeSize":100,
          "public":false
        },
        "securityGroup":
        {
          "description":null,
          "securityGroupId":null,
          "cloudPlatform":"AWS",
          "name":"sg4c745e66-82a8-48f1-96a4-88ae3cf9cfd8",
          "id":3,
          "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "securityRules":
          [
            {
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4207
            }
          ],
          "publicInAccount":false
        },
        "id":6,
        "metadata":
        [
          {
            "privateIp":"10.0.125.204",
            "publicIp":"52.51.255.205",
            "sshPort":22,
            "instanceId":"i-084488aa2fc89ca7e",
            "ambariServer":false,
            "discoveryFQDN":"ip-10-0-125-204.eu-west-1.compute.internal",
            "instanceGroup":"worker",
            "instanceStatus":"CREATED",
            "instanceType":"CORE"
          }
        ]
      }
    ],
    "failurePolicy":null,
    "orchestrator":
    {
      "parameters":{},
      "apiEndpoint":"52.16.174.228:9443",
      "type":"SALT"
    },
    "created":1515151753768,
    "gatewayPort":9443,
    "image":
    {
      "imageName":"ami-590ba020"
    },
    "cloudbreakDetails":
    {
      "version":"2.4.0-dev.1"
    },
    "flexSubscription":null,
    "stackAuthentication":
    {
      "publicKey":null,
      "publicKeyId":"seq-master",
      "loginUserName":"cloudbreak"
    },
    "nodeCount":2,
    "hardwareInfos":[],
    "cloudbreakEvents":[],
    "cloudbreakUsages":[],
    "cloudPlatform":"AWS",
    "public":false
  },{
    "name":"azure-cluster",
    "availabilityZone":null,
    "region":"North Europe",
    "platformVariant":"AZURE",
    "credentialId":2,
    "onFailureAction":"DO_NOTHING",
    "networkId":3,
    "ambariVersion":"2.6.0.0",
    "hdpVersion":"2.6.3.0-235",
    "parameters":
    {
      "encryptStorage":"false"
    },
    "customDomain":null,
    "customHostname":null,
    "clusterNameAsSubdomain":false,
    "hostgroupNameAsHostname":false,
    "applicationTags":{},
    "userDefinedTags":
    {
      "kisnyul":"pityuka"
    },
    "defaultTags":{},
    "id":3,
    "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
    "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
    "status":"CREATE_IN_PROGRESS",
    "cluster":
    {
      "id":3,
      "name":"azure-cluster",
      "status":"REQUESTED",
      "hoursUp":0,
      "minutesUp":0,
      "cluster":null,
      "blueprintId":7,
      "blueprint":
      {
        "ambariBlueprint":"eyJCbHVlcHJpbnRzIjp7ImJsdWVwcmludF9uYW1lIjoiaGRwMjYtZWR3LWFuYWx5dGljcyIsInN0YWNrX25hbWUiOiJIRFAiLCJzdGFja192ZXJzaW9uIjoiMi42In0sInNldHRpbmdzIjpbeyJyZWNvdmVyeV9zZXR0aW5ncyI6W119LHsic2VydmljZV9zZXR0aW5ncyI6W3sibmFtZSI6IkhJVkUiLCJjcmVkZW50aWFsX3N0b3JlX2VuYWJsZWQiOiJmYWxzZSJ9XX0seyJjb21wb25lbnRfc2V0dGluZ3MiOltdfV0sImNvbmZpZ3VyYXRpb25zIjpbeyJoaXZlLWludGVyYWN0aXZlLWVudiI6eyJlbmFibGVfaGl2ZV9pbnRlcmFjdGl2ZSI6InRydWUifX0seyJjb3JlLXNpdGUiOnsiZnMudHJhc2guaW50ZXJ2YWwiOiI0MzIwIn19LHsiaGRmcy1zaXRlIjp7ImRmcy5uYW1lbm9kZS5zYWZlbW9kZS50aHJlc2hvbGQtcGN0IjoiMC45OSJ9fSx7ImhpdmUtaW50ZXJhY3RpdmUtc2l0ZSI6eyJoaXZlLmV4ZWMub3JjLnNwbGl0LnN0cmF0ZWd5IjoiQkkiLCJoaXZlLnN0YXRzLmZldGNoLmJpdHZlY3RvciI6InRydWUiLCJoaXZlLm1ldGFzdG9yZS5yYXdzdG9yZS5pbXBsIjoib3JnLmFwYWNoZS5oYWRvb3AuaGl2ZS5tZXRhc3RvcmUuY2FjaGUuQ2FjaGVkU3RvcmUifX0seyJoaXZlLXNpdGUiOnsiaGl2ZS5leGVjLmNvbXByZXNzLm91dHB1dCI6InRydWUiLCJoaXZlLm1lcmdlLm1hcGZpbGVzIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50ZXouaW5pdGlhbGl6ZS5kZWZhdWx0LnNlc3Npb25zIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50cmFuc3BvcnQubW9kZSI6Imh0dHAifX0seyJtYXByZWQtc2l0ZSI6eyJtYXByZWR1Y2Uuam9iLnJlZHVjZS5zbG93c3RhcnQuY29tcGxldGVkbWFwcyI6IjAuNyIsIm1hcHJlZHVjZS5tYXAub3V0cHV0LmNvbXByZXNzIjoidHJ1ZSIsIm1hcHJlZHVjZS5vdXRwdXQuZmlsZW91dHB1dGZvcm1hdC5jb21wcmVzcyI6InRydWUifX0seyJ0ZXotc2l0ZSI6eyJ0ZXoucnVudGltZS5zaHVmZmxlLnBhcmFsbGVsLmNvcGllcyI6IjQiLCJ0ZXoucnVudGltZS5lbmFibGUuZmluYWwtbWVyZ2UuaW4ub3V0cHV0IjoiZmFsc2UiLCJ0ZXouYW0uYW0tcm0uaGVhcnRiZWF0LmludGVydmFsLW1zLm1heCI6IjIwMDAifX0seyJ5YXJuLXNpdGUiOnsieWFybi5hY2wuZW5hYmxlIjoidHJ1ZSJ9fV0sImhvc3RfZ3JvdXBzIjpbeyJuYW1lIjoibWFzdGVyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiQVBQX1RJTUVMSU5FX1NFUlZFUiJ9LHsibmFtZSI6IkhDQVQifSx7Im5hbWUiOiJIREZTX0NMSUVOVCJ9LHsibmFtZSI6IkhJU1RPUllTRVJWRVIifSx7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IkhJVkVfTUVUQVNUT1JFIn0seyJuYW1lIjoiSElWRV9TRVJWRVIifSx7Im5hbWUiOiJISVZFX1NFUlZFUl9JTlRFUkFDVElWRSJ9LHsibmFtZSI6IkpPVVJOQUxOT0RFIn0seyJuYW1lIjoiTElWWV9TRVJWRVIifSx7Im5hbWUiOiJNQVBSRURVQ0UyX0NMSUVOVCJ9LHsibmFtZSI6Ik1FVFJJQ1NfQ09MTEVDVE9SIn0seyJuYW1lIjoiTUVUUklDU19NT05JVE9SIn0seyJuYW1lIjoiTVlTUUxfU0VSVkVSIn0seyJuYW1lIjoiTkFNRU5PREUifSx7Im5hbWUiOiJQSUcifSx7Im5hbWUiOiJSRVNPVVJDRU1BTkFHRVIifSx7Im5hbWUiOiJTRUNPTkRBUllfTkFNRU5PREUifSx7Im5hbWUiOiJTTElERVIifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJTUEFSS19KT0JISVNUT1JZU0VSVkVSIn0seyJuYW1lIjoiVEVaX0NMSUVOVCJ9LHsibmFtZSI6IldFQkhDQVRfU0VSVkVSIn0seyJuYW1lIjoiWUFSTl9DTElFTlQifSx7Im5hbWUiOiJaRVBQRUxJTl9NQVNURVIifSx7Im5hbWUiOiJaT09LRUVQRVJfQ0xJRU5UIn0seyJuYW1lIjoiWk9PS0VFUEVSX1NFUlZFUiJ9XSwiY2FyZGluYWxpdHkiOiIxIn0seyJuYW1lIjoid29ya2VyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiSElWRV9DTElFTlQifSx7Im5hbWUiOiJURVpfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiREFUQU5PREUifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9LHsibmFtZSI6ImNvbXB1dGUiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9XX0=",
        "description":"Useful for EDW analytics using Hive LLAP",
        "inputs":[],
        "name":"EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0",
        "id":7,
        "hostGroupCount":3,
        "status":"DEFAULT",
        "public":true
      },
      "description":"",
      "statusReason":null,
      "ambariServerIp":null,
      "ambariServerUrl":null,
      "userName":"admin",
      "secure":false,
      "hostGroups":
      [
        {
          "name":"worker",
          "constraint":
          {
            "instanceGroupName":"worker",
            "constraintTemplateName":null,
            "hostCount":1
          },
          "recipeIds":[],
          "recoveryMode":"AUTO",
          "id":7,
          "recipes":[],
          "metadata":[]
        },{
          "name":"compute",
          "constraint":
          {
            "instanceGroupName":"compute",
            "constraintTemplateName":null,
            "hostCount":0
          },
          "recipeIds":[],
          "recoveryMode":"AUTO",
          "id":8,
          "recipes":[],
          "metadata":[]
        },{
          "name":"master",
          "constraint":
          {
            "instanceGroupName":"master",
            "constraintTemplateName":null,
            "hostCount":1
          },
          "recipeIds":[],
          "recoveryMode":"AUTO",
          "id":9,
          "recipes":[],
          "metadata":[]
        }
      ],
      "rdsConfigIds":[],
      "rdsConfigs":[],
      "serviceEndPoints":{},
      "configStrategy":"ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES",
      "ldapConfigId":null,
      "ldapConfig":null,
      "attributes":{},
      "blueprintInputs":[],
      "blueprintCustomProperties":null,
      "executorType":"DEFAULT",
      "gateway":
      {
        "enableGateway":false,
        "path":"azure-cluster",
        "topologyName":"services",
        "exposedServices":[],
        "ssoProvider":"/azure-cluster/knoxsso/api/v1/websso",
        "tokenCert":null,
        "gatewayType":"INDIVIDUAL",
        "ssoType":"NONE"
      },
      "customContainers":
      {
        "definitions":{}
      },
      "ambariStackDetails":
      {
        "stack":
        {
          "repoid":"HDP",
          "repository-version":"2.6.3.0-235",
          "redhat7":null,
          "vdf-url":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
        },
        "util":
        {
          "repoid":null,
          "redhat7":null
        },
        "knox":null,
        "verify":false,
        "hdpVersion":"2.6"
      },
      "ambariRepoDetailsJson":
      {
        "version":"2.6.0.0","baseUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
        "gpgKeyUrl":"http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
      },
      "ambariDatabaseDetails":
      {
        "vendor":null,
        "name":"postgres",
        "host":"localhost",
        "port":5432,
        "userName":null,
        "password":null
      },
      "customQueue":"default",
      "creationFinished":null
    },
    "statusReason":"Image setup","credential":
    {
      "name":"azure",
      "cloudPlatform":"AZURE",
      "parameters":
      {
        "tenantId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
        "spDisplayName":null,
        "subscriptionId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
        "roleType":null,
        "accessKey":"a12b1234-1234-12aa-3bcc-4d5e6f78900g"
      },
      "description":"",
      "topologyId":null,
      "id":2,
      "public":false
    },
    "network":
    {
      "description":null,
      "subnetCIDR":"10.0.0.0/16",
      "cloudPlatform":"AZURE",
      "parameters":
      {
        "publicNetId":null,
        "routerId":null,
        "internetGatewayId":null,
        "noPublicIp":false,
        "noFirewallRules":false,
        "networkId":null,
        "vpcId":null
      },
      "topologyId":null,
      "name":"nceddf172-7b62-41cc-b9ef-e6bdf15b08ea",
      "id":3,
      "publicInAccount":false
    },
    "instanceGroups":
    [
      {
        "templateId":4,
        "securityGroupId":5,
        "nodeCount":1,
        "group":"master",
        "type":"GATEWAY",
        "parameters":{},
        "template":
        {
          "cloudPlatform":"AZURE",
          "parameters":
          {
            "encrypted":false,
            "managedDisk":true
          },
          "description":"",
          "volumeType":"Standard_LRS",
          "instanceType":"Standard_D3_v2",
          "customInstanceType":null,
          "topologyId":null,
          "name":"t38826b32-a741-473a-9ab9-0510b4fd7829",
          "id":4,
          "volumeCount":1,
          "volumeSize":100,
          "public":false
        },
        "securityGroup":
        {
          "description":null,
          "securityGroupId":null,
          "cloudPlatform":"AZURE",
          "name":"sgebd08127-2a83-4165-b944-c868894fe45a",
          "id":5,
          "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "securityRules":
          [
            {
              "subnet":"0.0.0.0/0",
              "ports":"443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4211
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4210
            },{
              "subnet":"0.0.0.0/0",
              "ports":"9443",
              "protocol":"tcp",
              "modifiable":false,
              "id":4208
            },{
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4209
            }
          ],
          "publicInAccount":false
        },
        "id":7,
        "metadata":[]
      },{
        "templateId":4,
        "securityGroupId":6,
        "nodeCount":1,
        "group":"worker",
        "type":"CORE",
        "parameters":{},
        "template":
        {
          "cloudPlatform":"AZURE",
          "parameters":
          {
            "encrypted":false,
            "managedDisk":true
          },
          "description":"",
          "volumeType":"Standard_LRS",
          "instanceType":"Standard_D3_v2",
          "customInstanceType":null,
          "topologyId":null,
          "name":"t38826b32-a741-473a-9ab9-0510b4fd7829",
          "id":4,
          "volumeCount":1,
          "volumeSize":100,
          "public":false
        },
        "securityGroup":
        {
          "description":null,
          "securityGroupId":null,
          "cloudPlatform":"AZURE",
          "name":"sg17b54a58-abb4-4eaa-9558-4d9596a28ce1",
          "id":6,
          "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "securityRules":
          [
            {
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4212
            }
          ],
          "publicInAccount":false
        },
        "id":8,
        "metadata":[]
      },{
        "templateId":4,
        "securityGroupId":6,
        "nodeCount":0,
        "group":"compute",
        "type":"CORE",
        "parameters":{},
        "template":
        {
          "cloudPlatform":"AZURE",
          "parameters":
          {
            "encrypted":false,
            "managedDisk":true
          },
          "description":"",
          "volumeType":"Standard_LRS",
          "instanceType":"Standard_D3_v2",
          "customInstanceType":null,
          "topologyId":null,
          "name":"t38826b32-a741-473a-9ab9-0510b4fd7829",
          "id":4,
          "volumeCount":1,
          "volumeSize":100,
          "public":false
        },
        "securityGroup":
        {
          "description":null,
          "securityGroupId":null,
          "cloudPlatform":"AZURE",
          "name":"sg17b54a58-abb4-4eaa-9558-4d9596a28ce1",
          "id":6,
          "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
          "securityRules":
          [
            {
              "subnet":"0.0.0.0/0",
              "ports":"22",
              "protocol":"tcp",
              "modifiable":false,
              "id":4212
            }
          ],
          "publicInAccount":false
        },
        "id":9,
        "metadata":[]
      }
    ],
    "failurePolicy":null,
    "orchestrator":
    {
      "parameters":{},
      "apiEndpoint":null,
      "type":"SALT"
    },
    "created":1515152314614,
    "gatewayPort":9443,
    "image":
    {
      "imageName":"https://sequenceiqnortheurope2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd"
    },
    "cloudbreakDetails":
    {
      "version":"2.4.0-dev.1"
    },
    "flexSubscription":null,
    "stackAuthentication":
    {
      "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh",
      "publicKeyId":null,
      "loginUserName":"cloudbreak"
    },
    "nodeCount":2,
    "hardwareInfos":[],
    "cloudbreakEvents":[],
    "cloudbreakUsages":[],
    "cloudPlatform":"AZURE",
    "public":false
  },{
      "name": "gcp-cluster",
      "availabilityZone": "europe-west1-b",
      "region": "europe-west1",
      "platformVariant": "GCP",
      "credentialId": 3,
      "onFailureAction": "DO_NOTHING",
      "networkId": 4,
      "ambariVersion": "2.6.0.0",
      "hdpVersion": "2.6.3.0-235",
      "parameters": {},
      "customDomain": null,
      "customHostname": null,
      "clusterNameAsSubdomain": false,
      "hostgroupNameAsHostname": false,
      "applicationTags": {},
      "userDefinedTags": {
          "kisnyul": "pityuka"
      },
      "defaultTags": {},
      "id": 4,
      "owner": "8781acdb-4d87-4dff-814c-35c191031ad3",
      "account": "8781acdb-4d87-4dff-814c-35c191031ad3",
      "status": "AVAILABLE",
      "cluster": {
          "id": 4,
          "name": "gcp-cluster",
          "status": "AVAILABLE",
          "hoursUp": 0,
          "minutesUp": 10,
          "cluster": null,
          "blueprintId": 7,
          "blueprint": {
              "ambariBlueprint": "eyJCbHVlcHJpbnRzIjp7ImJsdWVwcmludF9uYW1lIjoiaGRwMjYtZWR3LWFuYWx5dGljcyIsInN0YWNrX25hbWUiOiJIRFAiLCJzdGFja192ZXJzaW9uIjoiMi42In0sInNldHRpbmdzIjpbeyJyZWNvdmVyeV9zZXR0aW5ncyI6W119LHsic2VydmljZV9zZXR0aW5ncyI6W3sibmFtZSI6IkhJVkUiLCJjcmVkZW50aWFsX3N0b3JlX2VuYWJsZWQiOiJmYWxzZSJ9XX0seyJjb21wb25lbnRfc2V0dGluZ3MiOltdfV0sImNvbmZpZ3VyYXRpb25zIjpbeyJoaXZlLWludGVyYWN0aXZlLWVudiI6eyJlbmFibGVfaGl2ZV9pbnRlcmFjdGl2ZSI6InRydWUifX0seyJjb3JlLXNpdGUiOnsiZnMudHJhc2guaW50ZXJ2YWwiOiI0MzIwIn19LHsiaGRmcy1zaXRlIjp7ImRmcy5uYW1lbm9kZS5zYWZlbW9kZS50aHJlc2hvbGQtcGN0IjoiMC45OSJ9fSx7ImhpdmUtaW50ZXJhY3RpdmUtc2l0ZSI6eyJoaXZlLmV4ZWMub3JjLnNwbGl0LnN0cmF0ZWd5IjoiQkkiLCJoaXZlLnN0YXRzLmZldGNoLmJpdHZlY3RvciI6InRydWUiLCJoaXZlLm1ldGFzdG9yZS5yYXdzdG9yZS5pbXBsIjoib3JnLmFwYWNoZS5oYWRvb3AuaGl2ZS5tZXRhc3RvcmUuY2FjaGUuQ2FjaGVkU3RvcmUifX0seyJoaXZlLXNpdGUiOnsiaGl2ZS5leGVjLmNvbXByZXNzLm91dHB1dCI6InRydWUiLCJoaXZlLm1lcmdlLm1hcGZpbGVzIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50ZXouaW5pdGlhbGl6ZS5kZWZhdWx0LnNlc3Npb25zIjoidHJ1ZSIsImhpdmUuc2VydmVyMi50cmFuc3BvcnQubW9kZSI6Imh0dHAifX0seyJtYXByZWQtc2l0ZSI6eyJtYXByZWR1Y2Uuam9iLnJlZHVjZS5zbG93c3RhcnQuY29tcGxldGVkbWFwcyI6IjAuNyIsIm1hcHJlZHVjZS5tYXAub3V0cHV0LmNvbXByZXNzIjoidHJ1ZSIsIm1hcHJlZHVjZS5vdXRwdXQuZmlsZW91dHB1dGZvcm1hdC5jb21wcmVzcyI6InRydWUifX0seyJ0ZXotc2l0ZSI6eyJ0ZXoucnVudGltZS5zaHVmZmxlLnBhcmFsbGVsLmNvcGllcyI6IjQiLCJ0ZXoucnVudGltZS5lbmFibGUuZmluYWwtbWVyZ2UuaW4ub3V0cHV0IjoiZmFsc2UiLCJ0ZXouYW0uYW0tcm0uaGVhcnRiZWF0LmludGVydmFsLW1zLm1heCI6IjIwMDAifX0seyJ5YXJuLXNpdGUiOnsieWFybi5hY2wuZW5hYmxlIjoidHJ1ZSJ9fV0sImhvc3RfZ3JvdXBzIjpbeyJuYW1lIjoibWFzdGVyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiQVBQX1RJTUVMSU5FX1NFUlZFUiJ9LHsibmFtZSI6IkhDQVQifSx7Im5hbWUiOiJIREZTX0NMSUVOVCJ9LHsibmFtZSI6IkhJU1RPUllTRVJWRVIifSx7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IkhJVkVfTUVUQVNUT1JFIn0seyJuYW1lIjoiSElWRV9TRVJWRVIifSx7Im5hbWUiOiJISVZFX1NFUlZFUl9JTlRFUkFDVElWRSJ9LHsibmFtZSI6IkpPVVJOQUxOT0RFIn0seyJuYW1lIjoiTElWWV9TRVJWRVIifSx7Im5hbWUiOiJNQVBSRURVQ0UyX0NMSUVOVCJ9LHsibmFtZSI6Ik1FVFJJQ1NfQ09MTEVDVE9SIn0seyJuYW1lIjoiTUVUUklDU19NT05JVE9SIn0seyJuYW1lIjoiTVlTUUxfU0VSVkVSIn0seyJuYW1lIjoiTkFNRU5PREUifSx7Im5hbWUiOiJQSUcifSx7Im5hbWUiOiJSRVNPVVJDRU1BTkFHRVIifSx7Im5hbWUiOiJTRUNPTkRBUllfTkFNRU5PREUifSx7Im5hbWUiOiJTTElERVIifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJTUEFSS19KT0JISVNUT1JZU0VSVkVSIn0seyJuYW1lIjoiVEVaX0NMSUVOVCJ9LHsibmFtZSI6IldFQkhDQVRfU0VSVkVSIn0seyJuYW1lIjoiWUFSTl9DTElFTlQifSx7Im5hbWUiOiJaRVBQRUxJTl9NQVNURVIifSx7Im5hbWUiOiJaT09LRUVQRVJfQ0xJRU5UIn0seyJuYW1lIjoiWk9PS0VFUEVSX1NFUlZFUiJ9XSwiY2FyZGluYWxpdHkiOiIxIn0seyJuYW1lIjoid29ya2VyIiwiY29uZmlndXJhdGlvbnMiOltdLCJjb21wb25lbnRzIjpbeyJuYW1lIjoiSElWRV9DTElFTlQifSx7Im5hbWUiOiJURVpfQ0xJRU5UIn0seyJuYW1lIjoiU1BBUktfQ0xJRU5UIn0seyJuYW1lIjoiREFUQU5PREUifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9LHsibmFtZSI6ImNvbXB1dGUiLCJjb25maWd1cmF0aW9ucyI6W10sImNvbXBvbmVudHMiOlt7Im5hbWUiOiJISVZFX0NMSUVOVCJ9LHsibmFtZSI6IlRFWl9DTElFTlQifSx7Im5hbWUiOiJTUEFSS19DTElFTlQifSx7Im5hbWUiOiJNRVRSSUNTX01PTklUT1IifSx7Im5hbWUiOiJOT0RFTUFOQUdFUiJ9XSwiY2FyZGluYWxpdHkiOiIxKyJ9XX0=",
              "description": "Useful for EDW analytics using Hive LLAP",
              "inputs": [],
              "name": "EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0",
              "id": 7,
              "hostGroupCount": 3,
              "status": "DEFAULT",
              "public": true
          },
          "description": "",
          "statusReason": "",
          "ambariServerIp": "35.187.4.62",
          "ambariServerUrl": "https://35.187.4.62/ambari/",
          "userName": "admin",
          "secure": false,
          "hostGroups": [
              {
                  "name": "master",
                  "constraint": {
                      "instanceGroupName": "master",
                      "constraintTemplateName": null,
                      "hostCount": 1
                  },
                  "recipeIds": [
                      1
                  ],
                  "recoveryMode": "AUTO",
                  "id": 12,
                  "recipes": [
                      {
                          "name":"pre-start-recipe",
                          "description":"mock test recipe",
                          "recipeType":"PRE_AMBARI_START",
                          "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                          "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                          "id":1,
                          "public":true
                      }
                  ],
                  "metadata": [
                      {
                          "name": "gcpcluster-m-1-20180119120605.c.siq-haas.internal",
                          "groupName": "master",
                          "id": 6,
                          "state": "HEALTHY"
                      }
                  ]
              },
              {
                  "name": "worker",
                  "constraint": {
                      "instanceGroupName": "worker",
                      "constraintTemplateName": null,
                      "hostCount": 3
                  },
                  "recipeIds": [
                      1
                  ],
                  "recoveryMode": "AUTO",
                  "id": 11,
                  "recipes": [
                      {
                          "name":"pre-start-recipe",
                          "description":"mock test recipe",
                          "recipeType":"PRE_AMBARI_START",
                          "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                          "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                          "id":5,
                          "public":true
                      }
                  ],
                  "metadata": [
                      {
                          "name": "gcpcluster-w-4-20180119120629.c.siq-haas.internal",
                          "groupName": "worker",
                          "id": 4,
                          "state": "HEALTHY"
                      },
                      {
                          "name": "gcpcluster-w-2-20180119120629.c.siq-haas.internal",
                          "groupName": "worker",
                          "id": 3,
                          "state": "HEALTHY"
                      },
                      {
                          "name": "gcpcluster-w-3-20180119120629.c.siq-haas.internal",
                          "groupName": "worker",
                          "id": 2,
                          "state": "HEALTHY"
                      }
                  ]
              },
              {
                  "name": "compute",
                  "constraint": {
                      "instanceGroupName": "compute",
                      "constraintTemplateName": null,
                      "hostCount": 1
                  },
                  "recipeIds": [
                      1
                  ],
                  "recoveryMode": "AUTO",
                  "id": 10,
                  "recipes": [
                      {
                          "name":"pre-start-recipe",
                          "description":"mock test recipe",
                          "recipeType":"PRE_AMBARI_START",
                          "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
                          "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
                          "id":1,
                          "public":true
                      }
                  ],
                  "metadata": [
                      {
                          "name": "gcpcluster-c-0-20180119120534.c.siq-haas.internal",
                          "groupName": "compute",
                          "id": 1,
                          "state": "HEALTHY"
                      }
                  ]
              }
          ],
          "rdsConfigIds": [],
          "rdsConfigs": [],
          "serviceEndPoints": {
              "Hive Server": "http://35.187.4.62:10000",
              "Zeppelin UI": "http://35.187.4.62:9995",
              "Job History Server": "http://35.187.4.62:19888",
              "Name Node": "http://35.187.4.62:50070",
              "Zeppelin Web Socket": "http://35.187.4.62:9996",
              "Resource Manager": "http://35.187.4.62:8088",
              "Spark History Server": "http://35.187.4.62:18080"
          },
          "configStrategy": "ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES",
          "ldapConfigId": null,
          "ldapConfig": null,
          "attributes": {
              "VIEW_DEFINITIONS": [
                  "ADMIN_VIEW/2.6.0.0/INSTANCE",
                  "CAPACITY-SCHEDULER/1.0.0/AUTO_CS_INSTANCE",
                  "FILES/1.0.0/AUTO_FILES_INSTANCE",
                  "HIVE/1.5.0/AUTO_HIVE_INSTANCE",
                  "HIVE/2.0.0/AUTO_HIVE20_INSTANCE",
                  "TEZ/0.7.0.2.6.2.0-205/TEZ_CLUSTER_INSTANCE"
              ]
          },
          "blueprintInputs": [],
          "blueprintCustomProperties": null,
          "executorType": "DEFAULT",
          "gateway": {
              "enableGateway": false,
              "path": "gcp-cluster",
              "topologyName": "services",
              "exposedServices": [],
              "ssoProvider": "/gcp-cluster/knoxsso/api/v1/websso",
              "gatewayType": "INDIVIDUAL",
              "ssoType": "NONE"
          },
          "customContainers": {
              "definitions": {}
          },
          "ambariStackDetails": {
              "stack": {
                  "repoid": "HDP",
                  "repository-version": "2.6.3.0-235",
                  "redhat7": null,
                  "vdf-url": "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
              },
              "util": {
                  "repoid": null,
                  "redhat7": null
              },
              "knox": null,
              "verify": false,
              "hdpVersion": "2.6"
          },
          "ambariRepoDetailsJson": {
              "version": "2.6.0.0",
              "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
              "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
          },
          "ambariDatabaseDetails": null,
          "customQueue": "default",
          "creationFinished": 1516365174382
      },
      "statusReason": "Cluster creation finished.",
      "credential": {
          "name":"google",
          "cloudPlatform":"GCP",
          "parameters":
              {
                  "serviceAccountId":"1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com",
                  "projectId":"cloudbreak"
              },
          "description":"",
          "topologyId":null,
          "id":3,
          "public":false
      },
      "network": {
          "description": null,
          "subnetCIDR": "10.0.0.0/16",
          "cloudPlatform": "GCP",
          "parameters": {
              "subnetId": "gcpcluster-20180119120325",
              "publicNetId": null,
              "routerId": null,
              "internetGatewayId": null,
              "vpcId": null,
              "noFirewallRules": false,
              "networkId": "gcpcluster-20180119120314",
              "noPublicIp": false
          },
          "topologyId": null,
          "name": "n5001012c-3a70-4c3e-b944-2f8703354dcc",
          "id": 4,
          "publicInAccount": false
      },
      "instanceGroups": [
          {
              "templateId": 5,
              "securityGroupId": 7,
              "nodeCount": 1,
              "group": "master",
              "type": "GATEWAY",
              "parameters": {},
              "template": {
                  "cloudPlatform": "GCP",
                  "parameters": {
                      "encrypted": false
                  },
                  "description": "",
                  "volumeType": "pd-standard",
                  "instanceType": "n1-standard-4",
                  "customInstanceType": null,
                  "topologyId": null,
                  "name": "t2c7d1a00-45f0-4dca-a6bd-ac992e761d90",
                  "id": 5,
                  "volumeCount": 1,
                  "volumeSize": 100,
                  "public": false
              },
              "securityGroup": {
                  "description": null,
                  "securityGroupId": null,
                  "cloudPlatform": "GCP",
                  "name": "sg23b6d523-fe70-48cb-a42d-5348f550e456",
                  "id": 7,
                  "owner": "8781acdb-4d87-4dff-814c-35c191031ad3",
                  "account": "8781acdb-4d87-4dff-814c-35c191031ad3",
                  "securityRules": [
                      {
                          "subnet": "0.0.0.0/0",
                          "ports": "22",
                          "protocol": "tcp",
                          "modifiable": false,
                          "id": 4808
                      },
                      {
                          "subnet": "0.0.0.0/0",
                          "ports": "22",
                          "protocol": "tcp",
                          "modifiable": false,
                          "id": 4807
                      },
                      {
                          "subnet": "0.0.0.0/0",
                          "ports": "443",
                          "protocol": "tcp",
                          "modifiable": false,
                          "id": 4809
                      },
                      {
                          "subnet": "0.0.0.0/0",
                          "ports": "9443",
                          "protocol": "tcp",
                          "modifiable": false,
                          "id": 4806
                      }
                  ],
                  "publicInAccount": false
              },
              "id": 12,
              "metadata": [
                  {
                      "privateIp": "10.0.0.3",
                      "publicIp": "35.187.4.62",
                      "sshPort": 22,
                      "instanceId": "gcpcluster-m-1-20180119120605",
                      "ambariServer": true,
                      "discoveryFQDN": "gcpcluster-m-1-20180119120605.c.siq-haas.internal",
                      "instanceGroup": "master",
                      "instanceStatus": "REGISTERED",
                      "instanceType": "GATEWAY_PRIMARY"
                  }
              ]
          },
          {
              "templateId": 5,
              "securityGroupId": 8,
              "nodeCount": 3,
              "group": "worker",
              "type": "CORE",
              "parameters": {},
              "template": {
                  "cloudPlatform": "GCP",
                  "parameters":
                      {
                          "encrypted": false
                      },
                  "description": "",
                  "volumeType": "pd-standard",
                  "instanceType": "n1-standard-4",
                  "customInstanceType": null,
                  "topologyId": null,
                  "name": "t2c7d1a00-45f0-4dca-a6bd-ac992e761d90",
                  "id": 5,
                  "volumeCount": 1,
                  "volumeSize": 100,
                  "public": false
              },
              "securityGroup": {
                  "description": null,
                  "securityGroupId": null,
                  "cloudPlatform": "GCP",
                  "name": "sgfa2d1b3a-05e2-4e7c-aa66-f8160fa3471d",
                  "id": 8,
                  "owner": "8781acdb-4d87-4dff-814c-35c191031ad3",
                  "account": "8781acdb-4d87-4dff-814c-35c191031ad3",
                  "securityRules": [
                      {
                          "subnet": "0.0.0.0/0",
                          "ports": "22",
                          "protocol": "tcp",
                          "modifiable": false,
                          "id": 4810
                      }
                  ],
                  "publicInAccount": false
              },
              "id": 11,
              "metadata": [
                  {
                      "privateIp": "10.0.0.4",
                      "publicIp": "104.155.46.207",
                      "sshPort": 22,
                      "instanceId": "gcpcluster-w-2-20180119120629",
                      "ambariServer": false,
                      "discoveryFQDN": "gcpcluster-w-2-20180119120629.c.siq-haas.internal",
                      "instanceGroup": "worker",
                      "instanceStatus": "REGISTERED",
                      "instanceType": "CORE"
                  },
                  {
                      "privateIp": "10.0.0.6",
                      "publicIp": "35.195.236.118",
                      "sshPort": 22,
                      "instanceId": "gcpcluster-w-3-20180119120629",
                      "ambariServer": false,
                      "discoveryFQDN": "gcpcluster-w-3-20180119120629.c.siq-haas.internal",
                      "instanceGroup": "worker",
                      "instanceStatus": "REGISTERED",
                      "instanceType": "CORE"
                  },
                  {
                      "privateIp": "10.0.0.5",
                      "publicIp": "35.187.88.131",
                      "sshPort": 22,
                      "instanceId": "gcpcluster-w-4-20180119120629",
                      "ambariServer": false,
                      "discoveryFQDN": "gcpcluster-w-4-20180119120629.c.siq-haas.internal",
                      "instanceGroup": "worker",
                      "instanceStatus": "REGISTERED",
                      "instanceType": "CORE"
                  }
              ]
          },
          {
              "templateId": 5,
              "securityGroupId": 8,
              "nodeCount": 1,
              "group": "compute",
              "type": "CORE",
              "parameters": {},
              "template": {
                  "cloudPlatform": "GCP",
                  "parameters":
                      {
                          "encrypted": false
                      },
                  "description": "",
                  "volumeType": "pd-standard",
                  "instanceType": "n1-standard-4",
                  "customInstanceType": null,
                  "topologyId": null,
                  "name": "t2c7d1a00-45f0-4dca-a6bd-ac992e761d90",
                  "id": 5,
                  "volumeCount": 1,
                  "volumeSize": 100,
                  "public": false
              },
              "securityGroup": {
                  "description": null,
                  "securityGroupId": null,
                  "cloudPlatform": "GCP",
                  "name": "sgfa2d1b3a-05e2-4e7c-aa66-f8160fa3471d",
                  "id": 8,
                  "owner": "8781acdb-4d87-4dff-814c-35c191031ad3",
                  "account": "8781acdb-4d87-4dff-814c-35c191031ad3",
                  "securityRules":
                      [
                          {
                              "subnet": "0.0.0.0/0",
                              "ports": "22",
                              "protocol": "tcp",
                              "modifiable": false,
                              "id": 4810
                          }
                      ],
                  "publicInAccount": false
              },
              "id": 10,
              "metadata": [
                  {
                      "privateIp": "10.0.0.2",
                      "publicIp": "35.195.160.159",
                      "sshPort": 22,
                      "instanceId": "gcpcluster-c-0-20180119120534",
                      "ambariServer": false,
                      "discoveryFQDN": "gcpcluster-c-0-20180119120534.c.siq-haas.internal",
                      "instanceGroup": "compute",
                      "instanceStatus": "REGISTERED",
                      "instanceType": "CORE"
                  }
              ]
          }
      ],
      "failurePolicy": null,
      "orchestrator": {
          "parameters": {},
          "apiEndpoint": "35.187.4.62:9443",
          "type": "SALT"
      },
      "created": 1516363385416,
      "gatewayPort": 9443,
      "image": {
          "imageName": "sequenceiqimage/hdc-hdp--1711170803.tar.gz",
          "imageCatalogUrl": "https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json",
          "imageId": "99d1bab8-2a40-4b4a-531a-447030b54e0f",
          "imageCatalogName": "cloudbreak-default"
      },
      "cloudbreakDetails": {
          "version": "2.4.0-dev.71"
      },
      "flexSubscription": null,
      "stackAuthentication": {
          "publicKey": "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh",
          "publicKeyId": null,
          "loginUserName": "cloudbreak"
      },
      "nodeCount": 5,
      "hardwareInfos": [],
      "cloudbreakEvents": [],
      "cloudbreakUsages": [],
      "cloudPlatform": "GCP",
      "public": false
  }
];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getStack = function(args, res, next) {
    /**
     * retrieve stack by id
     * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
     *
     * id Long
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

exports.getStackForAmbari = function(args, res, next) {
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

exports.postPrivateStack = function(args, res, next) {
    /**
     * create stack as private resource
     * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
     *
     * body StackRequest  (optional)
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

exports.postPublicStack = function(args, res, next) {
    /**
     * create stack as public resource
     * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
     *
     * body StackRequest  (optional)
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

exports.putStack = function(args, res, next) {
    /**
     * update stack by id
     * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
     *
     * id Long
     * body UpdateStack  (optional)
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

exports.statusStack = function(args, res, next) {
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

exports.validateStack = function(args, res, next) {
    /**
     * validate stack
     * Stacks are template instances - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.
     *
     * body StackValidationRequest  (optional)
     * no response value expected for this operation
     **/
    res.end();
}

exports.variantsStack = function(args, res, next) {
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

