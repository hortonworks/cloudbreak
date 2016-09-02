'use strict';

var log = log4javascript.getLogger("clusterController-logger");
var $jq = jQuery.noConflict();

angular.module('uluwatuControllers').controller('clusterController', ['$scope', '$rootScope', '$filter', 'UluwatuCluster', 'GlobalStack', 'Cluster', 'GlobalStackInstance', '$interval', 'UserEvents', 'PeriscopeCluster', 'PlatformVariant',
    function($scope, $rootScope, $filter, UluwatuCluster, GlobalStack, Cluster, GlobalStackInstance, $interval, UserEvents, PeriscopeCluster, PlatformVariant) {

        $rootScope.ledStyles = {
            "REQUESTED": "state2-run-blink",
            "CREATE_IN_PROGRESS": "state2-run-blink",
            "AVAILABLE": "state5-run",
            "UPDATE_IN_PROGRESS": "state2-run-blink",
            "UPDATE_REQUESTED": "state1-ready-blink",
            "UPDATE_FAILED": "state3-stop",
            "CREATE_FAILED": "state3-stop",
            "ENABLE_SECURITY_FAILED": "state3-stop",
            "DELETE_IN_PROGRESS": "state0-stop-blink",
            "DELETE_FAILED": "state3-stop",
            "DELETE_COMPLETED": "state3-stop",
            "STOPPED": "state4-ready",
            "STOP_REQUESTED": "state1-ready-blink",
            "START_REQUESTED": "state1-ready-blink",
            "STOP_IN_PROGRESS": "state1-ready-blink",
            "START_IN_PROGRESS": "state1-ready-blink",
            "START_FAILED": "state3-stop",
            "STOP_FAILED": "state3-stop"
        }

        $rootScope.buttonStyles = {
            "REQUESTED": "fa-pause",
            "CREATE_IN_PROGRESS": "fa-pause",
            "AVAILABLE": "fa-stop",
            "UPDATE_IN_PROGRESS": "fa-pause",
            "UPDATE_REQUESTED": "fa-refresh",
            "UPDATE_FAILED": "fa-play",
            "CREATE_FAILED": "fa-play",
            "ENABLE_SECURITY_FAILED": "fa-play",
            "DELETE_IN_PROGRESS": "fa-pause",
            "DELETE_FAILED": "fa-play",
            "DELETE_COMPLETED": "fa-stop",
            "STOPPED": "fa-play",
            "STOP_REQUESTED": "fa-refresh",
            "START_REQUESTED": "fa-refresh",
            "STOP_IN_PROGRESS": "fa-refresh",
            "START_IN_PROGRESS": "fa-refresh",
            "START_FAILED": "fa-play",
            "STOP_FAILED": "fa-play"
        }

        $scope.configStrategies = [
            "ALWAYS_APPLY",
            "ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES",
            "NEVER_APPLY",
            "ONLY_STACK_DEFAULTS_APPLY"
        ]

        $rootScope.activeCluster = {};

        $scope.detailsShow = true;
        $scope.periscopeShow = false;
        $scope.metricsShow = false;
        $scope.showAdvancedOptionForm = false;
        $scope.newCredential = {};
        $scope.inherited = {
            forcedTermination: false
        };
        $scope.updateStatuses = [
                "REQUESTED", "CREATE_IN_PROGRESS", "UPDATE_IN_PROGRESS", "DELETE_IN_PROGRESS", "START_REQUESTED", "START_IN_PROGRESS"
            ],
            $scope.terminatableStatuses = [
                'CREATED', 'FAILED', 'UNREGISTERED', 'DECOMMISSIONED'
            ];
        getUluwatuClusters();
        initCluster();
        initWizard();

        function initWizard() {
            $scope.configureHostGroups = false;
            $scope.configureFailureAction = false;
            $scope.configureAmbariRepository = false;
            $scope.configureAmbariDatabase = false;
            $scope.configureSecurity = false;
            $scope.configureFileSystem = false;
            $scope.configureCluster = true;
            $scope.configureReview = false;
        }

        $scope.showWizardActualElement = function(element) {
            $scope.configureHostGroups = false;
            $scope.configureFailureAction = false;
            $scope.configureAmbariRepository = false;
            $scope.configureAmbariDatabase = false;
            $scope.configureSecurity = false;
            $scope.configureFileSystem = false;
            $scope.configureCluster = false;
            $scope.configureReview = false;

            if (element == 'configureHostGroups') {
                $scope.configureHostGroups = true;
            } else if (element == 'configureFailureAction') {
                $scope.configureFailureAction = true;
            } else if (element == 'configureAmbariRepository') {
                $scope.configureAmbariRepository = true;
            } else if (element == 'configureAmbariDatabase') {
                $scope.configureAmbariDatabase = true;
            } else if (element == 'configureSecurity') {
                $scope.configureSecurity = true;
            } else if (element == 'configureFileSystem') {
                $scope.configureFileSystem = true;
            } else if (element == 'configureCluster') {
                $scope.configureCluster = true;
            } else if (element == 'configureReview') {
                $scope.configureReview = true;
            }
        }

        $scope.showAdvancedOption = function() {
            if ($scope.showAdvancedOptionForm === false) {
                $scope.showAdvancedOptionForm = true;
                initWizard();
            } else {
                $scope.showAdvancedOptionForm = false;
                initWizard();
            }
        }

        $scope.selectedAdjustmentChange = function() {
            if ($scope.cluster.bestEffort === "BEST_EFFORT") {
                $scope.cluster.failurePolicy.adjustmentType = "BEST_EFFORT";
            } else {
                $scope.cluster.failurePolicy.adjustmentType = "EXACT";
                $scope.cluster.failurePolicy.threshold = 3;
            }
        }

        $scope.selectBlueprintreinstallChange = function() {
            var actualBp = $filter('filter')($rootScope.blueprints, {
                id: $rootScope.reinstallClusterObject.blueprintId
            }, true);
            var hostGroups = [];
            var index = 0;
            if ($rootScope.activeCluster.instanceGroups.length > 0) {
                $rootScope.activeCluster.instanceGroups.forEach(function(value) {
                    var tmpRecipes = $filter('filter')($rootScope.activeCluster.cluster.hostGroups, {
                        instanceGroupName: value.group
                    }, true)[0];
                    hostGroups.push({
                        name: actualBp[0].ambariBlueprint.host_groups[index].name,
                        instanceGroupName: value.group,
                        constraint: {
                            instanceGroupName: value.group,
                            hostCount: 1
                        },
                        recipeIds: tmpRecipes != undefined ? tmpRecipes.recipeIds : []
                    });
                    index++;
                });
            } else {
                actualBp[0].ambariBlueprint.host_groups.forEach(function(bpHostGroup) {
                    hostGroups.push({
                        name: bpHostGroup.name,
                        instanceGroupName: bpHostGroup.name,
                        constraint: {
                            constraintTemplateName: $rootScope.constraints[0].name,
                            hostCount: 1
                        },
                        recipeIds: []
                    });
                });
            }
            $rootScope.reinstallClusterObject.hostgroups = hostGroups;
            $rootScope.reinstallClusterObject.fullBp = actualBp[0];
        }

        $scope.selectedBlueprintChange = function() {
            if ($rootScope.activeCredential) {
                var tmpCloudPlatform = $rootScope.activeCredential.cloudPlatform;
                var templatesByProvider = $filter('filter')($rootScope.templates, {
                    cloudPlatform: tmpCloudPlatform
                }, true);
                var tmpTemplate = $filter('orderBy')(templatesByProvider, 'name', false)[0];
                var tmpTemplateId = null;
                if (tmpTemplate != null) {
                    tmpTemplateId = tmpTemplate.id;
                }
                var actualBp = $filter('filter')($rootScope.blueprints, {
                    id: $scope.cluster.blueprintId
                }, true);
                var instanceGroups = [];
                var hostGroups = [];
                var sgroupsActiveId = null;
                if ($rootScope.securitygroups && $rootScope.securitygroups.length != 0) {
                    var sgroups = $filter('orderBy')($rootScope.securitygroups, 'name', false);
                    sgroupsActiveId = sgroups[0].id;
                }
                actualBp[0].ambariBlueprint.host_groups.forEach(function(k) {
                    instanceGroups.push({
                        templateId: tmpTemplateId,
                        securityGroupId: sgroupsActiveId,
                        group: k.name,
                        nodeCount: 1,
                        type: "CORE"
                    });
                    hostGroups.push({
                        name: k.name,
                        constraint: {
                            instanceGroupName: k.name,
                            hostCount: 1
                        },
                        recipeIds: []
                    })
                });


                $scope.cluster.instanceGroups = instanceGroups;
                $scope.cluster.hostGroups = hostGroups;
                $scope.cluster.activeGroup = $filter('orderBy')($scope.cluster.instanceGroups, 'group', false)[0].group;
            } else {
                var constraintTemplates = $rootScope.constraints;
                var tmpConstraint = $filter('orderBy')(constraintTemplates, 'name', false)[0];
                var tmpConstraintName
                if (tmpConstraint != null) {
                    tmpConstraintName = tmpConstraint.name;
                }
                var actualBp = $filter('filter')($rootScope.blueprints, {
                    id: $scope.cluster.blueprintId
                }, true);
                var hostGroups = [];
                actualBp[0].ambariBlueprint.host_groups.forEach(function(k) {
                    hostGroups.push({
                        name: k.name,
                        constraint: {
                            constraintTemplateName: tmpConstraintName,
                            hostCount: 1
                        },
                        recipeIds: []
                    })
                });
                $scope.cluster.hostGroups = hostGroups;
                $scope.cluster.activeGroup = $scope.cluster.hostGroups[0].name;
            }
        }

        $scope.isUndefined = function(variable) {
            return angular.isUndefined(variable) || variable === null;
        }

        $scope.changeRecipeRun = function(recipeId, hostGroupName, model) {
            var recipe = $filter('filter')($rootScope.recipes, {
                id: recipeId
            }, true)[0];
            if (model === true) {
                $filter('filter')($scope.cluster.hostGroups, {
                    name: hostGroupName
                }, true)[0].recipeIds.push(recipe.id);
            } else {
                var recipeList = $filter('filter')($scope.cluster.hostGroups, {
                    name: hostGroupName
                }, true)[0];
                var index = recipeList.recipeIds.indexOf(recipe.id);
                $filter('filter')($scope.cluster.hostGroups, {
                    name: hostGroupName
                }, true)[0].recipeIds.splice(index, 1);
            }
        }

        $scope.isVisibleServiceValue = function(element) {
            return element.indexOf("null") !== 0
        }

        $scope.isEmptyObj = function(obj) {
            var prop;
            for (prop in obj) {
                if (obj.hasOwnProperty(prop)) {
                    return false
                }
            }
            return true
        }

        $scope.toggleAmbariStackDetailsVerify = function() {
            if ($scope.isUndefined($scope.cluster.ambariStackDetails)) {
                $scope.cluster.ambariStackDetails = {};
            }
            if ($scope.isUndefined($scope.cluster.ambariStackDetails.verify) || $scope.cluster.ambariStackDetails.verify == false) {
                $scope.cluster.ambariStackDetails.verify = true;
            } else {
                delete $scope.cluster.ambariStackDetails.verify;
            }
        }

        $scope.createCluster = function() {
            var blueprint = $filter('filter')($rootScope.blueprints, {
                id: $scope.cluster.blueprintId
            }, true)[0];
            var tmpNodeCount = 0;
            angular.forEach($scope.cluster.hostGroups, function(hg) {
                if ($scope.cluster.instanceGroups) {
                    angular.forEach($scope.cluster.instanceGroups, function(ig) {
                        if (ig.group === hg.name) {
                            hg.constraint.hostCount = ig.nodeCount;
                        }
                    });
                }
                tmpNodeCount += hg.constraint.hostCount;
            });

            if (blueprint.hostGroupCount > tmpNodeCount) {
                $scope.showErrorMessage($rootScope.msg.hostgroup_invalid_node_count);
                return;
            }
            if (!$scope.isUndefined($scope.cluster.ambariStackDetails)) {
                for (var item in $scope.cluster.ambariStackDetails) {
                    if ($scope.cluster.ambariStackDetails[item] === "" || $scope.cluster.ambariStackDetails[item] === undefined) {
                        delete $scope.cluster.ambariStackDetails[item];
                    }
                }
            }
            if (!$scope.isUndefined($scope.cluster.fileSystem) && $scope.cluster.fileSystem.type == "LOCAL") {
                delete $scope.cluster.fileSystem;
            } else if (!$scope.isUndefined($scope.cluster.fileSystem) && $scope.cluster.fileSystem.type != "LOCAL") {
                $scope.cluster.fileSystem.name = $scope.cluster.name;
                if ($scope.errorInFileSystemConfig()) {
                    $scope.showErrorMessage($rootScope.msg.filesystem_config_error);
                    return;
                }
            }

            if ($rootScope.activeCredential && ($rootScope.activeCredential.cloudPlatform == 'AWS' || $rootScope.activeCredential.cloudPlatform == 'OPENSTACK')) {
                delete $scope.cluster.fileSystem;
            }
            if ($scope.cluster.orchestrator.type === 'SALT') {
                $scope.cluster.enableShipyard = false;
            }
            if ($rootScope.activeCredential && $rootScope.activeCredential.cloudPlatform !== 'AWS') {
                delete $scope.cluster.parameters.s3Role;
                delete $scope.cluster.parameters.instanceProfileStrategy;
            } else {
                if ($scope.cluster.parameters.instanceProfileStrategy == "NONE") {
                     delete $scope.cluster.parameters.s3Role;
                     delete $scope.cluster.parameters.instanceProfileStrategy;
                } else if ($scope.cluster.parameters.instanceProfileStrategy == "USE_EXISTING" && $scope.isUndefined($scope.cluster.parameters.s3Role)) {
                    $scope.showErrorMessage($rootScope.msg.s3_role_error);
                    return;
                } else if ($scope.cluster.parameters.instanceProfileStrategy == "CREATE") {
                    delete $scope.cluster.parameters.s3Role;
                }
            }
            if (!$scope.isUndefined($scope.cluster.ambariStackDetails) && Object.keys($scope.cluster.ambariStackDetails).length !== 0) {
                if ($scope.isUndefined($scope.cluster.ambariStackDetails.stack) ||
                    $scope.isUndefined($scope.cluster.ambariStackDetails.version) ||
                    $scope.isUndefined($scope.cluster.ambariStackDetails.stackRepoId) ||
                    $scope.isUndefined($scope.cluster.ambariStackDetails.stackBaseURL) ||
                    $scope.isUndefined($scope.cluster.ambariStackDetails.utilsRepoId) ||
                    $scope.isUndefined($scope.cluster.ambariStackDetails.utilsBaseURL)) {
                    $scope.showErrorMessage($rootScope.msg.ambari_repository_config_error);
                    return;
                } else {
                    $scope.cluster.ambariStackDetails.os = "redhat7";
                    if ($scope.isUndefined($scope.cluster.ambariStackDetails.verify)) {
                        $scope.cluster.ambariStackDetails.verify = false;
                    }
                }
            } else {
                $scope.cluster.ambariStackDetails = null;
            }

            if (typeof($scope.cluster.ambariDatabaseDetails) !== "undefined" && !$scope.cluster.ambariDatabaseDetails.vendor) {
                delete $scope.cluster.ambariDatabaseDetails;
            }

            if ($scope.activeCredential) {
                $scope.cluster.credentialId = $rootScope.activeCredential.id;
                $scope.prepareParameters($scope.cluster);
                UluwatuCluster.save($scope.cluster, $rootScope.activeCredential.cloudPlatform, function(result) {
                    var nodeCount = 0;
                    angular.forEach(result.instanceGroups, function(group) {
                        nodeCount += group.nodeCount;
                    });
                    result.nodeCount = nodeCount;
                    result.cloudPlatform = $filter('filter')($rootScope.credentials, {
                        id: $rootScope.activeCredential.id
                    }, true)[0].cloudPlatform;
                    result.public = $scope.cluster.public;
                    angular.forEach(result.instanceGroups, function(item) {
                        item.templateId = parseFloat(item.templateId);
                    });
                    result.blueprintId = parseFloat(result.blueprintId);

                    var existingCluster = $filter('filter')($rootScope.clusters, {
                        id: result.id
                    }, true)[0];
                    if (result !== undefined && result.id !== undefined) {
                        GlobalStack.get({
                            id: result.id
                        }, function(success) {
                            result.platformVariant = success.platformVariant;
                        });
                    }
                    if (existingCluster != undefined) {
                        existingCluster = result;
                    } else {
                        pushCluster(result);
                    }
                }, function(failure) {
                    $scope.showError(failure, $rootScope.msg.cluster_failed);
                });
            } else {
                $scope.prepareParameters($scope.cluster);
                var cbCluster = {
                    name: $scope.cluster.name,
                    blueprintId: $scope.cluster.blueprintId,
                    emailNeeded: $scope.cluster.email,
                    hostGroups: $scope.cluster.hostGroups,
                    password: $scope.cluster.password,
                    userName: $scope.cluster.userName,
                    enableSecurity: $scope.cluster.enableSecurity || false,
                    kerberosMasterKey: $scope.cluster.kerberosMasterKey || null,
                    kerberosAdmin: $scope.cluster.kerberosAdmin || null,
                    kerberosPassword: $scope.cluster.kerberosPassword || null,
                    validateBlueprint: $scope.cluster.validateBlueprint,
                    fileSystem: $scope.cluster.fileSystem || null,
                    configStrategy: $scope.cluster.configStrategy || null,
                    ambariStackDetails: $scope.cluster.ambariStackDetails === 'undefined' ? null : $scope.cluster.ambariStackDetails,
                    ambariDatabaseDetails: $scope.cluster.ambariDatabaseDetails === 'undefined' ? null : $scope.cluster.ambariDatabaseDetails
                }
                Cluster.save({
                    id: $scope.activeStack.id
                }, cbCluster, function(result) {
                    result.id = $scope.activeStack.id
                    var hostCount = 0;
                    angular.forEach(result.hostGroups, function(group) {
                        hostCount += group.constraint.hostCount;
                    });
                    result.nodeCount = hostCount;
                    result.cloudPlatform = $rootScope.activeStack.orchestrator.type;
                    result.public = $scope.cluster.public;
                    result.hoursUp = 0;
                    result.minutesUp = 0;
                    result.status = 'REQUESTED';
                    pushCluster(result);
                }, function(failure) {
                    $scope.showError(failure, $rootScope.msg.cluster_failed);
                });
            }
        }

        function pushCluster(cluster) {
            $rootScope.clusters.push(cluster);
            $scope.$parent.orderClusters();
            $jq('.carousel').carousel(0);
            // enable toolbar buttons
            $jq('#toggle-cluster-block-btn').removeClass('disabled');
            $jq('#sort-clusters-btn').removeClass('disabled');
            $jq('#create-cluster-btn').removeClass('disabled');
            $jq("#notification-n-filtering").prop("disabled", false);
            $scope.clusterCreationForm.$setPristine();
            initCluster();
        }

        $scope.errorInFileSystemConfig = function() {
            if ($scope.cluster.fileSystem.type == 'DASH') {
                if (!$scope.isUndefined($scope.cluster.fileSystem.properties.accountName) && !$scope.isUndefined($scope.cluster.fileSystem.properties.accountKey)) {
                    return false;
                }
            } else if ($scope.cluster.fileSystem.type == 'WASB') {
                if (!$scope.isUndefined($scope.cluster.fileSystem.properties.accountName) && !$scope.isUndefined($scope.cluster.fileSystem.properties.accountKey)) {
                    return false;
                }
            } else if ($scope.cluster.fileSystem.type == 'GCS') {
                if (!$scope.isUndefined($scope.cluster.fileSystem.properties.projectId) && !$scope.isUndefined($scope.cluster.fileSystem.properties.serviceAccountEmail) && !$scope.isUndefined($scope.cluster.fileSystem.properties.privateKeyEncoded) && !$scope.isUndefined($scope.cluster.fileSystem.properties.defaultBucketName)) {
                    return false;
                }
            }
            return true;

        }

        $scope.prepareParameters = function(cluster) {
            for (var item in cluster.parameters) {
                if (cluster.parameters[item] === "" || cluster.parameters[item] === undefined) {
                    delete cluster.parameters[item];
                }
            }
        }

        $scope.deleteStackInstance = function(stackId, instanceId) {
            GlobalStackInstance.delete({
                stackid: stackId,
                instanceid: instanceId
            }, null, function(result) {

            }, function(failure) {
                $scope.showError(failure, $rootScope.msg.stack_instance_delete_failed);
            });
        }

        $scope.changeClusterCredential = function(activeCluster) {
            var userNamePasswordJson = {
                "userNamePasswordJson": {
                    "userName": $scope.newCredential.newUserName,
                    "password": $scope.newCredential.newPassword,
                    "oldPassword": $scope.newCredential.oldPassword
                }
            };
            Cluster.update({
                id: activeCluster.id
            }, userNamePasswordJson, function(success) {
                $rootScope.activeCluster.cluster.userName = $scope.newCredential.newUserName;
                $rootScope.activeCluster.cluster.password = $scope.newCredential.newPassword;
                var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                    return value.host === activeCluster.cluster.ambariServerIp;
                }, true)[0];
                if (periCluster != undefined) {
                    var ambariJson = {
                        'host': activeCluster.cluster.ambariServerIp,
                        'port': '443',
                        'user': $scope.newCredential.newUserName,
                        'pass': $scope.newCredential.newPassword
                    };
                    PeriscopeCluster.update({
                        id: periCluster.id
                    }, ambariJson, function(success) {
                        $scope.showSuccess($rootScope.msg.cluster_credential_update_success);
                    }, function(error) {
                        $scope.showError(error, $rootScope.msg.cluster_credential_update_failed);
                    });
                }
            }, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_credential_update_failed);
            });
        }

        $scope.deleteCluster = function(cluster) {
            if ($scope.inherited.forcedTermination) {
                del(UluwatuCluster.forcedDelete, cluster);
                $scope.inherited.forcedTermination = false;
            } else {
                del(UluwatuCluster.delete, cluster);
            }
            $rootScope.deselectActiveCluster();
        }

        function del(deleteFunction, cluster) {
            deleteFunction(cluster, function(result) {
                var actCluster = $filter('filter')($rootScope.clusters, {
                    id: cluster.id
                }, true)[0];
                actCluster.status = "DELETE_IN_PROGRESS";
                $scope.$broadcast('DELETE_PERISCOPE_CLUSTER', cluster.id);
            }, function(failure) {
                $scope.showError(failure, $rootScope.msg.cluster_delete_failed);
            });
        }

        $scope.selectActiveCluster = function(clusterId) {
            var actClusterIndex = $rootScope.clusters.indexOf($filter('filter')($rootScope.clusters, {
                id: clusterId
            })[0]);
            var disabled = $rootScope.clusters[actClusterIndex].status == "DELETE_IN_PROGRESS";
            if (disabled) {
                $rootScope.activeCluster = $rootScope.clusters[actClusterIndex];
            } else {
                UluwatuCluster.get(clusterId, function(success) {
                    $scope.pagination = {
                        currentPage: 1,
                        itemsPerPage: 10,
                        totalItems: success.metadata.length
                    }

                    $rootScope.activeCluster = $rootScope.clusters[actClusterIndex] = success;
                    $rootScope.activeClusterBlueprint = $filter('filter')($rootScope.blueprints, {
                        id: $rootScope.activeCluster.blueprintId.toString()
                    }, true)[0];
                    if (typeof($rootScope.activeCluster.credentialId) !== "undefined" && $rootScope.activeCluster.credentialId) {
                        if ($rootScope.activeCluster.cloudPlatform) {
                            $rootScope.activeClusterCredential = $filter('filter')($rootScope.credentials, {
                                id: $rootScope.activeCluster.credentialId
                            }, true)[0];
                            if (typeof($rootScope.activeClusterCredential) !== "undefined") {
                                $rootScope.activeCluster.cloudPlatform = $rootScope.activeClusterCredential.cloudPlatform
                            }
                        }
                    } else {
                        $rootScope.activeCluster.cloudPlatform = $rootScope.activeCluster.orchestrator.type
                    }
                    $rootScope.activeCluster.activeGroup = $rootScope.activeCluster.hostGroups != undefined && $rootScope.activeCluster.hostGroups.length > 0 ? $rootScope.activeCluster.hostGroups[0].name : '';
                    if ($rootScope.activeCluster.networkId) {
                        $rootScope.activeClusterNetwork = $filter('filter')($rootScope.networks, {
                            id: $rootScope.activeCluster.networkId
                        })[0];
                    }
                    if ($rootScope.activeCluster.instanceGroups && $rootScope.activeCluster.instanceGroups.length > 0) {
                        $rootScope.activeClusterSecurityGroup = $filter('filter')($rootScope.securitygroups, {
                            id: $rootScope.activeCluster.instanceGroups[0].securityGroupId
                        })[0];
                    }
                    $scope.newCredential = {};
                    $scope.newCredential.newUserName = $rootScope.activeCluster.cluster.userName;
                    $scope.upscaleCluster = {
                        hostGroup: $scope.getHostGroupName(),
                        numberOfInstances: 1
                    };
                    $scope.downscaleCluster = {
                        hostGroup: $scope.getHostGroupName(),
                        numberOfInstances: 1
                    };
                });
            }
        };

        $scope.changeActiveClusterGroup = function(group) {
            $rootScope.activeCluster.activeGroup = group;
        }

        $scope.startUpScaleCluster = function() {
            if ($scope.upscaleCluster.numberOfInstances <= 0) {
                $scope.showErrorMessage($rootScope.msg.cluster_upscale_valid_failed);
                $scope.upscaleCluster.numberOfInstances = 1;
                return;
            }
            if ($rootScope.activeCluster.orchestrator != null && $rootScope.activeCluster.orchestrator.type === "MARATHON") {
                var scaleJson = {
                    "hostGroupAdjustment": {
                        "hostGroup": $scope.upscaleCluster.hostGroup,
                        "scalingAdjustment": $scope.upscaleCluster.numberOfInstances
                    }
                };
                Cluster.update({
                    id: $rootScope.activeCluster.id
                }, scaleJson, function(success) {
                    angular.forEach($rootScope.activeCluster.hostGroups, function(hg) {
                        if (hg.name == $scope.upscaleCluster.hostGroup) {
                            hg.hostCount += $scope.upscaleCluster.numberOfInstances;
                            return;
                        }
                    });
                    $scope.showSuccess($rootScope.msg.cluster_upscale_update_success);
                    $scope.upscaleCluster = {
                        hostGroup: $scope.getHostGroupName(),
                        numberOfInstances: 1
                    };
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.cluster_upscale_update_failed);
                });
            } else {
                var scaleJson = {
                    "instanceGroupAdjustment": {
                        "instanceGroup": $scope.upscaleCluster.hostGroup,
                        "scalingAdjustment": $scope.upscaleCluster.numberOfInstances,
                        "withClusterEvent": true
                    }
                };
                GlobalStack.update({
                    id: $rootScope.activeCluster.id
                }, scaleJson, function(success) {
                    angular.forEach($rootScope.activeCluster.hostGroups, function(hg) {
                        if (hg.name == $scope.upscaleCluster.hostGroup) {
                            hg.hostCount += $scope.upscaleCluster.numberOfInstances;
                            return;
                        }
                    });
                    $scope.showSuccess($rootScope.msg.cluster_upscale_update_success);
                    $scope.upscaleCluster = {
                        hostGroup: $scope.getHostGroupName(),
                        numberOfInstances: 1
                    };
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.cluster_upscale_update_failed);
                });
            }
        };

        $scope.startDownScaleCluster = function() {
            if ($scope.downscaleCluster.numberOfInstances <= 0) {
                $scope.showErrorMessage($rootScope.msg.cluster_downscale_valid_failed);
                $scope.downscaleCluster.numberOfInstances = 1;
                return;
            }
            var scaleJson = {
                "hostGroupAdjustment": {
                    "hostGroup": $scope.downscaleCluster.hostGroup,
                    "scalingAdjustment": $scope.downscaleCluster.numberOfInstances * (-1),
                    "withStackUpdate": false
                }
            };
            if ($rootScope.activeCluster.orchestrator != null && $rootScope.activeCluster.orchestrator.type === "MARATHON") {
                scaleJson.hostGroupAdjustment.withStackUpdate = false;
            } else {
                scaleJson.hostGroupAdjustment.withStackUpdate = true;
            }
            Cluster.update({
                id: $rootScope.activeCluster.id
            }, scaleJson, function(success) {
                angular.forEach($rootScope.activeCluster.hostGroups, function(hg) {
                    if (hg.name == $scope.downscaleCluster.hostGroup) {
                        hg.nodeCount -= $scope.downscaleCluster.numberOfInstances;
                        return;
                    }
                });
                $scope.showSuccess($rootScope.msg.cluster_downscale_update_success);
                $scope.downscaleCluster = {
                    hostGroup: $scope.getHostGroupName(),
                    numberOfInstances: 1
                };
            }, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_downscale_update_failed);
            });
        };

        $scope.getHostGroupName = function() {
            if ($rootScope.activeCluster.hostGroups !== null && $rootScope.activeCluster.hostGroups.length > 0) {
                return $rootScope.activeCluster.hostGroups[0].name
            }
            return ''
        };

        $scope.initReinstallClusterObject = function() {
            $rootScope.reinstallClusterObject = {
                validateBlueprint: true,
                blueprintId: $rootScope.activeClusterBlueprint.id,
                hostgroups: $rootScope.activeCluster.cluster != undefined && $rootScope.activeCluster.cluster.hostGroups.length > 0 ? $rootScope.activeCluster.cluster.hostGroups : [],
                ambariStackDetails: $rootScope.activeCluster.cluster != undefined ? $rootScope.activeCluster.cluster.ambariStackDetails : '',
                fullBp: $rootScope.activeClusterBlueprint,
            };
            $scope.selectBlueprintreinstallChange();
        };

        $scope.changeActiveGroup = function(group) {
            $scope.cluster.activeGroup = group;
        }

        $scope.$watch('pagination.currentPage + pagination.itemsPerPage', function() {
            if ($rootScope.activeCluster.metadata != null) {
                paginateMetadata();
            }
        });

        $rootScope.$watch('activeCredential', function() {
            if ($rootScope.activeCredential != null) {
                $rootScope.activeStack = undefined;
                $scope.cluster.bestEffort = "BEST_EFFORT";
                $scope.cluster.failurePolicy.adjustmentType = "BEST_EFFORT";
                $scope.cluster.failurePolicy.threshold = null;
                $scope.cluster.parameters = {};
                $scope.cluster.orchestrator = {};
                $scope.cluster.availabilityZone = null;
                $scope.cluster.region = null;
                delete $scope.cluster.hostGroups;
                delete $scope.cluster.instanceGroups;
                delete $scope.cluster.blueprintId;
                setFileSystem();
                setNetwork();
                setRegion();
                initWizard();
                setPlatformVariant();
                setOrchestrator();
                setInstanceProfile();
            }
        });

        $rootScope.$watch('activeStack', function() {
            if ($rootScope.activeStack != null) {
                $rootScope.activeCredential = undefined;
                $scope.cluster.parameters = {};
                $scope.cluster.region = null;
                $scope.cluster.activeGroup = $rootScope.activeCluster.hostGroups != undefined && $rootScope.activeCluster.hostGroups.length > 0 ? $rootScope.activeCluster.hostGroups[0].name : '';
                delete $scope.cluster.fileSystem;
                delete $scope.cluster.network;
                delete $scope.cluster.securityGroup;
                delete $scope.cluster.hostGroups;
                delete $scope.cluster.instanceGroups;
                delete $scope.cluster.blueprintId;
                initWizard();
            }
        });

        function setFileSystem() {
            if ($rootScope.activeCredential != undefined && $rootScope.activeCredential.cloudPlatform != undefined) {
                if ($rootScope.activeCredential.cloudPlatform == 'AZURE_RM') {
                    $scope.cluster.fileSystem = {};
                    $scope.cluster.fileSystem.type = "WASB";
                    $scope.cluster.fileSystem.defaultFs = true;
                    $scope.cluster.relocateDocker = true;
                    $scope.cluster.parameters.persistentStorage = "cbstore";
                    $scope.cluster.parameters.attachedStorageOption = "SINGLE";
                } else if ($rootScope.activeCredential.cloudPlatform == 'GCP') {
                    $scope.cluster.fileSystem = {};
                    $scope.cluster.fileSystem.type = "LOCAL";
                    $scope.cluster.fileSystem.properties = {};
                    $scope.cluster.relocateDocker = false;
                    $scope.cluster.fileSystem.properties.projectId = $rootScope.activeCredential.parameters.projectId;
                    $scope.cluster.fileSystem.properties.serviceAccountEmail = $rootScope.activeCredential.parameters.serviceAccountId;
                    $scope.cluster.fileSystem.properties.privateKeyEncoded = $rootScope.activeCredential.parameters.serviceAccountPrivateKey;
                } else {
                    $scope.cluster.relocateDocker = false;
                    delete $scope.cluster.fileSystem;
                    delete $scope.cluster.parameters.persistentStorage;
                }
            }
        }

        function setInstanceProfile() {
            if ($rootScope.activeCredential != undefined && $rootScope.activeCredential.cloudPlatform != undefined) {
                if ($rootScope.activeCredential.cloudPlatform == 'AWS') {
                    delete $scope.cluster.parameters.s3Role;
                    $scope.cluster.parameters.instanceProfileStrategy = "NONE";
                }
            }
        }

        function setNetwork() {
            if ($rootScope.activeCredential != undefined) {
                var nets = $filter('filter')($rootScope.networks, function(value, index, array) {value.cloudPlatform === $rootScope.activeCredential.cloudPlatform}, true);
                var orderedNets = $filter('orderBy')($rootScope.networks, 'name', false);
                if (orderedNets.length > 0) {
                    $scope.cluster.networkId = orderedNets[0].id;
                }
            }
        }

        function setRegion() {
            if ($rootScope.activeCredential !== undefined) {
                $scope.cluster.region = $rootScope.params.defaultRegions[$rootScope.activeCredential.cloudPlatform];
            }
        }

        function setPlatformVariant() {
            if ($rootScope.activeCredential !== undefined) {
                $scope.cluster.platformVariant = $rootScope.params.defaultVariants[$rootScope.activeCredential.cloudPlatform];
            }
        }

         function setOrchestrator() {
            if ($rootScope.activeCredential !== undefined) {
                $scope.cluster.orchestrator.type = $rootScope.params.defaultOrchestrators[$rootScope.activeCredential.cloudPlatform];
            }
         }

        $rootScope.$watch('activeCluster.metadata', function() {
            if ($rootScope.activeCluster.metadata != null) {
                paginateMetadata();
            }
        });

        function paginateMetadata() {
            if ($scope.pagination != null) {
                $scope.pagination.totalItems = $rootScope.activeCluster.metadata.length;
                var begin = (($scope.pagination.currentPage - 1) * $scope.pagination.itemsPerPage),
                    end = begin + $scope.pagination.itemsPerPage;
                $scope.filteredActiveClusterData = addStatesToMetadata($rootScope.activeCluster.metadata.slice(begin, end));
            } else {
                $scope.filteredActiveClusterData = [];
            }
        }

        function addStatesToMetadata(filteredData) {
            angular.forEach(filteredData, function(data) {
                if (data != null && data.discoveryFQDN != null) {
                    angular.forEach($rootScope.activeCluster.cluster.hostGroups, function(hg) {
                        if (hg.constraint.instanceGroupName == data.instanceGroup) {
                            angular.forEach(hg.metadata, function(m) {
                                if (m.name == data.discoveryFQDN) {
                                    data.state = m.state;
                                }
                            });
                        }
                    });
                }
            });
            return filteredData;
        }

        $scope.getSelectedTemplate = function(templateId) {
            return $filter('filter')($rootScope.templates, {
                id: templateId
            }, true)[0];
        }

        $rootScope.events = [];

        $scope.loadEvents = function() {
            $rootScope.events = UserEvents.query(function(success) {
                angular.forEach(success, function(item) {
                    item.customTimeStamp = new Date(item.eventTimestamp).toLocaleDateString() + " " + new Date(item.eventTimestamp).toLocaleTimeString();
                });
            });
        }

        $scope.loadEvents();

        $scope.stopCluster = function(activeCluster) {
            var newStatus = {
                "status": "STOPPED"
            };
            Cluster.update({
                id: activeCluster.id
            }, newStatus, function(success) {
                GlobalStack.update({
                    id: activeCluster.id
                }, newStatus, function(result) {
                    activeCluster.status = "STOP_REQUESTED";
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.cluster_stop_failed);
                });

            }, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_stop_failed);
            });
        }

        $scope.isEphemeralCluster = function(activeCluster) {
            var isEphemeral = false;
            if (activeCluster.cloudPlatform === 'AWS') {
                angular.forEach(activeCluster.instanceGroups, function(item) {
                    var actualTemplate = $filter('filter')($rootScope.templates, {
                        id: item.templateId
                    });
                    if (actualTemplate.length > 0) {
                        if (actualTemplate[0].volumeType === 'ephemeral') {
                            isEphemeral = true;
                        }
                    }
                });
            }
            return isEphemeral;
        }

        $scope.reinstallCluster = function(activeCluster) {
            if (!$scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails)) {
                for (var item in $rootScope.reinstallClusterObject.ambariStackDetails) {
                    if ($rootScope.reinstallClusterObject.ambariStackDetails[item] === "" || $rootScope.reinstallClusterObject.ambariStackDetails[item] === undefined) {
                        delete $rootScope.reinstallClusterObject.ambariStackDetails[item];
                    }
                }
            }
            if (!$scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails) && Object.keys($rootScope.reinstallClusterObject.ambariStackDetails).length !== 0) {
                if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stack) ||
                    $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.version) ||
                    $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackRepoId) ||
                    $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackBaseURL) ||
                    $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsRepoId) ||
                    $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsBaseURL)) {
                    $scope.showErrorMessage($rootScope.msg.ambari_repository_config_error);
                    return;
                } else {
                    $rootScope.reinstallClusterObject.ambariStackDetails.os = "redhat7";
                    if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.verify)) {
                        $rootScope.reinstallClusterObject.ambariStackDetails.verify = false;
                    }
                }
            } else {
                $rootScope.reinstallClusterObject.ambariStackDetails = null;
            }
            var newInstall = {
                "blueprintId": $rootScope.reinstallClusterObject.blueprintId,
                "hostgroups": $rootScope.reinstallClusterObject.hostgroups,
                "validateBlueprint": $rootScope.reinstallClusterObject.validateBlueprint,
                "ambariStackDetails": $rootScope.reinstallClusterObject.ambariStackDetails
            };
            Cluster.update({
                id: activeCluster.id
            }, newInstall, function(success) {
                $rootScope.activeCluster.blueprintId = $rootScope.reinstallClusterObject.blueprintId;
                $rootScope.activeCluster.cluster.status = 'REQUESTED';
            }, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_reinstall_failed);
            });
        }

        $scope.startCluster = function(activeCluster) {
            var newStatus = {
                "status": "STARTED"
            };
            GlobalStack.update({
                id: activeCluster.id
            }, newStatus, function(result) {

                Cluster.update({
                    id: activeCluster.id
                }, newStatus, function(success) {
                    activeCluster.status = "START_REQUESTED";
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.cluster_start_failed);
                });

            }, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_start_failed);
            });
        }

        $scope.syncCluster = function(activeCluster) {
            var newStatus = {
                "status": "FULL_SYNC"
            };
            GlobalStack.update({
                id: activeCluster.id
            }, newStatus, function(result) {}, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_sync_failed);
            });
        }

        $scope.$watch('cluster.region', function() {
            if ($rootScope.activeCredential !== undefined && ($rootScope.activeCredential.cloudPlatform === 'AWS' || $rootScope.activeCredential.cloudPlatform === 'GCP')) {
                if ($scope.cluster.region === null) {
                    $scope.cluster.availabilityZone = null;
                } else {
                    $scope.avZones = $rootScope.params.zones[$rootScope.activeCredential.cloudPlatform][$scope.cluster.region];
                    if ($rootScope.activeCredential.cloudPlatform === 'GCP') {
                        $scope.cluster.availabilityZone = $scope.avZones[0];
                    }
                }
            } else {
                $scope.cluster.availabilityZone = null;
            }
        });


        $scope.requestStatusChange = function(cluster) {
            if (cluster.status == "STOPPED") {
                $scope.startCluster(cluster);
            } else if (cluster.status == "AVAILABLE") {
                $scope.stopCluster(cluster);
            }
        }

        function getUluwatuClusters() {
            UluwatuCluster.query(function(clusters) {
                $rootScope.clusters = clusters;
                $scope.$parent.orderClusters();
            });
        }

        function initCluster() {
            $scope.cluster = {
                password: "admin",
                userName: "admin",
                onFailureAction: "DO_NOTHING",
                bestEffort: "BEST_EFFORT",
                validateBlueprint: true,
                parameters: {},
                failurePolicy: {
                    adjustmentType: "BEST_EFFORT",
                },
                orchestrator: {},
                configStrategy: $scope.configStrategies[1],
                ldapRequired: false,
                sssdConfigId: null,
                enableShipyard: false
            };
            $scope.selectSssd = {
                show: false
            };
            setFileSystem();
            initWizard();
            setNetwork();
            setRegion();
            setPlatformVariant();
            setOrchestrator();
            setInstanceProfile();
        }

        $scope.getPlatformVariants = function() {
            var variants = [];
            if ($rootScope.activeCredential !== undefined) {
                variants = $rootScope.params.platformVariants[$rootScope.activeCredential.cloudPlatform];
            }
            return variants;
        }

        $scope.showDetails = function() {
            $scope.detailsShow = true;
            $scope.periscopeShow = false;
            $scope.metricsShow = false;
        }

        $scope.showPeriscope = function() {
            $scope.detailsShow = false;
            $scope.periscopeShow = true;
            $scope.metricsShow = false;
        }

        $scope.showMetrics = function() {
            $scope.detailsShow = false;
            $scope.periscopeShow = false;
            $scope.metricsShow = true;
        }

        $scope.jsonBp = true;
        $scope.serviceBp = false;

        $scope.changeViewJsonBp = function() {
            $scope.jsonBp = true;
            $scope.serviceBp = false;
        }

        $scope.changeViewServiceBp = function() {
            $scope.jsonBp = false;
            $scope.serviceBp = true;
        }

        $scope.logFilterFunction = function(element) {
            try {
                if (element.stackId === $rootScope.activeCluster.id) {
                    if (element.clusterId !== null && element.cloud === 'BYOS') {
                        if (element.clusterId === $rootScope.activeCluster.cluster.id && element.clusterName === $rootScope.activeCluster.cluster.name) {
                            return (!element.eventType.match('BILLING_STARTED') && !element.eventType.match('BILLING_STOPPED') && !element.eventType.match('BILLING_CHANGED')) ? true : false;
                        }
                    } else if ($rootScope.activeCluster.platformVariant !== 'BYOS') {
                        return (!element.eventType.match('BILLING_STARTED') && !element.eventType.match('BILLING_STOPPED') && !element.eventType.match('BILLING_CHANGED')) ? true : false;
                    }
                }
                return false;
            } catch (err) {
                return false;
            }
        }
        $scope.selectCluster = function(cluster) {
            $scope.selectedCluster = cluster
        }

        $scope.recipesToShow = [];
        $scope.toggleRecipes = function(index) {
            $scope.recipesToShow[index] = $scope.recipesToShow[index] ? false : true;
        }

        $scope.notStrictFilter = function(a, b) {
            return a == b;
        }

        $scope.filterByTopology = function(resource) {
            return !resource.topologyId || ($rootScope.activeCredential && resource.topologyId == $rootScope.activeCredential.topologyId)
        }

        $scope.isExistingVpc = function() {
            if ($rootScope.activeCredential && $rootScope.activeCredential.cloudPlatform == 'AZURE_RM' && $scope.cluster.networkId) {
                var network = $filter('filter')($rootScope.networks, {
                    id: $scope.cluster.networkId
                })[0];
                if (network && network.parameters && network.parameters.resourceGroupName && network.parameters.networkId && network.parameters.subnetId) {
                    return true
                }
            }
            return false
        }

        $scope.changeAmbariServer = function(instanceGroup) {
            angular.forEach($scope.cluster.instanceGroups, function(ig) {
                if (ig.group === instanceGroup.group) {
                    ig.type = 'GATEWAY'
                } else {
                    ig.type = 'CORE'
                }
            });
        }

        $scope.ambariServerSelected = function() {
            var result = false
            var activeStack = $rootScope.activeStack;
            if (activeStack && activeStack.orchestrator && activeStack.orchestrator.type === "MARATHON") {
                return true;
            }
            angular.forEach($scope.cluster.instanceGroups, function(ig) {
                if (ig.type === 'GATEWAY') {
                    result = true
                }
            });
            return result
        }

        $scope.noProxyBeforeAmbari = function() {
            return $rootScope.activeCluster.orchestrator.type === 'MARATHON'
            || $rootScope.activeCluster.cloudbreakDetails === null
            || !$rootScope.activeCluster.cloudbreakDetails
        }
    }
]);