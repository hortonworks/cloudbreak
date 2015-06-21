'use strict';

angular.module('uluwatuControllers').controller('securityGroupController', ['$scope', '$rootScope', '$filter', 'UserSecurityGroup',
    'AccountSecurityGroup', 'GlobalSecurityGroup',function($scope, $rootScope, $filter, UserSecurityGroup, AccountSecurityGroup, GlobalSecurityGroup) {

        $rootScope.securitygroups = AccountSecurityGroup.query();
        $scope.securitygroup = {};

        $scope.createSecurityGroup = function() {
            doCreateSecurityGroup();
        }

        function doCreateSecurityGroup() {
            var isPublicInAccount = $scope.securitygroup.publicInAccount;
            console.log($scope.securitygroup)
            if (isPublicInAccount) {
                console.log('create account group')
            } else {
                console.log('create private group')
            }
        }

        function handleSecurityGroupCreationSuccess(result) {
            $scope.securitygroup.id = result.id;
            $rootScope.securitygroups.push($scope.securitygroup);
            $scope.showSuccess($rootScope.msg.securitygroup_creation_success + $scope.securitygroup.name);
            collapseCreateSecurityGroupFormPanel();
            initializeFormsAndScopeSecurityGroup()
        }

        function collapseCreateSecurityGroupFormPanel() {
            angular.element(document.querySelector('#panel-create-securitygroup-collapse-btn')).click();
        }

        function initializeFormsAndScopeSecurityGroup() {
            $scope.securitygroup = {};
        }
    }
]);
