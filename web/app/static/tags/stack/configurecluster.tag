<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterName">{{msg.cluster_form_name_label}}</label>
    <div class="col-sm-8">
        <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" placeholder="{{msg.cluster_form_name_placeholder}}" ng-model="cluster.name" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="40" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterUserName">{{msg.cluster_form_ambari_user_label}}</label>
    <div class="col-sm-8">
        <input type="text" name="cl_clusterUserName" class="form-control" id="cl_clusterUserName" placeholder="{{msg.cluster_form_ambari_user_placeholder}}" ng-model="cluster.userName" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="15" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_user_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterPass">{{msg.cluster_form_ambari_password_label}}</label>
    <div class="col-sm-8">
        <input type="text" name="cl_clusterPass" class="form-control" id="cl_clusterPass" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-model="cluster.password" ng-minlength="5" ng-maxlength="50" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_password_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="activeCredential">
    <label class="col-sm-3 control-label" for="selectRegion">{{msg.cluster_form_region_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-required="activeCredential !== undefined" ng-show="activeCredential.cloudPlatform == 'AWS'">
            <option ng-repeat="region in $root.params.regions.AWS" value="{{region}}">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, region)}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'GCP'">
            <option ng-repeat="region in $root.params.regions.GCP" value="{{region}}">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, region)}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
            <option ng-repeat="region in $root.params.regions.OPENSTACK" value="{{region}}">{{region}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'AZURE_RM'">
            <option ng-repeat="region in $root.params.regions.AZURE_RM" value="{{region}}">{{region}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AWS' && cluster.region && showAdvancedOptionForm) || (activeCredential.cloudPlatform == 'GCP' && cluster.region)">
    <label class="col-sm-3 control-label" for="selectavailabilityZone">{{msg.availability_zone}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectavailabilityZone" ng-model="cluster.availabilityZone" ng-required="activeCredential.cloudPlatform === 'GCP'">
            <option ng-repeat="avZone in avZones" value="{{avZone}}">{{avZone}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AWS'">
    <label class="col-sm-3 control-label" for="awsDedicatedInstancesRequested">{{msg.cluster_form_dedicated_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" id="awsDedicatedInstancesRequested" ng-model="cluster.parameters.dedicatedInstances" name="awsDedicatedInstancesRequested">
    </div>
</div>

<div class="form-group" ng-show="activeCredential && showAdvancedOptionForm && cluster.platformVariant && getPlatformVariants().length > 1">
    <label class="col-sm-3 control-label" for="platformVariant">{{msg.cluster_form_platform_variant_label}}</label>
    <div class="col-sm-3">
        <select class="form-control" id="platformVariant" ng-model="cluster.platformVariant" ng-options="variant as variant for variant in getPlatformVariants()"></select>
    </div>
</div>

<div class="form-group" ng-show="activeCredential && showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="provisionCluster">Provision cluster </label>
    <div class="col-sm-8">
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-required="activeCredential !== undefined" ng-show="activeCredential.cloudPlatform == 'AWS'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.AWS" value="{{orchestrator}}">{{$root.displayNames.getPropertyName('orchestrators', orchestrator)}}</option>
        </select>
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'GCP'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.GCP" value="{{orchestrator}}">{{$root.displayNames.getPropertyName('orchestrators', orchestrator)}}</option>
        </select>
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.OPENSTACK" value="{{orchestrator}}">{{$root.displayNames.getPropertyName('orchestrators', orchestrator)}}</option>
        </select>
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'AZURE_RM'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.AZURE_RM" value="{{orchestrator}}">{{$root.displayNames.getOrchestrator('orchestrators', orchestrator)}}</option>
        </select>
    </div>
</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="enableShipyard">{{msg.shipyard_enabled_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" id="enableShipyard" ng-model="cluster.enableShipyard" name="enableShipyard">
    </div>
</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="emailneeded">{{msg.cluster_form_email_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" id="emailneeded" ng-model="cluster.email" name="emailneeded">
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="cluster_publicInAccount">{{msg.public_in_account_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_publicInAccount" id="cluster_publicInAccount" ng-model="cluster.public">
    </div>
</div>



<div class="form-group">
    <div class="col-sm-11">
        <div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-sm btn-default" ng-disabled="!cluster.name || (activeCredential !== undefined && !cluster.region)" ng-click="activeStack === undefined ? showWizardActualElement('configureSecurity') : showWizardActualElement('configureHostGroups')">
                    {{activeStack === undefined ? msg.cluster_form_ambari_network_tag : msg.cluster_form_ambari_blueprint_tag}} <i class="fa fa-angle-double-right"></i>
                </button>
            </div>
        </div>
    </div>

</div>