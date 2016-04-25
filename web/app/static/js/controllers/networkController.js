'use strict';

var log = log4javascript.getLogger("networkController-logger");

angular.module('uluwatuControllers').controller('networkController', ['$scope', '$rootScope', '$filter', 'UserNetwork', 'AccountNetwork', 'GlobalNetwork',
    function($scope, $rootScope, $filter, UserNetwork, AccountNetwork, GlobalNetwork) {

        $rootScope.networks = AccountNetwork.query();
        $scope.network = {
            parameters: {}
        };
        $scope.showAlert = false;
        $scope.alertMessage = "";
        var firstVisiblePlatform = $scope.firstVisible(["AWS", "AZURE_RM", "GCP", "OPENSTACK"]);
        if (firstVisiblePlatform != -1) {
            $scope[["awsNetwork", "azureNetwork", "gcpNetwork", "openstackNetwork"][firstVisiblePlatform]] = true;
        }

        $scope.createAwsNetworkRequest = function() {
            initializeFormsAndScopeNetwork()
            $scope.azureNetwork = false;
            $scope.awsNetwork = true;
            $scope.gcpNetwork = false;
            $scope.openstackNetwork = false;
        }

        $scope.createAzureNetworkRequest = function() {
            initializeFormsAndScopeNetwork()
            $scope.azureNetwork = true;
            $scope.awsNetwork = false;
            $scope.gcpNetwork = false;
            $scope.openstackNetwork = false;
        }

        $scope.createGcpNetworkRequest = function() {
            initializeFormsAndScopeNetwork()
            $scope.azureNetwork = false;
            $scope.awsNetwork = false;
            $scope.gcpNetwork = true;
            $scope.openstackNetwork = false;
        }

        $scope.createOpenstackNetworkRequest = function() {
            initializeFormsAndScopeNetwork()
            $scope.azureNetwork = false;
            $scope.awsNetwork = false;
            $scope.gcpNetwork = false;
            $scope.openstackNetwork = true;
        }

        $scope.createAwsNetwork = function() {
            $scope.network.cloudPlatform = 'AWS';
            doCreateNetwork();
        }

        $scope.createOpenStackNetwork = function() {
            $scope.network.cloudPlatform = 'OPENSTACK';
            doCreateNetwork();
        }

        $scope.createGcpNetwork = function() {
            $scope.network.cloudPlatform = 'GCP';
            doCreateNetwork();
        }

        $scope.createAzureNetwork = function() {
            $scope.network.cloudPlatform = "AZURE_RM";
            doCreateNetwork();
        }

        function doCreateNetwork() {
            var save = $scope.network.publicInAccount ? AccountNetwork.save : UserNetwork.save;
            save($scope.network, function(result) {
                handleNetworkCreationSuccess(result)
            }, function(error) {
                $scope.showError(error, $rootScope.msg.network_creation_failure + $scope.network.name);
                $scope.showErrorMessageAlert();
            });
        }

        function handleNetworkCreationSuccess(result) {
            $scope.network.id = result.id;
            $rootScope.networks.push($scope.network);
            $scope.showSuccess($rootScope.msg.network_creation_success + $scope.network.name);
            collapseCreateNetworkFormPanel();
            initializeFormsAndScopeNetwork();
            $scope.unShowErrorMessageAlert();
        }


        $scope.deleteNetwork = function(network) {
            GlobalNetwork.delete({
                id: network.id
            }, function(success) {
                $rootScope.networks.splice($rootScope.networks.indexOf(network), 1);
                $scope.showSuccess($rootScope.msg.network_deletion_success + network.name);
            }, function(error) {
                $scope.showError(error, $rootScope.msg.network_deletion_failure + network.name)
            });
        }

        function collapseCreateNetworkFormPanel() {
            angular.element(document.querySelector('#panel-create-network-collapse-btn')).click();
        }

        function initializeFormsAndScopeNetwork() {
            $scope.awsNetworkForm.$setPristine();
            $scope.gcpNetworkForm.$setPristine();
            $scope.azureNetworkForm.$setPristine();
            $scope.openstackNetworkForm.$setPristine();
            $scope.network = {
                parameters: {}
            };
        }

        $scope.filterByCloudPlatform = function(topology) {
            return (topology.cloudPlatform === 'AWS' && $scope.awsNetwork) ||
                (topology.cloudPlatform === 'GCP' && $scope.gcpNetwork) ||
                (topology.cloudPlatform === 'AZURE_RM' && $scope.azureNetwork) ||
                (topology.cloudPlatform === 'OPENSTACK' && $scope.openstackNetwork)
        }

        $scope.getTopologyNameById = function(topologyId) {
            var result;
            angular.forEach($rootScope.topologies, function(topology) {
                if (topology.id === topologyId) {
                    result = topology.name;
                }
            });
            return result;
        }

        $scope.unShowErrorMessageAlert = function() {
            $scope.showAlert = false;
            $scope.alertMessage = "";
        }

        $scope.showErrorMessageAlert = function() {
            $scope.showAlert = true;
            $scope.alertMessage = $scope.statusMessage;
        }

        $scope.selectGcpNetworkType1 = function() {
            delete $scope.network.parameters.networkId;
            delete $scope.network.parameters.subnetId;
        }

        $scope.selectGcpNetworkType2 = function() {
            delete $scope.network.parameters.subnetId;
        }

        $scope.selectGcpNetworkType3 = function() {
            delete $scope.network.subnetCIDR;
        }

        $scope.selectGcpNetworkType4 = function() {
            delete $scope.network.subnetCIDR;
            delete $scope.network.parameters.subnetId;
        }

        $scope.selectAwsNetworkType1 = function() {
            delete $scope.network.parameters.vpcId
            delete $scope.network.parameters.internetGatewayId
            delete $scope.network.parameters.subnetId
        }

        $scope.selectAwsNetworkType2 = function() {
            delete $scope.network.parameters.subnetId
        }

        $scope.selectAwsNetworkType3 = function() {
            delete $scope.network.subnetCIDR
            delete $scope.network.parameters.internetGatewayId
        }

        $scope.selectAzureNetworkType1 = function() {
            delete $scope.network.parameters.resourceGroupName
            delete $scope.network.parameters.networkId
            delete $scope.network.parameters.subnetId
        }

        $scope.selectAzureNetworkType2 = function() {
            delete $scope.network.subnetCIDR
        }

        $scope.selectOpenstackNetworkType1 = function() {
            delete $scope.network.parameters.networkId
            delete $scope.network.parameters.subnetId
            delete $scope.network.parameters.routerId
        }

        $scope.selectOpenstackNetworkType2 = function() {
            delete $scope.network.parameters.subnetId
        }

        $scope.selectOpenstackNetworkType3 = function() {
            delete $scope.network.subnetCIDR
            delete $scope.network.parameters.routerId
        }
    }
]);