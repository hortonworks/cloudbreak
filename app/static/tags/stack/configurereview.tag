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
            <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.AWS.awsRegions | filter:{key: cluster.region}:true">{{item.value}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'GCP' ">
            <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.GCP.gcpRegions | filter:{key: cluster.region}:true">{{item.value}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'AZURE_RM' ">
            <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.AZURE_RM.azureRegions | filter:{key: cluster.region}:true">{{item.value}}</p>
        </div>
        <div class="col-sm-8" ng-if="activeCredential.cloudPlatform == 'OPENSTACK' ">
            <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.OPENSTACK.regions | filter:{key: cluster.region}:true">{{item.value}}</p>
        </div>
    </div>
    <div class="panel panel-default" ng-repeat="group in cluster.instanceGroups">
        <div class="panel-heading">
            <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-{{$index}}-{{group.templateId}}'><span class="badge pull-right ng-binding">{{group.group}}: {{group.nodeCount}} {{msg.active_cluster_instance_group_node_label}}</span><i class="fa fa-file-o fa-fw"></i>Template: {{getSelectedTemplate(group.templateId).name}}</a></h5>
        </div>
        <div id="panel-collapsetmp-{{$index}}-{{group.templateId}}" class="panel-collapse collapse">
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AWS' ">
                <div ng-include="'tags/template/awslist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM'">
                <div ng-include="'tags/template/azurelist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'GCP' ">
                <div ng-include="'tags/template/gcplist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'OPENSTACK' ">
                <div ng-include="'tags/template/openstacklist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
            </div>
        </div>
    </div>
    <div class="panel panel-default" ng-repeat="securitygroup in $root.securitygroups|filter: { id: cluster.securityGroupId }:false">
        <div class="panel-heading">
            <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-009'><i class="fa fa-lock fa-fw"></i>{{msg.active_cluster_securitygroup_label}}: {{securitygroup.name}}</a></h5>
        </div>
        <div id="panel-collapsetmp-009" class="panel-collapse collapse">
            <div class="panel-body">
                <div ng-include="'tags/securitygroup/securitygrouplist.tag'" ng-repeat="securitygroup in $root.securitygroups|filter: { id: cluster.securityGroupId }:false"></div>
            </div>
        </div>
    </div>
    <div class="panel panel-default" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:false">
        <div class="panel-heading">
            <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-network-{{network.id}}'><i class="fa fa-sitemap fa-fw"></i>{{msg.active_cluster_network_label}} {{network.name}}</a></h5>
        </div>
        <div id="panel-collapsetmp-network-{{network.id}}" class="panel-collapse collapse">
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AWS' ">
                <div ng-include="'tags/network/awsnetworklist.tag'" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:false"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM'">
                <div ng-include="'tags/network/azurenetworklist.tag'" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:false"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'GCP' ">
                <div ng-include="'tags/network/gcpnetworklist.tag'" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:false"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'OPENSTACK' ">
                <div ng-include="'tags/network/openstacknetworklist.tag'" ng-repeat="network in $root.networks|filter: { id: cluster.networkId }:false"></div>
            </div>
        </div>

    </div>
    <div class="panel panel-default" ng-repeat="blueprint in $root.blueprints|filter: { id: cluster.blueprintId }:true">
        <div class="panel-heading">
            <h5><a href="" data-toggle="collapse" data-target="#panel-collapse06"><i class="fa fa-th fa-fw"></i>{{msg.active_cluster_blueprint_label}} {{blueprint.name}}</a></h5>
        </div>
        <div id="panel-collapse06" class="panel-collapse collapse">
            <div class="panel-body">
                <div class="row" ng-repeat="blueprint in $root.blueprints|filter: { id: cluster.blueprintId }:true" ng-include="'tags/blueprint/bplist.tag'"></div>
            </div>
        </div>

    </div>
    <div class="panel panel-default" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true">
        <div class="panel-heading">
            <h5><a href="" data-toggle="collapse" data-target="#panel-collapse04"><i class="fa fa-th fa-fw"></i>{{msg.active_cluster_credential_label}} {{credential.name}}</a></h5>
        </div>
        <div id="panel-collapse04" class="panel-collapse collapse">
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AWS' ">
                <div ng-include="'tags/credential/awslist.tag'" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AZURE' ">
                <div ng-include="'tags/credential/azurelist.tag'" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'GCP' ">
                <div ng-include="'tags/credential/gcplist.tag'" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'OPENSTACK' ">
                <div ng-include="'tags/credential/openstacklist.tag'" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true"></div>
            </div>
            <div class="panel-body" ng-if="activeCredential.cloudPlatform == 'AZURE_RM' ">
                <div ng-include="'tags/credential/azurermlist.tag'" ng-repeat="credential in $root.credentials|filter: { id: activeCredential.id }:true"></div>
            </div>
        </div>
    </div>

</form>
<a href="" id="createCluster" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" ng-hide="clusterCreationForm.$invalid" role="button" ng-click="createCluster()"><i class="fa fa-plus fa-fw"></i>{{msg.cluster_form_create}}</a>