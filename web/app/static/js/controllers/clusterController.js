'use strict';

var log = log4javascript.getLogger("clusterController-logger");
var $jq = jQuery.noConflict();

angular.module('uluwatuControllers').controller('clusterController', ['$scope', '$rootScope', '$filter', 'UluwatuCluster', 'GlobalStack', 'Cluster', 'DefaultSsh', 'GlobalStackInstance', '$interval', 'UserEvents', 'PeriscopeCluster', 'PlatformVariant', '$sce',
    function($scope, $rootScope, $filter, UluwatuCluster, GlobalStack, Cluster, DefaultSsh, GlobalStackInstance, $interval, UserEvents, PeriscopeCluster, PlatformVariant, $sce) {

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

        $scope.knoxPort = "8443";

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
        initDefaultSsh();
        getUluwatuClusters();
        initCluster();
        initWizard();

        $scope.defautSshKey = "";

        function initDefaultSsh() {
            DefaultSsh.get(function (result) {
                $scope.defautSshKey = result.defaultSshKey;
                $scope.cluster.publicKey = $scope.defautSshKey;
            });
        }

        function initWizard() {
            $scope.configureHostGroups = false;
            $scope.configureFailureAction = false;
            $scope.configureAmbariRepository = false;
            $scope.configureHdpRepository = false;
            $scope.configureAmbariDatabase = false;
            $scope.configureSecurity = false;
            $scope.configureFileSystem = false;
            $scope.configureCluster = true;
            $scope.configureReview = false;
        }

        $scope.clearWasb = function () {
            $scope.cluster.fileSystem.properties.accountKey = "";
            $scope.cluster.fileSystem.properties.secure = false;
        };

        $scope.showWizardActualElement = function(element) {
            $scope.configureHostGroups = false;
            $scope.configureFailureAction = false;
            $scope.configureAmbariRepository = false;
            $scope.configureHdpRepository = false;
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
            } else if (element == 'configureHdpRepository') {
                $scope.configureHdpRepository = true;
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
                        name: value.group
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
                if ($rootScope.securitygroups && $rootScope.securitygroups.length != 0 && $rootScope.activeCredential.cloudPlatform !== "BYOS") {
                    var sgroups = $filter('orderBy')($rootScope.securitygroups, 'name', false);
                    for (var i = 0; i < sgroups.length; i++) {
                         if (sgroups[i].cloudPlatform ===  $rootScope.activeCredential.cloudPlatform) {
                            sgroupsActiveId = sgroups[i].id;
                            break;
                         }
                    }
                }
                actualBp[0].ambariBlueprint.host_groups.forEach(function(k) {
                    instanceGroups.push({
                        templateId: tmpTemplateId,
                        securityGroupId: sgroupsActiveId,
                        group: k.name,
                        nodeCount: 1,
                        type: "CORE",
                        parameters : {
                            availabilitySet: {
                                name: "",
                                faultDomainCount: 3
                            }
                        },

                    });
                    if ($rootScope.activeCredential.cloudPlatform === "BYOS") {
                        var constraintTemplates = $rootScope.constraints;
                        var tmpConstraint = $filter('orderBy')(constraintTemplates, 'name', false)[0];
                        var tmpConstraintName;
                        if (tmpConstraint != null) {
                            tmpConstraintName = tmpConstraint.name;
                        }
                        hostGroups.push({
                            name: k.name,
                            constraint: {
                                constraintTemplateName: tmpConstraintName,
                                instanceGroupName: k.name,
                                hostCount: 1
                            },
                            recipeIds: []
                        });
                    } else {
                        hostGroups.push({
                            name: k.name,
                            constraint: {
                                instanceGroupName: k.name,
                                hostCount: 1
                            },
                            recipeIds: []
                        });
                    }
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

            $scope.showBlueprintKnoxError();
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
                setAmbariStackDetails($rootScope.activeCredential, $scope.cluster);
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
            if (!$scope.isUndefined($scope.cluster.ambariRepoDetailsJson)) {
                for (var item in $scope.cluster.ambariRepoDetailsJson) {
                    if ($scope.cluster.ambariRepoDetailsJson[item] === "" || $scope.cluster.ambariRepoDetailsJson[item] === undefined) {
                        delete $scope.cluster.ambariRepoDetailsJson[item];
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

            if ($rootScope.activeCredential && ($rootScope.activeCredential.cloudPlatform == 'AWS' || $rootScope.activeCredential.cloudPlatform == 'OPENSTACK' || $rootScope.activeCredential.cloudPlatform == 'BYOS')) {
                delete $scope.cluster.fileSystem;
            }
            if ($scope.cluster.platformVariant === 'BYOS') {
                $scope.cluster.networkId = null;
                if ($scope.cluster.customContainer == true) {
                    $scope.cluster.customContainerObj = {
                        definitions: {
                            "AMBARI_SERVER": $scope.cluster.ambariServerId,
                            "AMBARI_AGENT": $scope.cluster.ambariAgentId,
                            "AMBARI_DB": $scope.cluster.ambariDbId
                        }
                    }
                }
                if ($scope.cluster.customQueue !== true) {
                    $scope.cluster.customQueueId = "default";
                }
            }
            if ($rootScope.activeCredential && $rootScope.activeCredential.cloudPlatform !== 'AWS') {
                delete $scope.cluster.parameters.instanceProfile;
                delete $scope.cluster.parameters.instanceProfileStrategy;
            } else {
                if ($scope.cluster.parameters.instanceProfileStrategy == "NONE") {
                    delete $scope.cluster.parameters.instanceProfile;
                    delete $scope.cluster.parameters.instanceProfileStrategy;
                } else if ($scope.cluster.parameters.instanceProfileStrategy == "USE_EXISTING" && $scope.isUndefined($scope.cluster.parameters.instanceProfile)) {
                    $scope.showErrorMessage($rootScope.msg.insprof_role_error);
                    return;
                } else if ($scope.cluster.parameters.instanceProfileStrategy == "CREATE") {
                    delete $scope.cluster.parameters.instanceProfile;
                }
            }
            if (!$scope.isUndefined($scope.cluster.ambariStackDetails) && Object.keys($scope.cluster.ambariStackDetails).length !== 0) {
                if ($scope.isUndefined($scope.cluster.ambariStackDetails.os)) {
                    if ($scope.isUndefined($scope.cluster.ambariStackDetails.stack) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.version) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackRepoId) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackBaseURL) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsRepoId) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsBaseURL)) {
                        $scope.showErrorMessage($rootScope.msg.ambari_hdp_repository_config_error);
                        return;
                    } else {
                        if ($scope.isUndefined($scope.cluster.ambariStackDetails.verify)) {
                            $scope.cluster.ambariStackDetails.verify = false;
                        }
                    }
                } else {
                    if ($scope.isUndefined($scope.cluster.ambariStackDetails.stack) &&
                        $scope.isUndefined($scope.cluster.ambariStackDetails.version) &&
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackRepoId) &&
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackBaseURL) &&
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsRepoId) &&
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsBaseURL)) {
                        $scope.cluster.ambariStackDetails = null;
                    } else if ($scope.isUndefined($scope.cluster.ambariStackDetails.stack) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.version) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackRepoId) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackBaseURL) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsRepoId) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsBaseURL)) {
                        $scope.showErrorMessage($rootScope.msg.ambari_hdp_repository_config_error);
                        return;
                    } else {
                        if ($scope.isUndefined($scope.cluster.ambariStackDetails.verify)) {
                            $scope.cluster.ambariStackDetails.verify = false;
                        }
                    }
                }
                if (!$scope.isUndefined($scope.cluster.ambariStackDetails) &&
                    ($scope.isUndefined($scope.cluster.ambariStackDetails.stack) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.version) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackRepoId) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.stackBaseURL) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsRepoId) ||
                        $scope.isUndefined($scope.cluster.ambariStackDetails.utilsBaseURL))) {
                    $scope.showErrorMessage($rootScope.msg.ambari_hdp_repository_config_error);
                    return;
                } else {
                    if (!$scope.isUndefined($scope.cluster.ambariStackDetails) && $scope.isUndefined($scope.cluster.ambariStackDetails.verify)) {
                        $scope.cluster.ambariStackDetails.verify = false;
                    }
                }
                if ($scope.cluster.enableExSecurity == 'NONE') {
                    delete $scope.cluster.kerberos.domain;
                    delete $scope.cluster.kerberos.url;
                    delete $scope.cluster.kerberos.realm;
                    delete $scope.cluster.kerberos.principal;
                    delete $scope.cluster.kerberos.ldapUrl;
                    delete $scope.cluster.kerberos.containerDn;
                    delete $scope.cluster.kerberos.tcpAllowed;
                } else if ($scope.cluster.enableExSecurity == 'MIT-KERB') {
                    delete $scope.cluster.kerberos.masterKey;
                    delete $scope.cluster.kerberos.admin;
                    delete $scope.cluster.kerberos.ldapUrl;
                    delete $scope.cluster.kerberos.containerDn;
                } else if ($scope.cluster.enableExSecurity == 'AD-KERB') {
                    delete $scope.cluster.kerberos.masterKey;
                    delete $scope.cluster.kerberos.admin;
                }
            } else {
                $scope.cluster.ambariStackDetails = null;
            }

            if (!$scope.isUndefined($scope.cluster.ambariRepoDetailsJson) && Object.keys($scope.cluster.ambariRepoDetailsJson).length !== 0) {
                if ($scope.isUndefined($scope.cluster.ambariRepoDetailsJson.version) ||
                        $scope.isUndefined($scope.cluster.ambariRepoDetailsJson.baseUrl) ||
                        $scope.isUndefined($scope.cluster.ambariRepoDetailsJson.gpgKeyUrl)) {
                        $scope.showErrorMessage($rootScope.msg.ambari_repository_config_error);
                        return;
                    }
            } else {
                $scope.cluster.ambariRepoDetailsJson = null;
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
                    result.cloudPlatform = $filter('filter')($rootScope.credentials, { id: $rootScope.activeCredential.id }, true)[0].cloudPlatform;
                    result.public = $scope.cluster.public;
                    angular.forEach(result.instanceGroups, function(item) {
                        item.templateId = parseFloat(item.templateId);
                    });
                    result.blueprintId = parseFloat(result.blueprintId);

                    var existingCluster = null;
                    try {
                        existingCluster = $filter('filter')($rootScope.clusters, { id: result.id }, true)[0];
                    } catch(err) {
                        existingCluster = null;
                    }

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
                    setAmbariStackDetails($rootScope.activeCredential, $scope.cluster);
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
                    gateway: $scope.cluster.gateway,
                    validateBlueprint: $scope.cluster.validateBlueprint,
                    imageId:  $scope.cluster.imageId || null,
                    fileSystem: $scope.cluster.fileSystem || null,
                    configStrategy: $scope.cluster.configStrategy || null,
                    ambariRepoDetailsJson: $scope.cluster.ambariRepoDetailsJson === 'undefined' ? null : $scope.cluster.ambariRepoDetailsJson,
                    ambariStackDetails: $scope.cluster.ambariStackDetails === 'undefined' ? null : $scope.cluster.ambariStackDetails,
                    ambariDatabaseDetails: $scope.cluster.ambariDatabaseDetails === 'undefined' ? null : $scope.cluster.ambariDatabaseDetails
                }
                if ($scope.cluster.customContainer == true) {
                    cbCluster.customContainerObj = {
                        definitions: {
                            "AMBARI_SERVER": $scope.cluster.ambariServerId,
                            "AMBARI_AGENT": $scope.cluster.ambariAgentId,
                            "AMBARI_DB": $scope.cluster.ambariDbId
                        }
                    }
                }
                if ($scope.cluster.customQueue !== true) {
                    $scope.cluster.customQueueId = "default";
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
            if ($rootScope.clusters == null || $rootScope.clusters == undefined) {
                 $rootScope.clusters = [];
            }
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
            if ($scope.cluster.fileSystem.type == 'WASB') {
                if (!$scope.isUndefined($scope.cluster.fileSystem.properties.accountName) && !$scope.isUndefined($scope.cluster.fileSystem.properties.accountKey)) {
                    return false;
                }
            } else if ($scope.cluster.fileSystem.type == 'ADLS') {
                if (!$scope.isUndefined($scope.cluster.fileSystem.properties.accountName)) {
                    return false;
                }
            } else if ($scope.cluster.fileSystem.type == 'GCS') {
                if (!$scope.isUndefined($scope.cluster.fileSystem.properties.projectId) && !$scope.isUndefined($scope.cluster.fileSystem.properties.serviceAccountEmail) && !$scope.isUndefined($scope.cluster.fileSystem.properties.defaultBucketName)) {
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
                if (item === "timetolive") {
                    cluster.parameters[item] = cluster.parameters[item] * 60 * 1000;
                }
            }
        }

        $scope.delTimetolive = function(enabled) {
            if (!enabled) {
                delete $scope.cluster.parameters.timetolive
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
                    if ($rootScope.activeCluster.blueprintId) {
                        $rootScope.activeClusterBlueprint = $filter('filter')($rootScope.blueprints, {
                            id: $rootScope.activeCluster.blueprintId
                        }, true)[0];
                    }
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
            var isByos = $rootScope.activeCluster.orchestrator.type === "MARATHON" || $rootScope.activeCluster.orchestrator.type === "YARN";
            if ($rootScope.activeCluster.orchestrator != null && isByos) {
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
                fullBp: $rootScope.activeClusterBlueprint,
            };
            if ($rootScope.activeCluster.cluster != undefined) {
                setAmbariStackDetails($rootScope.activeCluster, $rootScope.reinstallClusterObject);
            }
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
                $scope.escapeRegex();
                setFileSystem();
                setNetwork();
                setRegion();
                initWizard();
                setPlatformVariant();
                setOrchestrator();
                setInstanceProfile();
                setAmbariStackDetails($rootScope.activeCredential, $scope.cluster);
                setFlex();
                $scope.cluster.customContainer = false;
                $scope.cluster.customQueue = false;
                $scope.cluster.customQueueId = "default";
                $scope.cluster.customImage = false;
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

        function setSecurity() {
            $scope.cluster.enableSecurity = false;
            $scope.cluster.enableExSecurity = 'NONE';
            $scope.cluster.kerberos = {
                "tcpAllowed": false
            };
        }

        function setFileSystem() {
            if ($rootScope.activeCredential != undefined && $rootScope.activeCredential.cloudPlatform != undefined) {
                if ($rootScope.activeCredential.cloudPlatform == 'AZURE') {
                    $scope.cluster.fileSystem = {};
                    $scope.cluster.fileSystem.type = "ADLS";
                    $scope.cluster.fileSystem.defaultFs = false;
                    $scope.cluster.fileSystem.properties = {};
                    $scope.cluster.fileSystem.properties.tenantId = $rootScope.activeCredential.parameters.tenantId;
                    $scope.cluster.fileSystem.properties.clientId = $rootScope.activeCredential.parameters.accessKey;
                    $scope.cluster.fileSystem.properties.secure = false;
                    $scope.cluster.parameters.persistentStorage = "cbstore";
                    $scope.cluster.parameters.attachedStorageOption = "SINGLE";
                } else if ($rootScope.activeCredential.cloudPlatform == 'GCP') {
                    $scope.cluster.fileSystem = {};
                    $scope.cluster.fileSystem.type = "LOCAL";
                    $scope.cluster.fileSystem.properties = {};
                    $scope.cluster.fileSystem.properties.projectId = $rootScope.activeCredential.parameters.projectId;
                    $scope.cluster.fileSystem.properties.serviceAccountEmail = $rootScope.activeCredential.parameters.serviceAccountId;
                    $scope.cluster.fileSystem.properties.privateKeyEncoded = $rootScope.activeCredential.parameters.serviceAccountPrivateKey;
                } else {
                    delete $scope.cluster.fileSystem;
                    delete $scope.cluster.parameters.persistentStorage;
                }
            }
        }

        function setInstanceProfile() {
            if ($rootScope.activeCredential != undefined && $rootScope.activeCredential.cloudPlatform != undefined) {
                if ($rootScope.activeCredential.cloudPlatform == 'AWS') {
                    delete $scope.cluster.parameters.instanceProfile;
                    $scope.cluster.parameters.instanceProfileStrategy = "NONE";
                }
            }
        }

        function setNetwork() {
            if ($rootScope.activeCredential != undefined && $rootScope.activeCredential.cloudPlatform !== "BYOS") {
                var nets = $filter('filter')($rootScope.networks, function(value, index, array) {
                    return value.cloudPlatform == $rootScope.activeCredential.cloudPlatform
                }, true);
                var orderedNets = $filter('orderBy')(nets, 'name', false);
                if (orderedNets.length > 0) {
                    $scope.cluster.networkId = orderedNets[0].id;
                }
            }
        }

        function setFlex() {
            if ($rootScope.activeCredential != undefined && $rootScope.activeCredential.cloudPlatform !== "BYOS") {
                var defaultFlexs = $filter('filter')($rootScope.flexs, {default: true});
                if (defaultFlexs.length > 0) {
                    $scope.cluster.flexId = defaultFlexs[0].id.toString();
                }
            }
        }

        function setAmbariStackDetails(platformObject, setObject) {
            setObject.ambariStackDetails = {};
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
            if ($rootScope.activeCredential !== undefined && $rootScope.activeCredential.cloudPlatform !== "BYOS") {
                $scope.cluster.orchestrator.type = $rootScope.params.defaultOrchestrators[$rootScope.activeCredential.cloudPlatform];
            } else if ($rootScope.activeCredential !== undefined && $rootScope.activeCredential.cloudPlatform === "BYOS") {
                $scope.cluster.orchestrator.type = $rootScope.activeCredential.parameters.type;
                $scope.cluster.orchestrator.apiEndpoint = $rootScope.activeCredential.parameters.apiEndpoint;
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
                if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.os)) {
                    if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stack) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.version) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackRepoId) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackBaseURL) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsRepoId) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsBaseURL)) {
                        $scope.showErrorMessage($rootScope.msg.ambari_hdp_repository_config_error);
                        return;
                    } else {
                        if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.verify)) {
                            $rootScope.reinstallClusterObject.ambariStackDetails.verify = false;
                        }
                    }
                } else {
                    if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stack) &&
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.version) &&
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackRepoId) &&
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackBaseURL) &&
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsRepoId) &&
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsBaseURL)) {
                        $rootScope.reinstallClusterObject.ambariStackDetails = null;
                    } else if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stack) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.version) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackRepoId) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackBaseURL) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsRepoId) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsBaseURL)) {
                        $scope.showErrorMessage($rootScope.msg.ambari_hdp_repository_config_error);
                        return;
                    } else {
                        if ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.verify)) {
                            $rootScope.reinstallClusterObject.ambariStackDetails.verify = false;
                        }
                    }
                }
                if (!$scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails) &&
                    ($scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stack) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.version) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackRepoId) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.stackBaseURL) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsRepoId) ||
                        $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.utilsBaseURL))) {
                    $scope.showErrorMessage($rootScope.msg.ambari_hdp_repository_config_error);
                    return;
                } else {
                    if (!$scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails) && $scope.isUndefined($rootScope.reinstallClusterObject.ambariStackDetails.verify)) {
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

        $scope.repairCluster = function(activeCluster) {
            var newStatus = {
                "status": "REPAIR_FAILED_NODES"
            };
            GlobalStack.update({
                id: activeCluster.id
            }, newStatus, function(result) {}, function(error) {
                $scope.showError(error, $rootScope.msg.cluster_replace_nodes_failed);
            });
        }

        $scope.$watch('cluster.region', function() {
            if ($rootScope.activeCredential !== undefined && ($rootScope.activeCredential.cloudPlatform === 'AWS' || $rootScope.activeCredential.cloudPlatform === 'OPENSTACK' || $rootScope.activeCredential.cloudPlatform === 'GCP')) {
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

        $scope.filterByZoneInstanceType = function() {
            if ($rootScope.activeCredential !== undefined && $scope.cluster.region !== null) {
                $rootScope.templates.$promise.then(function(templates) {
                    if ($rootScope.activeCredential.cloudPlatform === 'OPENSTACK') {
                        $rootScope.filteredTemplates = templates;
                    } else {
                        var zoneid;
                        if ($rootScope.activeCredential.cloudPlatform === 'AZURE') {
                            zoneid = $scope.cluster.region;
                        } else {
                            zoneid = ($scope.cluster.availabilityZone !== undefined && $scope.cluster.availabilityZone !== null) ? $scope.cluster.availabilityZone : $rootScope.params.zones[$rootScope.activeCredential.cloudPlatform][$scope.cluster.region][0]
                        }
                        $rootScope.filteredTemplates = templates.filter(function(template) {
                            var zoneVmTypes = $rootScope.params.vmTypesPerZone[$rootScope.activeCredential.cloudPlatform][zoneid];
                            if (typeof zoneVmTypes !== 'undefined') {
                                for (i = 0; i < zoneVmTypes.length; i++) {
                                    if (zoneVmTypes[i].value === template.instanceType) {
                                        return true;
                                    }
                                }
                            }
                        })
                    }
                })
            }
        }

        $rootScope.$watch('activeCredential', $scope.filterByZoneInstanceType);
        $rootScope.$watch('templates', $scope.filterByZoneInstanceType, true);
        $scope.$watch('cluster.region', $scope.filterByZoneInstanceType);
        $scope.$watch('cluster.availabilityZone', $scope.filterByZoneInstanceType);

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
                executorType: "DEFAULT",
                gateway: {
                  enableGateway: false,
                  exposedServices: ['ALL']
                },
                parameters: {},
                failurePolicy: {
                    adjustmentType: "BEST_EFFORT",
                },
                publicKey: $scope.defautSshKey,
                ambariRepoDetailsJson: {},
                ambariStackDetails: {},
                orchestrator: {},
                configStrategy: $scope.configStrategies[1],
                customImage: false,
                customContainer: false,
                customQueue: false,
                customQueueId: "default",
                userDefinedTags: [],
                azureAvailabilitySets: []
            };
            $scope.actualRegex = "";
            setSecurity();
            setFileSystem();
            initWizard();
            setNetwork();
            setFlex();
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

        function validateTag(pattern, value) {
            var caseInSensitivePattern = pattern.replace(/\(\?i\)/g, "");
            var i = caseInSensitivePattern != pattern ? 'i' : '';
            return new RegExp(caseInSensitivePattern, i).test(value);
        }

        $scope.validateUserDefinedTagKey = (function() {
            return {
                test: function(value) {
                    var pattern = $rootScope.params.tagSpecifications[$rootScope.activeCredential.cloudPlatform].keyValidator;
                    return validateTag(pattern, value);
                }
            };
        })();

        $scope.validateUserDefinedTagValue = (function() {
            return {
                test: function(value) {
                    var pattern = $rootScope.params.tagSpecifications[$rootScope.activeCredential.cloudPlatform].valueValidator;
                    return validateTag(pattern, value);
                }
            };
        })();

        $scope.addUserDefinedTag = function () {
            $scope.cluster.userDefinedTags.push({"key": "", "value": ""});
        }

        $scope.removeUserDefinedTag = function (tag) {
            var index = $scope.cluster.userDefinedTags.indexOf(tag);
            if (index > -1) {
                $scope.cluster.userDefinedTags.splice(index, 1);
            }
        }

        $scope.removeAvailabilitySetsIfDisabled = function () {
            if (!$scope.cluster.parameters.azureAvailabilitySetsEnabled) {
                $scope.cluster.azureAvailabilitySets = [];
            }
        }

        $scope.addAvailabilitySet = function () {
            $scope.cluster.azureAvailabilitySets.push({"name": "", "faultDomainCount": 3});
        }

        $scope.removeAvailabilitySet = function (availabilitySet) {
            var index = $scope.cluster.azureAvailabilitySets.indexOf(availabilitySet);
            if (index > -1) {
                $scope.cluster.azureAvailabilitySets.splice(index, 1);
            }
        }

        $scope.isAvailabilitySetsInvalid = function() {
            var hasError = false;
            var counts = {};
            angular.forEach($scope.cluster.azureAvailabilitySets, function (availabilitySet) {
                counts[availabilitySet.name] = (counts[availabilitySet.name] || 0)+1;
                // there must be no multiplications in names
                if (counts[availabilitySet.name] > 1) {
                    hasError = true;
                }
                if (!availabilitySet["name"] || availabilitySet["name"].length < 3) {
                    hasError = true;
                }
                if (!availabilitySet["faultDomainCount"] || availabilitySet["faultDomainCount"] < 2 || availabilitySet["faultDomainCount"] > 3) {
                    hasError = true;
                }
            });
            return hasError;
        }

        function uniqueUsingSet(array){
            var seen = new Set;
            return array.filter(function(item){
                if (!seen.has(item)) {
                    seen.add(item);
                    return true;
                }
            });
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

        $scope.filterDeleteCompletedServices = function(element) {
            try {
                return (element.status == "DELETE_COMPLETED") ? false : true;
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
            if ($rootScope.activeCredential && $rootScope.activeCredential.cloudPlatform == 'AZURE' && $scope.cluster.networkId) {
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
                    if (ig.type === 'CORE') {
                        ig.type = 'GATEWAY';
                    } else if (ig.type === 'GATEWAY') {
                        ig.type = 'CORE';
                    }
                }
            });
            $scope.showBlueprintKnoxError();
        }

        $scope.ambariServerSelected = function() {
            var result = false
            var activeStack = $rootScope.activeStack;
            if ((activeStack && activeStack.orchestrator && (activeStack.orchestrator.type === "MARATHON" || activeStack.orchestrator.type === "YARN"))
                || ($rootScope.activeCredential && $rootScope.activeCredential.cloudPlatform === 'BYOS')) {
                return true;
            }
            angular.forEach($scope.cluster.instanceGroups, function(ig) {
                if (ig.type === 'GATEWAY') {
                    result = true
                }
            });
            return result
        }

        $scope.blueprintKnoxError = false;

        $scope.showBlueprintKnoxError = function() {
            var result = false;
                var actualBp = $filter('filter')($rootScope.blueprints, {
                    id: $scope.cluster.blueprintId
                }, true);
                actualBp[0].ambariBlueprint.host_groups.forEach(function (group) {
                    if (!result) {
                        var gatewayGroup = $filter('filter')($scope.cluster.instanceGroups, {
                            group: group.name,
                            type: 'GATEWAY'
                        }, true);
                        if (gatewayGroup && gatewayGroup[0]) {
                            var knoxGateway = $filter('filter')(group.components, {
                                name: 'KNOX_GATEWAY'
                            }, true);
                            if (knoxGateway && knoxGateway[0]) {
                                result = true;
                            }
                        }
                    }
                });
            $scope.blueprintKnoxError = result;
            return result;
        }

        $scope.showSecurityGroupKnoxWarning = function(instanceGroup) {
            var result = false;
            if ($rootScope.securitygroups &&
                $rootScope.securitygroups.length != 0 &&
                $rootScope.activeCredential.cloudPlatform !== "BYOS" &&
                instanceGroup.type === 'GATEWAY' &&
                $scope.cluster.gateway &&
                $scope.cluster.gateway.enableGateway) {
                result = true;
                var actualSg = $filter('filter')($rootScope.securitygroups, {
                    id: instanceGroup.securityGroupId
                }, true);
                if (actualSg[0]) {
                    angular.forEach(actualSg[0].securityRules, function(rules) {
                        if (result) {
                            angular.forEach(rules.portarray, function (port) {
                                if (result) {
                                    if (port === $scope.knoxPort) {
                                        result = false;
                                    } else if (port.split('-').length == 2) {
                                        var min = port.split('-')[0];
                                        var max = port.split('-')[1];
                                        if (parseInt(min) <= parseInt($scope.knoxPort) && parseInt(max) >= parseInt($scope.knoxPort)) {
                                            result = false;
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            }
            return result;
        }

        $scope.hideAvailabilitySetHostgroupWarning = function(instanceGroup) {
            var result = false;
            // Show warning
            if (instanceGroup.nodeCount == 1) {
                if (instanceGroup.parameters.availabilitySet == null || instanceGroup.parameters.availabilitySet.name === "") {
                    result = true;
                }
            } else {
                result = true;
            }
            return result;
        }

        $scope.isAvailabilitySetDisabled = function(asName, instanceGroup) {
            var result = false;
            angular.forEach($scope.cluster.instanceGroups, function(ig) {
                if (ig.group != instanceGroup.group && ig.parameters.availabilitySet != null && ig.parameters.availabilitySet.name === asName) {
                    result = true;
                }
            });
            return result;
        }

        $scope.getUserDefinedTags = function() {
            var userDefined, defaultTags, tags;
            if ($rootScope.activeCluster.tags && $rootScope.activeCluster.tags.userDefined) {
                userDefined = $rootScope.activeCluster.tags.userDefined;
            } else {
                userDefined = {}
            }
            if ($rootScope.activeCluster.tags && $rootScope.activeCluster.tags.defaultTags) {
                defaultTags = $rootScope.activeCluster.tags.defaultTags
            } else {
                defaultTags = {}
            }
            tags = Object.assign({}, userDefined, defaultTags)
            return (tags) ? tags : [];
        }

        $scope.escapeRegex = function() {
            if ($rootScope.activeCredential != null) {
                var tmpRegex = $rootScope.params.imagesRegex[$rootScope.activeCredential.cloudPlatform];
                if (typeof tmpRegex != 'undefined' && tmpRegex != null) {
                    $scope.actualRegex = new RegExp(tmpRegex);
                } else {
                    $scope.actualRegex = new RegExp("");
                }
            }

        }

        $scope.isUserDefinedTagsInvalid = function() {
            var hasError = false;
            angular.forEach($scope.cluster.userDefinedTags, function (userDefinedTag) {
                if (!userDefinedTag["key"] || userDefinedTag["key"].length < 3) {
                    hasError = true;
                }
                if (!userDefinedTag["value"] || userDefinedTag["value"].length < 3) {
                    hasError = true;
                }
            });
            return hasError;
        }

        $scope.printAdlsAclWarning = function() {
            if ($rootScope.activeCredential && $rootScope.activeCredential.cloudPlatform == 'AZURE') {
                var appId = $rootScope.activeCredential.parameters.accessKey;
                var displayName = $rootScope.activeCredential.parameters.spDisplayName;
                var spId = displayName != undefined ? "[ Name: " + displayName + ", AppId: " + appId + " ]" : "[ AppId: " + appId + " ]";
                var message = "Service principal " + spId + " will be configured to access ADLS, but ACL-s should be set manually!";
                return message;
            }
            return "";
        }

    }
]);