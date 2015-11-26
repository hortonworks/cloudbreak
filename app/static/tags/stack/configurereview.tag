<form class="form-horizontal" role="document">
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_cloudPlatform">{{msg.active_cluster_platform_label}}</label>
        <div class="col-sm-8">
            <p id="sl_cloudPlatform" class="form-control-static">{{activeCredential.cloudPlatform}}</p>
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
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_region">{{msg.active_cluster_region_label}}</label>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'AWS' ">
            <p id="sl_region" class="form-control-static">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, cluster.region)}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'GCP' ">
            <p id="sl_region" class="form-control-static">{{$root.displayNames.getRegion(activeCredential.cloudPlatform, cluster.region)}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'AZURE_RM' ">
            <p id="sl_region" class="form-control-static">{{cluster.region}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'OPENSTACK' ">
            <p id="sl_region" class="form-control-static">{{cluster.region}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_credential_active">{{msg.active_cluster_credential_label}}</label>
        <div class="credentialselect col-sm-8">
            <a id="sl_credential_active" segment="#panel-credential-collapse{{activeCredential.id}}" class="credentialselect form-control-static review-a" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true">{{credential.name}}</a>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="sl_network_active">{{msg.active_cluster_network_label}}</label>
        <div class="networkselect col-sm-8">
            <a id="sl_network_active" class="networkselect form-control-static review-a" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:false" segment="#panel-network-collapse{{cluster.networkId}}">{{network.name}}</a>
        </div>
    </div>



    <div class="row" style="margin-top: 20px;">
        <div class="col-sm-8 col-md-offset-2" data-example-id="togglable-tabs">
            <ul id="myTabs" class="nav nav-tabs" role="tablist">
                <li role="presentation" ng-class="{true:'active', false:''}[group.group == cluster.activeGroup]" ng-repeat="group in cluster.instanceGroups| orderBy: 'group'"><a ng-click="changeActiveGroup(group.group)" href="" id="{{group.group}}-tab" role="tab" data-toggle="tab" aria-controls="{{group.group}}" aria-expanded="true">{{group.group}}</a></li>
            </ul>
            <div id="myTabContent" class="tab-content">
                <div role="tabpanel" class="tab-pane fade active review-tab" ng-class="{true:'in', false:''}[group.group == cluster.activeGroup]" ng-hide="group.group != cluster.activeGroup" ng-show="group.group == cluster.activeGroup" ng-repeat="group in cluster.instanceGroups" id="{{group.group}}" aria-labelledby="{{group.group}}-tab">
                    <div class="container">
                        <div class="form-group">
                            <label class="col-sm-2 control-label" for="sl_template_active">Template: </label>
                            <div class="templateselect col-sm-9">
                                <a id="sl_template_active" class="templateselect form-control-static review-a" ng-repeat="template in $root.templates|filter: { id: group.templateId }:true" segment="#panel-template-collapse{{template.id}}">{{template.name}}</a>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label" for="sl_securitygroup_active">Security group: </label>
                            <div class="securitygroupselect col-sm-9">
                                <a id="sl_securitygroup_active" class="securitygroupselect form-control-static review-a" ng-repeat="securitygroup in $root.securitygroups|filter: { id: cluster.securityGroupId }:true" segment="#panel-securitygroup-collapse{{securitygroup.id}}">{{securitygroup.name}}</a>
                            </div>
                        </div>

                        <div class="form-group" ng-show="group.group != 'cbgateway'" ng-repeat="blueprint in $root.blueprints|filter: { id: cluster.blueprintId }:true">
                            <label class="col-sm-2 control-label" for="sl_comps_active">Components: </label>
                            <div class="col-sm-5 col-lg-6">
                                <div class="host-group-table row" ng-repeat="hostgroup in blueprint.ambariBlueprint.host_groups|filter: { name: group.group }:true">
                                    <div class="list-group">
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
</form>
<a href="" id="createCluster" style="margin-top: 20px;" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" ng-hide="clusterCreationForm.$invalid" role="button" ng-click="createCluster()"><i class="fa fa-plus fa-fw"></i>{{msg.cluster_form_create}}</a>