'use strict';

var log = log4javascript.getLogger("accountuserController-logger");

angular.module('uluwatuControllers').controller('accountuserController', ['$scope', '$rootScope', '$filter', 'UserInvite', 'AccountUsers',
    function ($scope, $rootScope, $filter, UserInvite, AccountUsers) {

        initInvite();

        $scope.inviteUser = function() {
            UserInvite.save({ invite_email: $scope.invite.mail }, function (result) {
                initInvite();

            }, function (error) {
                $scope.modifyStatusMessage(error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.getUsers = function() {
            $rootScope.accountUsers =  AccountUsers.query(function (result) {
                angular.forEach(result, function(item) {
                    item.idx = item.username.toString().replace('.', '').replace('@', '');
                });
            });
        }

        $scope.getUsers();

        function initInvite() {
            $scope.invite = {
                mail: ""
            };
        }


    }
]);
