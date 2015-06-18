<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.cname.$dirty && azureCredentialForm.cname.$invalid }">
    <label class="col-sm-3 control-label" for="cname">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-z][a-z0-9]*$/" name="cname" id="cname" ng-model="credentialAzure.name" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="azureCredentialForm.cname.$dirty && azureCredentialForm.cname.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.cdescription.$dirty && azureCredentialForm.cdescription.$invalid }">
    <label class="col-sm-3 control-label" for="cdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialAzure.description" id="cdescription" name="cdescription" ng-maxlength="1000"  placeholder="{{msg.credential_form_description_placeholder}}">
        <div class="help-block" ng-show="azureCredentialForm.cdescription.$dirty && azureCredentialForm.cdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.csubscriptionId.$dirty && azureCredentialForm.csubscriptionId.$invalid }">
    <label class="col-sm-3 control-label" for="csubscriptionId">{{msg.credential_azure_form_subscription_id_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" id="credentialAzure.parameters.subscriptionId" name="csubscriptionId" ng-model="credentialAzure.parameters.subscriptionId" required placeholder="{{msg.credential_azure_form_subscription_id_placeholder}}">
        <div class="help-block" ng-show="azureCredentialForm.csubscriptionId.$dirty && azureCredentialForm.csubscriptionId.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_subscription_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': azureCredentialForm.azure_sshPublicKey.$dirty && azureCredentialForm.azure_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="azure_sshPublicKey">{{msg.credential_azure_form_ssh_cert_label}}</label>

    <div class="col-sm-9">
        <textarea rows="4" type="text" placeholder="{{msg.credential_azure_form_ssh_cert_placeholder}}" class="form-control" ng-model="credentialAzure.publicKey" name="azure_sshPublicKey" id="azure_sshPublicKey" required></textarea>
        <div class="help-block" ng-show="azureCredentialForm.azure_sshPublicKey.$dirty && azureCredentialForm.azure_sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_ssh_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
      <label class="col-sm-3 control-label" for="azureCred_publicInAccount">{{msg.public_in_account_label}}</label>
      <div class="col-sm-9">
          <input type="checkbox" name="azureCred_publicInAccount" id="azureCred_publicInAccount" ng-model="credentialAzure.public">
      </div>
       <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createAzureCredential" ng-disabled="azureCredentialForm.$invalid" class="btn btn-success btn-block" ng-click="createAzureCredential()" role="button"><i
                class="fa fa-plus fa-fw"></i>
            {{msg.credential_form_create}}</a>
    </div>
</div>
