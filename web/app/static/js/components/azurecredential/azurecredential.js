angular.module('cloudbreakApp').component('azureCredential', {
    templateUrl: 'js/components/azurecredential/azurecredential.html',
    controller: AzureCredentialController
});

AzureCredentialController.$inject = ['$rootScope', '$filter', 'InteractiveLogin', 'AccountCredential', 'UserCredential'];

function AzureCredentialController($rootScope, $filter, InteractiveLogin, AccountCredential, UserCredential) {
    var ctrl = this;
    var defaultSshKey;

    ctrl.$onInit = function () {
        init();
    };

    ctrl.cancel = function () {
        init();
    };

    init = function () {
        ctrl.activePanel = 1;
        ctrl.credentialAzure = {
            parameters: {
                selector: 'interactive',
                roleType: 'CONTRIBUTOR',
                roleName: 'Contributor'
            }
        };
        if (ctrl.azureCredentialForm) {
            ctrl.azureCredentialForm.$setPristine();
        }
        ctrl.credentialInCreation = false;
        ctrl.interactiveLoginResult = null;
        ctrl.errorMessage = null;
    };

    ctrl.showSecond = function () {
        ctrl.activePanel = 2;
    };

    ctrl.showThird = function () {
        ctrl.credentialAzure.cloudPlatform = "AZURE";
        if (ctrl.credentialAzure.parameters.selector == 'interactive') {
            azureInteractiveLogin(ctrl.credentialAzure);
        } else {
            createAzureCredential(ctrl.credentialAzure)
        }
    };

    function showError(error, prefix) {
        $rootScope.$broadcast('showError', {error: error, prefix: prefix});
        ctrl.errorMessage = (error.data.message ? error.data.message : error.data);
        ctrl.activePanel = 3;
    }

    azureInteractiveLogin = function (azureCredential) {
        InteractiveLogin.save(azureCredential, function (success) {
            ctrl.interactiveLoginResult = success;
            ctrl.activePanel = 3;
        }, function (error) {
            showError(error, null)
        });
    };

    createAzureCredential = function (azureCredential) {
        if (ctrl.credentialAzure.public) {
            AccountCredential.save(azureCredential, function (result) {
                ctrl.activePanel = 3;
            }, function (error) {
                showError(error, $filter("format")($rootScope.msg.azure_credential_failed, azureCredential.name))
            });
        } else {
            UserCredential.save(azureCredential, function (result) {
                ctrl.activePanel = 3;
            }, function (error) {
                showError(error, $filter("format")($rootScope.msg.azure_credential_failed, azureCredential.name));
            });
        }
    };

    ctrl.handleAzureCredentialSuccess = function () {
        init();
        $jq('#panel-credentials-collapse').collapse('hide');
    }
}