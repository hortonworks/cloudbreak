<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstack_keystone_version.$dirty && openstackCredentialForm.openstack_keystone_version.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_keystone_version">{{msg.credential_openstack_form_keystoneVersion_label}}</label>

    <div class="col-sm-2">
        <select class="form-control" name="keystoneVersion" id="keystoneVersion" ng-model="credentialOpenstack.parameters.keystoneVersion" ng-dropdown required ng-init="credentialOpenstack.parameters.keystoneVersion='cb-keystone-v2'">
            <option ng-option value="cb-keystone-v2" ng-selected="true">v2</option>
            <option ng-option value="cb-keystone-v3">v3</option>
        </select>
        <div class="help-block" ng-show="openstackCredentialForm.openstack_keystone_version.$dirty && openstackCredentialForm.openstack_keystone_version.$invalid">
            <i class="fa fa-warning"></i> {{msg.keystone_version_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-if="credentialOpenstack.parameters.keystoneVersion == 'cb-keystone-v3'" ng-class="{ 'has-error': openstackCredentialForm.openstack_keystone_scope.$dirty && openstackCredentialForm.openstack_keystone_scope.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_keystone_scope">{{msg.credential_openstack_form_keystoneAuthScope_label}}</label>

    <div class="col-sm-2">
        <select class="form-control" name="keystoneAuthScope" id="keystoneAuthScope" ng-model="credentialOpenstack.parameters.keystoneAuthScope" ng-dropdown required ng-init="credentialOpenstack.parameters.keystoneAuthScope='cb-keystone-v3-default-scope'">
            <option ng-option value="cb-keystone-v3-default-scope" ng-selected="true">Default</option>
            <option ng-option value="cb-keystone-v3-domain-scope">Domain</option>
            <option ng-option value="cb-keystone-v3-project-scope">Project</option>
        </select>
        <div class="help-block" ng-show="openstackCredentialForm.openstack_keystone_scope.$dirty && openstackCredentialForm.openstack_keystone_scope.$invalid">
            <i class="fa fa-warning"></i> {{msg.keystone_scope_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstackcname.$dirty && openstackCredentialForm.openstackcname.$invalid }">
    <label class="col-sm-3 control-label" for="openstackcname">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" class="form-control" ng-model="credentialOpenstack.name" id="openstackcname" name="openstackcname" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
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
    <label class="col-sm-3 control-label" for="ouser">{{msg.credential_openstack_form_userName_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="ouser" ng-model="credentialOpenstack.parameters.userName" required id="ouser" placeholder="{{msg.credential_openstack_user_placeholder}}">
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
<div class="form-group" ng-if="credentialOpenstack.parameters.keystoneVersion == 'cb-keystone-v3'" ng-class="{ 'has-error': openstackCredentialForm.ouserDomain.$dirty && openstackCredentialForm.ouserDomain.$invalid }">
    <label class="col-sm-3 control-label" for="ouserDomain">{{msg.credential_openstack_form_userDomain_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="ouserDomain" ng-model="credentialOpenstack.parameters.userDomain" required id="ouserDomain" placeholder="{{msg.credential_openstack_user_domain_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.ouserDomain.$dirty && openstackCredentialForm.ouserDomain.$invalid">
            <i class="fa fa-warning"></i> {{msg.openstack_credential_odomainName_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-if="credentialOpenstack.parameters.keystoneVersion == 'cb-keystone-v2'" ng-class="{ 'has-error': openstackCredentialForm.otenantName.$dirty && openstackCredentialForm.otenantName.$invalid }">
    <label class="col-sm-3 control-label" for="otenantName">{{msg.credential_openstack_form_tenantName_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="otenantName" ng-model="credentialOpenstack.parameters.tenantName" required id="otenantName" placeholder="{{msg.credential_openstack_form_tenant_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.otenantName.$dirty && openstackCredentialForm.otenantName.$invalid">
            <i class="fa fa-warning"></i> {{msg.openstack_credential_otenantName_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-if="credentialOpenstack.parameters.keystoneVersion == 'cb-keystone-v3' && credentialOpenstack.parameters.keystoneAuthScope == 'cb-keystone-v3-domain-scope'" ng-class="{ 'has-error': openstackCredentialForm.odomainName.$dirty && openstackCredentialForm.odomainName.$invalid }">
    <label class="col-sm-3 control-label" for="odomainName">{{msg.credential_openstack_form_domainName_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="odomainName" ng-model="credentialOpenstack.parameters.domainName" required id="odomainName" placeholder="{{msg.credential_openstack_form_domain_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.odomainName.$dirty && openstackCredentialForm.odomainName.$invalid">
            <i class="fa fa-warning"></i> {{msg.openstack_credential_odomainName_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-if="credentialOpenstack.parameters.keystoneVersion == 'cb-keystone-v3' && credentialOpenstack.parameters.keystoneAuthScope == 'cb-keystone-v3-project-scope'" ng-class="{ 'has-error': openstackCredentialForm.oprojectName.$dirty && openstackCredentialForm.oprojectName.$invalid }">
    <label class="col-sm-3 control-label" for="oprojectName">{{msg.credential_openstack_form_projectName_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="oprojectName" ng-model="credentialOpenstack.parameters.projectName" required id="oprojectName" placeholder="{{msg.credential_openstack_form_project_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.oprojectName.$dirty && openstackCredentialForm.oprojectName.$invalid">
            <i class="fa fa-warning"></i> {{msg.openstack_credential_oprojectName_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<!-- .form-group -->
<div class="form-group" ng-if="credentialOpenstack.parameters.keystoneVersion == 'cb-keystone-v3' && credentialOpenstack.parameters.keystoneAuthScope == 'cb-keystone-v3-project-scope'" ng-class="{ 'has-error': openstackCredentialForm.oprojectDomainName.$dirty && openstackCredentialForm.oprojectDomainName.$invalid }">
    <label class="col-sm-3 control-label" for="oprojectDomainName">{{msg.credential_openstack_form_projectDomainName_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="oprojectDomainName" ng-model="credentialOpenstack.parameters.projectDomainName" required id="oprojectDomainName" placeholder="{{msg.credential_openstack_form_project_domain_placeholder}}">
        <div class="help-block" ng-show="openstackCredentialForm.oprojectDomainName.$dirty && openstackCredentialForm.oprojectDomainName.$invalid">
            <i class="fa fa-warning"></i> {{msg.openstack_credential_odomainName_invalid}}
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

<!-- .form-group -->
<div class="form-group">
    <label class="col-sm-3 control-label" for="facing">{{msg.credential_openstack_form_facing_label}}</label>

    <div class="col-sm-2">
        <select  class="form-control" name="facing" id="facing" ng-model="credentialOpenstack.parameters.facing" ng-dropdown required ng-init="credentialOpenstack.parameters.facing='public'">
            <option ng-option value="public" ng-selected="true">public</option>
            <option ng-option value="admin">admin</option>
            <option ng-option value="internal">internal</option>
        </select>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group" ng-class="{ 'has-error': openstackCredentialForm.openstack_sshPublicKey.$dirty && openstackCredentialForm.openstack_sshPublicKey.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_sshPublicKey">{{msg.credential_openstack_form_ssh_key_label}}</label>

    <div class="col-sm-9">
        <textarea ng-attr-placeholder="{{msg.credential_openstack_form_ssh_key_placeholder}}" rows="4" type="text" class="form-control" ng-model="credentialOpenstack.publicKey" name="openstack_sshPublicKey" id="openstack_sshPublicKey" required></textarea>
        <div class="help-block" ng-show="openstackCredentialForm.openstack_sshPublicKey.$dirty && openstackCredentialForm.openstack_sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_ssh_key_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="topologySelect" name="topologySelect" ng-model="credentialOpenstack.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
            <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
        </select>
    </div>
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