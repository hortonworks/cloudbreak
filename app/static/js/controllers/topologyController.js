'use strict';

var log = log4javascript.getLogger("topologyController-logger");

angular.module('uluwatuControllers').controller('topologyController', ['$scope', '$rootScope', '$filter', 'AccountTopology', 'File', '$base64',
    function($scope, $rootScope, $filter, AccountTopology, File, $base64) {

        $rootScope.topologies = AccountTopology.query();
        $scope.openstackTopologyForm = {};
        initializeOpenstackTemp();
        $scope.showAlert = false;
        $scope.alertMessage = "";
        $scope.openstackTopology = true;
        $scope.tmpMapping = {}
        $scope.modify = false
        $scope.modifyTopology = {}

        $scope.createOpenstackTopologyRequest = function() {
            $scope.openstackTopology = true;
        }

        $scope.createOpenstackTopology = function() {
            $scope.openstackTemp.cloudPlatform = 'OPENSTACK';
            AccountTopology.save($scope.openstackTemp, function(result) {
                handleOpenstackTopologySuccess(result)
            }, function(error) {
                $scope.showError(error, $rootScope.msg.openstack_topology_failed);
                $scope.showErrorMessageAlert();
            });

            function handleOpenstackTopologySuccess(result) {
                $scope.openstackTemp.id = result.id;
                if (!$scope.modify) {
                    $rootScope.topologies.push($scope.openstackTemp);
                }
                initializeOpenstackTemp();
                $scope.showSuccess($filter("format")($scope.modify ? $rootScope.msg.openstack_topology_modify_success : $rootScope.msg.openstack_topology_success, String(result.id)));
                $scope.modify = false
                $scope.openstackTopologyForm.$setPristine();
                collapseCreateTopologyFormPanel();
                $scope.unShowErrorMessageAlert()
            }
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
            $scope.openstackTemp.nodes[$scope.tmpMapping.hypervisor] = $scope.tmpMapping.rack
            $scope.tmpMapping = {}
            form.$setPristine();
        }

        $scope.deleteMapping = function(hypervisor) {
            delete $scope.openstackTemp.nodes[hypervisor]
        }

        $scope.deleteMappingFrom = function(topology, hypervisor) {
            delete topology.nodes[hypervisor]
        }

        $scope.modifyTopology = function(topology) {
            $scope.modify = true
            $scope.tmpModifyTopology = angular.copy(topology)
            $scope.openstackTemp = topology
        }

        $scope.cancelModify = function() {
            $scope.modify = false;
            if ($scope.tmpModifyTopology && $scope.openstackTemp.id) {
                var topology = $filter('filter')($rootScope.topologies, {
                        id: $scope.openstackTemp.id
                    })[0],
                    indx = $rootScope.topologies.indexOf(topology);
                if (indx > -1) {
                    $rootScope.topologies[indx] = $scope.tmpModifyTopology
                }
                $scope.tmpModifyTopology = undefined
            }
            initializeOpenstackTemp();
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
                        $scope.openstackTemp.nodes = tmpMap
                    });
                }
            });
            document.getElementById('mappingFile').value = '';
            $scope.$apply();
        }

        function collapseCreateTopologyFormPanel() {
            angular.element(document.querySelector('#panel-create-topologies-collapse-btn')).click();
        }


        function initializeOpenstackTemp() {
            $scope.openstackTemp = {
                nodes: {}
            }
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