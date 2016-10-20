<!-- .... TOPOLOGIES PANEL ................................................. -->

<div id="panel-topoligies" ng-controller="topologyController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="topologies-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-topologies-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{countList($root.topologies)}}</span> {{msg.topology_manage_title}}</h4>
        </div>

        <div id="panel-topologies-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('platforms', userDetails.groups)">
                    <a href="" id="panel-create-topologies-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-topologies-collapse" >
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.topology_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-topologies-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">

                                    <div class="btn-group btn-group-justified">
                                        <a id="awsTopologyChange" ng-if="isVisible('AWS')" type="button" ng-class="{'btn':true, 'btn-info':awsTopology, 'btn-default':!awsTopology}" role="button" ng-click="createAwsTopologyRequest()">{{msg.aws_label}}</a>
                                        <a id="azureTopologyChange" ng-if="isVisible('AZURE_RM')" ng-class="{'btn':true, 'btn-info':azureTopology, 'btn-default':!azureTopology}" role="button" ng-click="createAzureTopologyRequest()">{{msg.azure_label}}</a>
                                    </div>
                                    <div class="btn-group btn-group-justified" ng-if="isVisible('GCP') || isVisible('OPENSTACK')">
                                        <a id="gcpTopologyChange" ng-if="isVisible('GCP')" class="btn btn-default" ng-class="{'btn':true, 'btn-info':gcpTopology, 'btn-default':!gcpTopology}" role="button" ng-click="createGcpTopologyRequest()">{{msg.gcp_label}}</a>
                                        <a id="openstackTopologyChange" ng-if="isVisible('OPENSTACK')" ng-class="{'btn':true, 'btn-info':openstackTopology, 'btn-default':!openstackTopology}" role="button" ng-click="createOpenstackTopologyRequest()">{{msg.openstack_label}}</a>
                                    </div>

                                </div>
                            </div>

                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>

                            <form class="form-horizontal" role="form" name="azureTopologyForm" ng-show="azureTopology && isVisible('AZURE_RM')">
                                <div ng-include src="'tags/topology/azureform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="awsTopology && isVisible('AWS')" name="awsTopologyForm">
                                <div ng-include src="'tags/topology/awsform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="gcpTopology && isVisible('GCP')" name="gcpTopologyForm" ng-show="gcpTopology">
                                <div ng-include src="'tags/topology/gcpform.tag'"></div>
                            </form>
                            <form class="form-horizontal" role="form" ng-show="openstackTopology && isVisible('OPENSTACK')" name="openstackTopologyForm" ng-show="openstackTopology">
                                <div ng-include src="'tags/topology/openstackform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TOPOLOGY LIST ........................................... -->

                <div class="panel-group" id="topoligies-list-accordion">

                    <!-- .............. TOPOLOGY .............................................. -->

                    <div class="panel panel-default" ng-repeat="topology in $root.topologies | filter:filterByVisiblePlatform | orderBy:['cloudPlatform', 'name']">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#topoligies-list-accordion" data-target="#panel-topoligies-collapse{{topology.id}}"><i class="fa fa-file-o fa-fw"></i>{{topology.name}}</a>
                                <span class="label label-info pull-right" >{{topology.cloudPlatform}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px"></i>
                            </h5>
                        </div>
                        <div id="panel-topoligies-collapse{{topology.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('platforms', userDetails.groups)">
                                <a style="width: 90px" href="" class="btn btn-danger" role="button" ng-click="deleteTopology(topology)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.topology_list_delete}}</span>
                                </a>
                            </p>
                            <p class="btn-row-over-panel">
                                <a style="width: 90px" href="" class="btn btn-warning" role="button" ng-show="topology.cloudPlatform === 'OPENSTACK'" ng-click="modifyTopology(topology)" data-toggle="collapse" data-target="#panel-create-topologies-collapse">
                                    <i class="fa fa-times fa-fw"></i><span> modify</span>
                                </a>
                            </p>

                            <div class="panel-body" ng-if="topology.cloudPlatform === 'OPENSTACK' ">
                                <div ng-include src="'tags/topology/openstacklist.tag'"></div>
                            </div>
                            <div class="panel-body" ng-if="topology.cloudPlatform === 'AWS' ">
                                <div ng-include src="'tags/topology/awslist.tag'"></div>
                            </div>
                            <div class="panel-body" ng-if="topology.cloudPlatform === 'GCP' ">
                                <div ng-include src="'tags/topology/gcplist.tag'"></div>
                            </div>
                            <div class="panel-body" ng-if="topology.cloudPlatform === 'AZURE_RM' ">
                                <div ng-include src="'tags/topology/azurelist.tag'"></div>
                            </div>

                        </div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #topologies-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>