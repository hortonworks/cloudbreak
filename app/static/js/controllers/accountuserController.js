'use strict';

var log = log4javascript.getLogger("accountuserController-logger");

angular.module('uluwatuControllers').controller('accountuserController', ['$scope', '$rootScope', '$filter', 'UserInvite', 'AccountUsers', 'ActivateAccountUsers', 'UserPermission',
    function ($scope, $rootScope, $filter, UserInvite, AccountUsers, ActivateAccountUsers, UserPermission) {

        initInvite();
        $rootScope.accountUsers = [];
        getUsersForAccount();

        $scope.inviteUser = function() {
            UserInvite.save({ invite_email: $scope.invite.mail }, function (result) {
                $scope.accountUsers.push({active: false, username: $scope.invite.mail, idx:  $scope.invite.mail.toString().replace(/\./g, '').replace(/@/g, '')});
                $scope.inviteForm.$setPristine();
                initInvite();
                collapseInviteUsersFormPanel();
            }, function (error) {
                $scope.modifyStatusMessage(error.data.message);
                $scope.modifyStatusClass("has-error");
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
                $scope.modifyStatusMessage(error.data.message);
                $scope.modifyStatusClass("has-error");
            })
        }
        
        $scope.makeAdmin = function(userId, index) {
            UserPermission.save({id: userId, role: 'admin'}, function(result) {
                $scope.getUsers();
            }, function (error) {
                $scope.modifyStatusMessage(error.data.message);
                $scope.modifyStatusClass("has-error");
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
