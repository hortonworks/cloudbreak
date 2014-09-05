<form class="form-horizontal" role="form" name=gccCredentialForm" ng-show="gccCredential">
    <div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcccname.$dirty && gccCredentialForm.gcccname.$invalid }">
        <label class="col-sm-3 control-label" for="gcccname">Name</label>

        <div class="col-sm-9">
            <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" ng-model="gcccname" id="gcccname" name="gcccname" ng-minlength="5"
                   ng-maxlength="20" required placeholder="min. 5 max. 20 char">

            <div class="help-block" ng-show="gccCredentialForm.gcccname.$dirty && gccCredentialForm.gcccname.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcccdescription.$dirty && gccCredentialForm.gcccdescription.$invalid }">
        <label class="col-sm-3 control-label" for="gcccdescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-model="gcccdescription" id="gcccdescription" name="gcccdescription" ng-maxlength="20"
                   placeholder="max. 20 char">

            <div class="help-block" ng-show="gccCredentialForm.gcccdescription.$dirty && gccCredentialForm.gcccdescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcc_tprojectId.$dirty && gccCredentialForm.gcc_tprojectId.$invalid }">
        <label class="col-sm-3 control-label" for="gcc_tprojectId">Project Id</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="gcc_tprojectId" name="gcc_tprojectId" ng-model="gcc_tprojectId" placeholder="projectid" required">
            <div class="help-block" ng-show="gccTemplateForm.gcc_tprojectId.$dirty && gccTemplateForm.gcc_tprojectId.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_project_id_empty}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcccsubscriptionId.$dirty && gccCredentialForm.gcccsubscriptionId.$invalid }">
        <label class="col-sm-3 control-label" for="gcccsubscriptionId">Subscription ID</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="gcccsubscriptionId" name="gcccsubscriptionId" ng-model="gcccsubscriptionId" required
                   placeholder="your subscription id">

            <div class="help-block" ng-show="gccCredentialForm.gcccsubscriptionId.$dirty && gccCredentialForm.gcccsubscriptionId.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_subscription_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcc_csshPublicKey.$dirty && gccCredentialForm.gcc_csshPublicKey.$invalid }">
        <label class="col-sm-3 control-label" for="gcc_csshPublicKey">Credential public key:</label>

        <div class="col-sm-9">
            <textarea rows="4" type="text" class="form-control" ng-model="gcc_csshPublicKey" name="gcc_csshPublicKey" id="gcc_csshPublicKey"
                      required></textarea>

            <div class="help-block" ng-show="gccCredentialForm.gcc_csshPublicKey.$dirty && gccCredentialForm.gcc_csshPublicKey.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': gccCredentialForm.gcc_sshPublicKey.$dirty && gccCredentialForm.gcc_sshPublicKey.$invalid }">
        <label class="col-sm-3 control-label" for="gcc_sshPublicKey">SSH public key:</label>

        <div class="col-sm-9">
            <textarea rows="4" type="text" class="form-control" ng-model="gcc_sshPublicKey" name="gcc_sshPublicKey" id="gcc_sshPublicKey" required></textarea>

            <div class="help-block" ng-show="gccCredentialForm.gcc_sshPublicKey.$dirty && gccCredentialForm.gcc_sshPublicKey.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a href="#" id="createGccCredential" ng-disabled="gccCredentialForm.$invalid" ng-click="createGccCredential()" class="btn btn-success btn-block"
               role="button"><i
                    class="fa fa-plus fa-fw"></i>
                create credential</a>
        </div>
    </div>
</form>
