'use strict';

var log = log4javascript.getLogger("accountuserController-logger");

angular.module('uluwatuControllers').controller('accountuserController', ['$scope', '$rootScope', '$filter', 'UserInvite',
    function ($scope, $rootScope, $filter, UserInvite) {

        initInvite();

        $scope.inviteUser = function() {
            UserInvite.save({ invite_email: $scope.invite.mail }, function (result) {
                initInvite();
            }, function (error) {
                $scope.modifyStatusMessage("Can not invite this user...");
                $scope.modifyStatusClass("has-error");
            });
        }

        function initInvite() {
            $scope.invite = {
                mail: ""
            };
        }


    }
]);
