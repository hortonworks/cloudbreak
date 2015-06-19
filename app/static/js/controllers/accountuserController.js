'use strict';

var log = log4javascript.getLogger("accountuserController-logger");

angular.module('uluwatuControllers').controller('accountuserController', ['$scope', '$rootScope', '$filter', 'UserInvite', 'AccountUsers', 'ActivateAccountUsers', 'UserPermission', 'AccountDetails', 'UserOperation',
    function ($scope, $rootScope, $filter, UserInvite, AccountUsers, ActivateAccountUsers, UserPermission, AccountDetails, UserOperation) {

        initInvite();
        $rootScope.accountUsers = [];
        $scope.userDetails = AccountDetails.get()
        getUsersForAccount();

        $scope.inviteUser = function() {
            UserInvite.save({ invite_email: $scope.invite.mail }, function (result) {
                var newUser = $filter('filter')($scope.accountUsers, { username: $scope.invite.mail })[0];
                if (newUser == null) {
                    $scope.accountUsers.push({active: false, username: $scope.invite.mail, idx:  $scope.invite.mail.toString().replace(/\./g, '').replace(/@/g, '')});
                }
                $scope.inviteForm.$setPristine();
                initInvite();
                collapseInviteUsersFormPanel();
            }, function (error) {
                $scope.showError(error);
            });
        }

        $scope.getUsers = function() {
            $rootScope.accountUsers =  AccountUsers.query(function (result) {
                angular.forEach(result, function(item) {
                    item.idx = item.username.toString().replace(/\./g, '').replace(/@/g, '');
                });
            });
        }

        $scope.activateUser = function(activate, email) {
            ActivateAccountUsers.save({ activate: activate, email: email },  function (result) {
                $filter('filter')($scope.accountUsers, { username: email })[0].active = activate;
            }, function (error) {
                $scope.showError(error);
            })
        }

        $scope.makeAdmin = function(userId, userName, index) {
            UserPermission.save({id: userId, role: 'admin'}, function(result) {
                UserOperation.update({userId: userId}, {username: userName},
                 function(cacheUpdateResult){
                   $scope.showSuccess($filter("format")($rootScope.msg.user_form_admin_success, userName))
                 }, function(cacheUpdateError) {
                   $scope.showError(cacheUpdateError);
                });
                $scope.getUsers();
            }, function (error) {
                $scope.showError(error);
            })
        }

        function getUsersForAccount() {
            UserPermission.get(function(success){
               $scope.user.admin = success.admin;
               if ($scope.user.admin != undefined && $scope.user.admin) {
                 $scope.getUsers();
               }
            });
        }

        function initInvite() {
            $scope.invite = {
                mail: ""
            };
        }

        function collapseInviteUsersFormPanel() {
            angular.element(document.querySelector('#inviteCollapse')).click();
        }
    }
]);
