<div class="form-group" ng-show="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'GCP'">
    <label class="col-sm-3 control-label" for="selectFileSystem">{{msg.filesystem_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectFileSystem" ng-change="clearWasb()" ng-model="cluster.fileSystem.type">
            <option value="LOCAL">{{msg.filesystem_local_label}}</option>
            <option value="WASB" ng-if="activeCredential.cloudPlatform == 'AZURE'">{{msg.filesystem_wasb_label}}</option>
            <option value="GCS" ng-if="activeCredential.cloudPlatform == 'GCP'">{{msg.filesystem_gcs_label}}</option>
            <option value="ADLS" ng-if="activeCredential.cloudPlatform == 'AZURE'">{{msg.filesystem_adls_label}}</option>
        </select>

        <div class="help-block" ng-show="(activeCredential.cloudPlatform == 'AZURE') &&  cluster.fileSystem.type == 'WASB'"><i class="fa fa-warning"></i> {{msg.filesystem_wasb_label_azure_warning}}</div>
        <div class="help-block" ng-show="(activeCredential.cloudPlatform == 'AZURE') &&  cluster.fileSystem.type == 'ADLS'"><i class="fa fa-warning"></i> {{msg.filesystem_adls_label_azure_warning}}</div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.wasbaccountname.$dirty && clusterCreationForm.wasbaccountname.$invalid }" ng-show="(activeCredential.cloudPlatform == 'AZURE') && cluster.fileSystem.type == 'WASB'">
    <label class="col-sm-3 control-label" for="wasbaccountname">{{msg.filesystem_wasb_account_name_label}}
        <i class="fa fa-question-circle" popover-placement="top" popover={{msg.filesystem_azure_account_name_label_wasb_popover}} popover-trigger="mouseenter"></i>
    </label>

    <div class="col-sm-8">
        <input class="form-control" type="text" name="wasbaccountname" id="wasbaccountname" ng-model="cluster.fileSystem.properties.accountName" ng-pattern="/^[a-z0-9]{3,24}$/" ng-minlength="3" ng-maxlength="24" ng-required="cluster.fileSystem.type == 'WASB'">
        <div class="help-block" ng-show="clusterCreationForm.wasbaccountname.$dirty && clusterCreationForm.wasbaccountname.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_azure_wasb_account_name_warning}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AZURE') && cluster.fileSystem.type == 'WASB'" ng-class="{ 'has-error': clusterCreationForm.wasbaccountkey.$dirty && clusterCreationForm.wasbaccountkey.$invalid }">
    <label class="col-sm-3 control-label" for="wasbaccountkey">{{msg.filesystem_wasb_account_key_label}}</label>
    <div class="col-sm-8">
        <input class="form-control" type="text" name="wasbaccountkey" id="wasbaccountkey" ng-model="cluster.fileSystem.properties.accountKey" ng-pattern="/^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$/" ng-required="cluster.fileSystem.type == 'WASB'">
        <div class="help-block" ng-show="clusterCreationForm.wasbaccountkey.$dirty && clusterCreationForm.wasbaccountkey.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_azure_wasb_account_key_warning}}
        </div>
    </div>
</div>
  <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.adlsaccountname.$dirty && clusterCreationForm.adlsaccountname.$invalid }" ng-show="(activeCredential.cloudPlatform == 'AZURE') && cluster.fileSystem.type == 'ADLS'">
    <label class="col-sm-3 control-label" for="adlsaccountname">{{msg.filesystem_adls_account_name_label}}
        <i class="fa fa-question-circle" popover-placement="top" popover={{msg.filesystem_adls_account_name_label_popover}} popover-trigger="mouseenter"></i>
    </label>

    <div class="col-sm-8">
        <input class="form-control" type="text" name="adlsaccountname" id="adlsaccountname" ng-model="cluster.fileSystem.properties.accountName" ng-pattern="/^[a-z0-9]{3,24}$/" ng-minlength="3" ng-maxlength="24" ng-required="cluster.fileSystem.type == 'ADLS'">
        <div class="help-block" ng-show="clusterCreationForm.adlsaccountname.$dirty && clusterCreationForm.adlsaccountname.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_adls_account_name_warning}}
        </div>
        <div class="help-block" ng-show="cluster.fileSystem.properties.accountName"><i class="fa fa-warning"></i> {{printAdlsAclWarning()}} Check the Azure Portal <a href="https://portal.azure.com/#blade/Microsoft_Azure_DataLakeStore/WebHdfsFolderBlade/endpoint/{{cluster.fileSystem.properties.accountName}}.azuredatalakestore.net/path/%2F">here</a> and the documentation <a href="https://docs.microsoft.com/en-us/azure/data-lake-store/data-lake-store-access-control">here</a>
        </div>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type == 'GCS'">
    <label class="col-sm-3 control-label" for="projectId">{{msg.credential_gcp_form_project_id_label}}</label>
    <div class="col-sm-8">
        <input class="form-control" type="text" id="projectId" disabled ng-model="cluster.fileSystem.properties.projectId">
    </div>
</div>


<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type == 'GCS'">
    <label class="col-sm-3 control-label" for="serviceAccountEmail">{{msg.credential_gcp_form_service_account_label}}</label>
    <div class="col-sm-8">
        <input class="form-control" type="text" id="serviceAccountEmail" disabled ng-model="cluster.fileSystem.properties.serviceAccountEmail">
    </div>
</div>

<div class="form-group" ng-show="false">
    <label class="col-sm-3 control-label" for="privateKeyEncoded">{{msg.credential_gcp_form_p12_label}}</label>
    <div class="col-sm-8">
        <input class="form-control" type="text" id="privateKeyEncoded" disabled ng-model="cluster.fileSystem.properties.privateKeyEncoded">
    </div>
</div>


<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type == 'GCS'">
    <label class="col-sm-3 control-label" for="defaultBucketName">{{msg.filesystem_gcs_bucket_label}}</label>
    <div class="col-sm-8">
        <input class="form-control" type="text" id="defaultBucketName" ng-model="cluster.fileSystem.properties.defaultBucketName">
    </div>
</div>

<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AZURE') && cluster.fileSystem.type != 'LOCAL' && cluster.fileSystem.type != 'ADLS'">
    <label class="col-sm-3 control-label" for="asdefaultfs">{{msg.filesystem_default_fs}}</label>
    <div class="col-sm-8">
        <input type="checkbox" id="asdefaultfs" ng-model="cluster.fileSystem.defaultFs" ng-disabled="activeCredential.cloudPlatform == 'GCP'" name="asdefaultfs">
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE' && cluster.orchestrator.type !== 'SALT'">
    <label class="col-sm-3 control-label" for="relocateDocker">{{msg.relocate_docker_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" id="relocateDocker" ng-model="cluster.relocateDocker" name="relocateDocker">
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'">
    <label class="col-sm-3 control-label" for="attachedstoragetype">{{msg.attached_storage_type}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="attachedstoragetype" ng-model="cluster.parameters.attachedStorageOption">
            <option value="SINGLE">{{msg.single_storage_label}}</option>
            <option value="PER_VM">{{msg.per_vm_storage_label}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-if="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'" ng-class="{ 'has-error': clusterCreationForm.persistentstoragename.$dirty && clusterCreationForm.persistentstoragename.$invalid }">
    <label class="col-sm-3 control-label" for="persistentstoragename">{{msg.persistent_storage_name}}</label>
    <div class="col-sm-8">
        <input class="form-control" type="text" id="persistentstoragename" name="persistentstoragename" ng-model="cluster.parameters.persistentStorage" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="4" ng-maxlength="20" ng-required="($root.activeCredential.cloudPlatform=='AZURE' && cluster.parameters.persistentStorage != '' && cluster.parameters.persistentStorage)">
        <div class="help-block" ng-show="clusterCreationForm.persistentstoragename.$dirty && clusterCreationForm.persistentstoragename.$invalid"><i class="fa fa-warning"></i> {{msg.persistent_storage_name_invalid}}</div>
    </div>
</div>

<div class="form-group">
    <div class="col-sm-11">

        <div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureHostGroups')"><i class="fa fa-angle-double-left"></i> {{msg.cluster_form_ambari_blueprint_tag}}</button>
            </div>
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group" ng-hide="clusterCreationForm.$invalid || showAdvancedOptionForm">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureReview')">{{msg.cluster_form_ambari_launch_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
            <div class="btn-group" role="group" ng-show="showAdvancedOptionForm">
                <button type="button" class="btn btn-sm btn-default" ng-disabled="!cluster.name || !cluster.region || !cluster.networkId || !cluster.blueprintId" ng-click="showWizardActualElement('configureFailureAction')">{{msg.cluster_form_ambari_failure_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
        </div>
    </div>
</div>