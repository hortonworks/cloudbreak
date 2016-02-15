<div class="form-group">
    <label class="col-sm-3 control-label" for="awscredentialtype">{{msg.credential_aws_select}}</label>
    <div class="col-sm-3">
        <select class="form-control" id="awscredentialtype" name="awscredentialtype" ng-model="credentialAws.parameters.selector" ng-options="option.id as option.name for option in $root.config.AWS_SELECTOR" ng-init="credentialAws.parameters.selector == 'role-based'">
        </select>
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': awsCredentialForm.awscname.$dirty && awsCredentialForm.awscname.$invalid }">
    <label class="col-sm-3 control-label" for="awscname">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" class="form-control" ng-model="credentialAws.name" id="awscname" name="awscname" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="awsCredentialForm.awscname.$dirty && awsCredentialForm.awscname.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': awsCredentialForm.awscdescription.$dirty && awsCredentialForm.awscdescription.$invalid }">
    <label class="col-sm-3 control-label" for="awscdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialAws.description" id="awscdescription" name="awscdescription" ng-maxlength="1000" placeholder="{{msg.credential_form_description_placeholder}}">
        <div class="help-block" ng-show="awsCredentialForm.awscdescription.$dirty && awsCredentialForm.awscdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': awsCredentialForm.croleArn.$dirty && awsCredentialForm.croleArn.$invalid }" ng-if="credentialAws.parameters.selector=='role-based'">
    <label class="col-sm-3 control-label" for="croleArn">{{msg.credential_aws_form_iam_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="croleArn" ng-model="credentialAws.parameters.roleArn" ng-minlength="5" required id="croleArn">
        <div class="help-block" ng-show="awsCredentialForm.croleArn.$dirty && awsCredentialForm.croleArn.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_iam_role_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error': awsCredentialForm.accessKey.$dirty && awsCredentialForm.accessKey.$invalid }" ng-if="credentialAws.parameters.selector=='key-based'">
    <label class="col-sm-3 control-label" for="accessKey">{{msg.credential_aws_form_access_key_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="accessKey" ng-model="credentialAws.parameters.accessKey" ng-minlength="5" required id="accessKey">
        <div class="help-block" ng-show="awsCredentialForm.accessKey.$dirty && awsCredentialForm.accessKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_access_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error': awsCredentialForm.secretKey.$dirty && awsCredentialForm.secretKey.$invalid }" ng-if="credentialAws.parameters.selector=='key-based'">
    <label class="col-sm-3 control-label" for="croleArn">{{msg.credential_aws_form_secret_key_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="secretKey" ng-model="credentialAws.parameters.secretKey" ng-minlength="5" required id="secretKey">
        <div class="help-block" ng-show="awsCredentialForm.secretKey.$dirty && awsCredentialForm.secretKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_secret_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': awsCredentialForm.aws_sshPublicKey.$dirty && awsCredentialForm.aws_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="aws_sshPublicKey">{{msg.credential_aws_form_ssh_key_label}}</label>

    <div class="col-sm-9">
        <textarea placeholder="{{msg.credential_aws_form_ssh_key_placeholder}}" rows="4" type="text" class="form-control" ng-model="credentialAws.publicKey" name="aws_sshPublicKey" id="aws_sshPublicKey" required></textarea>
        <div class="help-block" ng-show="awsCredentialForm.aws_sshPublicKey.$dirty && awsCredentialForm.aws_sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_ssh_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="topologySelect" name="topologySelect" ng-model="credentialAws.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
            <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
        </select>
    </div>
</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="credPublic">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="credPublic" id="credPublic" ng-model="credentialAws.public">
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createAwsCredential" ng-disabled="awsCredentialForm.$invalid" ng-click="createAwsCredential()" class="btn btn-success btn-block" role="button"><i
                class="fa fa-plus fa-fw"></i>
            {{msg.credential_form_create}}</a>
    </div>
</div>