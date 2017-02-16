angular.module('cloudbreakApp').component('azureCredentialSecond', {
    templateUrl: 'js/components/azurecredential/azurecredentialsecond.html',
    controller: AzureCredentialSecondController,
    bindings: {
        onCredentialFilled: '&',
        credentialAzure: '=',
        azureCredentialForm: '='
    }
});

AzureCredentialSecondController.$inject = ['$rootScope'];

function AzureCredentialSecondController($rootScope) {
    var ctrl = this;
    ctrl.msg = $rootScope.msg;

    this.next = function () {
        ctrl.onCredentialFilled();
    };

    ctrl.selectors = [{
        name: 'App based',
        id: 'app-based'
    }, {
        name: 'Interactive',
        id: 'interactive'
    }];
}