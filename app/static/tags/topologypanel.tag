<!-- .... TOPOLOGIES PANEL ................................................. -->

<div id="panel-topoligies" ng-controller="topologyController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="topologies-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-topologies-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.topologies.length}}</span> {{msg.topology_manage_title}}</h4>
        </div>

        <div id="panel-topologies-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel">
                    <a href="" id="panel-create-topoligies-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-topologies-collapse" ng-click="cleanupScope()">
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
                                        <a id="openstackTopologyChange" class="btn btn-default" role="button" ng-click="createOpenstackTopologyRequest()">{{msg.openstack_label}}</a>
                                    </div>
                                </div>
                            </div>

                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>

                            <form class="form-horizontal" role="form" ng-show="openstackTopology" name="openstackTopologyForm" ng-show="openstackTopology">
                                <div ng-include src="'tags/topology/openstackform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TOPOLOGY LIST ........................................... -->

                <div class="panel-group" id="topoligies-list-accordion">

                    <!-- .............. TOPOLOGY .............................................. -->

                    <div class="panel panel-default" ng-repeat="topology in $root.topologies | orderBy:['cloudPlatform', 'name']">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#topoligies-list-accordion" data-target="#panel-topoligies-collapse{{topology.id}}"><i class="fa fa-file-o fa-fw"></i>{{topology.name}}</a>
                                <span class="label label-info pull-right" >{{topology.cloudPlatform}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px"></i>
                            </h5>
                        </div>
                        <div id="panel-topoligies-collapse{{topology.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel">
                                <a style="width: 90px" href="" class="btn btn-danger" role="button" ng-click="deleteTopology(topology)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.topology_list_delete}}</span>
                                </a>
                            </p>
                            <p class="btn-row-over-panel">
                                <a style="width: 90px" href="" class="btn btn-warning" role="button" ng-click="modifyTopology(topology)" data-toggle="collapse" data-target="#panel-create-topologies-collapse">
                                    <i class="fa fa-times fa-fw"></i><span> modify</span>
                                </a>
                            </p>

                            <div class="panel-body" ng-if="topology.cloudPlatform == 'OPENSTACK' ">
                                <div ng-include src="'tags/topology/openstacklist.tag'"></div>
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