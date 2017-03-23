<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
    <label class="col-sm-3 control-label" for="cl_clusterName">{{msg.cluster_form_name_label}}</label>
    <div class="col-sm-8">
        <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" placeholder="{{msg.cluster_form_name_placeholder}}" ng-model="cluster.name" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="40" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_name_invalid}}
        </div>
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.userDefinedTags.$dirty && clusterCreationForm.userDefinedTags.$invalid }">
    <label class="col-sm-3 control-label" for="userDefinedTags">Tags</label>
    <div class="col-sm-8" name="userDefinedTags" id="userDefinedTags">
        <div class="col-sm-12" ng-repeat="tag in cluster.userDefinedTags" style="padding-bottom: 15px;    padding-left: 0px;" ng-class="{ 'has-error': (clusterCreationForm.tagname{{$index}}.$dirty && clusterCreationForm.tagname{{$index}}.$invalid) || (clusterCreationForm.tagkey{{$index}}.$dirty && clusterCreationForm.tagkey{{$index}}.$invalid) }">
            <div>
                <div class="form-inline">

                    <div class="col-md-4 input-group" >
                        <span class="input-group-addon">key</span>
                        <input type="text" class="form-control" id="tagkey{{$index}}" name="tagkey{{$index}}" required ng-model="tag.key" ng-maxlength="127" ng-minlength="3" ng-required="true" placeholder="(REQUIRED) Max 127 chars">
                    </div>
                    <div class="col-md-offset-1 col-md-4 input-group">
                        <span class="input-group-addon">value</span>
                        <input type="text" class="form-control" id="tagname{{$index}}" name="tagname{{$index}}" required ng-model="tag.value" ng-maxlength="255" ng-minlength="3" ng-required="true" placeholder="(REQUIRED) Max 255 chars">
                    </div>
                    <div class="col-md-2 pull-right">
                        <a class="btn btn-info btn-block" role="button" ng-click="removeUserDefinedTag(tag)" style="margin-top: 0px;margin-bottom: 0px;"> - Remove</a>
                    </div>
                </div>
            </div>
            <div>
                <div class="help-block" ng-show="(clusterCreationForm.tagname{{$index}}.$dirty && clusterCreationForm.tagname{{$index}}.$invalid) || (clusterCreationForm.tagkey{{$index}}.$dirty && clusterCreationForm.tagkey{{$index}}.$invalid)">
                    <i class="fa fa-warning"></i> Please set the custom tag because it is required
                </div>
            </div>
        </div>
        <div class="row col-md-4" style="padding-top: 0px;padding-bottom: 0px;">
            <button type="button" class="btn btn-success btn-block" role="button" ng-disabled="isUserDefinedTagsInvalid()" ng-click="addUserDefinedTag()"> + Add</button>
        </div>
    </div>

    <!-- .col-sm-9 -->
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
        <input type="text" name="cl_clusterPass" class="form-control" id="cl_clusterPass" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-model="cluster.password" ng-minlength="5" ng-maxlength="100" required>
        <div class="help-block" ng-show="clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_password_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="activeCredential">
    <label class="col-sm-3 control-label" for="selectRegion">{{msg.cluster_form_region_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'AWS'">
            <option ng-repeat="region in $root.params.regions.AWS" value="{{region}}">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, region)}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'GCP'">
            <option ng-repeat="region in $root.params.regions.GCP" value="{{region}}">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, region)}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
            <option ng-repeat="region in $root.params.regions.OPENSTACK" value="{{region}}">{{region}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'AZURE'">
            <option ng-repeat="region in $root.params.regions.AZURE" value="{{region}}">{{region}}</option>
        </select>
        <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'BYOS'">
            <option ng-repeat="region in $root.params.regions.BYOS" value="{{region}}">{{region}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="(activeCredential.cloudPlatform == 'AWS' && cluster.region && showAdvancedOptionForm) || (activeCredential.cloudPlatform == 'GCP' && cluster.region) || (activeCredential.cloudPlatform == 'OPENSTACK' && cluster.region)">
    <label class="col-sm-3 control-label" for="selectavailabilityZone">{{msg.availability_zone}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectavailabilityZone" ng-model="cluster.availabilityZone" ng-required="(activeCredential.cloudPlatform === 'GCP') || (activeCredential.cloudPlatform === 'OPENSTACK')">
            <option ng-repeat="avZone in avZones" value="{{avZone}}">{{avZone}}</option>
        </select>
    </div>
</div>

<div class="form-group" ng-show="activeCredential && cluster.platformVariant && getPlatformVariants().length > 1">
    <label class="col-sm-3 control-label" for="platformVariant">{{msg.cluster_form_platform_variant_label}}</label>
    <div class="col-sm-3">
        <select class="form-control" id="platformVariant" ng-model="cluster.platformVariant" ng-options="variant as variant for variant in getPlatformVariants()"></select>
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AWS'">
    <label class="col-sm-3 control-label" for="awsDedicatedInstancesRequested">{{msg.cluster_form_dedicated_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" id="awsDedicatedInstancesRequested" ng-model="cluster.parameters.dedicatedInstances" name="awsDedicatedInstancesRequested">
    </div>
</div>

<div class="form-group" ng-show="activeCredential && showAdvancedOptionForm && activeCredential.cloudPlatform !== 'BYOS'">
    <label class="col-sm-3 control-label" for="provisionCluster">Provision cluster </label>
    <div class="col-sm-8">
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'AWS'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.AWS" value="{{orchestrator}}">{{$root.displayNames.getPropertyName('orchestrators', orchestrator)}}</option>
        </select>
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'GCP'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.GCP" value="{{orchestrator}}">{{$root.displayNames.getPropertyName('orchestrators', orchestrator)}}</option>
        </select>
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.OPENSTACK" value="{{orchestrator}}">{{$root.displayNames.getPropertyName('orchestrators', orchestrator)}}</option>
        </select>
        <select class="form-control" id="provisionCluster" ng-model="cluster.orchestrator.type" ng-show="activeCredential.cloudPlatform == 'AZURE'">
            <option ng-repeat="orchestrator in $root.params.orchestrators.AZURE" value="{{orchestrator}}">{{$root.displayNames.getOrchestrator('orchestrators', orchestrator)}}</option>
        </select>
    </div>
</div>

<div class="form-group" ng-show="activeCredential && showAdvancedOptionForm && activeCredential.cloudPlatform === 'BYOS'">
    <label class="col-sm-3 control-label" for="provisionCluster">Provision cluster </label>
    <div class="col-sm-8">
        <label class="control-label" for="provisionCluster">{{activeCredential.parameters.type}}</label>
    </div>
</div>


<div class="form-group" ng-show="showAdvancedOptionForm && $root.params.specialParameters.enableCustomImage == true && activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS'">
    <label class="col-sm-3 control-label" for="custom_image">{{msg.cluster_form_custom_image}} <i class="fa fa-question-circle" popover-placement="top" popover={{msg.use_custom_image_popup}} popover-trigger="mouseenter"></i></label>
    <div class="col-sm-8">
        <input type="checkbox" name="custom_image" id="custom_image" ng-model="cluster.customImage">
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && cluster.customImage && $root.params.specialParameters.enableCustomImage == true && activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS'" ng-class="{ 'has-error': clusterCreationForm.image_id.$dirty && clusterCreationForm.image_id.$invalid }">
    <label class="col-sm-3 control-label" for="image_id">{{msg.cluster_form_custom_image_id}}</label>
    <div class="col-sm-8">
        <input type="text" name="image_id" class="form-control" id="image_id" ng-model="cluster.imageId" ng-required="cluster.customImage" ng-pattern="actualRegex" placeholder="{{$root.params.images[activeCredential.cloudPlatform][cluster.region]}}" >
        <div class="help-block" ng-show="$parent.clusterCreationForm.image_id.$dirty && $parent.clusterCreationForm.image_id.$invalid">
            <i class="fa fa-warning"></i> {{msg.custom_image_error}}
        </div>
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && (activeStack !== undefined || activeCredential.cloudPlatform === 'BYOS')">
    <label class="col-sm-3 control-label" for="custom_container">{{msg.cluster_form_custom_container}} <i class="fa fa-question-circle" popover-placement="top" popover={{msg.use_custom_container_popup}} popover-trigger="mouseenter"></i></label>
    <div class="col-sm-8">
        <input type="checkbox" name="custom_container" id="custom_container" ng-model="cluster.customContainer">
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && cluster.customContainer && (activeStack !== undefined || activeCredential.cloudPlatform === 'BYOS')" ng-class="{ 'has-error': clusterCreationForm.ambari_server_container_id.$dirty && clusterCreationForm.ambari_server_container_id.$invalid }">
    <label class="col-sm-3 control-label" for="ambari_server_container_id">{{msg.cluster_form_custom_ambariserver_id}}</label>
    <div class="col-sm-8">
        <input type="text" name="ambari_server_container_id" class="form-control" id="ambari_server_container_id" ng-model="cluster.ambariServerId" ng-required="cluster.customContainer" placeholder="" >
        <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_server_container_id.$dirty && $parent.clusterCreationForm.ambari_server_container_id.$invalid">
            <i class="fa fa-warning"></i> {{msg.custom_container_error}}
        </div>
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && cluster.customContainer && (activeStack !== undefined || activeCredential.cloudPlatform === 'BYOS')" ng-class="{ 'has-error': clusterCreationForm.ambari_agent_container_id.$dirty && clusterCreationForm.ambari_agent_container_id.$invalid }">
    <label class="col-sm-3 control-label" for="ambari_agent_container_id">{{msg.cluster_form_custom_ambariagent_id}}</label>
    <div class="col-sm-8">
        <input type="text" name="ambari_agent_container_id" class="form-control" id="ambari_agent_container_id" ng-model="cluster.ambariAgentId" ng-required="cluster.customContainer" placeholder="" >
        <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_agent_container_id.$dirty && $parent.clusterCreationForm.ambari_agent_container_id.$invalid">
            <i class="fa fa-warning"></i> {{msg.custom_container_error}}
        </div>
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && cluster.customContainer && (activeStack !== undefined || activeCredential.cloudPlatform === 'BYOS')" ng-class="{ 'has-error': clusterCreationForm.ambari_db_container_id.$dirty && clusterCreationForm.ambari_db_container_id.$invalid }">
    <label class="col-sm-3 control-label" for="ambari_db_container_id">{{msg.cluster_form_custom_ambaridb_id}}</label>
    <div class="col-sm-8">
        <input type="text" name="ambari_db_container_id" class="form-control" id="ambari_db_container_id" ng-model="cluster.ambariDbId" ng-required="cluster.customContainer" placeholder="" >
        <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_db_container_id.$dirty && $parent.clusterCreationForm.ambari_db_container_id.$invalid">
            <i class="fa fa-warning"></i> {{msg.custom_container_error}}
        </div>
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'">
    <label class="col-sm-3 control-label" for="azureAvailabilitySetsEnabled">{{msg.cluster_form_enable_availabilitysets_label}} <i class="fa fa-question-circle" popover-placement="top" popover={{msg.cluster_form_availabilitysets_popup}} popover-trigger="mouseenter"></i></label>
    <div class="col-sm-8">
        <input type="checkbox" id="azureAvailabilitySetsEnabled" ng-model="cluster.parameters.azureAvailabilitySetsEnabled" name="azureAvailabilitySetsEnabled">
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': clusterCreationForm.azureAvailabilitySets.$dirty && clusterCreationForm.azureAvailabilitySets.$invalid }">
    <label class="col-sm-3 control-label" ng-show="cluster.parameters.azureAvailabilitySetsEnabled && showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'" for="azureAvailabilitySets">{{msg.cluster_form_availabilitysets_label}} <i class="fa fa-question-circle" popover-placement="top" popover={{msg.cluster_form_availabilitysets_uniqeness}} popover-trigger="mouseenter"></i></label>
    <div class="col-sm-8" name="azureAvailabilitySets" id="azureAvailabilitySets" ng-show="cluster.parameters.azureAvailabilitySetsEnabled && showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'">
        <div class="col-sm-12" ng-repeat="as in cluster.azureAvailabilitySets track by $index" style="padding-bottom: 15px;    padding-left: 0px;" ng-class="{ 'has-error': (clusterCreationForm.asname{{$index}}.$dirty && clusterCreationForm.asname{{$index}}.$invalid) || (clusterCreationForm.asfaultdomainnumber{{$index}}.$dirty && clusterCreationForm.asfaultdomainnumber{{$index}}.$invalid) }">
            <div>
                <div class="form-inline">

                    <div class="col-md-4 input-group" >
                        <span class="input-group-addon">AS name</span>
                        <input type="text" class="form-control" id="asname{{$index}}" name="asname{{$index}}" required ng-model="as.name" ng-maxlength="80" ng-minlength="3" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-required="true" placeholder="(REQUIRED) Name of availabilty set">
                    </div>
                    <div class="col-md-offset-1 col-md-4 input-group">
                        <span class="input-group-addon">Fault domain count</span>
                        <input type="number" class="form-control" id="asfaultdomainnumber{{$index}}" name="asfaultdomainnumber{{$index}}" required ng-model="as.faultDomainCount" min="2" max="3" placeholder="{{cluster_form_availabilitysets_faultdomaincount_placeholder}}" ng-required="true" placeholder="Fault domain count">
                    </div>
                    <div class="col-md-2 pull-right">
                        <a class="btn btn-info btn-block" role="button" ng-click="removeAvailabilitySet(as)" style="margin-top: 0px;margin-bottom: 0px;"> - Remove</a>
                    </div>
                </div>
            </div>
            <div>
                <div class="help-block" ng-show="(clusterCreationForm.asname{{$index}}.$dirty && clusterCreationForm.asname{{$index}}.$invalid)">
                    <i class="fa fa-warning"></i> {{msg.cluster_form_availabilitysets_missing_name}}
                </div>
                <div class="help-block" ng-show="(clusterCreationForm.asfaultdomainnumber{{$index}}.$dirty && clusterCreationForm.asfaultdomainnumber{{$index}}.$invalid)">
                    <i class="fa fa-warning"></i> {{msg.cluster_form_availabilitysets_incorrect_fault_domain}}
                </div>
            </div>
        </div>
        <div class="row col-md-4" style="padding-top: 0px;padding-bottom: 0px;">
            <a class="btn btn-success btn-block" role="button" ng-disabled="isAvailabilitySetsInvalid()" ng-click="addAvailabilitySet()"> + Add</a>
        </div>
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
<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform !== 'BYOS'">
    <label class="col-sm-3 control-label" for="cluster_enableLifetime">{{msg.cluster_form_enable_lifetime_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_enableLifetime" id="cluster_enableLifetime" ng-model="enableLifetime" ng-change="delTimetolive(enableLifetime)">
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && enableLifetime && activeCredential.cloudPlatform !== 'BYOS'" ng-class="{ 'has-error': clusterCreationForm.cl_timetolive.$dirty && clusterCreationForm.cl_timetolive.$invalid }">
    <label class="col-sm-3 control-label" for="cl_timetolive">{{msg.cluster_form_lifetime_label}}</label>
    <div class="col-sm-8">
        <input type="number" name="cl_timetolive" class="form-control" id="cl_timetolive" placeholder="" ng-model="cluster.parameters.timetolive" min="1">
        </div>
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
                <button type="button" class="btn btn-sm btn-sm btn-default" ng-disabled="!cluster.name || isUserDefinedTagsInvalid() || isAvailabilitySetsInvalid() || (activeCredential !== undefined && !cluster.region) || (activeCredential.cloudPlatform == 'OPENSTACK' && !cluster.availabilityZone)" ng-click="(activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS') ? showWizardActualElement('configureSecurity') : showWizardActualElement('configureHostGroups')">
                    {{(activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS') ? msg.cluster_form_ambari_network_tag : msg.cluster_form_ambari_blueprint_tag}} <i class="fa fa-angle-double-right"></i>
                </button>
            </div>
        </div>
    </div>

</div>