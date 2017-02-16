angular.module('cloudbreakApp').component('azureCredentialFirst', {
    templateUrl: 'js/components/azurecredential/azurecredentialfirst.html',
    controller: AzureCredentialFirstController,
    bindings: {
        onNextClicked: '&'
    }
});

AzureCredentialFirstController.$inject = ['$rootScope'];

function AzureCredentialFirstController($rootScope) {
    var ctrl = this;
    ctrl.msg = $rootScope.msg;

    ctrl.next = function () {
        ctrl.onNextClicked();
    }
}