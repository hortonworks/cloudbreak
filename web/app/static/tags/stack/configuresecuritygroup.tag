<div class="form-group">
    <label class="col-sm-3 control-label" for="selectClusterNetwork">{{msg.cluster_form_network_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectClusterNetwork" ng-model="cluster.networkId" ng-required="activeCredential !== undefined">
            <option ng-repeat="network in $root.networks | filter:filterByTopology | filter:{cloudPlatform: activeCredential.cloudPlatform} | orderBy:'name'" value="{{network.id}}">{{network.name}}</option>
        </select>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="select-cluster-securitygroup">{{msg.cluster_form_securitygroup_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="select-cluster-securitygroup" ng-model="cluster.securityGroupId" ng-required="activeCredential !== undefined">
            <option ng-repeat="securitygroup in $root.securitygroups | orderBy:'name'" value="{{securitygroup.id}}">{{securitygroup.name}}</option>
        </select>
        <div class="help-block" ng-show="isExistingVpc()"><i class="fa fa-warning"></i> {{msg.cluster_form_securitygroup_existingvpc}}
    </div>
</div>
<div class="form-group" name="cluster_security1">
    <label class="col-sm-3 control-label" for="cluster_security">{{msg.cluster_form_enable_security}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_security" id="cluster_security" ng-model="cluster.enableSecurity">
        <div class="help-block" ng-show="cluster.enableSecurity"><i class="fa fa-warning"></i> {{msg.cluster_form_enable_security_hint}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_master_key1" ng-show="cluster.enableSecurity" ng-class="{ 'has-error': clusterCreationForm.kerberos_master_key.$dirty && clusterCreationForm.kerberos_master_key.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_master_key">{{msg.cluster_form_kerberos_master_key}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_master_key" class="form-control" ng-model="cluster.kerberosMasterKey" id="kerberos_master_key" placeholder="{{msg.cluster_form_kerberos_master_key_placeholder}}" ng-minlength="3" ng-maxlength="50" ng-required="cluster.enableSecurity">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_master_key.$dirty && clusterCreationForm.kerberos_master_key.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_master_key_invalid}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_admin1" ng-show="cluster.enableSecurity" ng-class="{ 'has-error': clusterCreationForm.kerberos_admin.$dirty && clusterCreationForm.kerberos_admin.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_admin">{{msg.cluster_form_kerberos_admin}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_admin" class="form-control" ng-model="cluster.kerberosAdmin" id="kerberos_admin" placeholder="{{msg.cluster_form_kerberos_admin_placeholder}}" ng-minlength="5" ng-maxlength="15" ng-required="cluster.enableSecurity">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_admin.$dirty && clusterCreationForm.kerberos_admin.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_admin}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_password1" ng-show="cluster.enableSecurity" ng-class="{ 'has-error': clusterCreationForm.kerberos_password.$dirty && clusterCreationForm.kerberos_password.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_password">{{msg.cluster_form_kerberos_password}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_password" class="form-control" ng-model="cluster.kerberosPassword" id="kerberos_password" placeholder="{{msg.cluster_form_kerberos_password_placeholder}}" ng-minlength="5" ng-maxlength="50" ng-required="cluster.enableSecurity">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_password.$dirty && clusterCreationForm.kerberos_password.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_password}}
        </div>
    </div>
</div>
<div class="form-group" name="cluster_ldap1">
    <label class="col-sm-3 control-label" for="cluster_ldap">{{msg.cluster_form_enable_ldap}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_ldap" id="cluster_ldap" ng-model="cluster.ldapRequired">
        <div class="help-block" ng-show="cluster.ldapRequired"><i class="fa fa-warning"></i> {{msg.cluster_form_enable_ldap_hint}}</div>
    </div>
</div>
<div class="form-group" name="cluster_sssd1" ng-show="$root.sssdConfigs.length">
    <label class="col-sm-3 control-label" for="cluster_sssd">{{msg.cluster_form_enable_sssd}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_sssd" id="cluster_sssd" ng-model="selectSssd.show" ng-change="cluster.sssdConfigId = null">
    </div>
</div>
<div class="form-group" name="sssd_configuration1" ng-show="selectSssd.show">
    <label class="col-sm-3 control-label" for="sssd_configuration">{{msg.cluster_form_sssd_configuration}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="sssd_configuration" name="sssd_configuration" ng-options="sssdConfig.id as sssdConfig.name for sssdConfig in $root.sssdConfigs" ng-model="cluster.sssdConfigId" ng-required="selectSssd.show"></select>
    </div>
</div>
<div class="form-group">
    <div class="col-sm-11">
        <div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureCluster')"><i class="fa fa-angle-double-left"></i> {{msg.cluster_form_ambari_cluster_tag}}</button>
            </div>
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-default" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || (selectSssd.show && !cluster.sssdConfigId)" ng-click="showWizardActualElement('configureHostGroups')">{{msg.cluster_form_ambari_blueprint_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
        </div>
    </div>
</div>