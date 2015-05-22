<div class="form-group" ng-class="{ 'has-error': gcpCredentialForm.gcpcname.$dirty && gcpCredentialForm.gcpcname.$invalid }">
    <label class="col-sm-3 control-label" for="gcpcname">Name</label>
    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*$/" class="form-control" ng-model="credentialGcp.name" id="gcpcname" name="gcpcname" ng-minlength="5"  ng-model="credentialGcp.name"  ng-maxlength="100" required placeholder="min. 5 max. 100 char">

        <div class="help-block" ng-show="gcpCredentialForm.gcpcname.$dirty && gcpCredentialForm.gcpcname.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpCredentialForm.gcpcdescription.$dirty && gcpCredentialForm.gcpcdescription.$invalid }">
    <label class="col-sm-3 control-label" for="gcpcdescription">Description</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="gcpcdescription" ng-model="credentialGcp.description" name="gcpcdescription" ng-maxlength="1000"  placeholder="max. 1000 char">
        <div class="help-block" ng-show="gcpCredentialForm.gcpcdescription.$dirty && gcpCredentialForm.gcpcdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_description_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpCredentialForm.gcp_tprojectId.$dirty && gcpCredentialForm.gcp_tprojectId.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_tprojectId">Project Id</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="gcp_tprojectId" ng-model="credentialGcp.parameters.projectId" name="gcp_tprojectId" placeholder="projectid" required>

        <div class="help-block" ng-show="gcpCredentialForm.gcp_tprojectId.$dirty && gcpCredentialForm.gcp_tprojectId.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.project_id_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpCredentialForm.gcpcsubscriptionId.$dirty && gcpCredentialForm.gcpcsubscriptionId.$invalid }">
    <label class="col-sm-3 control-label" for="gcpcsubscriptionId">Service Account Email Address</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialGcp.parameters.serviceAccountId" id="gcpcsubscriptionId" name="gcpcsubscriptionId" required  placeholder="the email address of your Google service account">
        <div class="help-block" ng-show="gcpCredentialForm.gcpcsubscriptionId.$dirty && gcpCredentialForm.gcpcsubscriptionId.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_subscription_invalid}}
        </div>
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': gcpCredentialForm.gcp_caccPrivateKey.$dirty && gcpCredentialForm.gcp_caccPrivateKey.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_caccPrivateKey">Service account private (p12) key</label>
    <div class="col-sm-9">
        <input type="file" data-file="gcp.p12"/>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpCredentialForm.gcp_sshPublicKey.$dirty && gcpCredentialForm.gcp_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_sshPublicKey">SSH public key</label>
        <div class="col-sm-9">
            <textarea rows="4" placeholder="ssh-rsa AAAAB3... user-eu" type="text" class="form-control" ng-model="credentialGcp.publicKey" name="gcp_sshPublicKey" id="gcp_sshPublicKey" required></textarea>
            <div class="help-block" ng-show="gcpCredentialForm.gcp_sshPublicKey.$dirty && gcpCredentialForm.gcp_sshPublicKey.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
            </div>
    </div>
</div>
<div class="form-group">
      <label class="col-sm-3 control-label" for="gcpCred_publicInAccount">Public in account</label>
      <div class="col-sm-9">
          <input type="checkbox" name="gcpCred_publicInAccount" id="gcpCred_publicInAccount" ng-model="credentialGcp.public">
      </div>
       <!-- .col-sm-9 -->
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createGcpCredential" ng-disabled="gcpCredentialForm.$invalid || !gcp.p12" ng-click="createGcpCredential()" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> create credential</a>
    </div>
</div>
