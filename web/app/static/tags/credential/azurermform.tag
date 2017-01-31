<div ng-hide="interactiveLoginResult != null">
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azurecredentialtype">{{msg.credential_select}}</label>
        <div class="col-sm-3">
            <select class="form-control" id="azurecredentialtype" name="azurecredentialtype" ng-model="credentialAzureRm.parameters.selector"
                    ng-options="option.id as option.name for option in $root.config.AZURE_SELECTOR"
                    ng-init="credentialAzureRm.parameters.selector == 'interactive'">
            </select>
        </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': azureRmCredentialForm.cname.$dirty && azureRmCredentialForm.cname.$invalid }">
        <label class="col-sm-3 control-label" for="cname">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-z][a-z0-9]*$/" name="cname" id="cname" ng-model="credentialAzureRm.name" ng-minlength="5"
                   ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
            <div class="help-block" ng-show="azureRmCredentialForm.cname.$dirty && azureRmCredentialForm.cname.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': azureRmCredentialForm.cdescription.$dirty && azureRmCredentialForm.cdescription.$invalid }">
        <label class="col-sm-3 control-label" for="cdescription">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-model="credentialAzureRm.description" id="cdescription" name="cdescription" ng-maxlength="1000"
                   placeholder="{{msg.credential_form_description_placeholder}}">
            <div class="help-block" ng-show="azureRmCredentialForm.cdescription.$dirty && azureRmCredentialForm.cdescription.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <!-- .form-group -->
    <div class="form-group" ng-if="credentialAzureRm.parameters.selector=='app-based'"
         ng-class="{ 'has-error': azureRmCredentialForm.csubscriptionId.$dirty && azureRmCredentialForm.csubscriptionId.$invalid }">
        <label class="col-sm-3 control-label" for="csubscriptionId">{{msg.credential_azure_form_subscription_id_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="credentialAzureRm.parameters.subscriptionId" name="csubscriptionId"
                   ng-model="credentialAzureRm.parameters.subscriptionId" required placeholder="{{msg.credential_azure_form_subscription_id_placeholder}}">
            <div class="help-block" ng-show="azureRmCredentialForm.csubscriptionId.$dirty && azureRmCredentialForm.csubscriptionId.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_subscription_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <!-- .form-group -->
    <div class="form-group" ng-if="credentialAzureRm.parameters.selector=='app-based'"
         ng-class="{ 'has-error': azureRmCredentialForm.caccesKey.$dirty && azureRmCredentialForm.caccesKey.$invalid }">
        <label class="col-sm-3 control-label" for="caccesKey">{{msg.credential_azure_rm_form_access_key_id_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="credentialAzureRm.parameters.accesKey" name="caccesKey"
                   ng-model="credentialAzureRm.parameters.accessKey" required placeholder="{{msg.credential_azure_rm_form_access_key_id_placeholder}}">
            <div class="help-block" ng-show="azureRmCredentialForm.caccesKey.$dirty && azureRmCredentialForm.caccesKey.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_accesskey_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <!-- .form-group -->
    <div class="form-group" ng-if="credentialAzureRm.parameters.selector=='app-based'"
         ng-class="{ 'has-error': azureRmCredentialForm.csecretKey.$dirty && azureRmCredentialForm.csecretKey.$invalid }">
        <label class="col-sm-3 control-label" for="csecretKey">{{msg.credential_azure_rm_form_secret_key_id_label}}</label>

        <div class="col-sm-9">
            <input type="password" class="form-control" id="credentialAzureRm.parameters.secretKey" name="csecretKey"
                   ng-model="credentialAzureRm.parameters.secretKey" required placeholder="{{msg.credential_azure_rm_form_secret_key_id_placeholder}}">
            <div class="help-block" ng-show="azureRmCredentialForm.csecretKey.$dirty && azureRmCredentialForm.csecretKey.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_secretkey_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <!-- .form-group -->
    <div class="form-group" ng-if="credentialAzureRm.parameters.selector=='app-based'"
         ng-class="{ 'has-error': azureRmCredentialForm.ctenantId.$dirty && azureRmCredentialForm.ctenantId.$invalid }">
        <label class="col-sm-3 control-label" for="ctenantId">{{msg.credential_azure_rm_form_tenant_id_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="credentialAzureRm.parameters.tenantId" name="ctenantId" ng-model="credentialAzureRm.parameters.tenantId"
                   required placeholder="{{msg.credential_azure_rm_form_tenant_id_placeholder}}">
            <div class="help-block" ng-show="azureRmCredentialForm.ctenantId.$dirty && azureRmCredentialForm.ctenantId.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_tenantid_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': azureRmCredentialForm.azure_sshPublicKey.$dirty && azureRmCredentialForm.azure_sshPublicKey.$invalid }">
        <label class="col-sm-3 control-label" for="azure_sshPublicKey">{{msg.credential_azure_form_ssh_cert_label}}</label>

        <div class="col-sm-9">
            <textarea rows="4" type="text" ng-attr-placeholder="{{msg.credential_azure_form_ssh_cert_placeholder}}" class="form-control"
                      ng-model="credentialAzureRm.publicKey" name="azure_sshPublicKey" id="azure_sshPublicKey" required></textarea>
            <div class="help-block" ng-show="azureRmCredentialForm.azure_sshPublicKey.$dirty && azureRmCredentialForm.azure_sshPublicKey.$invalid">
                <i class="fa fa-warning"></i> {{msg.credential_ssh_key_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
        <div class="col-sm-8">
            <select class="form-control" id="topologySelect" name="topologySelect" ng-model="credentialAzureRm.topologyId"
                    ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
                <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
            </select>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="azureCred_publicInAccount">{{msg.public_in_account_label}}</label>
        <div class="col-sm-9">
            <input type="checkbox" name="azureCred_publicInAccount" id="azureCred_publicInAccount" ng-model="credentialAzureRm.public">
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div ng-if="credentialAzureRm.parameters.selector=='app-based'" class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createAppBasedAzureRmCredential" ng-disabled="azureRmCredentialForm.$invalid" class="btn btn-success btn-block"
               ng-click="createAzureRmCredential()" role="button"><i
                    class="fa fa-plus fa-fw"></i>
                {{msg.credential_form_create}}</a>
        </div>
    </div>

    <div ng-if="credentialAzureRm.parameters.selector=='interactive'" class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createInteractiveAzureRmCredential" ng-disabled="azureRmCredentialForm.$invalid" class="btn btn-success btn-block"
               ng-click="azureInteractiveLogin()" role="button"><i
                    class="fa fa-fw"></i>
                {{msg.credential_form_interactive_login}}</a>
        </div>
    </div>
</div>

<div id="interactive-login" ng-if="interactiveLoginResult != null" class="panel-body form-horizontal">
    <div class="form-group" ng-show="!defaultCredentialError">
        <div class="col-sm-8 col-sm-offset-2">
            <label class="col-sm-5 control-label">{{msg.credential_interactive_copy}}</label>
            <div class="high col-sm-3 text-center" onclick="document.getElementById('interactive-code').select(); document.execCommand('copy');">
                <input id="interactive-code" readonly="readonly" ng-value="interactiveLoginResult.user_code"/>
                <i class="fa fa-clipboard" aria-hidden="true"></i>
            </div>
            <div class="col-sm-4">
                <a target="_blank" class="btn btn-success btn-block"
                   ng-href="{{interactiveLoginResult.verification_url}}">Azure login</a>
            </div>
        </div>
    </div>
    <div class="row row col-sm-offset-2 col-sm-8" ng-show="defaultCredentialError">
        <span class="text-center text-danger"><b>{{defaultCredentialError}}</b></span>
    </div>
</div>