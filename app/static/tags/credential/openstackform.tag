<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstackcname.$dirty && openstackCredentialForm.openstackcname.$invalid }">
    <label class="col-sm-3 control-label" for="openstackcname">Name</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*$/" class="form-control" ng-model="credentialOpenstack.name" id="openstackcname" name="openstackcname" ng-minlength="5" ng-maxlength="100" required placeholder="min. 5 max. 100 char">
        <div class="help-block" ng-show="openstackCredentialForm.openstackcname.$dirty && openstackCredentialForm.openstackcname.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstackcdescription.$dirty && openstackCredentialForm.openstackcdescription.$invalid }">
    <label class="col-sm-3 control-label" for="openstackcdescription">Description</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialOpenstack.description" id="openstackcdescription" name="openstackcdescription" ng-maxlength="1000" placeholder="max. 1000 char">
        <div class="help-block" ng-show="openstackCredentialForm.openstackcdescription.$dirty && openstackCredentialForm.openstackcdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.ouser.$dirty && openstackCredentialForm.ouser.$invalid }">
    <label class="col-sm-3 control-label" for="ouser">User</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="ouser" ng-model="credentialOpenstack.parameters.user" ng-minlength="5" required id="ouser" placeholder="min. 5 char">
        <div class="help-block" ng-show="openstackCredentialForm.ouser.$dirty && openstackCredentialForm.ouser.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.openstack_credential_ouser_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.opassword.$dirty && openstackCredentialForm.opassword.$invalid }">
  <label class="col-sm-3 control-label" for="opassword">Password</label>

  <div class="col-sm-9">
    <input type="text" class="form-control" name="opassword" ng-model="credentialOpenstack.parameters.password" ng-minlength="5" required id="opassword" placeholder="min. 5 char">
      <div class="help-block" ng-show="openstackCredentialForm.opassword.$dirty && openstackCredentialForm.opassword.$invalid">
        <i class="fa fa-warning"></i> {{error_msg.openstack_credential_opassword_invalid}}
      </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.otenantName.$dirty && openstackCredentialForm.otenantName.$invalid }">
  <label class="col-sm-3 control-label" for="otenantName">Tenant Name</label>

  <div class="col-sm-9">
    <input type="text" class="form-control" name="otenantName" ng-model="credentialOpenstack.parameters.tenantName" ng-minlength="5" required id="otenantName" placeholder="min. 5 char">
      <div class="help-block" ng-show="openstackCredentialForm.otenantName.$dirty && openstackCredentialForm.otenantName.$invalid">
        <i class="fa fa-warning"></i> {{error_msg.openstack_credential_otenantName_invalid}}
      </div>
  </div>
    <!-- .col-sm-9 -->
</div>


<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.oendpoint.$dirty && openstackCredentialForm.oendpoint.$invalid }">
  <label class="col-sm-3 control-label" for="oendpoint">Endpoint</label>

  <div class="col-sm-9">
    <input type="text" class="form-control" name="oendpoint" ng-model="credentialOpenstack.parameters.endpoint" ng-minlength="5" required id="oendpoint" placeholder="min. 5 char">
      <div class="help-block" ng-show="openstackCredentialForm.oendpoint.$dirty && openstackCredentialForm.oendpoint.$invalid">
        <i class="fa fa-warning"></i> {{error_msg.openstack_credential_oendpoint_invalid}}
      </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstack_sshPublicKey.$dirty && openstackCredentialForm.openstack_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_sshPublicKey">SSH public key</label>

    <div class="col-sm-9">
        <textarea placeholder="ssh-rsa AAAAB3... user-eu" rows="4" type="text" class="form-control" ng-model="credentialOpenstack.publicKey" name="openstack_sshPublicKey" id="openstack_sshPublicKey" required></textarea>
        <div class="help-block" ng-show="openstackCredentialForm.openstack_sshPublicKey.$dirty && openstackCredentialForm.openstack_sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
      <label class="col-sm-3 control-label" for="credPublic">Public in account</label>
      <div class="col-sm-9">
          <input type="checkbox" name="credPublic" id="credPublic" ng-model="credentialOpenstack.public">
      </div>
       <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createopenstackCredential" ng-disabled="openstackCredentialForm.$invalid" ng-click="createOpenstackCredential()" class="btn btn-success btn-block" role="button"><i
                class="fa fa-plus fa-fw"></i>
            create credential</a>
    </div>
</div>
