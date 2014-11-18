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

uluwatuServices.factory('GlobalTemplate', ['$resource',
    function ($resource) {
        return $resource('templates/:id');
    }]);

uluwatuServices.factory('UserBlueprint', ['$resource',
    function ($resource) {
        return $resource('user/blueprints');
    }]);

uluwatuServices.factory('GlobalBlueprint', ['$resource',
    function ($resource) {
        return $resource('blueprints/:id');
    }]);

uluwatuServices.factory('UserCredential', ['$resource',
    function ($resource) {
        return $resource('user/credentials');
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

uluwatuServices.factory('UserUsages', ['$resource',
    function ($resource) {
        return $resource('user/usages?:param');
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

uluwatuServices.factory('UluwatuCluster', ['UserStack', 'Cluster', 'GlobalStack',
    function (UserStack, Cluster, GlobalStack) {
        function AggregateCluster(UserStack, Cluster) {

            this.query = function (successHandler) {
                UserStack.query(function (stacks) {
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
                var stack = {
                    nodeCount: cluster.nodeCount,
                    name: cluster.name,
                    templateId: cluster.templateId,
                    credentialId: cluster.credentialId,
                    password: cluster.password
                }
                UserStack.save(stack, function (result) {
                    cluster.id = result.id;
                    var cbCluster = {
                        name: cluster.name,
                        blueprintId: cluster.blueprintId,
                        emailNeeded: cluster.email
                    }
                    Cluster.save({ id: result.id }, cbCluster, function (result) {
                        cluster.hoursUp = 0;
                        cluster.minutesUp = 0;
                        cluster.status = 'REQUESTED';
                        successHandler(cluster);
                    }, function (failure) {
                        failureHandler(failure);
                    });
                }, function (failure) {
                    failureHandler(failure);
                });
            }

            this.delete = function (cluster, successHandler, failureHandler) {
                GlobalStack.delete({ id: cluster.id }, function (result) {
                  successHandler(result);
                }, function (failure){
                  failureHandler(failure);
                });
            }
        }

        return new AggregateCluster(UserStack, Cluster);
    }])
;
