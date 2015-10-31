<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'">
    <label class="col-sm-3 control-label" for="diskPerStorageAccount">{{msg.cluster_form_disk_per_storage_label}}</label>
    <div class="col-sm-3">
        <input class="form-control" type="number" id="diskPerStorageAccount" name="diskPerStorageAccount" ng-model="cluster.parameters.diskPerStorage" min="1" max="10000">
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="onFailureConfig">{{msg.cluster_form_onfailure_label}}</label>
    <div class="col-sm-3">
        <select class="form-control" id="onFailureConfig" ng-model="cluster.onFailureAction">
            <option value="DO_NOTHING">{{msg.cluster_form_onfailure_donothing}}</option>
            <option value="ROLLBACK">{{msg.cluster_form_onfailure_rollback}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="selectAdjustment">{{msg.cluster_form_adjustment_min_label}}</label>
    <div class="col-sm-3">
        <select class="form-control" id="bestEffort" ng-model="cluster.bestEffort" ng-change="selectedAdjustmentChange()" ng-disabled="activeCredential.cloudPlatform == 'AWS' || activeCredential.cloudPlatform == 'OPENSTACK'">
            <option value="EXACT">{{msg.cluster_form_adjustment_exact_label}}</option>
            <option value="BEST_EFFORT">{{msg.cluster_form_adjustment_best_effort_label}}</option>
        </select>
    </div>

    <div class="col-sm-6">
        <div class="col-sm-6">
            <div class="input-group col-sm-12" ng-show="cluster.bestEffort != 'BEST_EFFORT'">
                <select class="form-control" id="selectAdjustment" ng-model="cluster.failurePolicy.adjustmentType" ng-disabled="activeCredential.cloudPlatform == 'AWS' || activeCredential.cloudPlatform == 'OPENSTACK'">
                    <option value="EXACT">{{msg.cluster_form_adjustment_exact_number_label}}</option>
                    <option value="PERCENTAGE">{{msg.cluster_form_adjustment_exact_percentage_label}}</option>
                </select>
            </div>
        </div>
        <div class="col-sm-6">
            <div class="input-group" ng-show="cluster.bestEffort != 'BEST_EFFORT'">
                <span class="input-group-addon" id="basic-addon1">=</span>
                <input type="number" name="fthreshold" class="form-control" ng-model="cluster.failurePolicy.threshold" id="fthreshold" ng-disabled="activeCredential.cloudPlatform == 'AWS'">
            </div>
        </div>
    </div>
</div>
<div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
    <div class="btn-group" role="group" ng-show="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM' || activeCredential.cloudPlatform == 'GCP'">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureFileSystem')"><i class="fa fa-angle-double-left"></i> {{msg.cluster_form_ambari_filesystem_tag}}</button>
    </div>
    <div class="btn-group" role="group" ng-show="activeCredential.cloudPlatform != 'AZURE' && activeCredential.cloudPlatform != 'AZURE_RM' && activeCredential.cloudPlatform != 'GCP'">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureHostGroups')"><i class="fa fa-angle-double-left"></i> {{msg.cluster_form_ambari_blueprint_tag}}</button>
    </div>
    <div class="btn-group" role="group" style="opacity: 0;">
        <button type="button" class="btn btn-sm btn-default"></button>
    </div>
    <div class="btn-group" role="group">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureAmbariRepository')">{{msg.cluster_form_ambari_hdprepo_tag}} <i class="fa fa-angle-double-right"></i></button>
    </div>
</div>