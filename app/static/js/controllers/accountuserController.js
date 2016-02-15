'use strict';

var log = log4javascript.getLogger("accountuserController-logger");

angular.module('uluwatuControllers').controller('accountuserController', ['$scope', '$rootScope', '$filter', 'UserInvite', 'AccountUsers', 'ActivateAccountUsers', 'UserPermission', 'AccountDetails', 'UserOperation', 'AccountPreferences',
    function($scope, $rootScope, $filter, UserInvite, AccountUsers, ActivateAccountUsers, UserPermission, AccountDetails, UserOperation, AccountPreferences) {

        initInvite();
        $rootScope.accountUsers = [];
        $scope.userDetails = AccountDetails.get()
        getUsersForAccount();

        $scope.inviteUser = function() {
            UserInvite.save({
                invite_email: $scope.invite.mail,
                groups: $scope.invite.scopes
            }, function(result) {
                var newUser = $filter('filter')($scope.accountUsers, {
                    username: $scope.invite.mail
                })[0];
                if (newUser == null) {
                    $scope.accountUsers.push({
                        active: false,
                        groups: $scope.invite.scopes,
                        username: $scope.invite.mail,
                        idx: $scope.invite.mail.toString().replace(/\./g, '').replace(/@/g, '')
                    });
                }
                $scope.inviteForm.$setPristine();
                $scope.showSuccess($filter("format")($rootScope.msg.user_form_invite_success, $scope.invite.mail))
                initInvite();
                collapseInviteUsersFormPanel();
                $scope.getUsers();
            }, function(error) {
                $scope.showError(error);
            });
        }

        $scope.contains = function(str, suffix) {
            if (str != undefined && str.indexOf(suffix) !== -1) {
                return true;
            } else {
                return false;
            }
        }

        $scope.endsWith = function(str, suffix) {
            if (str != undefined && str.indexOf(suffix, str.length - suffix.length) !== -1) {
                return true;
            } else {
                return false;
            }
        }

        $scope.searchScopeElement = function(element, groups) {
            var result = null;
            if (groups != undefined && groups != null) {
                angular.forEach(groups, function(item) {
                    if ($scope.endsWith(item.display, element)) {
                        result = item;
                        return result;
                    }
                });
            }
            return result;
        }

        $scope.isWriteScope = function(name, groups) {
            if ($scope.searchScopeElement(name, groups) != null) {
                return true;
            } else {
                return false;
            }
        }


        $scope.getUsers = function() {
            $rootScope.accountUsers = AccountUsers.query(function(result) {
                angular.forEach(result, function(item) {
                    item.idx = item.username.toString().replace(/\./g, '-').replace(/@/g, '-');
                });
            });
        }

        $scope.activateUser = function(activate, email) {
            ActivateAccountUsers.save({
                activate: activate,
                email: email
            }, function(result) {
                $filter('filter')($scope.accountUsers, {
                    username: email
                })[0].active = activate;
                $scope.getUsers();
            }, function(error) {
                $scope.showError(error);
            })
        }

        $scope.makeAdmin = function(userId, userName, index) {
            UserPermission.save({
                id: userId,
                role: 'admin'
            }, function(result) {
                UserOperation.update({
                        userId: userId
                    }, {
                        username: userName
                    },
                    function(cacheUpdateResult) {
                        $scope.showSuccess($filter("format")($rootScope.msg.user_form_admin_success, userName))
                    },
                    function(cacheUpdateError) {
                        $scope.showError(cacheUpdateError);
                    });
                $scope.getUsers();
            }, function(error) {
                $scope.showError(error);
            })
        }

        $scope.removeUser = function(userName, userId) {
            UserOperation.delete({
                    userId: userId
                }, null,
                function(deleteResult) {
                    $scope.showSuccess($filter("format")($rootScope.msg.user_form_delete_success, userName))
                    $scope.getUsers();
                },
                function(deleteError) {
                    $scope.showError(deleteError)
                }
            );
        }

        $scope.changePlatformVisibility = function(platform) {
            AccountPreferences.get().$promise.then(function(preferences) {
                var platf = []
                if (preferences.platforms) {
                    platf = preferences.platforms.split(',')
                } else {
                    generateAllPlatform(platf);
                }
                if (platf.indexOf(platform) > -1) {
                    if (platf.length > 1) {
                        platf.splice(platf.indexOf(platform), 1)
                    }
                } else {
                    platf.push(platform)
                }
                $rootScope.params.platforms = platf

                preferences.platforms = platf.join()
                AccountPreferences.save(preferences)
            })
        }

        $scope.isChecked = function(platform) {
            return $rootScope.params.platforms.indexOf(platform) > -1
        }

        function generateAllPlatform(platforms) {
            angular.forEach($rootScope.params.platformVariants, function(value, key) {
                this.push(key)
            }, platforms)
        }

        function getUsersForAccount() {
            UserPermission.get(function(success) {
                $scope.user.admin = success.admin;
                if ($scope.user.admin != undefined && $scope.user.admin) {
                    $scope.getUsers();
                }
            });
        }

        function initInvite() {
            $scope.invite = {
                mail: "",
                scopes: {
                    blueprints: {
                        write: false,
                        read: true
                    },
                    recipes: {
                        write: false,
                        read: true
                    },
                    credentials: {
                        write: false,
                        read: true
                    },
                    templates: {
                        write: false,
                        read: true
                    },
                    stacks: {
                        write: false,
                        read: true
                    },
                    securitygroups: {
                        write: false,
                        read: true
                    },
                    networks: {
                        write: false,
                        read: true
                    }
                }
            };
        }

        function collapseInviteUsersFormPanel() {
            angular.element(document.querySelector('#inviteCollapse')).click();
        }
    }
]);