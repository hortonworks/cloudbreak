angular.module('cloudbreakApp').component('azureCredentialThird', {
    templateUrl: 'js/components/azurecredential/azurecredentialthird.html',
    controller: AzureCredentialThirdController,
    bindings: {
        interactiveLoginResult: '<',
        credentialAzure: '<',
        errorMessage: '=',
        credentialInCreation: '=',
        credentialSuccess: '&'
    }
});

AzureCredentialThirdController.$inject = ['$rootScope', '$filter'];

function AzureCredentialThirdController($rootScope, $filter) {
    var ctrl = this;
    ctrl.msg = $rootScope.msg;
    ctrl.credentialInCreation = false;
    ctrl.credentialCreated = false;

    $rootScope.$on('interactiveCredentialCreationInProgress', function (event, message) {
        ctrl.inprogressMessage = message;
        ctrl.credentialInCreation = true;
        if (message.toLowerCase().indexOf('application created') != -1) {
            ctrl.inprogressPercent = 33;
        } else if (message.toLowerCase().indexOf('principal created') != -1) {
            ctrl.inprogressPercent = 66;
        } else if (message.toLowerCase().indexOf('role assigned') != -1) {
            ctrl.inprogressPercent = 100;
        }
    });

    $rootScope.$on('credentialCreateFailed', function (event, message) {
        ctrl.interactiveLoginResult = null;
        ctrl.credentialInCreation = false;
        ctrl.errorMessage = message;
        $rootScope.$broadcast('showErrorMessage', message);
    });

    $rootScope.$on('credentialCreated', function (event, credentials) {
        ctrl.interactiveLoginResult = null;
        ctrl.credentialInCreation = false;
        var createdCredential = credentials.filter(function (credential) {
            return credential.name == ctrl.credentialAzure.name;
        })[0];
        if (credentials.length == 1) {
            $rootScope.activeCredential = createdCredential;
        }
        $rootScope.credentials.push(createdCredential);
        ctrl.credentialAzure.id = createdCredential.id;
        $rootScope.$broadcast('showSuccessMessage', $filter("format")($rootScope.msg.azure_credential_success, createdCredential.id));
    });

}