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

    ctrl.azureRole = [{
        key: 'CONTRIBUTOR',
        value: 'Use existing "Contributor" role'
    }, {
        key: 'REUSE_EXISTING',
        value: 'Reuse existing custom role'
    }, {
        key: 'CREATE_CUSTOM',
        value: 'Let Cloudbreak create a custom role'
    }];

    this.refreshRole =  function() {
        if (ctrl.credentialAzure.parameters.roleType == 'CONTRIBUTOR') {
            ctrl.credentialAzure.parameters.roleName = 'Contributor'
        } else {
            ctrl.credentialAzure.parameters.roleName = ''
        }
    }

    this.test = function () {
        console.log('roleName', ctrl.credentialAzure.parameters.roleName);
    }

}