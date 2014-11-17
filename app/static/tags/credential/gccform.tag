<div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcccname.$dirty && gccCredentialForm.gcccname.$invalid }">
    <label class="col-sm-3 control-label" for="gcccname">Name</label>
    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" ng-model="credentialGcc.name" id="gcccname" name="gcccname" ng-minlength="5"  ng-model="credentialGcc.name"  ng-maxlength="20" required placeholder="min. 5 max. 20 char">

        <div class="help-block" ng-show="gccCredentialForm.gcccname.$dirty && gccCredentialForm.gcccname.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcccdescription.$dirty && gccCredentialForm.gcccdescription.$invalid }">
    <label class="col-sm-3 control-label" for="gcccdescription">Description</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="gcccdescription" ng-model="credentialGcc.description" name="gcccdescription" ng-maxlength="50"  placeholder="max. 50 char">
        <div class="help-block" ng-show="gccCredentialForm.gcccdescription.$dirty && gccCredentialForm.gcccdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_description_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcc_tprojectId.$dirty && gccCredentialForm.gcc_tprojectId.$invalid }">
    <label class="col-sm-3 control-label" for="gcc_tprojectId">Project Id</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="gcc_tprojectId" ng-model="credentialGcc.parameters.projectId" name="gcc_tprojectId" placeholder="projectid" required>

        <div class="help-block" ng-show="gccCredentialForm.gcc_tprojectId.$dirty && gccCredentialForm.gcc_tprojectId.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.project_id_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcccsubscriptionId.$dirty && gccCredentialForm.gcccsubscriptionId.$invalid }">
    <label class="col-sm-3 control-label" for="gcccsubscriptionId">Service Account Email Address</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialGcc.parameters.serviceAccountId" id="gcccsubscriptionId" name="gcccsubscriptionId" required  placeholder="the email address of your Google service account">
        <div class="help-block" ng-show="gccCredentialForm.gcccsubscriptionId.$dirty && gccCredentialForm.gcccsubscriptionId.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_subscription_invalid}}
        </div>
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcc_caccPrivateKey.$dirty && gccCredentialForm.gcc_caccPrivateKey.$invalid }">
    <label class="col-sm-3 control-label" for="gcc_caccPrivateKey">Service account private key:</label>
    <div class="col-sm-9">
        <input type="file" data-file="gcc.p12"/>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcc_sshPublicKey.$dirty && gccCredentialForm.gcc_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="gcc_sshPublicKey">SSH public key:</label>
        <div class="col-sm-9">
            <textarea rows="4" placeholder="ssh-rsa AAAAB3... user-eu" type="text" class="form-control" ng-model="credentialGcc.publicKey" name="gcc_sshPublicKey" id="gcc_sshPublicKey" required></textarea>
            <div class="help-block" ng-show="gccCredentialForm.gcc_sshPublicKey.$dirty && gccCredentialForm.gcc_sshPublicKey.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
            </div>
    </div>
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createGccCredential" ng-disabled="gccCredentialForm.$invalid || !gcc.p12" ng-click="createGccCredential()" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> create credential</a>
    </div>
</div>
