'use strict';

/* Services */
var log = log4javascript.getLogger("uluwatuServices-logger");
var uluwatuServices = angular.module('uluwatuServices', ['ngResource']);

uluwatuServices.factory('User', ['$resource',
    function($resource) {
        return $resource('user', {
            'get': {
                method: 'GET'
            }
        });
    }
]);

uluwatuServices.factory('UserTemplate', ['$resource',
    function($resource) {
        return $resource('user/templates');
    }
]);

uluwatuServices.factory('AccountTemplate', ['$resource',
    function($resource) {
        return $resource('account/templates');
    }
]);

uluwatuServices.factory('GlobalTemplate', ['$resource',
    function($resource) {
        return $resource('templates/:id');
    }
]);

uluwatuServices.factory('UserBlueprint', ['$resource',
    function($resource) {
        return $resource('user/blueprints');
    }
]);

uluwatuServices.factory('AccountBlueprint', ['$resource',
    function($resource) {
        return $resource('account/blueprints');
    }
]);

uluwatuServices.factory('GlobalBlueprint', ['$resource',
    function($resource) {
        return $resource('blueprints/:id');
    }
]);

uluwatuServices.factory('UserRecipe', ['$resource',
    function($resource) {
        return $resource('user/recipes');
    }
]);

uluwatuServices.factory('AccountRecipe', ['$resource',
    function($resource) {
        return $resource('account/recipes');
    }
]);

uluwatuServices.factory('GlobalRecipe', ['$resource',
    function($resource) {
        return $resource('recipes/:id');
    }
]);

uluwatuServices.factory('UserCredential', ['$resource',
    function($resource) {
        return $resource('user/credentials');
    }
]);

uluwatuServices.factory('AccountCredential', ['$resource',
    function($resource) {
        return $resource('account/credentials');
    }
]);

uluwatuServices.factory('GlobalCredential', ['$resource',
    function($resource) {
        return $resource('credentials/:id');
    }
]);

uluwatuServices.factory('GlobalCredentialCertificate', ['$resource',
    function($resource) {
        return $resource('credentials/certificate/:id', {}, {
            get: {
                method: 'GET',
                isArray: false,
                headers: {
                    'Content-Type': 'application/json'
                },
                transformResponse: function(data, headersGetter) {
                    return {
                        cert: data
                    };
                }
            },
            'update': {
                method: 'PUT',
                isArray: false,
                headers: {
                    'Content-Type': 'application/json'
                },
                transformResponse: function(data, headersGetter) {
                    return {
                        cert: data
                    };
                }
            }
        });
    }
]);

uluwatuServices.factory('UserStack', ['$resource',
    function($resource) {
        return $resource('user/stacks');
    }
]);

uluwatuServices.factory('AccountStack', ['$resource',
    function($resource) {
        return $resource('account/stacks');
    }
]);

uluwatuServices.factory('StackValidation', ['$resource',
    function($resource) {
        return $resource('stacks/validate');
    }
]);

uluwatuServices.factory('UserUsages', ['$resource',
    function($resource) {
        return $resource('user/usages?:param');
    }
]);

uluwatuServices.factory('AccountUsages', ['$resource',
    function($resource) {
        return $resource('account/usages?:param');
    }
]);

uluwatuServices.factory('UserOperation', ['$resource',
    function($resource) {
        return $resource('users/:userId', null, {
            'update': {
                method: 'PUT'
            }
        });
    }
]);

uluwatuServices.factory('UserOperation', ['$resource',
    function($resource) {
        return $resource('users/:userId', null, {
            'update': {
                method: 'PUT'
            }
        });
    }
]);

uluwatuServices.factory('UserInvite', ['$resource',
    function($resource) {
        return $resource('sultans/invite');
    }
]);

uluwatuServices.factory('AccountUsers', ['$resource',
    function($resource) {
        return $resource('sultans/users');
    }
]);

uluwatuServices.factory('AccountDetails', ['$resource',
    function($resource) {
        return $resource('sultans/account/details');
    }
]);

uluwatuServices.factory('ActivateAccountUsers', ['$resource',
    function($resource) {
        return $resource('sultans/activate');
    }
]);

uluwatuServices.factory('UserPermission', ['$resource',
    function($resource) {
        return $resource('sultans/permission');
    }
]);

uluwatuServices.factory('UserEvents', ['$resource',
    function($resource) {
        return $resource('events');
    }
]);

uluwatuServices.factory('Cluster', ['$resource',
    function($resource) {
        return $resource('stacks/:id/cluster', null, {
            'update': {
                method: 'PUT'
            }
        });
    }
]);

uluwatuServices.factory('GlobalStack', ['$resource',
    function($resource) {
        return $resource('stacks/:id', null, {
            'update': {
                method: 'PUT'
            }
        });
    }
]);

uluwatuServices.factory('GlobalStackInstance', ['$resource',
    function($resource) {
        return $resource('stacks/:stackid/:instanceid');
    }
]);

uluwatuServices.factory('UluwatuCluster', ['StackValidation', 'UserStack', 'AccountStack', 'Cluster', 'GlobalStack',
    function(StackValidation, UserStack, AccountStack, Cluster, GlobalStack) {
        function AggregateCluster(UserStack, AccountStack, Cluster) {

            var decorateCluster = function(cluster) {
                cluster.hoursUp = cluster.cluster.hoursUp;
                cluster.minutesUp = cluster.cluster.minutesUp;
                cluster.blueprintId = cluster.cluster.blueprintId;
                cluster.nodeCount = 0;
                angular.forEach(cluster.instanceGroups, function(group) {
                    cluster.nodeCount += group.nodeCount;
                });
                cluster.metadata = [];
                angular.forEach(cluster.instanceGroups, function(group) {
                    angular.forEach(group.metadata, function(metadata) {
                        cluster.metadata.push(metadata);
                    });
                });
            };

            this.get = function(clusterId, successHandler) {
                GlobalStack.get({
                    id: clusterId
                }, function(success) {
                    decorateCluster(success);
                    successHandler(success);
                });
            }

            this.query = function(successHandler) {
                AccountStack.query(function(stacks) {
                    var clusters = [];
                    for (var i = 0; i < stacks.length; i++) {
                        clusters[i] = stacks[i];
                        decorateCluster(clusters[i]);
                    }
                    successHandler(clusters);
                });
            }

            this.save = function(cluster, successHandler, failureHandler) {
                var stackValidation = {
                    instanceGroups: cluster.instanceGroups,
                    hostGroups: cluster.hostGroups,
                    blueprintId: cluster.blueprintId,
                    networkId: cluster.networkId
                }
                if (cluster.validateBlueprint === true) {
                    StackValidation.save(stackValidation, function(result) {
                        stackValidationSuccessHandler(result)
                    }, function(failure) {
                        failureHandler(failure);
                    });
                } else {
                    stackValidationSuccessHandler(null)
                }

                function stackValidationSuccessHandler(result) {
                    var stack = {
                        name: cluster.name,
                        credentialId: cluster.credentialId,
                        region: cluster.region,
                        failurePolicy: cluster.failurePolicy,
                        onFailureAction: cluster.onFailureAction,
                        instanceGroups: cluster.instanceGroups,
                        parameters: cluster.parameters,
                        consulServerCount: cluster.consulServerCount,
                        networkId: cluster.networkId,
                        securityGroupId: cluster.securityGroupId,
                        platformVariant: cluster.platformVariant
                    }
                    if (cluster.public) {
                        AccountStack.save(stack, function(result) {
                            stackSuccessHandler(result)
                        }, function(failure) {
                            failureHandler(failure);
                        });
                    } else {
                        UserStack.save(stack, function(result) {
                            stackSuccessHandler(result)
                        }, function(failure) {
                            failureHandler(failure);
                        });
                    }
                }

                function stackSuccessHandler(result) {
                    cluster.id = result.id;
                    var cbCluster = {
                        name: cluster.name,
                        blueprintId: cluster.blueprintId,
                        emailNeeded: cluster.email,
                        hostGroups: cluster.hostGroups,
                        password: cluster.password,
                        userName: cluster.userName,
                        enableSecurity: cluster.enableSecurity || false,
                        kerberosMasterKey: cluster.kerberosMasterKey || null,
                        kerberosAdmin: cluster.kerberosAdmin || null,
                        kerberosPassword: cluster.kerberosPassword || null,
                        validateBlueprint: cluster.validateBlueprint,
                        fileSystem: cluster.fileSystem || null,
                        ambariStackDetails: cluster.ambariStackDetails === 'undefined' ? null : cluster.ambariStackDetails
                    }
                    successHandler(cluster);
                    Cluster.save({
                        id: result.id
                    }, cbCluster, function(result) {
                        cluster.hoursUp = 0;
                        cluster.minutesUp = 0;
                        cluster.status = 'REQUESTED';
                        successHandler(cluster);
                    }, function(failure) {
                        failureHandler(failure);
                    });
                }
            }

            this.delete = function(cluster, successHandler, failureHandler) {
                GlobalStack.delete({
                    id: cluster.id
                }, function(result) {
                    successHandler(result);
                }, function(failure) {
                    failureHandler(failure);
                });
            }
        }

        return new AggregateCluster(UserStack, AccountStack, Cluster);
    }
]);

uluwatuServices.factory('PeriscopeCluster', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id', null, {
            'update': {
                method: 'PUT',
                isArray: false
            }
        });
    }
]);

uluwatuServices.factory('PeriscopeClusterScalingHistory', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/history');
    }
]);

uluwatuServices.factory('PeriscopeClusterScalingConfiguration', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/configurations/scaling');
    }
]);

uluwatuServices.factory('PeriscopeClusterState', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/state');
    }
]);

uluwatuServices.factory('MetricAlert', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/alerts/metric/:alertId', null, {
            'save': {
                method: 'POST',
                isArray: false
            }
        });
    }
]);

uluwatuServices.factory('MetricDefinitions', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/alerts/metric/definitions');
    }
]);

uluwatuServices.factory('TimeAlert', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/alerts/time/:alertId', null, {
            'save': {
                method: 'POST',
                isArray: false
            }
        });
    }
]);

uluwatuServices.factory('ScalingPolicy', ['$resource',
    function($resource) {
        return $resource('periscope/clusters/:id/policies/:policyId', null, {
            'query': {
                method: 'GET',
                isArray: true
            }
        });
    }
]);

uluwatuServices.factory('ErrorHandler', function() {
    return {
        handleError: function(error) {
            var failedMsg = ""
            if (error.data != null && error.data.message != null) {
                failedMsg += error.data.message
            } else if (error.data != null && Object.prototype.toString.call(error.data) === '[object Object]' && "validationErrors" in error.data) {
                var errorData = error.data["validationErrors"]
                for (var key in errorData) {
                    failedMsg += errorData[key] + "; "
                }
            } else if (error.error_description != null) {
                failedMsg += error.error_description
            } else if (error.data != null && Object.prototype.toString.call(error.data) === '[object String]') {
                failedMsg += error.data
            } else {
                failedMsg += "Unknown internal error. Error code: " + error.status
            }
            return failedMsg;
        }
    }
});

uluwatuServices.factory('UserNetwork', ['$resource',
    function($resource) {
        return $resource('user/networks');
    }
]);
uluwatuServices.factory('AccountNetwork', ['$resource',
    function($resource) {
        return $resource('account/networks');
    }
]);
uluwatuServices.factory('GlobalNetwork', ['$resource',
    function($resource) {
        return $resource('networks/:id');
    }
]);
uluwatuServices.factory('UserSecurityGroup', ['$resource',
    function($resource) {
        return $resource('user/securitygroups');
    }
]);
uluwatuServices.factory('AccountSecurityGroup', ['$resource',
    function($resource) {
        return $resource('account/securitygroups');
    }
]);
uluwatuServices.factory('GlobalSecurityGroup', ['$resource',
    function($resource) {
        return $resource('securitygroups/:id');
    }
]);

uluwatuServices.factory('File', function() {
    return {
        getBase64ContentById: function(inputId, callback) {
            var input = document.getElementById(inputId);
            var reader = new FileReader();
            try {
                reader.readAsDataURL(input.files[0]);
                reader.onloadend = function(event) {
                    callback(event.target.result.substr(event.target.result.indexOf(',') + 1));
                }
            } catch (e) {
                callback(null);
            }
        }
    }
});