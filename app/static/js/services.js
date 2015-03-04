'use strict';

/* Services */
var log = log4javascript.getLogger("uluwatuServices-logger");
var uluwatuServices = angular.module('uluwatuServices', ['ngResource']);

uluwatuServices.factory('User', ['$resource',
    function ($resource) {
        return $resource('user',
            {
                'get': { method: 'GET' }
            });
    }]);

uluwatuServices.factory('UserTemplate', ['$resource',
    function ($resource) {
        return $resource('user/templates');
    }]);

uluwatuServices.factory('AccountTemplate', ['$resource',
    function ($resource) {
        return $resource('account/templates');
    }]);

uluwatuServices.factory('GlobalTemplate', ['$resource',
    function ($resource) {
        return $resource('templates/:id');
    }]);

uluwatuServices.factory('UserBlueprint', ['$resource',
    function ($resource) {
        return $resource('user/blueprints');
    }]);

uluwatuServices.factory('AccountBlueprint', ['$resource',
    function ($resource) {
        return $resource('account/blueprints');
    }]);

uluwatuServices.factory('GlobalBlueprint', ['$resource',
    function ($resource) {
        return $resource('blueprints/:id');
    }]);

uluwatuServices.factory('UserCredential', ['$resource',
    function ($resource) {
        return $resource('user/credentials');
    }]);

uluwatuServices.factory('AccountCredential', ['$resource',
    function ($resource) {
        return $resource('account/credentials');
    }]);

uluwatuServices.factory('GlobalCredential', ['$resource',
    function ($resource) {
        return $resource('credentials/:id');
    }]);

uluwatuServices.factory('GlobalCredentialCertificate', ['$resource',
    function ($resource) {
        return $resource('credentials/certificate/:id', {}, {
            get: {  method: 'GET',
                isArray: false,
                headers: { 'Content-Type': 'application/json' },
                transformResponse: function (data, headersGetter) {
                    return {cert: data};
                }
            }
        });
    }]);

uluwatuServices.factory('UserStack', ['$resource',
    function ($resource) {
        return $resource('user/stacks');
    }]);

uluwatuServices.factory('AccountStack', ['$resource',
    function ($resource) {
        return $resource('account/stacks');
    }]);

uluwatuServices.factory('StackValidation', ['$resource',
    function ($resource) {
      return $resource('stacks/validate');
    }]);

uluwatuServices.factory('UserUsages', ['$resource',
    function ($resource) {
        return $resource('user/usages?:param');
    }]);

uluwatuServices.factory('AccountUsages', ['$resource',
  function ($resource) {
    return $resource('account/usages?:param');
  }]);

uluwatuServices.factory('UserInvite', ['$resource',
    function ($resource) {
        return $resource('sultans/invite');
    }]);

uluwatuServices.factory('AccountUsers', ['$resource',
    function ($resource) {
        return $resource('sultans/users');
    }]);

uluwatuServices.factory('AccountDetails', ['$resource',
    function ($resource) {
        return $resource('sultans/account/details');
    }]);

uluwatuServices.factory('ActivateAccountUsers', ['$resource',
    function ($resource) {
        return $resource('sultans/activate');
    }]);

uluwatuServices.factory('UserPermission', ['$resource',
    function ($resource) {
      return $resource('sultans/permission');
    }]);

uluwatuServices.factory('UserEvents', ['$resource',
    function ($resource) {
        return $resource('events');
    }]);

uluwatuServices.factory('Cluster', ['$resource',
    function ($resource) {
        return $resource('stacks/:id/cluster', null, { 'update': { method:'PUT' } });
    }]);

uluwatuServices.factory('GlobalStack', ['$resource',
    function ($resource) {
        return $resource('stacks/:id', null, { 'update': { method:'PUT' } });
    }]);

uluwatuServices.factory('UluwatuCluster', ['StackValidation', 'UserStack', 'AccountStack', 'Cluster', 'GlobalStack',
    function (StackValidation, UserStack, AccountStack, Cluster, GlobalStack) {
        function AggregateCluster(UserStack, AccountStack, Cluster) {

            this.query = function (successHandler) {
                AccountStack.query(function (stacks) {
                    var clusters = [];
                    for (var i = 0; i < stacks.length; i++) {
                        clusters[i] = stacks[i];
                        clusters[i].hoursUp = stacks[i].cluster.hoursUp;
                        clusters[i].minutesUp = stacks[i].cluster.minutesUp;
                        clusters[i].blueprintId = stacks[i].cluster.blueprintId;
                    }
                    successHandler(clusters);
                });
            }

            this.save = function (cluster, successHandler, failureHandler) {
                var stackValidation = {
                  instanceGroups: cluster.instanceGroups,
                  hostGroups: cluster.hostGroups,
                  blueprintId: cluster.blueprintId
                }
                StackValidation.save(stackValidation, function (result) {
                  stackValidationSuccessHandler(result)
                }, function (failure) {
                  failureHandler(failure);
                });

                function stackValidationSuccessHandler(result) {
                    var stack = {
                        name: cluster.name,
                        credentialId: cluster.credentialId,
                        password: cluster.password,
                        userName: cluster.userName,
                        region: cluster.region,
                        failurePolicy: cluster.failurePolicy,
                        onFailure: cluster.onFailure,
                        instanceGroups: cluster.instanceGroups,
                        parameters: cluster.parameters
                    }
                    if (cluster.public) {
                        AccountStack.save(stack, function (result) {
                            stackSuccessHandler(result)
                        }, function (failure) {
                            failureHandler(failure);
                        });
                    } else {
                        UserStack.save(stack, function (result) {
                            stackSuccessHandler(result)
                        }, function (failure) {
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
                        hostGroups: cluster.hostGroups
                    }
                    successHandler(cluster);
                    Cluster.save({ id: result.id }, cbCluster, function (result) {
                        cluster.hoursUp = 0;
                        cluster.minutesUp = 0;
                        cluster.status = 'REQUESTED';
                        successHandler(cluster);
                    }, function (failure) {
                        failureHandler(failure);
                    });
                }
            }

            this.delete = function (cluster, successHandler, failureHandler) {
                GlobalStack.delete({ id: cluster.id }, function (result) {
                  successHandler(result);
                }, function (failure){
                  failureHandler(failure);
                });
            }
        }

        return new AggregateCluster(UserStack, AccountStack, Cluster);
    }]);

    uluwatuServices.factory('PeriscopeCluster', ['$resource',
    function ($resource) {
      return $resource('periscope/clusters/:id', null, {'update': {method:'PUT', isArray:false}});
    }]);

    uluwatuServices.factory('PeriscopeClusterScalingConfiguration', ['$resource',
    function ($resource) {
      return $resource('periscope/clusters/:id/configurations/scaling');
    }]);

    uluwatuServices.factory('PeriscopeClusterState', ['$resource',
    function ($resource) {
      return $resource('periscope/clusters/:id/state');
    }]);

    uluwatuServices.factory('MetricAlarm', ['$resource',
    function ($resource) {
      return $resource('periscope/clusters/:id/alarms/metric/:alarmId', null, {'save': {method:'POST', isArray:false}});
    }]);

    uluwatuServices.factory('TimeAlarm', ['$resource',
    function ($resource) {
      return $resource('periscope/clusters/:id/alarms/time/:alarmId', null, {'save':  {method:'POST', isArray:false}});
    }]);

    uluwatuServices.factory('ScalingPolicy', ['$resource',
    function ($resource) {
      return $resource('periscope/clusters/:id/policies/:policyId', null, {'query':  {method:'GET', isArray:true}});
    }]);

    uluwatuServices.factory('ErrorHandler', function() {
        return {
            handleError: function(error) {
                var failedMsg = ""
                if (error.data != null && error.data.message != null) {
                    failedMsg += error.data.message
                } else if (error.data != null && "validationErrors" in error.data) {
                    var errorData = error.data["validationErrors"]
                    for (var key in errorData) {
                        failedMsg += errorData[key] + "; "
                    }
                } else if (error.error_description != null) {
                    failedMsg += error.error_description
                } else {
                    failedMsg += "Unknown error or Cloudbreak Server is not running."
                }
                return failedMsg;
            }
        }
    });
