<form class="form-horizontal" role="document">
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_cloudPlatform">{{msg.active_cluster_platform_label}}</label>
        <div class="col-sm-8">
            <p id="sl_cloudPlatform" class="form-control-static">{{activeCredential.cloudPlatform !== 'BYOS' ? activeCredential.cloudPlatform : activeCredential.parameters.type}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_username">
            </i>{{msg.active_cluster_username_label}}</label>
        <div class="col-sm-8">
            <p id="sl_cloudPlatform" class="form-control-static">{{cluster.userName}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_username">
            </i>{{msg.active_cluster_password_label}}</label>
        <div class="col-sm-8">
            <p id="sl_cloudPlatform" class="form-control-static">{{cluster.password}}</p>
        </div>
    </div>
    <div class="form-group" ng-show="activeCredential">
        <label class="col-sm-3 control-label" for="sl_region">{{msg.active_cluster_region_label}}</label>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'AWS' ">
            <p id="sl_region" class="form-control-static">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, cluster.region)}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'GCP' ">
            <p id="sl_region" class="form-control-static">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, cluster.region)}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'AZURE' ">
            <p id="sl_region" class="form-control-static">{{cluster.region}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'OPENSTACK' ">
            <p id="sl_region" class="form-control-static">{{cluster.region}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'BYOS' ">
            <p id="sl_region" class="form-control-static">{{cluster.region}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_credential_active">{{msg.active_cluster_credential_label}}</label>
        <div class="credentialselect col-sm-8" ng-show="activeCredential">
            <a id="sl_credential_active" segment="#panel-credential-collapse-{{activeCredential.id}}" class="credentialselect form-control-static review-a">{{activeCredential.name}}</a>
        </div>
        <div class="credentialselect col-sm-8" ng-show="activeStack">
            <a id="sl_credential_active" segment="#panel-credential-collapse-imported{{activeStack.id}}" class="credentialselect form-control-static review-a">{{activeStack.name}}</a>
        </div>
    </div>
    <div class="form-group" ng-show="cluster.networkId && activeCredential.cloudPlatform !== 'BYOS'">
        <label class="col-sm-3 control-label" for="sl_network_active">{{msg.active_cluster_network_label}}</label>
        <div class="networkselect col-sm-8">
            <a id="sl_network_active" class="networkselect form-control-static review-a" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:true" segment="#panel-network-collapse{{cluster.networkId}}">{{network.name}}</a>
        </div>
    </div>

    <div class="form-group" ng-show="cluster.blueprintId">
        <label class="col-sm-3 control-label" for="sl_network_active">{{msg.active_cluster_blueprint_label}}</label>
        <div class="col-sm-8">
            <a id="sl_selected_blueprint" class="blueprintselect form-control-static" ng-repeat="bp in $root.blueprints|filter: { id: cluster.blueprintId }:true" segment="#panel-blueprint-collapse{{cluster.blueprintId}}">{{bp.name}}</a>
            <a id="panel-hostgroups-show-btn" class="form-control-static" data-toggle="collapse" data-target="#panel-hostgroups-show" style="margin-left: 20px">{{msg.cluster_creation_show_hostgroups_label}}</a>
        </div>
    </div>


    <div class="row" style="margin-top: 20px;">
        <div id="panel-hostgroups-show" class="panel-btn-in-header-collapse collapse">
            <div class="col-sm-8 col-md-offset-2" data-example-id="togglable-tabs" ng-show="activeCredential && activeCredential.cloudPlatform !== 'BYOS'">
                <ul id="myTabs" class="nav nav-tabs" role="tablist">
                    <li role="presentation" ng-class="{true:'active', false:''}[group.group == cluster.activeGroup]" ng-repeat="group in cluster.instanceGroups| orderBy: 'group'"><a ng-click="changeActiveGroup(group.group)" href="" id="{{group.group}}-tab" role="tab" data-toggle="tab" aria-controls="{{group.group}}" aria-expanded="true">{{group.group}}</a></li>
                </ul>
                <div id="myTabContent" class="tab-content">
                    <div role="tabpanel" class="tab-pane fade active review-tab" ng-class="{true:'in', false:''}[group.group == cluster.activeGroup]" ng-hide="group.group != cluster.activeGroup" ng-show="group.group == cluster.activeGroup" ng-repeat="group in cluster.instanceGroups" id="{{group.group}}" aria-labelledby="{{group.group}}-tab">
                        <div class="">
                            <div class="form-group">
                                <label class="col-sm-2 control-label" for="sl_template_active">Template: </label>
                                <div class="templateselect col-sm-9">
                                    <a id="sl_template_active" class="templateselect form-control-static review-a" ng-repeat="template in $root.templates|filter: { id: group.templateId }:true" segment="#panel-template-collapse{{template.id}}">{{template.name}}</a>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-2 control-label" for="sl_securitygroup_active">{{msg.cluster_form_securitygroup_label}}: </label>
                                <div class="securitygroupselect col-sm-9">
                                    <a id="sl_securitygroup_active" class="securitygroupselect form-control-static review-a" ng-repeat="securitygroup in $root.securitygroups|filter: { id: group.securityGroupId }:true" segment="#panel-securitygroup-collapse{{securitygroup.id}}">{{securitygroup.name}}</a>
                                </div>
                            </div>

                            <div class="form-group" ng-repeat="blueprint in $root.blueprints|filter: { id: cluster.blueprintId }:true">
                                <label class="col-sm-2 control-label" for="sl_comps_active">Components: </label>
                                <div class="col-sm-6 col-md-6 col-lg-6">
                                    <div class="host-group-table row" ng-repeat="hostgroup in blueprint.ambariBlueprint.host_groups|filter: { name: group.group }:true">
                                        <div class="list-group col-sm-12">
                                            <a href="" class="list-group-item active" style="text-decoration: none;    font-size: 15px;">{{hostgroup.name}}</a>
                                            <a href="" ng-repeat="component in hostgroup.components" class="list-group-item" style="text-decoration: none;    font-size: 15px;">{{component.name}}</a>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-sm-8 col-md-offset-2" data-example-id="togglable-tabs" ng-show="activeStack || activeCredential.cloudPlatform === 'BYOS'">
                <ul id="myTabs" class="nav nav-tabs" role="tablist">
                    <li role="presentation" ng-class="{true:'active', false:''}[group.name == cluster.activeGroup]" ng-repeat="group in cluster.hostGroups| orderBy: 'name'"><a ng-click="changeActiveGroup(group.name)" href="" id="{{group.name}}-tab" role="tab" data-toggle="tab" aria-controls="{{group.name}}" aria-expanded="true">{{group.name}}</a></li>
                </ul>
                <div id="myTabContent" class="tab-content">
                    <div role="tabpanel" class="tab-pane fade active review-tab" ng-class="{true:'in', false:''}[group.name == cluster.activeGroup]" ng-hide="group.name != cluster.activeGroup" ng-show="group.name == cluster.activeGroup" ng-repeat="group in cluster.hostGroups" id="{{group.name}}" aria-labelledby="{{group.name}}-tab">
                        <div class="">
                            <div class="form-group">
                                <label class="col-sm-2 control-label" for="sl_template_active">{{msg.cluster_form_hostgroup_constraint_label}}: </label>
                                <div class="templateselect col-sm-9">
                                    <a id="sl_template_active" class="templateselect form-control-static review-a" ng-repeat="template in $root.constraints|filter: { name: group.constraint.constraintTemplateName }:true" segment="#panel-constraint-collapse{{template.id}}">{{template.name}}</a>
                                </div>
                            </div>
                            <div class="form-group" ng-repeat="blueprint in $root.blueprints|filter: { id: cluster.blueprintId }:true">
                                <label class="col-sm-2 control-label" for="sl_comps_active">Components: </label>
                                <div class="col-sm-6 col-md-6 col-lg-6">
                                    <div class="host-group-table row" ng-repeat="hostgroup in blueprint.ambariBlueprint.host_groups|filter: { name: group.name }:true">
                                        <div class="list-group col-sm-12">
                                            <a href="" class="list-group-item active" style="text-decoration: none;    font-size: 15px;">{{hostgroup.name}}</a>
                                            <a href="" ng-repeat="component in hostgroup.components" class="list-group-item" style="text-decoration: none;    font-size: 15px;">{{component.name}}</a>
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
</form>
<a href="" id="createCluster" style="margin-top: 20px;" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" ng-hide="clusterCreationForm.$invalid" role="button" ng-click="createCluster()"><i class="fa fa-plus fa-fw"></i>{{msg.cluster_form_create}}</a>