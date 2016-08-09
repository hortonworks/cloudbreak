<div id="active-cluster-panel" class="col-sm-11 col-md-11 col-lg-11">
    <input type="hidden" id="cloudbreak-details-{{activeCluster.id}}" name="cloudbreak-details-{{activeCluster.name}}" value="{{activeCluster.cloudbreakDetails.version}}">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="cluster-details-back-btn" class="btn btn-info btn-fa-2x" role="button" ng-click="deselectActiveCluster()"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>{{activeCluster.name}}</h4>
        </div>
        <div id="cluster-details-panel-collapse">
            <div class="panel-body">
                <ul class="nav nav-pills nav-justified" role="tablist">
                    <li class="active"><a ng-click="showDetails()" role="tab" data-toggle="tab">{{msg.active_cluster_details_title}}</a></li>
                    <li><a role="tab" ng-click="showPeriscope()" role="tab" data-toggle="tab" ng-show="activeCluster.cluster.status == 'AVAILABLE' && activeCluster.status == 'AVAILABLE'">{{msg.active_cluster_periscope_title}}</a></li>
                </ul>
                <div class="tab-content">
                    <section id="cluster-details-pane" ng-class="{ 'active': detailsShow }" ng-show="detailsShow" class="tab-pane fade in">
                        <p class="text-right">
                            <a href="" class="btn btn-success" role="button" data-toggle="modal" ng-show="activeCluster.cluster.status == 'AVAILABLE' && activeCluster.status == 'AVAILABLE'" data-target="#modal-upscale-cluster">
                                <i class="fa fa-arrow-up"></i><span> {{msg.active_cluster_command_add_node_label}}</span>
                            </a>
                            <a href="" class="btn btn-success" role="button" data-toggle="modal" ng-show="activeCluster.cluster.status == 'AVAILABLE' && activeCluster.status == 'AVAILABLE'" data-target="#modal-downscale-cluster">
                                <i class="fa fa-arrow-down"></i><span> {{msg.active_cluster_command_remove_node_label}}</span>
                            </a>
                            <a href="" class="btn btn-info" role="button" data-toggle="modal" data-target="#modal-sync-cluster">
                                <i class="fa fa-refresh fa-fw"></i><span> {{msg.active_cluster_command_sync_label}}</span>
                            </a>
                            <a href="" class="btn btn-success" role="button" ng-show="activeCluster.cluster.status == 'CREATE_FAILED'" data-toggle="modal" data-target="#modal-reset-cluster" ng-click="initReinstallClusterObject()">
                                <i class="fa fa-undo fa-fw"></i><span> {{msg.active_cluster_command_reinstall_label}}</span>
                            </a>
                            <a href="" class="btn btn-success" role="button" ng-show="activeCluster.status == 'STOPPED' || (activeCluster.cluster.status == 'START_REQUESTED' && activeCluster.status == 'AVAILABLE')" data-toggle="modal" data-target="#modal-start-cluster">
                                <i class="fa fa-play fa-fw"></i><span> {{msg.active_cluster_command_start_label}}</span>
                            </a>
                            <a href="" class="btn btn-warning" role="button" ng-show="((activeCluster.status == 'AVAILABLE' && activeCluster.cluster.status != 'START_REQUESTED') || ((activeCluster.status == 'STOP_REQUESTED' || activeCluster.status == 'STOP_FAILED') && activeCluster.cluster.status == 'STOPPED')) &&  activeCluster.cluster.status != 'WAIT_FOR_SYNC' && !isEphemeralCluster(activeCluster)" data-toggle="modal" data-target="#modal-stop-cluster">
                                <i class="fa fa-pause fa-fw"></i><span> {{msg.active_cluster_command_stop_label}}</span>
                            </a>
                            <a href="" id="terminate-btn" class="btn btn-danger" role="button" ng-show="activeCluster.status != 'DELETE_IN_PROGRESS'" data-toggle="modal" data-target="#modal-terminate" ng-click='inherited.forcedTermination = false'>
                                <i class="fa fa-trash-o fa-fw"></i><span> {{msg.active_cluster_command_terminate_label}}</span>
                            </a>
                        </p>
                        <form class="form-horizontal" role="document">
                            <!-- role: 'document' - non-editable "form" -->
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_ambariServerIp">{{msg.active_cluster_ambari_address_label}}</label>
                                <div class="col-sm-9">
                                    <div ng-if="activeCluster.cluster.ambariServerIp != null">
                                        <a ng-if="noProxyBeforeAmbari()" target="_blank" class="form-control-static review-a" href="http://{{activeCluster.cluster.ambariServerIp}}:8080">http://{{activeCluster.cluster.ambariServerIp}}:8080</a>
                                        <a ng-if="!noProxyBeforeAmbari()" target="_blank" class="form-control-static review-a" href="https://{{activeCluster.cluster.ambariServerIp}}/ambari/">https://{{activeCluster.cluster.ambariServerIp}}/ambari/</a>
                                    </div>
                                    <div ng-if="activeCluster.cluster.ambariServerIp == null">
                                        <a target="_blank" class="form-control-static review-a" href="">{{msg.active_cluster_ambari_not_available_label}}</a>
                                    </div>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_cloudPlatform">{{msg.active_cluster_platform_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudPlatform" class="form-control-static">{{activeCluster.cloudPlatform}}</p>
                                </div>
                            </div>
                            <div class="form-group" ng-show="activeCluster.copyState && activeCluster.copyState!=100 && (activeCluster.status != 'AVAILABLE' || activeCluster.status != 'DELETE_IN_PROGRESS' || activeCluster.status != 'CREATE_FAILED')">
                                <label class="col-sm-3 control-label" for="sl_imagecopy">{{msg.active_cluster_image_copy_label}}</label>
                                <div class="col-sm-6" style="padding-left: 10px;">
                                    <div class="progress" style="height: 25px;margin-bottom: 0px;">
                                        <div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="{{activeCluster.copyState}}" aria-valuemin="0" aria-valuemax="100" style="min-width: 2em;width: {{activeCluster.copyState}}%;;padding-top: 4px;">
                                            {{activeCluster.copyState}}%
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="form-group" ng-show="activeCluster.ambariProgressState && activeCluster.ambariProgressState!=100 && (activeCluster.status != 'AVAILABLE' || activeCluster.status != 'DELETE_IN_PROGRESS' || activeCluster.status != 'CREATE_FAILED')">
                                <label class="col-sm-3 control-label" for="sl_imagecopy">{{activeCluster.progressMessage}}</label>
                                <div class="col-sm-6" style="padding-left: 10px;">
                                    <div class="progress" style="height: 25px;margin-bottom: 0px;">
                                        <div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="{{activeCluster.ambariProgressState}}" aria-valuemin="0" aria-valuemax="100" style="min-width: 2em;width: {{activeCluster.ambariProgressState}}%;;padding-top: 4px;">
                                            {{activeCluster.ambariProgressState}}%
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="form-group" ng-show="activeCluster.credentialId">
                                <label class="col-sm-3 control-label" for="sl_platformVariant">{{msg.active_cluster_variant_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_platformVariant" class="form-control-static">{{activeCluster.platformVariant}}</p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_username">{{msg.active_cluster_username_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_username" class="form-control-static">{{activeCluster.cluster.userName}}
                                        <a ng-if="activeCluster.cluster.ambariServerIp != null" class="btn-sm btn-info" role="button" data-toggle="modal" data-target="#modal-credential-cluster" style="text-decoration: none;">
                                            <i class="fa fa-key fa-fw"></i><span> {{msg.active_cluster_command_credential_label}}</span>
                                        </a>
                                    </p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_nodeCount">{{msg.active_cluster_node_count_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_nodeCount" class="form-control-static">{{activeCluster.nodeCount}}</p>
                                </div>
                            </div>
                            <div class="form-group" ng-show="activeCluster.region">
                                <label class="col-sm-3 control-label" for="sl_region">{{msg.active_cluster_region_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_region" class="form-control-static">{{$root.displayNames.getRegion(activeCluster.cloudPlatform, activeCluster.region)}}</p>
                                </div>
                            </div>

                            <div class="form-group" ng-if="activeCluster.cluster.statusReason === null && (activeCluster.statusReason != null && activeCluster.statusReason != '')">
                                <label class="col-sm-3 control-label" for="sl_cloudStatus">{{msg.active_cluster_status_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.statusReason}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cluster.statusReason != null && activeCluster.cluster.statusReason != ''">
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.cluster.statusReason}}</p>
                                </div>
                            </div>
                            <div class="form-group" ng-if="activeCluster.cluster.statusReason != null && activeCluster.cluster.statusReason != ''">
                                <label class="col-sm-3 control-label" for="sl_cloudStatus">{{msg.active_cluster_status_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.cluster.statusReason}}</p>
                                </div>
                            </div>
                            <div class="form-group" ng-show="activeCluster.credentialId">
                                <label class="col-sm-3 control-label" for="sl_credential_active">{{msg.active_cluster_credential_label}}</label>
                                <div class="credentialselect col-sm-8">
                                    <a id="sl_credential_active" segment="#panel-credential-collapse{{activeClusterCredential.id}}" class="credentialselect form-control-static review-a">{{activeClusterCredential.name}}</a>
                                </div>
                            </div>
                            <div class="form-group" ng-hide="activeCluster.credentialId">
                                <label class="col-sm-3 control-label" for="sl_credential_active">{{msg.active_cluster_credential_label}}</label>
                                <div class="credentialselect col-sm-8">
                                    <a id="sl_credential_active" segment="#panel-credential-collapse-imported{{activeCluster.id}}" class="credentialselect form-control-static review-a">{{activeCluster.stackName}}</a>
                                </div>
                            </div>
                            <div class="form-group" ng-show="activeCluster.networkId">
                                <label class="col-sm-3 control-label" for="sl_network_active">{{msg.active_cluster_network_label}}</label>
                                <div class="networkselect col-sm-8">
                                    <a id="sl_network_active" class="networkselect form-control-static review-a" ng-repeat="network in $root.networks|filter: { id: $root.activeCluster.networkId }:notStrictFilter" segment="#panel-network-collapse{{$root.activeCluster.networkId}}">{{network.name}}</a>
                                </div>
                            </div>
                            <div class="row" style="margin-top: 20px;    margin-bottom: 30px;">
                                <div class="col-sm-8 col-md-offset-2" data-example-id="togglable-tabs" ng-show="activeCluster.instanceGroups[0] != null">
                                    <ul id="myTabs" class="nav nav-tabs" role="tablist">
                                        <li role="presentation" ng-class="{true:'active', false:''}[group.group == $root.activeCluster.activeGroup]" ng-repeat="group in $root.activeCluster.instanceGroups| orderBy: 'group'"><a ng-click="changeActiveClusterGroup(group.group)" href="" id="{{group.group}}-tab" role="tab" data-toggle="tab" aria-controls="{{group.group}}" aria-expanded="true">{{group.group}}</a></li>
                                    </ul>
                                    <div id="myTabContent" class="tab-content">
                                        <div role="tabpanel" class="tab-pane fade active review-tab" ng-class="{true:'in', false:''}[group.group == $root.activeCluster.activeGroup]" ng-hide="group.group != $root.activeCluster.activeGroup" ng-show="group.group == $root.activeCluster.activeGroup" ng-repeat="group in $root.activeCluster.instanceGroups" id="{{group.group}}" aria-labelledby="{{group.group}}-tab">
                                            <div class="container">
                                                <div class="form-group">
                                                    <label class="col-sm-2 control-label" for="sl_template_active">Template: </label>
                                                    <div class="templateselect col-sm-9">
                                                        <a id="sl_template_active" class="templateselect form-control-static review-a" ng-repeat="template in $root.templates|filter: { id: group.templateId }:true" segment="#panel-template-collapse{{template.id}}">{{template.name}}</a>
                                                    </div>
                                                </div>

                                                <div class="form-group" ng-show="group.securityGroupId">
                                                    <label class="col-sm-2 control-label" for="sl_securitygroup_active">{{msg.cluster_form_securitygroup_label}}: </label>
                                                    <div class="securitygroupselect col-sm-9">
                                                        <a id="sl_securitygroup_active" class="securitygroupselect form-control-static review-a" ng-repeat="securitygroup in $root.securitygroups|filter: { id: group.securityGroupId }:true" segment="#panel-securitygroup-collapse{{securitygroup.id}}">{{securitygroup.name}}</a>
                                                    </div>
                                                </div>
                                                <div class="form-group">
                                                    <label class="col-sm-2 control-label" for="sl_nodecount">{{msg.node_count}}: </label>
                                                    <div class="col-sm-9">
                                                        <p id="sl_nodecount" class="form-control-static">{{group.nodeCount}}</p>
                                                    </div>
                                                </div>
                                                <div class="form-group">
                                                    <label class="col-sm-2 control-label" for="sl_comps_active">Components: </label>
                                                    <div class="col-sm-5 col-lg-6">
                                                        <div class="host-group-table row" ng-repeat="hostgroup in $root.activeClusterBlueprint.ambariBlueprint.host_groups|filter: { name: group.group }:true">
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
                                <div class="col-sm-8 col-md-offset-2" data-example-id="togglable-tabs" ng-show="activeCluster.instanceGroups[0] == null">
                                    <ul id="myTabs" class="nav nav-tabs" role="tablist">
                                        <li role="presentation" ng-class="{true:'active', false:''}[group.name == $root.activeCluster.activeGroup]" ng-repeat="group in $root.activeCluster.hostGroups| orderBy: 'name'"><a ng-click="changeActiveClusterGroup(group.name)" href="" id="{{group.name}}-tab" role="tab" data-toggle="tab" aria-controls="{{group.name}}" aria-expanded="true">{{group.name}}</a></li>
                                    </ul>
                                    <div id="myTabContent" class="tab-content">
                                        <div role="tabpanel" class="tab-pane fade active review-tab" ng-class="{true:'in', false:''}[group.name == $root.activeCluster.activeGroup]" ng-hide="group.name != $root.activeCluster.activeGroup" ng-show="group.name == $root.activeCluster.activeGroup" ng-repeat="group in $root.activeCluster.hostGroups" id="{{group.name}}" aria-labelledby="{{group.name}}-tab">
                                            <div class="container">
                                                <div class="form-group">
                                                    <label class="col-sm-2 control-label" for="sl_template_active">Constraint template: </label>
                                                    <div class="templateselect col-sm-9">
                                                        <a id="sl_template_active" class="templateselect form-control-static review-a" ng-repeat="template in $root.constraints|filter: { name: group.constraint.constraintTemplateName }:true" segment="#panel-constraint-collapse{{template.id}}">{{template.name}}</a>
                                                    </div>
                                                </div>

                                                <div class="form-group">
                                                    <label class="col-sm-2 control-label" for="sl_nodecount">Node count: </label>
                                                    <div class="col-sm-9">
                                                        <p id="sl_nodecount" class="form-control-static">{{group.constraint.hostCount}}</p>
                                                    </div>
                                                </div>
                                                <div class="form-group">
                                                    <label class="col-sm-2 control-label" for="sl_comps_active">Components: </label>
                                                    <div class="col-sm-5 col-lg-6">
                                                        <div class="host-group-table row" ng-repeat="hostgroup in $root.activeClusterBlueprint.ambariBlueprint.host_groups|filter: { name: group.name }:true">
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
                        <div class="panel panel-default" ng-hide="isEmptyObj($root.activeCluster.cluster.serviceEndPoints)">
                            <div class="panel-heading">
                                <h5><a data-toggle="collapse" data-target="#cluster-exposed-ports-collapse01"><i class="fa fa-link fa-fw"></i>{{msg.active_cluster_service_title_label}}</a></h5>
                            </div>

                            <div id="cluster-exposed-ports-collapse01" class="panel-collapse collapse">
                                <div class="panel-body">
                                    <form class="form" role="document">
                                        <div class="form-group">
                                            <div class="col-sm-12">
                                                <table id="metadataTable" class="table table-report table-sortable-cols table-with-pagination table-condensed" style="background-color: transparent;">
                                                    <thead>
                                                        <tr>
                                                            <th>
                                                                <span>{{msg.active_cluster_service_name_label}}</span>
                                                            </th>
                                                            <th>
                                                                <span>{{msg.active_cluster_service_address_label}}</span>
                                                            </th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        <tr ng-repeat="(key, value) in $root.activeCluster.cluster.serviceEndPoints">
                                                            <td data-title="'servicename'" class="col-md-4">{{key}}</td>
                                                            <td data-title="'address'" class="col-md-3"><a target="_blank" href="{{isVisibleServiceValue(value) ? 'http://'+value : '#' }}">{{isVisibleServiceValue(value) ? value : msg.active_cluster_pending}}</a></td>
                                                        </tr>
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default" ng-show="filteredActiveClusterData.length !== 0">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse0002"><i class="fa fa-align-justify fa-fw"></i>{{msg.active_cluster_stack_description_title_prefix_label}}</a></h5>
                            </div>
                            <div id="panel-collapse0002" class="panel-collapse collapse">
                                <div class="panel-body pagination">
                                    <select name="itemsPerPageSelector" class="form-control pull-right" style="width: auto" data-live-search="true" ng-model="pagination.itemsPerPage">
                                        <option selected value="10">10</option>
                                        <option value="25">25</option>
                                        <option value="50">50</option>
                                        <option value="100">100</option>
                                    </select>
                                    <table id="metadataTable" class="table table-report table-sortable-cols table-with-pagination table-condensed" style="background-color: transparent;">
                                        <thead>
                                            <tr>
                                                <th class="col-md-4">
                                                    <span>{{msg.active_cluster_stack_description_name_label}}</span>
                                                </th>
                                                <th class="col-md-2">
                                                    <span>{{msg.active_cluster_stack_description_public_address_label}}</span>
                                                </th>
                                                <th class="col-md-2">
                                                    <span>{{msg.active_cluster_stack_description_private_address_label}}</span>
                                                </th>
                                                <th class="col-md-2 text-center">
                                                    <span>{{msg.active_cluster_stack_description_hostgroup_name_label}}</span>
                                                </th>
                                                <th class="col-md-1 text-center">
                                                    <span>{{msg.active_cluster_stack_description_host_status_label}}</span>
                                                </th>
                                                <th class="col-md-1 text-center">
                                                    <span>Action</span>
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr ng-repeat="instance in filteredActiveClusterData | orderBy: ['instanceGroup', 'privateIp']" ng-class="instance.state == 'UNHEALTHY' ? 'danger' : ''">
                                                <td class="col-md-4" data-title="'name'">{{instance.instanceId||msg.active_cluster_pending}}</td>
                                                <td class="col-md-2" data-title="'public IP'">{{instance.publicIp||msg.active_cluster_pending}}</td>
                                                <td class="col-md-2" data-title="'private IP'">{{instance.privateIp||msg.active_cluster_pending}}</td>
                                                <td class="col-md-2 text-center" data-title="'host group'"><span class="label label-default">{{instance.instanceGroup}}</span></td>
                                                <td class="col-md-1 text-center" data-title="'state'">
                                                    <div ng-if="activeCluster.status != 'WAIT_FOR_SYNC' && activeCluster.cluster.status != 'WAIT_FOR_SYNC'">
                                                        <span ng-if="instance.instanceStatus !== 'STOPPED' && (instance.instanceStatus === 'FAILED' || instance.state === 'UNHEALTHY' || instance.instanceStatus === 'DECOMMISSIONED')" class="label label-danger" style="font-size: 12px;">
                                                                {{msg.active_cluster_stack_description_hostgroup_unhealthy_label}}
                                                        </span>
                                                        <span ng-if="activeCluster.status != 'AVAILABLE' && (instance.instanceStatus == 'REQUESTED' || instance.instanceStatus == 'CREATED' || instance.instanceStatus == 'UNREGISTERED')" title="{{msg.active_cluster_stack_description_hostgroup_state_tooltip}}" class="label label-warning" style="font-size: 12px;">
                                                            {{msg.active_cluster_stack_description_hostgroup_in_progress_label}}
                                                        </span>
                                                        <span ng-if="activeCluster.status == 'AVAILABLE' && (instance.instanceStatus == 'CREATED' || instance.instanceStatus == 'UNREGISTERED')" title="{{msg.active_cluster_stack_description_hostgroup_state_tooltip}}" class="label label-warning" style="font-size: 12px;">
                                                            {{msg.active_cluster_stack_description_hostgroup_unused_label}}
                                                        </span>
                                                        <span ng-if="instance.state !== 'UNHEALTHY' && instance.instanceStatus == 'REGISTERED'" title="{{msg.active_cluster_stack_description_hostgroup_state_tooltip}}" class="label label-info" style="font-size: 12px;">
                                                            {{msg.active_cluster_stack_description_hostgroup_healthy_label}}
                                                        </span>
                                                        <span ng-if="instance.instanceStatus == 'STOPPED'" title="{{msg.active_cluster_stack_description_hostgroup_state_tooltip}}" class="label label-warning" style="font-size: 12px;">
                                                            {{msg.active_cluster_stack_description_hostgroup_stopped_label}}
                                                        </span>
                                                    </div>
                                                    <span ng-if="activeCluster.status == 'WAIT_FOR_SYNC' || activeCluster.cluster.status == 'WAIT_FOR_SYNC'" title="{{msg.active_cluster_stack_description_hostgroup_state_tooltip}}" class="label label-danger" style="font-size: 12px;">
                                                      {{msg.active_cluster_stack_description_hostgroup_unknown_label}}
                                                    </span>
                                                </td>
                                                <td class="col-md-1" data-title="'action'">
                                                    <span class="label label-danger" style="font-size: 12px;">
                                                        <a title="{{msg.active_cluster_stack_description_hostgroup_terminate_tooltip}}" href="" class="btn label label-block label-danger fa fa-trash-o fa-fw" role="button" style="font-size: 12px; width: 100px;"
                                                            data-toggle="modal" data-target="#modal-terminate-instance" ng-mouseover="activeCluster.instanceId=instance.instanceId"
                                                            ng-disabled="updateStatuses.indexOf(activeCluster.status) != -1 || updateStatuses.indexOf(activeCluster.cluster.status) != -1 || (terminatableStatuses.indexOf(instance.instanceStatus) == -1 && instance.state !== 'UNHEALTHY')">
                                                          {{msg.active_cluster_stack_description_hostgroup_terminate_label}}</a>
                                                    </span>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <pagination boundary-links="true" total-items="pagination.totalItems" items-per-page="pagination.itemsPerPage" ng-model="pagination.currentPage" max-size="10"></pagination>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default panel-cluster-history">
                            <div class="panel-heading">
                                <h5>
                                    <a data-toggle="collapse" data-target="#cluster-history-collapse01"><i class="fa fa-clock-o fa-fw"></i>{{msg.active_cluster_event_history_label}}</a>
                                </h5>
                            </div>
                            <div id="cluster-history-collapse01" class="panel-collapse collapse" style="height: auto;">
                                <div class="panel-body">
                                    <form class="form-horizontal" role="document">
                                        <div class="form-group">
                                            <div class="col-sm-12">
                                                <pre class="form-control-static event-history" style="overflow: auto; word-wrap: normal; height:300px; white-space: pre;">
<span ng-repeat="actual in $root.events |filter:logFilterFunction|orderBy:eventTimestampAsFloat"><span class="{{$root.config.EVENT_CLASS[actual .eventType]}}" style="word-wrap: break-word">{{actual.customTimeStamp}} {{actual.stackName}} - {{$root.config.EVENT_TYPE[actual .eventType]}}: {{actual.eventMessage}}</span><br/></span>
                                                </pre>
                                            </div>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </section>
                    <section id="periscope-pane" ng-class="{ 'active': periscopeShow }" ng-show="periscopeShow" class="tab-pane fade in">
                        <div onload="addPanelJQueryEventListeners('alert'); addPanelJQueryEventListeners('scaling');" ng-include="'tags/periscope/periscope.tag'"></div>
                    </section>
                    <section id="metrics-pane" ng-class="{ 'active': metricsShow }" ng-show="metricsShow" class="tab-pane fade in">
                    </section>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-terminate" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_terminate_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_list_dialog_suffix}}</p>
                    <input id='modal-terminate-forced' type='checkbox' ng-model="inherited.forcedTermination">
                    <label for='modal-terminate-forced'>{{msg.cluster_list_terminate_dialog_forced}}</label>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" id="terminateStackBtn" ng-click="deleteCluster(activeCluster)"><i class="fa fa-trash-o fa-fw"></i>{{msg.active_cluster_command_terminate_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-terminate-instance" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_terminate_instance_dialog_prefix}} <strong>{{activeCluster.instanceId}}</strong> {{msg.cluster_list_terminate_instance_dialog_suffix}}</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" id="terminateStackInstanceBtn" ng-click="deleteStackInstance(activeCluster.id, activeCluster.instanceId)"><i class="fa fa-trash-o fa-fw"></i>{{msg.active_cluster_command_terminate_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-stop-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title">
        <div class="modal-dialog modal-sm" role="document">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_stop_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_list_dialog_suffix}}</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-warning" data-dismiss="modal" id="stackStackBtn" ng-click="stopCluster(activeCluster)"><i class="fa fa-pause fa-fw"></i>{{msg.cluster_list_stop_command_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-sync-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_sync_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_list_dialog_suffix}}</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-info" data-dismiss="modal" id="stackStackBtn" ng-click="syncCluster(activeCluster)"><i class="fa fa-refresh fa-fw"></i>{{msg.active_cluster_command_sync_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-upscale-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-md">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_upscale_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_upscale_dialog_suffix}}</p>
                    <form class="form-horizontal" role="form" name="upscaleCluster1">
                        <div class="form-group">
                            <label class="col-sm-3 col-sm-offset-1 control-label" for="hostgroupselected">{{msg.cluster_upscale_form_hostgroup}}</label>
                            <div class="col-sm-6">
                                <select class="form-control" id="hostgroupselected" ng-model="upscaleCluster.hostGroup" ng-options="group.name as group.name for group in $root.activeCluster.hostGroups">
                                </select>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-3 col-sm-offset-1 control-label" for="hostgroupselectednumber">{{msg.cluster_upscale_form_hostgroup_number}}</label>
                            <div class="col-sm-6">
                                <div class="input-group">
                                    <span class="input-group-addon" id="basic-addon1">+</span>
                                    <input type="number" class="form-control" id="numberOfInstances" ng-model="upscaleCluster.numberOfInstances" placeholder="1" aria-describedby="basic-addon1">
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-success" data-dismiss="modal" id="stackStackBtn" ng-click="startUpScaleCluster()"><i class="fa fa-play fa-fw"></i>{{msg.active_cluster_command_start_upscale_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-downscale-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-md">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_downscale_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_upscale_dialog_suffix}}</p>
                    <form class="form-horizontal" role="form" name="downscaleCluster1">
                        <div class="form-group">
                            <label class="col-sm-3 col-sm-offset-1 control-label" for="hostgroupselected">{{msg.cluster_upscale_form_hostgroup}}</label>
                            <div class="col-sm-6">
                                <select class="form-control" id="hostgroupselected" ng-model="downscaleCluster.hostGroup" ng-options="group.name as group.name for group in $root.activeCluster.hostGroups">
                                </select>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-3 col-sm-offset-1 control-label" for="hostgroupselectednumber">{{msg.cluster_upscale_form_hostgroup_number}}</label>
                            <div class="col-sm-6">
                                <div class="input-group">
                                    <span class="input-group-addon" id="basic-addon1">-</span>
                                    <input type="number" class="form-control" id="numberOfInstances" ng-model="downscaleCluster.numberOfInstances" placeholder="1" aria-describedby="basic-addon1">
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-success" data-dismiss="modal" id="stackStackBtn" ng-click="startDownScaleCluster()"><i class="fa fa-play fa-fw"></i>{{msg.active_cluster_command_start_downscale_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-start-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_start_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_list_dialog_suffix}}</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-success" data-dismiss="modal" id="stackStackBtn" ng-click="startCluster(activeCluster)"><i class="fa fa-play fa-fw"></i>{{msg.active_cluster_command_start_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-credential-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-md">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_credential_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_list_credential_dialog_suffix}}</p>
                    <form class="form" name="changeCredential">
                        <div class="row">
                            <div class="form-group">
                                <label class="col-sm-4 control-label" for="newUserName">{{msg.cluster_form_ambari_user_label}}</label>
                                <div class="col-sm-8">
                                    <input type="text" name="newUserName" class="form-control" id="newUserName" ng-model="newCredential.newUserName" placeholder="{{msg.cluster_form_ambari_user_placeholder}}" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="15" required>
                                </div>
                            </div>
                        </div>
                        <div class="row" style="margin-top: 8px">
                            <div class="form-group">
                                <label class="col-sm-4 control-label" for="newPassword">{{msg.cluster_form_ambari_old_password_label}}</label>
                                <div class="col-sm-8">
                                    <input type="password" name="oldPassword" class="form-control" id="oldPassword" ng-model="newCredential.oldPassword" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-minlength="5" ng-maxlength="100" required>
                                </div>
                            </div>
                        </div>
                        <div class="row" style="margin-top: 8px">
                            <div class="form-group">
                                <label class="col-sm-4 control-label" for="newPassword">{{msg.cluster_form_ambari_new_password_label}}</label>
                                <div class="col-sm-8">
                                    <input type="password" name="newPassword" class="form-control" id="newPassword" ng-model="newCredential.newPassword" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-minlength="5" ng-maxlength="100" required>
                                </div>
                            </div>
                        </div>
                        <div class="row" style="margin-top: 8px">
                            <div class="form-group" ng-class="{ 'has-error': changeCredential.newPasswordforce.$dirty && changeCredential.newPasswordforce.$invalid }">
                                <label class="col-sm-4 control-label" for="newPasswordforce">{{msg.cluster_form_ambari_new_password_label}}</label>
                                <div class="col-sm-8">
                                    <input type="password" name="newPasswordforce" match="newCredential.newPassword" class="form-control" id="newPasswordforce" ng-model="newCredential.newPasswordforce" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-minlength="5" ng-maxlength="100" required>
                                    <div class="help-block" ng-show="changeCredential.newPasswordforce.$dirty && changeCredential.newPasswordforce.$invalid"><i class="fa fa-warning"></i> {{msg.error_change_credentail_cluster}}
                                    </div>
                                </div>
                            </div>

                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-success" data-dismiss="modal" id="stackStackBtn" ng-disabled="changeCredential.$invalid" ng-click="changeClusterCredential(activeCluster)"><i class="fa fa-key fa-fw"></i>{{msg.active_cluster_command_credential_short_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modal-reset-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-md">
            <div class="modal-content">
                <!-- .modal-header -->
                <div class="modal-body">
                    <p>{{msg.cluster_list_reinstall_dialog_prefix}} <strong>{{activeCluster.name}}</strong> {{msg.cluster_list_reinstall_dialog_suffix}}</p>
                    <div class="form">
                        <div class="row">
                            <div class="form-group">
                                <label class="col-sm-2 control-label" for="selectBlueprintreinstall">{{msg.active_cluster_reset_blueprint_label}}</label>
                                <div class="col-sm-10">
                                    <select class="form-control" id="selectBlueprintreinstall" ng-model="reinstallClusterObject.blueprintId" required ng-change="selectBlueprintreinstallChange()">
                                        <option ng-repeat="blueprint in $root.blueprints | orderBy:'name'" data-value="{{blueprint.id}}" value="{{blueprint.id}}" id="{{blueprint.id}}" ng-show="blueprint.ambariBlueprint.host_groups.length === $root.activeClusterBlueprint.ambariBlueprint.host_groups.length">{{blueprint.name}}
                                        </option>
                                    </select>
                                </div>
                            </div>
                        </div>
                        <div class="row" style="margin-top: 8px">
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="validateBlueprintreinstall">{{msg.active_cluster_reset_validate_blueprint_label}}</label>
                                <div class="col-sm-6">
                                    <input type="checkbox" name="validateBlueprintreinstall" id="validateBlueprintreinstall" ng-model="reinstallClusterObject.validateBlueprint">
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div style="padding: 1em;padding-bottom: 0px;">
                                <div ng-repeat="newhostgroup in $root.reinstallClusterObject.hostgroups | orderBy:'instanceGroupName'" id="newhostgroups" ng-show="$root.reinstallClusterObject.hostgroups">
                                    <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                        <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                            <h3 class="panel-title">{{msg.active_cluster_reset_hostgroup_message_prefix}} <kbd>{{newhostgroup.instanceGroupName}}</kbd> {{msg.active_cluster_reset_hostgroup_message_suffix}}</h3>
                                        </div>
                                        <div class="panel-body" ng-show="newhostgroup.constraint.instanceGroupName">
                                            <div class="form-group" name="templateNodeform{{$index}}">
                                                <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">{{msg.active_cluster_reset_hostgroup_label}}</label>
                                                <div class="col-sm-9">
                                                    <select class="form-control" ng-model="newhostgroup.name" required ng-options="hgvalue.name as hgvalue.name for hgvalue in $root.reinstallClusterObject.fullBp.ambariBlueprint.host_groups">
                                                    </select>
                                                </div>
                                                <div ng-show="recipes && recipes.length">
                                                    <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">{{msg.active_cluster_details_show_recipes}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="checkbox" ng-click="toggleRecipes($index)">
                                                    </div>
                                                    <div ng-show="recipesToShow[$index]">
                                                        <div class="col-sm-9">
                                                            <select class="form-control" ng-model="newhostgroup.recipeIds" ng-options="rvalue.id as rvalue.name for rvalue in $root.recipes" multiple="true">
                                                            </select>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="panel-body" ng-show="newhostgroup.constraint.constraintTemplateName">
                                            <div class="form-group" name="templateNodeform{{$index}}">
                                                <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">{{msg.active_cluster_reset_hostgroup_label}}</label>
                                                <div class="col-sm-9">
                                                    <select class="form-control" ng-model="newhostgroup.constraint.constraintTemplateName" required ng-options="const.name as const.name for const in $root.constraints">
                                                    </select>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="row" style="margin-top: 8px">
                            <div style="padding: 1em;padding-bottom: 0px;">
                                <div id="ambariStackDetailsrestart">
                                    <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                        <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                            <h3 class="panel-title">{{msg.active_cluster_ambari_details_title}}</h3>
                                        </div>
                                        <div class="panel-body">
                                            <div class="form-group" name="ambariStackDetailsreinstallpane">
                                                <div class="form-group" name="ambari_stack1">
                                                    <label class="col-sm-3 control-label" for="ambari_stack">{{msg.cluster_form_ambari_repo_stack_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_stack" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.stack" id="ambari_stack" placeholder="{{msg.cluster_form_ambari_repo_stack_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_version1">
                                                    <label class="col-sm-3 control-label" for="ambari_version">{{msg.cluster_form_ambari_repo_version_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_version" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.version" id="ambari_version" placeholder="{{msg.cluster_form_ambari_repo_version_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_os1">
                                                    <label class="col-sm-3 control-label" for="ambari_os">{{msg.cluster_form_ambari_repo_os_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" disabled name="ambari_os" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.os" id="ambari_os" placeholder="{{msg.cluster_form_ambari_repo_os_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_stackRepoId1">
                                                    <label class="col-sm-3 control-label" for="ambari_stackRepoId">{{msg.cluster_form_ambari_repo_stack_repoid_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_stackRepoId" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.stackRepoId" id="ambari_stackRepoId" placeholder="{{msg.cluster_form_ambari_repo_stack_repoid_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_stackBaseURL1">
                                                    <label class="col-sm-3 control-label" for="ambari_stackBaseURL">{{msg.cluster_form_ambari_repo_baseurl_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_stackBaseURL" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.stackBaseURL" id="ambari_stackBaseURL" placeholder="{{msg.cluster_form_ambari_repo_baseurl_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_utilsRepoId1">
                                                    <label class="col-sm-3 control-label" for="ambari_utilsRepoId">{{msg.cluster_form_ambari_repo_utils_repoid_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_utilsRepoId" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.utilsRepoId" id="ambari_utilsRepoId" placeholder="{{msg.cluster_form_ambari_repo_utils_repoid_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_utilsBaseURL1">
                                                    <label class="col-sm-3 control-label" for="ambari_utilsBaseURL">{{msg.cluster_form_ambari_repo_utils_baseurl_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_utilsBaseURL" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.utilsBaseURL" id="ambari_utilsBaseURL" placeholder="{{msg.cluster_form_ambari_repo_utils_baseurl_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="cluster_verify1">
                                                    <label class="col-sm-3 control-label" for="cluster_verify">{{msg.cluster_form_ambari_repo_verify_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="checkbox" name="cluster_verify" id="cluster_verify" ng-model="reinstallClusterObject.ambariStackDetails.verify">
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
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.cluster_list_cancel_command_label}}</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-success" data-dismiss="modal" id="stackStackBtn" ng-click="reinstallCluster(activeCluster)"><i class="fa fa-check fa-fw"></i>{{msg.active_cluster_command_reinstall_label}}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>