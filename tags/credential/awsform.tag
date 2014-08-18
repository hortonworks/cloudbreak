<div class="alert alert-info" role="alert" ng-show="awsCredentialInCreate"><b>Please wait!</b> creation in progress...</div>
<form class="form-horizontal" role="form" ng-show="awsCredential" name="awsCredentialForm" ng-hide="awsCredentialInCreate">
    <div class="form-group" ng-class="{ 'has-error': awsCredentialForm.awscname.$dirty && awsCredentialForm.awscname.$invalid }">
        <label class="col-sm-3 control-label" for="awscname">Name</label>

        <div class="col-sm-9">
            <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" ng-model="awscname" id="awscname" name="awscname" ng-minlength="5" ng-maxlength="20" required placeholder="min. 5 max. 20 char">
            <div class="help-block" ng-show="awsCredentialForm.awscname.$dirty && awsCredentialForm.awscname.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': awsCredentialForm.awscdescription.$dirty && awsCredentialForm.awscdescription.$invalid }">
        <label class="col-sm-3 control-label" for="awscdescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-model="awscdescription" id="awscdescription" name="awscdescription" ng-maxlength="20" placeholder="max. 20 char">
            <div class="help-block" ng-show="awsCredentialForm.awscdescription.$dirty && awsCredentialForm.awscdescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': awsCredentialForm.croleArn.$dirty && awsCredentialForm.croleArn.$invalid }">
        <label class="col-sm-3 control-label" for="croleArn">IAM Role ARN</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="croleArn" ng-model="croleArn" ng-minlength="5" required id="croleArn">
            <div class="help-block" ng-show="awsCredentialForm.croleArn.$dirty && awsCredentialForm.croleArn.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_iam_role_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': awsCredentialForm.aws_sshPublicKey.$dirty && awsCredentialForm.aws_sshPublicKey.$invalid }">
        <label class="col-sm-3 control-label" for="aws_sshPublicKey">SSH public key:</label>

        <div class="col-sm-9">
            <textarea rows="4" type="text" class="form-control" ng-model="aws_sshPublicKey" name="aws_sshPublicKey" id="aws_sshPublicKey" required></textarea>
            <div class="help-block" ng-show="awsCredentialForm.aws_sshPublicKey.$dirty && awsCredentialForm.aws_sshPublicKey.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a href="#" id="createAwsCredential" ng-disabled="awsCredentialForm.$invalid" ng-click="createAwsCredential()" class="btn btn-success btn-block" role="button"><i
                    class="fa fa-plus fa-fw"></i>
                create credential</a>
        </div>
    </div>
</form>