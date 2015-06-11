<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstackcname.$dirty && openstackCredentialForm.openstackcname.$invalid }">
    <label class="col-sm-3 control-label" for="openstackcname">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*$/" class="form-control" ng-model="credentialOpenstack.name" id="openstackcname" name="openstackcname" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.openstackcname.$dirty && openstackCredentialForm.openstackcname.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstackcdescription.$dirty && openstackCredentialForm.openstackcdescription.$invalid }">
    <label class="col-sm-3 control-label" for="openstackcdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="credentialOpenstack.description" id="openstackcdescription" name="openstackcdescription" ng-maxlength="1000" placeholder="{{msg.credential_form_description_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.openstackcdescription.$dirty && openstackCredentialForm.openstackcdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.ouser.$dirty && openstackCredentialForm.ouser.$invalid }">
    <label class="col-sm-3 control-label" for="ouser">{{msg.credential_openstack_form_user_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="ouser" ng-model="credentialOpenstack.parameters.user" required id="ouser" placeholder="{{msg.credential_openstack_user_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.ouser.$dirty && openstackCredentialForm.ouser.$invalid">
            <i class="fa fa-warning"></i> {{msg.openstack_credential_ouser_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.opassword.$dirty && openstackCredentialForm.opassword.$invalid }">
  <label class="col-sm-3 control-label" for="opassword">{{msg.credential_openstack_form_password_label}}</label>

  <div class="col-sm-9">
    <input type="password" class="form-control" name="opassword" ng-model="credentialOpenstack.parameters.password" required id="opassword" placeholder="{{msg.credential_openstack_password_placeholder}}">
      <div class="help-block" ng-show="openstackCredentialForm.opassword.$dirty && openstackCredentialForm.opassword.$invalid">
        <i class="fa fa-warning"></i> {{msg.openstack_credential_opassword_invalid}}
      </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.otenantName.$dirty && openstackCredentialForm.otenantName.$invalid }">
  <label class="col-sm-3 control-label" for="otenantName">{{msg.credential_openstack_form_tenant_label}}</label>

  <div class="col-sm-9">
    <input type="text" class="form-control" name="otenantName" ng-model="credentialOpenstack.parameters.tenantName" required id="otenantName" placeholder="{{msg.credential_openstack_form_tenant_placeholder}}">
      <div class="help-block" ng-show="openstackCredentialForm.otenantName.$dirty && openstackCredentialForm.otenantName.$invalid">
        <i class="fa fa-warning"></i> {{msg.openstack_credential_otenantName_invalid}}
      </div>
  </div>
    <!-- .col-sm-9 -->
</div>


<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.oendpoint.$dirty && openstackCredentialForm.oendpoint.$invalid }">
  <label class="col-sm-3 control-label" for="oendpoint">{{msg.credential_openstack_form_endpoint_label}}</label>

  <div class="col-sm-9">
    <input type="text" class="form-control" name="oendpoint" ng-model="credentialOpenstack.parameters.endpoint" ng-minlength="5" required id="oendpoint" placeholder="{{msg.credential_openstack_form_endpoint_placeholder}}">
      <div class="help-block" ng-show="openstackCredentialForm.oendpoint.$dirty && openstackCredentialForm.oendpoint.$invalid">
        <i class="fa fa-warning"></i> {{msg.openstack_credential_oendpoint_invalid}}
      </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstack_sshPublicKey.$dirty && openstackCredentialForm.openstack_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_sshPublicKey">{{msg.credential_openstack_form_ssh_key_label}}</label>

    <div class="col-sm-9">
        <textarea placeholder="{{msg.credential_openstack_form_ssh_key_placeholder}}" rows="4" type="text" class="form-control" ng-model="credentialOpenstack.publicKey" name="openstack_sshPublicKey" id="openstack_sshPublicKey" required></textarea>
        <div class="help-block" ng-show="openstackCredentialForm.openstack_sshPublicKey.$dirty && openstackCredentialForm.openstack_sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_ssh_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
      <label class="col-sm-3 control-label" for="credPublic">{{msg.public_in_account_label}}</label>
      <div class="col-sm-9">
          <input type="checkbox" name="credPublic" id="credPublic" ng-model="credentialOpenstack.public">
      </div>
       <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createopenstackCredential" ng-disabled="openstackCredentialForm.$invalid" ng-click="createOpenstackCredential()" class="btn btn-success btn-block" role="button"><i
                class="fa fa-plus fa-fw"></i>
          {{msg.credential_form_create}}</a>
    </div>
</div>
