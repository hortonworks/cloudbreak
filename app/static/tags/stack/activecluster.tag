<div id="active-cluster-panel" class="col-sm-11 col-md-11 col-lg-11">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="cluster-details-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>{{activeCluster.name}}</h4>
        </div>
        <div id="cluster-details-panel-collapse">
            <div class="panel-body">
                <ul class="nav nav-pills nav-justified" role="tablist">
                  <li class="active"><a ng-click="showDetails()" role="tab" data-toggle="tab">{{msg.active_cluster_details_title}}</a></li>
                  <li><a role="tab" ng-click="showPeriscope()" role="tab" data-toggle="tab">{{msg.active_cluster_periscope_title}}</a></li>
                </ul>
                <div class="tab-content">
                    <section id="cluster-details-pane" ng-class="{ 'active': detailsShow }" ng-show="detailsShow" class="tab-pane fade in">
                        <p class="text-right">
                            <a href="" class="btn btn-info" role="button" data-toggle="modal" data-target="#modal-sync-cluster">
                                <i class="fa fa-refresh fa-fw"></i><span> {{msg.active_cluster_command_sync_label}}</span>
                            </a>
                            <a href="" class="btn btn-success" role="button" ng-show="activeCluster.cluster.status == 'CREATE_FAILED'" data-toggle="modal" data-target="#modal-reset-cluster">
                                <i class="fa fa-undo fa-fw"></i><span> {{msg.active_cluster_command_reinstall_label}}</span>
                            </a>
                            <a href="" class="btn btn-success" role="button" ng-show="activeCluster.status == 'STOPPED' || (activeCluster.cluster.status == 'START_REQUESTED' && activeCluster.status == 'AVAILABLE')" data-toggle="modal" data-target="#modal-start-cluster">
                                <i class="fa fa-play fa-fw"></i><span> {{msg.active_cluster_command_start_label}}</span>
                            </a>
                            <a href="" class="btn btn-warning" role="button" ng-show="(activeCluster.status == 'AVAILABLE' && activeCluster.cluster.status != 'START_REQUESTED') || ((activeCluster.status == 'STOP_REQUESTED' || activeCluster.status == 'STOP_FAILED') && activeCluster.cluster.status == 'STOPPED')" data-toggle="modal" data-target="#modal-stop-cluster">
                                <i class="fa fa-pause fa-fw"></i><span> {{msg.active_cluster_command_stop_label}}</span>
                            </a>
                            <a href="" id="terminate-btn" class="btn btn-danger" role="button" data-toggle="modal" data-target="#modal-terminate">
                                <i class="fa fa-trash-o fa-fw"></i><span> {{msg.active_cluster_command_terminate_label}}</span>
                            </a>
                        </p>
                        <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_ambariServerIp">{{msg.active_cluster_ambari_address_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_ambariServerIp" class="form-control-static">
                                        <div ng-if="activeCluster.cluster.ambariServerIp != null">
                                            <a ng-show="activeCluster.cluster.ambariServerIp != null" target="_blank" href="http://{{activeCluster.cluster.ambariServerIp}}:8080">http://{{activeCluster.cluster.ambariServerIp}}:8080</a>
                                        </div>
                                        <div ng-if="activeCluster.cluster.ambariServerIp == null">
                                            <a ng-show="activeCluster.cluster.ambariServerIp == null" target="_blank" href="">{{msg.active_cluster_ambari_not_available_label}}</a>
                                        </div>
                                    </p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_cloudPlatform">{{msg.active_cluster_platform_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudPlatform" class="form-control-static">{{activeCluster.cloudPlatform}}</p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_nodeCount">{{msg.active_cluster_node_count_label}}</label>
                                <div class="col-sm-9">
                                    <p id="sl_nodeCount" class="form-control-static">{{activeCluster.nodeCount}}</p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_region">{{msg.active_cluster_region_label}}</label>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'AWS' ">
                                    <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.AWS.awsRegions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'GCP' ">
                                     <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.GCP.gcpRegions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'AZURE' ">
                                    <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.AZURE.azureRegions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'OPENSTACK' ">
                                  <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.OPENSTACK.regions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
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
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.cluster.statusReason}}2</p>
                                </div>
                            </div>
                        </form>
                        <div class="panel panel-default" >
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
                                                            <tr ng-repeat="(key, value) in activeCluster.cluster.serviceEndPoints">
                                                                <td data-title="'servicename'" class="col-md-4">{{key}}</td>
                                                                <td data-title="'address'" class="col-md-3"><a target="_blank" href="http://{{value}}">{{value}}</a></td>
                                                            </tr>
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                        </div>

                        <div class="panel panel-default panel-cluster-history">
                            <div class="panel-heading">
                                <h5><a data-toggle="collapse" data-target="#cluster-history-collapse01"><i class="fa fa-clock-o fa-fw"></i>{{msg.active_cluster_event_history_label}}</a></h5>
                            </div>

                            <div id="cluster-history-collapse01" class="panel-collapse collapse" style="height: auto;">

                                <div class="panel-body">

                                    <form class="form-horizontal" role="document">

                                        <div class="form-group">
                                            <div class="col-sm-12">
												<pre class="form-control-static event-history" style="overflow: auto; word-wrap: normal; height:300px; white-space: pre;">
<span ng-repeat="actual in $root.events |filter:logFilterFunction|orderBy:eventTimestampAsFloat"><span class="{{$root.config.EVENT_CLASS[actual .eventType]}}" >{{actual.customTimeStamp}} {{actual.stackName}} - {{$root.config.EVENT_TYPE[actual .eventType]}}: {{actual.eventMessage}}</span><br/></span>
                                                </pre>
                                            </div><!-- .col-sm-12  -->
                                        </div><!-- .form-group -->

                                    </form>
                                </div>
                            </div>
                        </div>

                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse0002"><i class="fa fa-align-justify fa-fw"></i>{{msg.active_cluster_stack_description_title_prefix_label}} {{activeCluster.name}}</a></h5>
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
                                                 <th class="col-md-2 text-center">
                                                 <span>{{msg.active_cluster_stack_description_host_status_label}}</span>
                                                 </th>
                                               </tr>
                                            </thead>
                                            <tbody>
                                            <tr ng-repeat="instance in filteredActiveClusterData | orderBy: 'instanceId'" ng-class="instance.state == 'UNHEALTHY' ? 'danger' : ''">
                                                <td class="col-md-4" data-title="'name'" class="col-md-4">{{instance.instanceId}}</td>
                                                <td class="col-md-2" data-title="'public IP'" class="col-md-3">{{instance.publicIp}}</td>
                                                <td class="col-md-2" data-title="'private IP'" class="col-md-3">{{instance.privateIp}}</td>
                                                <td class="col-md-2 text-center" data-title="'host group'" class="col-md-2"><span class="label label-default">{{instance.instanceGroup}}</span></td>
                                                <td class="col-md-2 text-center" data-title="'state'" class="col-md-2" ng-switch on="instance.state">
                                                    <div ng-switch-when="UNHEALTHY">
                                                        <a ng-init="instance.unhealthyMessage=msg.active_cluster_stack_description_hostgroup_unhealthy_label" title="{{msg.active_cluster_stack_description_hostgroup_terminate_tooltip}}" href="" class="btn label label-block label-danger fa fa-trash-o fa-fw"
                                                        ng-mouseover="activeCluster.instanceId=instance.instanceId; instance.unhealthyMessage=msg.active_cluster_stack_description_hostgroup_terminate_label" ng-mouseleave="instance.unhealthyMessage=msg.active_cluster_stack_description_hostgroup_unhealthy_label"
                                                        role="button" style="font-size: 12px; width: 100px; display: inline-block; !important" data-toggle="modal" data-target="#modal-terminate-instance">
                                                        {{instance.unhealthyMessage}}</a>
                                                    </div>
                                                    <span title="{{msg.active_cluster_stack_description_hostgroup_state_tooltip}}" class="label label-info" style="font-size: 12px;" ng-switch-default>{{msg.active_cluster_stack_description_hostgroup_healthy_label}}</span>
                                                </td>
                                            </tr>
                                            </tbody>
                                        </table>
                                        <pagination boundary-links="true" total-items="pagination.totalItems" items-per-page="pagination.itemsPerPage"
                                             ng-model="pagination.currentPage" max-size="10"></pagination>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default" ng-repeat="group in $root.activeCluster.instanceGroups">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-{{$index}}-{{group.templateId}}'><span class="badge pull-right ng-binding">{{group.group}}: {{group.nodeCount}} {{msg.active_cluster_instance_group_node_label}}</span><i class="fa fa-file-o fa-fw"></i>Template: {{getSelectedTemplate(group.templateId).name}}</a></h5>
                            </div>
                            <div id="panel-collapsetmp-{{$index}}-{{group.templateId}}" class="panel-collapse collapse">
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'AWS' ">
                                    <div ng-include="'tags/template/awslist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
                                </div>
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'AZURE' ">
                                    <div ng-include="'tags/template/azurelist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
                                </div>
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'GCP' ">
                                    <div ng-include="'tags/template/gcplist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
                                </div>
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'OPENSTACK' ">
                                  <div ng-include="'tags/template/openstacklist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}:true"></div>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-securitygroup-{{$root.activeCluster.securityGroupId}}'><i class="fa fa-lock fa-fw"></i>{{msg.active_cluster_securitygroup_label}}: {{activeClusterSecurityGroup.name}}</a></h5>
                            </div>
                                <div id="panel-collapsetmp-securitygroup-{{$root.activeCluster.securityGroupId}}" class="panel-collapse collapse">
                                    <div class="panel-body">
                                        <div ng-include="'tags/securitygroup/securitygrouplist.tag'" ng-repeat="securitygroup in [activeClusterSecurityGroup]"></div>
                                    </div>
                                </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-network-{{$root.activeCluster.networkId}}'><i class="fa fa-sitemap fa-fw"></i>{{msg.active_cluster_network_label}} {{activeClusterNetwork.name}}</a></h5>
                            </div>
                                <div id="panel-collapsetmp-network-{{$root.activeCluster.networkId}}" class="panel-collapse collapse">
                                    <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'AWS' ">
                                        <div ng-include="'tags/network/awsnetworklist.tag'" ng-repeat="network in [activeClusterNetwork]"></div>
                                    </div>
                                    <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'AZURE' ">
                                        <div ng-include="'tags/network/azurenetworklist.tag'" ng-repeat="network in [activeClusterNetwork]"></div>
                                    </div>
                                    <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'GCP' ">
                                        <div ng-include="'tags/network/gcpnetworklist.tag'" ng-repeat="network in [activeClusterNetwork]"></div>
                                    </div>
                                    <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'OPENSTACK' ">
                                        <div ng-include="'tags/network/openstacknetworklist.tag'" ng-repeat="network in [activeClusterNetwork]"></div>
                                    </div>
                                </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse02"><i class="fa fa-th fa-fw"></i>{{msg.active_cluster_blueprint_label}} {{activeClusterBlueprint.name}}</a></h5>
                            </div>
                            <div id="panel-collapse02" class="panel-collapse collapse">
                                <div class="panel-body">
                                    <div class="row" ng-repeat="blueprint in [activeClusterBlueprint]" ng-include="'tags/blueprint/bplist.tag'" ></div>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse001"><i class="fa fa-tag fa-fw"></i>{{msg.active_cluster_credential_label}} {{activeClusterCredential.name}}</a></h5>
                            </div>
                            <div id="panel-collapse001" class="panel-collapse collapse">
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'AWS' ">
                                    <div ng-include="'tags/credential/awslist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
                                </div>
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'AZURE' ">
                                    <div ng-include="'tags/credential/azurelist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
                                </div>
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'GCP' ">
                                    <div ng-include="'tags/credential/gcplist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
                                </div>
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'OPENSTACK' ">
                                  <div ng-include="'tags/credential/openstacklist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
                                </div>
                            </div>
                        </div>
                    </section>
                    <section id="periscope-pane" ng-class="{ 'active': periscopeShow }" ng-show="periscopeShow" class="tab-pane fade in">
                        <div ng-include="'tags/periscope/periscope.tag'"></div>
                    </section>
                    <section id="metrics-pane" ng-class="{ 'active': metricsShow }" ng-show="metricsShow" targe class="tab-pane fade in">
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
    <div class="modal fade" id="modal-stop-cluster" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
        <div class="modal-dialog modal-sm">
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
                                        <select class="form-control" id="selectBlueprintreinstall" ng-model="reinstallClusterObject.blueprintId" required ng-change="selectBlueprintreinstallChange()" >
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
                                        <div class="panel-body">
                                            <div class="form-group" name="templateNodeform{{$index}}" >
                                                <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">{{msg.active_cluster_reset_hostgroup_label}}</label>
                                                <div class="col-sm-9">
                                                    <select class="form-control" id="newhostgroupsdiv" ng-model="newhostgroup.name" required ng-options="hgvalue.name as hgvalue.name for hgvalue in $root.reinstallClusterObject.fullBp.ambariBlueprint.host_groups">
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
                                                <div class="form-group" name="ambari_stack1" >
                                                    <label class="col-sm-3 control-label" for="ambari_stack">{{msg.cluster_form_ambari_repo_stack_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_stack" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.stack" id="ambari_stack" placeholder="{{msg.cluster_form_ambari_repo_stack_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_version1" >
                                                    <label class="col-sm-3 control-label" for="ambari_version">{{msg.cluster_form_ambari_repo_version_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_version" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.version" id="ambari_version" placeholder="{{msg.cluster_form_ambari_repo_version_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_os1" >
                                                    <label class="col-sm-3 control-label" for="ambari_os">{{msg.cluster_form_ambari_repo_os_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" disabled name="ambari_os" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.os" id="ambari_os" placeholder="{{msg.cluster_form_ambari_repo_os_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_stackRepoId1" >
                                                    <label class="col-sm-3 control-label" for="ambari_stackRepoId">{{msg.cluster_form_ambari_repo_stack_repoid_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_stackRepoId" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.stackRepoId" id="ambari_stackRepoId" placeholder="{{msg.cluster_form_ambari_repo_stack_repoid_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_stackBaseURL1" >
                                                    <label class="col-sm-3 control-label" for="ambari_stackBaseURL">{{msg.cluster_form_ambari_repo_baseurl_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_stackBaseURL" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.stackBaseURL" id="ambari_stackBaseURL" placeholder="{{msg.cluster_form_ambari_repo_baseurl_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_utilsRepoId1" >
                                                    <label class="col-sm-3 control-label" for="ambari_utilsRepoId">{{msg.cluster_form_ambari_repo_utils_repoid_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_utilsRepoId" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.utilsRepoId" id="ambari_utilsRepoId" placeholder="{{msg.cluster_form_ambari_repo_utils_repoid_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="ambari_utilsBaseURL1" >
                                                    <label class="col-sm-3 control-label" for="ambari_utilsBaseURL">{{msg.cluster_form_ambari_repo_utils_baseurl_label}}</label>
                                                    <div class="col-sm-9">
                                                        <input type="string" name="ambari_utilsBaseURL" class="form-control" ng-model="reinstallClusterObject.ambariStackDetails.utilsBaseURL" id="ambari_utilsBaseURL" placeholder="{{msg.cluster_form_ambari_repo_utils_baseurl_placeholder}}">
                                                    </div>
                                                </div>
                                                <div class="form-group" name="cluster_verify1" >
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
