<!-- .... network PANEL ................................................. -->

<div id="panel-network" ng-controller="networkController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="network-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-network-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.networks.length}}</span> {{msg.network_manage_title}}</h4>
        </div>

        <div id="panel-network-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('networks', userDetails.groups)">
                    <a href="" id="panel-create-network-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-network-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.network_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-network-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">
                                    <div class="btn-group btn-group-justified">
                                        <a id="awsNetworkChange" ng-if="isVisible('AWS')" type="button" ng-class="{'btn':true, 'btn-info':awsNetwork, 'btn-default':!awsNetwork}" role="button" ng-click="createAwsNetworkRequest()">{{msg.aws_label}}</a>
                                        <a id="azureNetworkChange" ng-if="isVisible('AZURE_RM')" ng-class="{'btn':true, 'btn-info':azureNetwork, 'btn-default':!azureNetwork}" role="button" ng-click="createAzureNetworkRequest()">{{msg.azure_label}}</a>
                                    </div>
                                    <div class="btn-group btn-group-justified">
                                        <a id="gcpNetworkChange" ng-if="isVisible('GCP')" ng-class="{'btn':true, 'btn-info':gcpNetwork, 'btn-default':!gcpNetwork}" role="button" ng-click="createGcpNetworkRequest()">{{msg.gcp_label}}</a>
                                        <a id="openstackNetworkChange" ng-if="isVisible('OPENSTACK')" ng-class="{'btn':true, 'btn-info':openstackNetwork, 'btn-default':!openstackNetwork}" role="button" ng-click="createOpenstackNetworkRequest()">{{msg.openstack_label}}</a>
                                    </div>
                                </div>
                            </div>

                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>

                            <form class="form-horizontal" role="form" name="azureNetworkForm" ng-show="azureNetwork && isVisible('AZURE_RM')">
                                <div ng-include src="'tags/network/azurenetworkform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="awsNetwork && isVisible('AWS')" name="awsNetworkForm">
                                <div ng-include src="'tags/network/awsnetworkform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="gcpNetwork && isVisible('GCP')" name="gcpNetworkForm" ng-show="gcpNetwork">
                                <div ng-include src="'tags/network/gcpnetworkform.tag'"></div>
                            </form>
                            <form class="form-horizontal" role="form" ng-show="openstackNetwork && isVisible('OPENSTACK')" name="openstackNetworkForm" ng-show="openstackNetwork">
                                <div ng-include src="'tags/network/openstacknetworkform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TEMPLATE LIST ........................................... -->

                <div class="panel-group" id="templete-list-accordion">

                    <!-- .............. TEMPLATE .............................................. -->

                    <div class="panel panel-default" ng-repeat="network in $root.networks | filter:filterByVisiblePlatform | orderBy:'name'">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#templete-list-accordion" data-target="#panel-network-collapse{{network.id}}"><i class="fa fa-file-o fa-fw"></i>{{network.name}}</a>
                                <span class="label label-info pull-right" >{{network.cloudPlatform}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="network.publicInAccount"></i>
                            </h5>
                        </div>
                        <div id="panel-network-collapse{{network.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('networks', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteNetwork(network)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.network_list_delete}}</span>
                                </a>
                            </p>

                            <div class="panel-body" ng-if="network.cloudPlatform == 'AZURE_RM'">
                                <div ng-include src="'tags/network/azurenetworklist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="network.cloudPlatform == 'GCP'">
                                <div ng-include src="'tags/network/gcpnetworklist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="network.cloudPlatform == 'AWS'">
                                <div ng-include src="'tags/network/awsnetworklist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="network.cloudPlatform == 'OPENSTACK'">
                                <div ng-include src="'tags/network/openstacknetworklist.tag'"></div>
                            </div>

                        </div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #network-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>