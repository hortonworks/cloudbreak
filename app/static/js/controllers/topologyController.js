'use strict';

var log = log4javascript.getLogger("topologyController-logger");

angular.module('uluwatuControllers').controller('topologyController', ['$scope', '$rootScope', '$filter', 'AccountTopology', 'File', '$base64',
    function($scope, $rootScope, $filter, AccountTopology, File, $base64) {

        $rootScope.topologies = AccountTopology.query();
        $scope.openstackTopologyForm = {};
        $scope.awsTopologyForm = {};
        $scope.azureTopologyForm = {};
        $scope.gcpTopologyForm = {};
        initializeTemps();
        $scope.showAlert = false;
        $scope.alertMessage = "";
        $scope.tmpMapping = {}
        $scope.modify = false
        $scope.modifyTopology = {}
        var firstVisiblePlatform = $scope.firstVisible(["AWS", "AZURE_RM", "GCP", "OPENSTACK"]);
        if (firstVisiblePlatform != -1) {
            $scope[["awsTopology", "azureTopology", "gcpTopology", "openstackTopology"][firstVisiblePlatform]] = true;
        }

        function createTopologyRequest(type) {
            if (type === 'OPENSTACK') {
                $scope.createOpenstackTopologyRequest()
            }
            if (type === 'AWS') {
                $scope.createAwsTopologyRequest()
            }
            if (type === 'GCP') {
                $scope.createGcpTopologyRequest()
            }
            if (type === 'AZURE_RM') {
                $scope.createAzureTopologyRequest()
            }
        }

        $scope.createOpenstackTopologyRequest = function() {
            $scope.openstackTopology = true;
            $scope.azureTopology = false;
            $scope.gcpTopology = false;
            $scope.awsTopology = false;
        }

        $scope.createAwsTopologyRequest = function() {
            $scope.openstackTopology = false;
            $scope.azureTopology = false;
            $scope.gcpTopology = false;
            $scope.awsTopology = true;
        }

        $scope.createGcpTopologyRequest = function() {
            $scope.openstackTopology = false;
            $scope.azureTopology = false;
            $scope.gcpTopology = true;
            $scope.awsTopology = false;
        }

        $scope.createAzureTopologyRequest = function() {
            $scope.openstackTopology = false;
            $scope.azureTopology = true;
            $scope.gcpTopology = false;
            $scope.awsTopology = false;
        }

        $scope.createTopology = function(type) {
            if (!$scope.modify) {
                $scope.topologyTemp.cloudPlatform = type;
            }
            AccountTopology.save($scope.topologyTemp, function(result) {
                handleTopologySuccess(result, $scope.topologyTemp)
            }, function(error) {
                $scope.showError(error, $rootScope.msg.openstack_topology_failed);
                $scope.showErrorMessageAlert();
            });

            function handleTopologySuccess(result, localTopology) {
                localTopology.id = result.id;
                if (!$scope.modify) {
                    $rootScope.topologies.push(localTopology);
                }
                initializeTemps();
                $scope.showSuccess($filter("format")(getCreateMessage(type, result.id)));
                $scope.modify = false
                setAllFormPristine()
                collapseCreateTopologyFormPanel();
                $scope.unShowErrorMessageAlert()
            }
        }

        function getCreateMessage(type, id) {
            if (type === 'OPENSTACK') {
                return $filter("format")($scope.modify ? $rootScope.msg.openstack_topology_modify_success : $rootScope.msg.openstack_topology_success, String(id))
            }
            if (type === 'AWS') {
                return $filter("format")($scope.modify ? $rootScope.msg.aws_topology_modify_success : $rootScope.msg.aws_topology_success, String(id))
            }
            if (type === 'GCP') {
                return $filter("format")($scope.modify ? $rootScope.msg.gcp_topology_modify_success : $rootScope.msg.gcp_topology_success, String(id))
            }
            if (type === 'AZURE_RM') {
                return $filter("format")($scope.modify ? $rootScope.msg.azure_topology_modify_success : $rootScope.msg.azure_topology_success, String(id))
            }
        }

        function setAllFormPristine() {
            $scope.openstackTopologyForm.$setPristine();
            $scope.awsTopologyForm.$setPristine();
            $scope.gcpTopologyForm.$setPristine();
            $scope.azureTopologyForm.$setPristine();
        }

        $scope.deleteTopology = function(topology) {
            AccountTopology.delete({
                id: topology.id
            }, function(success) {
                $rootScope.topologies.splice($rootScope.topologies.indexOf(topology), 1);
                $scope.showSuccess($filter("format")($rootScope.msg.topology_delete_success, String(topology.id)));
            }, function(error) {
                $scope.showError(error, $rootScope.msg.topology_delete_failed)
            });
        }

        $scope.addMapping = function(form) {
            $scope.topologyTemp.nodes[$scope.tmpMapping.hypervisor] = $scope.tmpMapping.rack
            $scope.tmpMapping = {}
            form.$setPristine();
        }

        $scope.deleteMapping = function(hypervisor) {
            delete $scope.topologyTemp.nodes[hypervisor]
        }

        $scope.deleteMappingFrom = function(topology, hypervisor) {
            delete topology.nodes[hypervisor]
        }

        $scope.modifyTopology = function(topology) {
            $scope.modify = true
            createTopologyRequest(topology.cloudPlatform)
            $scope.tmpModifyTopology = angular.copy(topology)
            $scope.topologyTemp = topology
        }

        $scope.cancelModify = function() {
            $scope.modify = false;
            if ($scope.tmpModifyTopology && $scope.topologyTemp.id) {
                var topology = $filter('filter')($rootScope.topologies, {
                        id: $scope.topologyTemp.id
                    })[0],
                    indx = $rootScope.topologies.indexOf(topology);
                if (indx > -1) {
                    $rootScope.topologies[indx] = $scope.tmpModifyTopology
                }
                $scope.tmpModifyTopology = undefined
            }
            initializeTemps();
        }

        $scope.cleanupScope = function() {
            if ($scope.modify) {
                $scope.cancelModify()
            }
        }

        $scope.generateMappingFromFile = function() {
            File.getBase64ContentById("mappingFile", function(content) {
                if (content) {
                    var fileContent = $base64.decode(content);
                    var reg = /^([^\s]+)\s+([^\s]+)$/;
                    var lines = fileContent.split('\n');
                    var tmpMap = {}
                    lines.forEach(function(line) {
                        if (reg.test(line)) {
                            var match = reg.exec(line)
                            tmpMap[match[1]] = match[2]
                        }
                    });
                    $scope.$apply(function() {
                        $scope.topologyTemp.nodes = tmpMap
                    });
                }
            });
            document.getElementById('mappingFile').value = '';
            $scope.$apply();
        }

        function collapseCreateTopologyFormPanel() {
            angular.element(document.querySelector('#panel-create-topologies-collapse-btn')).click();
        }


        function initializeTemps() {
            $scope.topologyTemp = {
                nodes: {}
            }
        }

        $scope.isAnyMappingSet = function() {
            return Object.keys($scope.topologyTemp.nodes).length > 0
        }

        $scope.unShowErrorMessageAlert = function() {
            $scope.showAlert = false;
            $scope.alertMessage = "";
        }

        $scope.showErrorMessageAlert = function() {
            $scope.showAlert = true;
            $scope.alertMessage = $scope.statusMessage;
        }
    }
]);