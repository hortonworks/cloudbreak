<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.cname.$dirty && azureCredentialForm.cname.$invalid }">
    <label class="col-sm-3 control-label" for="cname">Name</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-z][a-z0-9]*$/" name="cname" id="cname" ng-model="credentialAzure.name" ng-minlength="5" ng-maxlength="100" required placeholder="min. 5 max. 100 char">
        <div class="help-block" ng-show="azureCredentialForm.cname.$dirty && azureCredentialForm.cname.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.cdescription.$dirty && azureCredentialForm.cdescription.$invalid }">
    <label class="col-sm-3 control-label" for="cdescription">Description</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialAzure.description" id="cdescription" name="cdescription" ng-maxlength="1000"  placeholder="max. 1000 char">
        <div class="help-block" ng-show="azureCredentialForm.cdescription.$dirty && azureCredentialForm.cdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.csubscriptionId.$dirty && azureCredentialForm.csubscriptionId.$invalid }">
    <label class="col-sm-3 control-label" for="csubscriptionId">Subscription ID</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" id="credentialAzure.parameters.subscriptionId" name="csubscriptionId" ng-model="credentialAzure.parameters.subscriptionId" required placeholder="your subscription id">
        <div class="help-block" ng-show="azureCredentialForm.csubscriptionId.$dirty && azureCredentialForm.csubscriptionId.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_subscription_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.azure_sshPublicKey.$dirty && azureCredentialForm.azure_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="azure_sshPublicKey">SSH certificate:</label>

    <div class="col-sm-9">
        <textarea rows="4" type="text" placeholder="-----BEGIN CERTIFICATE-----
your key...
-----END CERTIFICATE-----" class="form-control" ng-model="credentialAzure.publicKey" name="azure_sshPublicKey" id="azure_sshPublicKey" required></textarea>
        <div class="help-block" ng-show="azureCredentialForm.azure_sshPublicKey.$dirty && azureCredentialForm.azure_sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
      <label class="col-sm-3 control-label" for="azureCred_publicInAccount">Public in account</label>
      <div class="col-sm-9">
          <input type="checkbox" name="azureCred_publicInAccount" id="azureCred_publicInAccount" ng-model="credentialAzure.public">
      </div>
       <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createAzureCredential" ng-disabled="azureCredentialForm.$invalid" class="btn btn-success btn-block" ng-click="createAzureCredential()" role="button"><i
                class="fa fa-plus fa-fw"></i>
            create credential</a>
    </div>
</div>
