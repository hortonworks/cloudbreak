<div id="active-cluster-panel" class="col-sm-11 col-md-11 col-lg-11">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="cluster-details-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>{{activeCluster.name}}</h4>
        </div>
        <div id="cluster-details-panel-collapse">
            <div class="panel-body">
                <ul class="nav nav-pills nav-justified" role="tablist">
                  <li class="active"><a ng-click="showDetails()" role="tab" data-toggle="tab">details</a></li>
                  <li><a role="tab" ng-click="showPeriscope()" role="tab" data-toggle="tab">autoscaling SLA policies</a></li>
                </ul>
                <div class="tab-content">
                    <section id="cluster-details-pane" ng-class="{ 'active': detailsShow }" ng-show="detailsShow" class="tab-pane fade in">
                        <p class="text-right">
                            <a href="" class="btn btn-success" role="button" ng-show="activeCluster.status == 'STOPPED'" data-toggle="modal" data-target="#modal-start-cluster">
                                <i class="fa fa-play fa-fw"></i><span> start</span>
                            </a>
                            <a href="" class="btn btn-warning" role="button" ng-show="activeCluster.status == 'AVAILABLE'" data-toggle="modal" data-target="#modal-stop-cluster">
                                <i class="fa fa-pause fa-fw"></i><span> stop</span>
                            </a>
                            <a href="" id="terminate-btn" class="btn btn-danger" role="button" data-toggle="modal" data-target="#modal-terminate">
                                <i class="fa fa-times fa-fw"></i><span> terminate</span>
                            </a>
                        </p>
                        <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_ambariServerIp">Ambari server adress</label>
                                <div class="col-sm-9">
                                    <p id="sl_ambariServerIp" class="form-control-static">
                                        <div ng-if="activeCluster.ambariServerIp != null">
                                            <a ng-show="activeCluster.ambariServerIp != null" target="_blank" href="http://{{activeCluster.ambariServerIp}}:8080">http://{{activeCluster.ambariServerIp}}:8080</a>
                                        </div>
                                        <div ng-if="activeCluster.ambariServerIp == null">
                                            <a ng-show="activeCluster.ambariServerIp == null" target="_blank" href="">Not Available</a>
                                        </div>
                                    </p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_cloudPlatform">Platform</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudPlatform" class="form-control-static">{{activeCluster.cloudPlatform}}</p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_nodeCount">Number of nodes</label>
                                <div class="col-sm-9">
                                    <p id="sl_nodeCount" class="form-control-static">{{activeCluster.nodeCount}}</p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="sl_region">Region</label>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'AWS' ">
                                    <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.AWS.awsRegions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'GCC' ">
                                     <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.GCC.gccRegions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'AZURE' ">
                                    <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.AZURE.azureRegions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cloudPlatform == 'OPENSTACK' ">
                                  <p id="sl_region" class="form-control-static" ng-repeat="item in $root.config.OPENSTACK.regions | filter:{key: activeCluster.region}:true">{{item.value}}</p>
                                </div>
                            </div>
                            <div class="form-group" ng-if="activeCluster.cluster.statusReason === null && (activeCluster.statusReason != null && activeCluster.statusReason != '')">
                                <label class="col-sm-3 control-label" for="sl_cloudStatus">Cluster status</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.statusReason}}</p>
                                </div>
                                <div class="col-sm-9" ng-if="activeCluster.cluster.statusReason != null && activeCluster.cluster.statusReason != ''">
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.cluster.statusReason}}</p>
                                </div>
                            </div>
                            <div class="form-group" ng-if="activeCluster.cluster.statusReason != null && activeCluster.cluster.statusReason != ''">
                                <label class="col-sm-3 control-label" for="sl_cloudStatus">Cluster status</label>
                                <div class="col-sm-9">
                                    <p id="sl_cloudStatus" class="form-control-static">{{activeCluster.cluster.statusReason}}2</p>
                                </div>
                            </div>
                        </form>

                        <div class="panel panel-default panel-cluster-history">
                            <div class="panel-heading">
                                <h5><a data-toggle="collapse" data-target="#cluster-history-collapse01"><i class="fa fa-clock-o fa-fw"></i>Event history</a></h5>
                            </div>

                            <div id="cluster-history-collapse01" class="panel-collapse collapse" style="height: auto;">

                                <div class="panel-body">

                                    <form class="form-horizontal" role="document">

                                        <div class="form-group">
                                            <div class="col-sm-12">
												<pre class="form-control-static event-history">
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
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse0002"><i class="fa fa-align-justify fa-fw"></i>Cloud stack description: {{activeCluster.name}}</a></h5>
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
                                                 <th>
                                                 <span>Name</span>
                                                 </th>
                                                 <th>
                                                 <span>Public Address</span>
                                                 </th>
                                                 <th>
                                                 <span>Private Address</span>
                                                 </th>
                                                 <th>
                                                 <span>Hostgroup Name</span>
                                                 </th>
                                               </tr>
                                            </thead>
                                            <tbody>
                                            <tr ng-repeat="instance in filteredActiveClusterData">
                                                <td data-title="'name'" class="col-md-4">{{instance.longName}}</td>
                                                <td data-title="'public IP'" class="col-md-3">{{instance.publicIp}}</td>
                                                <td data-title="'private IP'" class="col-md-3">{{instance.privateIp}}</td>
                                                <td data-title="'host group'" class="col-md-2"><span class="label label-default">{{instance.instanceGroup}}</span></td>
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
                                <h5><a href="" data-toggle="collapse" data-target='#panel-collapsetmp-{{$index}}-{{group.templateId}}'><span class="badge pull-right ng-binding">{{group.group}}: {{group.nodeCount}} node</span><i class="fa fa-file-o fa-fw"></i>Template: {{getSelectedTemplate(group.templateId).name}}</a></h5>
                            </div>
                            <div id="panel-collapsetmp-{{$index}}-{{group.templateId}}" class="panel-collapse collapse">
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'AWS' ">
                                    <div ng-include="'tags/template/awslist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}"></div>
                                </div>
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'AZURE' ">
                                    <div ng-include="'tags/template/azurelist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}"></div>
                                </div>
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'GCC' ">
                                    <div ng-include="'tags/template/gcclist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}"></div>
                                </div>
                                <div class="panel-body" ng-if="$root.activeCluster.cloudPlatform == 'OPENSTACK' ">
                                  <div ng-include="'tags/template/openstacklist.tag'" ng-repeat="template in $root.templates| filter:{id: group.templateId}"></div>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse02"><i class="fa fa-th fa-fw"></i>Blueprint: {{activeClusterBlueprint.name}}</a></h5>
                            </div>
                            <div id="panel-collapse02" class="panel-collapse collapse">
                                <div class="panel-body">
                                    <div class="row" ng-repeat="blueprint in [activeClusterBlueprint]" ng-include="'tags/blueprint/bplist.tag'" ></div>
                                </div>
                            </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h5><a href="" data-toggle="collapse" data-target="#panel-collapse001"><i class="fa fa-tag fa-fw"></i>Credential: {{activeClusterCredential.name}}</a></h5>
                            </div>
                            <div id="panel-collapse001" class="panel-collapse collapse">
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'AWS' ">
                                    <div ng-include="'tags/credential/awslist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
                                </div>
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'AZURE' ">
                                    <div ng-include="'tags/credential/azurelist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
                                </div>
                                <div class="panel-body" ng-if="activeClusterCredential.cloudPlatform == 'GCC' ">
                                    <div ng-include="'tags/credential/gcclist.tag'" ng-repeat="credential in [activeClusterCredential]"></div>
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
                    <p>Terminate cluster <strong>{{activeCluster.name}}</strong> and its stack?</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" id="terminateStackBtn" ng-click="deleteCluster(activeCluster)"><i class="fa fa-times fa-fw"></i>terminate</button>
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
                    <p>Stop cluster <strong>{{activeCluster.name}}</strong> and its stack?</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-warning" data-dismiss="modal" id="stackStackBtn" ng-click="stopCluster(activeCluster)"><i class="fa fa-pause fa-fw"></i>stop</button>
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
                    <p>Start cluster <strong>{{activeCluster.name}}</strong> and its stack?</p>
                </div>
                <div class="modal-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
                        </div>
                        <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-success" data-dismiss="modal" id="stackStackBtn" ng-click="startCluster(activeCluster)"><i class="fa fa-play fa-fw"></i>start</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
