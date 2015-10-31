<div class="form-group" ng-show="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM' || activeCredential.cloudPlatform == 'GCP'">
    <label class="col-sm-3 control-label" for="selectFileSystem">{{msg.filesystem_label}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="selectFileSystem" ng-model="cluster.fileSystem.type">
            <option value="LOCAL">{{msg.filesystem_local_label}}</option>
            <option value="DASH" ng-if="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM'">{{msg.filesystem_dash_label}}</option>
            <option value="WASB" ng-if="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM'">{{msg.filesystem_wasb_label}}</option>
            <option value="GCS" ng-if="activeCredential.cloudPlatform == 'GCP'">{{msg.filesystem_gcs_label}}</option>
        </select>

        <div class="help-block" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM') &&  cluster.fileSystem.type == 'LOCAL'"><i class="fa fa-warning"></i> {{msg.filesystem_local_label_azure_warning}}</div>
        <div class="help-block" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM') &&  cluster.fileSystem.type == 'WASB'"><i class="fa fa-warning"></i> {{msg.filesystem_wasb_label_azure_warning}}</div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.armaccountname.$dirty && clusterCreationForm.armaccountname.$invalid }" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM') && cluster.fileSystem.type == 'DASH'">
    <label class="col-sm-3 control-label" for="armaccountname">{{msg.filesystem_azure_account_name_label}}
        <i class="fa fa-question-circle" popover-placement="top" popover={{msg.filesystem_azure_account_name_label_popover}} popover-trigger="mouseenter"></i>
    </label>

    <div class="col-sm-9">
        <input class="form-control" type="text" name="armaccountname" id="armaccountname" ng-model="cluster.fileSystem.properties.accountName" ng-pattern="/^[a-z0-9]{3,24}$/" ng-minlength="3" ng-maxlength="24" ng-required="cluster.fileSystem.type == 'DASH'">
        <div class="help-block" ng-show="clusterCreationForm.armaccountname.$dirty && clusterCreationForm.armaccountname.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_azure_account_name_warning}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM') && cluster.fileSystem.type == 'DASH'" ng-class="{ 'has-error': clusterCreationForm.armaccountkey.$dirty && clusterCreationForm.armaccountkey.$invalid }">
    <label class="col-sm-3 control-label" for="armaccountkey">{{msg.filesystem_azure_account_key_label}}</label>
    <div class="col-sm-9">
        <input class="form-control" type="text" name="armaccountkey" id="armaccountkey" ng-model="cluster.fileSystem.properties.accountKey" ng-pattern="/^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$/" ng-required="cluster.fileSystem.type == 'DASH'">
        <div class="help-block" ng-show="clusterCreationForm.armaccountkey.$dirty && clusterCreationForm.armaccountkey.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_azure_account_key_warning}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.wasbaccountname.$dirty && clusterCreationForm.wasbaccountname.$invalid }" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM') && cluster.fileSystem.type == 'WASB'">
    <label class="col-sm-3 control-label" for="wasbaccountname">{{msg.filesystem_wasb_account_name_label}}
        <i class="fa fa-question-circle" popover-placement="top" popover={{msg.filesystem_azure_account_name_label_wasb_popover}} popover-trigger="mouseenter"></i>
    </label>

    <div class="col-sm-9">
        <input class="form-control" type="text" name="wasbaccountname" id="wasbaccountname" ng-model="cluster.fileSystem.properties.accountName" ng-pattern="/^[a-z0-9]{3,24}$/" ng-minlength="3" ng-maxlength="24" ng-required="cluster.fileSystem.type == 'WASB'">
        <div class="help-block" ng-show="clusterCreationForm.wasbaccountname.$dirty && clusterCreationForm.wasbaccountname.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_azure_wasb_account_name_warning}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM') && cluster.fileSystem.type == 'WASB'" ng-class="{ 'has-error': clusterCreationForm.wasbaccountkey.$dirty && clusterCreationForm.wasbaccountkey.$invalid }">
    <label class="col-sm-3 control-label" for="wasbaccountkey">{{msg.filesystem_wasb_account_key_label}}</label>
    <div class="col-sm-9">
        <input class="form-control" type="text" name="wasbaccountkey" id="wasbaccountkey" ng-model="cluster.fileSystem.properties.accountKey" ng-pattern="/^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$/" ng-required="cluster.fileSystem.type == 'WASB'">
        <div class="help-block" ng-show="clusterCreationForm.wasbaccountkey.$dirty && clusterCreationForm.wasbaccountkey.$invalid"><i class="fa fa-warning"></i> {{msg.filesystem_azure_wasb_account_key_warning}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type == 'GCS'">
    <label class="col-sm-3 control-label" for="projectId">{{msg.credential_gcp_form_project_id_label}}</label>
    <div class="col-sm-9">
        <input class="form-control" type="text" id="projectId" disabled ng-model="cluster.fileSystem.properties.projectId">
    </div>
</div>


<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type == 'GCS'">
    <label class="col-sm-3 control-label" for="serviceAccountEmail">{{msg.credential_gcp_form_service_account_label}}</label>
    <div class="col-sm-9">
        <input class="form-control" type="text" id="serviceAccountEmail" disabled ng-model="cluster.fileSystem.properties.serviceAccountEmail">
    </div>
</div>

<div class="form-group" ng-show="false">
    <label class="col-sm-3 control-label" for="privateKeyEncoded">{{msg.credential_gcp_form_p12_label}}</label>
    <div class="col-sm-9">
        <input class="form-control" type="text" id="privateKeyEncoded" disabled ng-model="cluster.fileSystem.properties.privateKeyEncoded">
    </div>
</div>


<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type == 'GCS'">
    <label class="col-sm-3 control-label" for="defaultBucketName">{{msg.filesystem_gcs_bucket_label}}</label>
    <div class="col-sm-9">
        <input class="form-control" type="text" id="defaultBucketName" ng-model="cluster.fileSystem.properties.defaultBucketName">
    </div>
</div>

<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM' || activeCredential.cloudPlatform == 'GCP') && cluster.fileSystem.type != 'LOCAL'">
    <label class="col-sm-3 control-label" for="asdefaultfs">{{msg.filesystem_default_fs}}</label>
    <div class="col-sm-9">
        <input type="checkbox" id="asdefaultfs" ng-model="cluster.fileSystem.defaultFs" ng-disabled="activeCredential.cloudPlatform == 'GCP'" name="asdefaultfs">
    </div>
</div>
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
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureFailureAction')">{{msg.cluster_form_ambari_failure_tag}} <i class="fa fa-angle-double-right"></i></button>
    </div>
</div>