<div class="form-group">
    <label class="col-sm-3 control-label" for="selectBlueprint">{{msg.cluster_form_blueprint_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="selectBlueprint" ng-model="cluster.blueprintId" required ng-change="selectedBlueprintChange()" ng-options="blueprint.id as blueprint.name for blueprint in $root.blueprints | orderBy:'name'">
        </select>
    </div>
</div>
<div class="form-group" ng-show="showAdvancedOptionForm">
    <label class="col-sm-3 control-label" for="cluster_validateBlueprint">{{msg.cluster_form_blueprint_validate_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_validateBlueprint" id="cluster_validateBlueprint" ng-model="cluster.validateBlueprint">
    </div>
</div>
<div class="form-group" ng-show="cluster.instanceGroups">
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
                                    <input type="number" name="templateNodeCount{{$index}}" ng-disabled="instanceGroup.type=='GATEWAY'" class="form-control" ng-model="instanceGroup.nodeCount" id="templateNodeCount{{$index}}" min="1" max="100000" placeholder="1 - 100000" required>
                                    <div class="help-block" ng-show="clusterCreationForm.templateNodeCount{{$index}}.$dirty && clusterCreationForm.templateNodeCount{{$index}}.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_size_invalid}}
                                    </div>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="templateName{{$index}}">{{msg.cluster_form_hostgroup_template_label}}</label>
                                <div class="col-sm-8">
                                    <select class="form-control" id="template-name-{{$index}}" name="template-name-{{$index}}" ng-model="instanceGroup.templateId" ng-options="template.id as template.name for template in $root.templates | filter: {'cloudPlatform': activeCredential.cloudPlatform.split('_')[0]} | orderBy:'name'" required>
                                    </select>
                                </div>
                            </div>
                            <div class="form-group" ng-hide="instanceGroup.type=='GATEWAY' || $root.recipes.length === 0">
                                <label class="col-sm-3 control-label" for="recipenames{{$index}}">Recipes</label>
                                <div class="col-sm-8">
                                    <div id="recipenames{{$index}}" name="recipenames{{$index}}">
                                        <div class="radio" ng-repeat="recipe in $root.recipes">
                                            <label>
                                                <input type="checkbox" style="margin-right: 10px;" ng-model="$index_recipe.id" name="{{$index}}_{{recipe.id}}" ng-change="changeRecipeRun(recipe.id, instanceGroup.group, $index_recipe.id)">{{recipe.name}}</label>
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
<div class="form-group">
    <div class="col-sm-11">

<div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
    <div class="btn-group" role="group">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureSecurity')"><i class="fa fa-angle-double-left"></i> {{msg.cluster_form_ambari_network_tag}}</button>
    </div>
    <div class="btn-group" role="group" style="opacity: 0;">
        <button type="button" class="btn btn-sm btn-default"></button>
    </div>
    <div class="btn-group" role="group" ng-if="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM' || activeCredential.cloudPlatform == 'GCP'">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureFileSystem')" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || !cluster.blueprintId">{{msg.cluster_form_ambari_filesystem_tag}} <i class="fa fa-angle-double-right"></i></button>
    </div>
    <div class="btn-group" role="group" ng-if="activeCredential.cloudPlatform != 'AZURE' && activeCredential.cloudPlatform != 'AZURE_RM' && activeCredential.cloudPlatform != 'GCP'" ng-hide="!showAdvancedOptionForm">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureFailureAction')" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || !cluster.blueprintId">{{msg.cluster_form_ambari_failure_tag}} <i class="fa fa-angle-double-right"></i></button>
    </div>
    <div class="btn-group" role="group" ng-if="activeCredential.cloudPlatform != 'AZURE' && activeCredential.cloudPlatform != 'AZURE_RM' && activeCredential.cloudPlatform != 'GCP'" ng-hide="clusterCreationForm.$invalid || showAdvancedOptionForm">
        <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureReview')" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || !cluster.blueprintId">{{msg.cluster_form_ambari_launch_tag}} <i class="fa fa-angle-double-right"></i></button>
    </div>
</div></div>
</div>
