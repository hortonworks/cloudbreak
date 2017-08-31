<div class="form-group">
    <label class="col-sm-3 control-label" for="selectClusterNetwork">{{msg.cluster_form_network_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectClusterNetwork" name="selectClusterNetwork" ng-model="cluster.networkId" ng-required="activeCredential !== undefined && activeCredential.cloudPlatform !== 'BYOS'" ng-options="network.id as network.name for network in $root.networks | filter:filterByTopology | filter:{cloudPlatform: activeCredential.cloudPlatform} | orderBy:'name'">
        </select>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="publicKey">{{msg.cluster_form_ssh_key_label}}</label>
    <div class="col-sm-8">
        <textarea ng-attr-placeholder="{{msg.cluster_form_ssh_key_placeholder}}" rows="4" type="text" class="form-control" ng-model="cluster.publicKey" name="publicKey" id="publicKey" required></textarea>
        <div class="help-block" ng-show="clusterCreationForm.sshPublicKey.$dirty && clusterCreationForm.sshPublicKey.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_ssh_key_invalid}}
        </div>
    </div>
</div>
<div class="form-group" name="cluster_perimeter_security1">
    <label class="col-sm-3 control-label" for="cluster_perimeter_security">{{msg.cluster_form_enable_knox_gateway}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_perimeter_security" id="cluster_perimeter_security" ng-model="cluster.gateway.enableGateway">
    </div>
</div>
<div class="form-group" name="cluster_security1">
    <label class="col-sm-3 control-label" for="cluster_security">{{msg.cluster_form_enable_security}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_security" id="cluster_security" ng-model="cluster.enableSecurity">
    </div>
</div>
<div class="form-group" name="kerberos_master_key1" ng-show="cluster.enableSecurity && cluster.enableExSecurity=='NONE'" ng-class="{ 'has-error': clusterCreationForm.kerberos_master_key.$dirty && clusterCreationForm.kerberos_master_key.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_master_key">{{msg.cluster_form_kerberos_master_key}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_master_key" class="form-control" ng-model="cluster.kerberos.masterKey" id="kerberos_master_key" placeholder="{{msg.cluster_form_kerberos_master_key_placeholder}}" ng-minlength="3" ng-maxlength="50" ng-required="cluster.enableSecurity && cluster.enableExSecurity=='NONE'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_master_key.$dirty && clusterCreationForm.kerberos_master_key.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_master_key_invalid}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_admin1" ng-show="cluster.enableSecurity && cluster.enableExSecurity=='NONE'" ng-class="{ 'has-error': clusterCreationForm.kerberos_admin.$dirty && clusterCreationForm.kerberos_admin.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_admin">{{msg.cluster_form_kerberos_admin}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_admin" class="form-control" ng-model="cluster.kerberos.admin" id="kerberos_admin" placeholder="{{msg.cluster_form_kerberos_admin_placeholder}}" ng-minlength="5" ng-maxlength="15" ng-required="cluster.enableSecurity && cluster.enableExSecurity=='NONE'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_admin.$dirty && clusterCreationForm.kerberos_admin.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_admin}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_password1" ng-show="cluster.enableSecurity" ng-class="{ 'has-error': clusterCreationForm.kerberos_password.$dirty && clusterCreationForm.kerberos_password.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_password">{{msg.cluster_form_kerberos_password}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_password" class="form-control" ng-model="cluster.kerberos.password" id="kerberos_password" placeholder="{{msg.cluster_form_kerberos_password_placeholder}}" ng-minlength="5" ng-maxlength="50" ng-required="cluster.enableSecurity">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_password.$dirty && clusterCreationForm.kerberos_password.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_password}}
        </div>
    </div>
</div>
<div class="form-group" name="cluster_exsecurity1" ng-show="cluster.enableSecurity">
    <label class="col-sm-3 control-label" for="cluster_exsecurity1">{{msg.cluster_form_enable_ex_security}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="cluster_exsecurity1" ng-model="cluster.enableExSecurity" ng-show="cluster.enableSecurity">
            <option value="NONE">Create New MIT Kerberos</option>
            <option value="MIT-KERB">Use existing MIT Kerberos</option>
            <option value="AD-KERB">Use existing Active Directory</option>
        </select>
    </div>
</div>
<div class="form-group" name="kerberos_kerberosPrincipal" ng-show="cluster.enableExSecurity!='NONE'" ng-class="{ 'has-error': clusterCreationForm.kerberos_kerberosPrincipal.$dirty && clusterCreationForm.kerberos_kerberosPrincipal.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_kerberosPrincipal">{{msg.cluster_form_kerberos_principal}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_kerberosPrincipal" class="form-control" ng-model="cluster.kerberos.principal" id="kerberos_kerberosPrincipal" placeholder="{{msg.cluster_form_kerberos_principal_placeholder}}" ng-required="cluster.enableExSecurity!='NONE'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_kerberosPrincipal.$dirty && clusterCreationForm.kerberos_kerberosPrincipal.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_principal}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_kerberosUrl" ng-show="cluster.enableExSecurity!='NONE'" ng-class="{ 'has-error': clusterCreationForm.kerberos_kerberosUrl.$dirty && clusterCreationForm.kerberos_kerberosUrl.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_kerberosUrl">{{msg.cluster_form_kerberos_url}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_kerberosUrl" class="form-control" ng-model="cluster.kerberos.url" id="kerberos_kerberosUrl" placeholder="{{msg.cluster_form_kerberos_url_placeholder}}" ng-required="cluster.enableExSecurity!='NONE'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_kerberosUrl.$dirty && clusterCreationForm.kerberos_kerberosUrl.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_url}}
        </div>
    </div>
</div>
<div class="form-group" name="clusterallowtcp" ng-show="cluster.enableExSecurity!='NONE'">
    <label class="col-sm-3 control-label" for="cluster_clusterallowtcp">{{msg.cluster_form_enable_tcp}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_clusterallowtcp" id="cluster_clusterallowtcp" ng-model="cluster.kerberos.tcpAllowed">
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_kerberosRealm" ng-show="cluster.enableExSecurity!='NONE'" ng-class="{ 'has-error': clusterCreationForm.kerberos_kerberosRealm.$dirty && clusterCreationForm.kerberos_kerberosRealm.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_kerberosRealm">{{msg.cluster_form_kerberos_kerberosRealm}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_kerberosRealm" class="form-control" ng-model="cluster.kerberos.realm" id="kerberos_kerberosRealm" placeholder="{{msg.cluster_form_kerberos_kerberosRealm_placeholder}}" ng-required="cluster.enableExSecurity!='NONE'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_kerberosRealm.$dirty && clusterCreationForm.kerberos_kerberosRealm.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_kerberosRealm}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_kerberossLdapUrl" ng-show="cluster.enableExSecurity=='AD-KERB'" ng-class="{ 'has-error': clusterCreationForm.kerberos_kerberossLdapUrl.$dirty && clusterCreationForm.kerberos_kerberossLdapUrl.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_kerberossLdapUrl">{{msg.cluster_form_kerberos_kerberosLdapUrl}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_kerberossLdapUrl" class="form-control" ng-model="cluster.kerberos.ldapUrl" id="kerberos_kerberossLdapUrl" placeholder="{{msg.cluster_form_kerberos_kerberosLdapUrl_placeholder}}" ng-required="cluster.enableExSecurity=='AD-KERB'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_kerberossLdapUrl.$dirty && clusterCreationForm.kerberos_kerberossLdapUrl.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_kerberosLdapUrl}}
        </div>
    </div>
</div>
<div class="form-group" name="kerberos_kerberosContainerDn" ng-show="cluster.enableExSecurity=='AD-KERB'" ng-class="{ 'has-error': clusterCreationForm.kerberos_kerberosContainerDn.$dirty && clusterCreationForm.kerberos_kerberosContainerDn.$invalid }">
    <label class="col-sm-3 control-label" for="kerberos_kerberosContainerDn">{{msg.cluster_form_kerberos_kerberosContainerDn}}</label>
    <div class="col-sm-8">
        <input type="string" name="kerberos_kerberosContainerDn" class="form-control" ng-model="cluster.kerberos.containerDn" id="kerberos_kerberosContainerDn" placeholder="{{msg.cluster_form_kerberos_kerberosContainerDn_placeholder}}" ng-required="cluster.enableExSecurity=='AD-KERB'">
        <div class="help-block" ng-show="clusterCreationForm.kerberos_kerberosContainerDn.$dirty && clusterCreationForm.kerberos_kerberosContainerDn.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_kerberos_kerberosContainerDn}}
        </div>
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
                <button type="button" class="btn btn-sm btn-default" ng-disabled="!cluster.name || !cluster.region || !cluster.networkId" ng-click="showWizardActualElement('configureHostGroups')">{{msg.cluster_form_ambari_blueprint_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
        </div>
    </div>
</div>