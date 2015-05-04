'use strict';

var log = log4javascript.getLogger("networkController-logger");

angular.module('uluwatuControllers').controller('networkController', ['$scope', '$rootScope', '$filter', 'UserNetwork', 'AccountNetwork', 'GlobalNetwork',
    function($scope, $rootScope, $filter, UserNetwork, AccountNetwork, GlobalNetwork) {

        $rootScope.networks = AccountNetwork.query();
        $scope.network = {};
        $scope.awsNetwork = true;

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
            $scope.network.cloudPlatform = 'GCC';
            doCreateNetwork();
        }

        $scope.createAzureNetwork = function() {
            $scope.network.cloudPlatform = "AZURE";
            doCreateNetwork();
        }

        function doCreateNetwork() {
            var isPublicInAccount = $scope.network.publicInAccount;
            console.log($scope.network)
            if (isPublicInAccount) {
                AccountNetwork.save($scope.network, function(result) {
                    handleNetworkCreationSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.error_msg.network_creation_failure + $scope.network.name)
                });
            } else {
                UserNetwork.save($scope.network, function(result) {
                    handleNetworkCreationSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.error_msg.network_creation_failure + $scope.network.name)
                });
            }
        }

        function handleNetworkCreationSuccess(result) {
            $scope.network.id = result.id;
            $rootScope.networks.push($scope.network);
            $scope.showSuccess($rootScope.error_msg.network_creation_success + $scope.network.name);
            collapseCreateNetworkFormPanel();
            initializeFormsAndScopeNetwork()
        }


        $scope.deleteNetwork = function(network) {
            GlobalNetwork.delete({
                id: network.id
            }, function(success) {
                $rootScope.networks.splice($rootScope.networks.indexOf(network), 1);
                $scope.showSuccess($rootScope.error_msg.network_deletion_success + network.name);
            }, function(error) {
                $scope.showError(error, $rootScope.error_msg.network_deletion_failure + network.name)
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
            $scope.network = {};
        }
    }
]);
