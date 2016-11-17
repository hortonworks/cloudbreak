'use strict';

angular.module('uluwatuControllers').controller('securityGroupController', ['$scope', '$rootScope', '$filter', 'UserSecurityGroup', 'AccountSecurityGroup', 'GlobalSecurityGroup',
    function($scope, $rootScope, $filter, UserSecurityGroup, AccountSecurityGroup, GlobalSecurityGroup) {

        $rootScope.securitygroups = AccountSecurityGroup.query();
        $scope.showAlert = false;
        $scope.alertMessage = "";
        initializeFormsAndScopeSecurityGroup();

        $scope.createSecurityGroup = function() {
            doCreateSecurityGroup();
        }

        $scope.addGroupProtocol = function() {
            if ($scope.tmpsecport.cidr !== null && $scope.tmpsecport.cidr !== undefined && $scope.tmpsecport.port !== null && $scope.tmpsecport.port !== undefined && $scope.tmpsecport.protocol !== null && $scope.tmpsecport.protocol !== undefined) {
                if ($scope.tmpsecport.port.match(/^[0-9]+(,[0-9]+)*$/g) === null) {
                    $scope.showErrorMessage($rootScope.msg.incorrect_port_definition_sec_group);
                    return;
                }
                angular.forEach($scope.tmpsecport.port.split(","), function(port_test) {
                    var tmp = {
                        "cidr": $scope.tmpsecport.cidr,
                        "protocol": $scope.tmpsecport.protocol,
                        "port": port_test
                    };
                    $scope.securitygroup.tmpsecurityRules.push(tmp);
                });
                $scope.tmpsecport = {
                    "cidr": "0.0.0.0/0",
                    "protocol": "tcp"
                };
            } else if ($scope.tmpsecport.cidr === null || $scope.tmpsecport.cidr === undefined) {
                $scope.tmpsecport.cidr = null;
                $scope.showErrorMessage($rootScope.msg.cidr_definition_incorrect);
            } else if ($scope.tmpsecport.port === null || $scope.tmpsecport.port === undefined) {
                $scope.tmpsecport.port = null;
                $scope.showErrorMessage($rootScope.msg.port_definition_incorrect);
            } else {
                $scope.tmpsecport.protocol = null;
                $scope.showErrorMessage($rootScope.msg.protocol_definition_incorrect);
            }
        }

        $scope.deleteGroupProtocol = function(sec) {
            var index = $scope.securitygroup.tmpsecurityRules.indexOf(sec);
            if (index > -1) {
                $scope.securitygroup.tmpsecurityRules.splice(index, 1);
            }
        }

        function doCreateSecurityGroup() {
            var newRules = [];
            angular.forEach($scope.securitygroup.tmpsecurityRules, function(rule) {
                var newRule = null;
                angular.forEach(newRules, function(test) {
                    if (test.protocol == rule.protocol) {
                        newRule = test;
                        return;
                    }
                });
                if (newRule == null || newRule == undefined) {
                    newRules.push({
                        "subnet": rule.cidr,
                        "protocol": rule.protocol,
                        "ports": rule.port
                    });
                } else {
                    newRules[newRules.indexOf(newRule)].ports += "," + rule.port;
                }
            });

            $scope.securitygroup.securityRules = newRules;
            if ($scope.securitygroup.public) {
                AccountSecurityGroup.save($scope.securitygroup, function(result) {
                    handleSecurityGroupCreationSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.securitygroup_creation_failure + $scope.securitygroup.name);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserSecurityGroup.save($scope.securitygroup, function(result) {
                    handleSecurityGroupCreationSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.securitygroup_creation_failure + $scope.securitygroup.name);
                    $scope.showErrorMessageAlert();
                });
            }
        }

        $rootScope.changeRule = function(rules) {
            angular.forEach(rules, function(rule) {
                rule.portarray = remove_duplicates(rule.ports.toString().split(","));
            });
            return rules;
        }

        function remove_duplicates(arr) {
            var obj = {};
            var ret_arr = [];
            for (var i = 0; i < arr.length; i++) { obj[arr[i]] = true; }
            for (var key in obj) { ret_arr.push(key); }
            return ret_arr;
        }

        $scope.deleteSecurityGroup = function(securityGroup) {
            GlobalSecurityGroup.delete({
                id: securityGroup.id
            }, function(success) {
                $rootScope.securitygroups.splice($rootScope.securitygroups.indexOf(securityGroup), 1);
                $scope.showSuccess($filter("format")($rootScope.msg.securitygroup_delete_success, String(securityGroup.id)));
            }, function(error) {
                $scope.showError(error, $rootScope.msg.securitygroup_delete_failed)
            });
        }

        function handleSecurityGroupCreationSuccess(result) {
            $scope.securitygroup.id = result.id;
            $scope.securitygroup.publicInAccount = $scope.securitygroup.public;
            $rootScope.securitygroups.push($scope.securitygroup);
            $scope.showSuccess($filter("format")($rootScope.msg.securitygroup_creation_success, String($scope.securitygroup.name)));
            initializeFormsAndScopeSecurityGroup();
            collapseCreateSecurityGroupFormPanel();
        }

        function collapseCreateSecurityGroupFormPanel() {
            angular.element(document.querySelector('#panel-create-securitygroup-collapse-btn')).click();
        }

        function initializeFormsAndScopeSecurityGroup() {
            $scope.securitygroup = {
                name: "",
                description: "",
                tmpsecurityRules: []
            };
            $scope.tmpsecport = {
                cidr: "0.0.0.0/0",
                protocol: "tcp"
            };
            $scope.tmpsecportForm = {};
            $scope.securitygroupForm = {};
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