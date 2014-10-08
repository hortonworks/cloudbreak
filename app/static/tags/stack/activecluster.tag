<div id="active-cluster-panel" ng-controller="clusterController" class="col-sm-11 col-md-9 col-lg-9">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="cluster-details-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>{{activeCluster.name}}</h4>
        </div>
        <div id="cluster-details-panel-collapse">
            <div class="panel-body">
                <p class="text-right">
                    <a href="" class="btn btn-info" role="button"><i class="fa fa-pause fa-fw"></i><span> stop</span></a>
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
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <h5><a href="" data-toggle="collapse" data-target="#panel-collapse0002"><i class="fa fa-align-justify fa-fw"></i>Cloud stack description: {{activeCluster.name}}</a></h5>
                    </div>
                    <div id="panel-collapse0002" class="panel-collapse collapse">
                        <div class="panel-body">
                            <pre id="sl_description" class="form-control-static">{{activeCluster.description | json}}</pre>
                        </div>
                    </div>
                </div>
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <h5><a href="" data-toggle="collapse" data-target='#panel-collapse01'><i class="fa fa-file-o fa-fw"></i>Template: {{activeClusterTemplate.name}}</a></h5>
                    </div>
                    <div id="panel-collapse01" class="panel-collapse collapse">
                        <div class="panel-body" ng-if="activeClusterTemplate.cloudPlatform == 'AWS' ">
                            <div ng-include="'tags/template/awslist.tag'" ng-repeat="template in [activeClusterTemplate]"></div>
                        </div>
                        <div class="panel-body" ng-if="activeClusterTemplate.cloudPlatform == 'AZURE' ">
                            <div ng-include="'tags/template/azurelist.tag'" ng-repeat="template in [activeClusterTemplate]"></div>
                        </div>
                        <div class="panel-body" ng-if="activeClusterTemplate.cloudPlatform == 'GCC' ">
                            <div ng-include="'tags/template/gcclist.tag'" ng-repeat="template in [activeClusterTemplate]"></div>
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
                    </div>
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

</div>
