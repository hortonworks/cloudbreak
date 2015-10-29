<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterName">{{msg.cluster_form_name_label}}</label>
    <div class="col-sm-9">
        <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" placeholder="{{msg.cluster_form_name_placeholder}}" ng-model="cluster.name" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="40" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterUserName">{{msg.cluster_form_ambari_user_label}}</label>
    <div class="col-sm-9">
        <input type="text" name="cl_clusterUserName" class="form-control" id="cl_clusterUserName" placeholder="{{msg.cluster_form_ambari_user_placeholder}}" ng-model="cluster.userName" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="15" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_user_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterPass">{{msg.cluster_form_ambari_password_label}}</label>
    <div class="col-sm-9">
        <input type="text" name="cl_clusterPass" class="form-control" id="cl_clusterPass" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-model="cluster.password" ng-minlength="5" ng-maxlength="50" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_password_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="selectRegion">{{msg.cluster_form_region_label}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="selectRegion" ng-model="cluster.region" required ng-show="activeCredential.cloudPlatform == 'AWS'">
            <option ng-repeat="region in $root.config.AWS.awsRegions" value="{{region.key}}">{{region.value}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" required ng-show="activeCredential.cloudPlatform == 'AZURE'">
            <option ng-repeat="region in $root.config.AZURE.azureRegions" value="{{region.key}}">{{region.value}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'GCP'">
            <option ng-repeat="region in $root.config.GCP.gcpRegions" value="{{region.key}}">{{region.value}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
            <option ng-repeat="region in $root.config.OPENSTACK.regions" value="{{region.key}}">{{region.value}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'AZURE_RM'">
            <option ng-repeat="region in $root.config.AZURE_RM.azureRegions" value="{{region.key}}">{{region.value}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="activeCredential.cloudPlatform == 'AWS' && cluster.region && showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="selectavailabilityZone">{{msg.availability_zone}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="selectavailabilityZone" ng-model="cluster.availabilityZone">
            <option ng-repeat="avZone in avZones" value="{{avZone}}">{{avZone}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && cluster.platformVariant">
    <label class="col-sm-3 control-label" for="platformVariant">{{msg.cluster_form_platform_variant_label}}</label>
    <div class="col-sm-3">
        <select class="form-control" id="platformVariant" ng-model="cluster.platformVariant">
            <option ng-repeat="variant in getPlatformVariants()" value="{{variant}}">{{variant}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="emailneeded">{{msg.cluster_form_email_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" id="emailneeded" ng-model="cluster.email" name="emailneeded">
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="cluster_publicInAccount">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="cluster_publicInAccount" id="cluster_publicInAccount" ng-model="cluster.public">
    </div>
</div>
<div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
    <div class="btn-group" role="group" style="opacity: 0;">
        <button type="button" class="btn btn-default"></button>
    </div>
    <div class="btn-group" role="group" style="opacity: 0;">
        <button type="button" class="btn btn-default"></button>
    </div>
    <div class="btn-group" role="group">
        <button type="button" class="btn btn-default" ng-click="showWizardActualElement('configureSecurity')">Configure Network and Security <i class="fa fa-angle-double-right"></i></button>
    </div>
</div>