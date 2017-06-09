<div class="form-group">
    <label class="col-sm-3 control-label" for="selectBlueprint">{{msg.cluster_form_blueprint_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectBlueprint" ng-model="cluster.blueprintId" required ng-change="selectedBlueprintChange()" ng-options="blueprint.id as blueprint.name for blueprint in $root.blueprints | orderBy:'name'">
        </select>
        <div class="help-block" ng-show="cluster.gateway.enableGateway && blueprintKnoxError"><i class="fa fa-warning"></i> {{msg.cluster_form_blueprint_knox_error}}</div>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="selectConfigStrategy">{{msg.cluster_form_config_strategy_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectConfigStrategy" ng-model="cluster.configStrategy" ng-options="v for v in configStrategies" required>
        </select>
    </div>
</div>

<div class="form-group" ng-show="showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="cluster_validateBlueprint">{{msg.cluster_form_blueprint_validate_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_validateBlueprint" id="cluster_validateBlueprint" ng-model="cluster.validateBlueprint">
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AWS'">
    <label class="col-sm-3 control-label" for="cluster_instanceProfileEnabled">{{msg.cluster_form_instanceprofile_validate_label}}</label>
    <div class="col-sm-6">
        <select class="form-control" ng-model="cluster.parameters.instanceProfileStrategy" id="cluster_instanceProfileEnabled" name="cluster_instanceProfileEnabled">
            <option ng-option value="NONE">{{$root.displayNames.getPropertyName('s3types', 'NONE')}}</option>
            <option ng-option value="CREATE">{{$root.displayNames.getPropertyName('s3types', 'CREATE')}}</option>
            <option ng-option value="USE_EXISTING">{{$root.displayNames.getPropertyName('s3types', 'USE_EXISTING')}}</option>
        </select>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AWS' && cluster.parameters.instanceProfileStrategy === 'USE_EXISTING'">
    <label class="col-sm-3 control-label" for="instanceProfile">{{msg.role_label}}</label>
    <div class="col-sm-8">
        <input type="text" name="instanceProfile" class="form-control" id="instanceProfile" ng-model="cluster.parameters.instanceProfile">
    </div>
</div>

<div class="form-group" ng-show="activeCredential && activeCredential.cloudPlatform !== 'BYOS'">
    <label class="col-sm-3 control-label" for="hostgroupconfig">{{msg.cluster_form_hostgroup_label}}</label>
    <div class="col-sm-7 col-sm-offset-1">
        <div ng-repeat="instanceGroup in cluster.instanceGroups" id="hostgroupconfig">
            <div class="row">
                <div>
                    <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                        <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                            <h3 class="panel-title">{{instanceGroup.group}}</h3>
                        </div>
                        <div class="panel-body">
                            <div class="form-group" name="templateNodeform{{$index}}">
                                <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">{{msg.cluster_form_hostgroup_group_size_label}}</label>
                                <div class="col-sm-8">
                                    <input type="number" name="templateNodeCount{{$index}}" ng-disabled="instanceGroup.type=='GATEWAY'" class="form-control" ng-model="instanceGroup.nodeCount" id="templateNodeCount{{$index}}" min="1" max="100000" placeholder="1 - 100000" ng-required="activeCredential && activeCredential.cloudPlatform !== 'BYOS'">
                                    <div class="help-block" ng-show="clusterCreationForm.templateNodeCount{{$index}}.$dirty && clusterCreationForm.templateNodeCount{{$index}}.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_size_invalid}}
                                    </div>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="templateName{{$index}}">{{msg.cluster_form_hostgroup_template_label}}</label>
                                <div class="col-sm-8">
                                    <select class="form-control" id="template-name-{{$index}}" name="template-name-{{$index}}" ng-model="instanceGroup.templateId" ng-options="template.id as template.name for template in $root.filteredTemplates | filter:filterByTopology | filter: {'cloudPlatform': activeCredential.cloudPlatform} | orderBy:'name'" ng-required="activeCredential && activeCredential.cloudPlatform !== 'BYOS'">
                                    </select>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="securityGroupNameName{{$index}}">{{msg.cluster_form_securitygroup_label}}</label>
                                <div class="col-sm-8">
                                    <select class="form-control" id="securityGroupNameName{{$index}}" name="securityGroupNameName{{$index}}" ng-model="instanceGroup.securityGroupId" ng-options="securitygroup.id as securitygroup.name for securitygroup in $root.securitygroups | filter: {'cloudPlatform': activeCredential.cloudPlatform} | orderBy:'name'" ng-required="activeCredential && activeCredential.cloudPlatform !== 'BYOS'">
                                    </select>
                                    <div class="help-block" ng-show="showSecurityGroupKnoxWarning(instanceGroup)"><i class="fa fa-warning"></i> {{msg.cluster_form_hostgroup_securitygroup_warning}}</div>
                                </div>
                            </div>
                             <div class="form-group" ng-show="cluster.parameters.azureAvailabilitySetsEnabled">
                                <label class="col-sm-3 control-label" for="availabilitySet{{$index}}">{{msg.cluster_form_availabilityset_label}}</label>
                                <div class="col-sm-8">
                                    <select class="form-control" id="availabilitySet{{$index}}" name="availabilitySet{{$index}}" ng-model="instanceGroup.parameters.availabilitySet" ng-options="availabilityset as availabilityset.name disable when isAvailabilitySetDisabled(availabilityset.name, instanceGroup) for availabilityset in cluster.azureAvailabilitySets | orderBy:'name'">
                                        <option value="">-- use none --</option>
                                    </select>
                                    <div class="help-block" ng-hide="hideAvailabilitySetHostgroupWarning(instanceGroup)"><i class="fa fa-warning"></i> {{msg.cluster_form_hostgroup_availabilityset_warning}}</div>
                                </div>
                            </div>
                            <div class="form-group" ng-hide='instanceGroup.nodeCount !== 1'>
                                <label class="col-sm-3 control-label" for="template-ambari-{{$index}}">{{msg.cluster_form_hostgroup_ambari_label}}</label>
                                <div class="col-sm-8">
                                    <input type="checkbox" id="template-ambari-{{$index}}" name="template-ambari-{{$index}}" ng-checked="instanceGroup.type === 'GATEWAY'" ng-disabled="instanceGroup.nodeCount !== 1 || instanceGroup.type === 'GATEWAY'" ng-click="changeAmbariServer(instanceGroup)" />
                                    <div class="help-block" ng-hide="ambariServerSelected()"><i class="fa fa-warning"></i> {{msg.cluster_form_hostgroup_ambari_warning}}</div>
                                </div>
                            </div>
                            <div class="form-group" ng-hide="$root.recipes.length === 0">
                                <label class="col-sm-3 control-label" for="recipenames{{$index}}">Recipes</label>
                                <div class="col-sm-8">
                                    <div id="recipenames{{$index}}" name="recipenames{{$index}}">
                                        <div class="radio" ng-repeat="recipe in $root.recipes" style="padding-top: 0;">
                                            <div>
                                                <input type="checkbox" style="margin-right: 10px;" ng-model="$index_recipe.id" name="{{recipe.name}}" ng-change="changeRecipeRun(recipe.id, instanceGroup.group, $index_recipe.id)">{{recipe.name}}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="form-group" ng-show="activeStack || activeCredential.cloudPlatform === 'BYOS'">
    <label class="col-sm-3 control-label" for="hostgroupconfig">{{msg.cluster_form_hostgroup_label}}</label>
    <div class="col-sm-7 col-sm-offset-1">
        <div ng-repeat="hostGroup in cluster.hostGroups" id="hostgroupconfig">
            <div class="row">
                <div>
                    <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                        <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                            <h3 class="panel-title">{{hostGroup.name}}</h3>
                        </div>
                        <div class="panel-body">
                            <div class="form-group" name="hostGroupNodeForm{{$index}}">
                                <label class="col-sm-3 control-label" for="hostCount{{$index}}">{{msg.cluster_form_hostgroup_group_size_label}}</label>
                                <div class="col-sm-8">
                                    <input type="number" name="hostCount{{$index}}" class="form-control" ng-model="hostGroup.constraint.hostCount" id="hostCount{{$index}}" min="1" max="100000" placeholder="1 - 100000" ng-required="activeStack">
                                    <div class="help-block" ng-show="clusterCreationForm.hostCount{{$index}}.$dirty && clusterCreationForm.hostCount{{$index}}.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_size_invalid}}
                                    </div>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="constraintName{{$index}}">{{msg.cluster_form_hostgroup_constraint_label}}</label>
                                <div class="col-sm-8">
                                    <select class="form-control" id="constraint-name-{{$index}}" name="constraint-name-{{$index}}" ng-model="hostGroup.constraint.constraintTemplateName" ng-options="constraint.name as constraint.name for constraint in $root.constraints | orderBy:'name'" ng-required="activeStack">
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="form-group">
    <div class="col-sm-11">

        <div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-default" ng-click="(activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS') ? showWizardActualElement('configureSecurity') : showWizardActualElement('configureCluster')">
                    <i class="fa fa-angle-double-left"></i> {{(activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS') ? msg.cluster_form_ambari_network_tag : msg.cluster_form_ambari_cluster_tag}}
                </button>
            </div>
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group" ng-if="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'GCP'">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureFileSystem')" ng-disabled="!cluster.name || !cluster.region || !cluster.networkId || !cluster.blueprintId || !ambariServerSelected() || (cluster.gateway.enableGateway && blueprintKnoxError)">{{msg.cluster_form_ambari_filesystem_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
            <div class="btn-group" role="group" ng-if="activeCredential.cloudPlatform != 'AZURE' && activeCredential.cloudPlatform != 'GCP'" ng-hide="!showAdvancedOptionForm">
                <button type="button" class="btn btn-sm btn-default" ng-click="(activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS') ? showWizardActualElement('configureFailureAction') : showWizardActualElement('configureAmbariRepository')" ng-disabled="!cluster.name || !cluster.blueprintId || ((activeCredential !== undefined && activeCredential.cloudPlatform !== 'BYOS') && (!cluster.region || !cluster.networkId)) || !ambariServerSelected() || (cluster.gateway.enableGateway && blueprintKnoxError)">
                    {{(activeStack === undefined && activeCredential.cloudPlatform !== 'BYOS') ? msg.cluster_form_ambari_failure_tag : msg.cluster_form_ambari_hdprepo_tag}} <i class="fa fa-angle-double-right"></i>
                </button>
            </div>
            <div class="btn-group" role="group" ng-if="activeCredential.cloudPlatform != 'AZURE' && activeCredential.cloudPlatform != 'GCP'" ng-hide="clusterCreationForm.$invalid || showAdvancedOptionForm">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureReview')" ng-disabled="!cluster.name || !cluster.blueprintId || ((activeCredential !== undefined && activeCredential.cloudPlatform !== 'BYOS') && (!cluster.region || !cluster.networkId)) || !ambariServerSelected() || (cluster.gateway.enableGateway && blueprintKnoxError)">
                    {{msg.cluster_form_ambari_launch_tag}} <i class="fa fa-angle-double-right"></i>
                </button>
            </div>
        </div>
    </div>
</div>