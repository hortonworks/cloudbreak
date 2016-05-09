</form>
<div class="panel-group" id="azure-net-accordion" role="tablist" aria-multiselectable="true">
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="azure-net-headingOne">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#azure-net-accordion" href="#azure-net-collapseOne" aria-expanded="true" aria-controls="azure-net-collapseOne" ng-click="selectAzureNetworkType1()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_azure_form_type1_title}}</span>
                </a>
            </h4>
        </div>
        <div id="azure-net-collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="azure-net-headingOne">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_azure_form_type1_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="azureNetworkForm_1">
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_1.azure_networkName.$dirty && azureNetworkForm_1.azure_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkName">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="azure_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="azure_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="azureNetworkForm_1.azure_networkName.$dirty && azureNetworkForm_1.azure_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_1.azure_networkDescription.$dirty && azureNetworkForm_1.azure_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="azure_networkDescription" ng-model="network.description" ng-maxlength="1000" id="azure_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="azureNetworkForm_1.azure_networkDescription.$dirty && azureNetworkForm_1.azure_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_1.azure_networkSubnet.$dirty && azureNetworkForm_1.azure_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkSubnet">{{msg.network_form_subnet_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="azure_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="azure_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" subnetrvalidation required>
                            <div class="help-block" ng-show="azureNetworkForm_1.azure_networkSubnet.$dirty && azureNetworkForm_1.azure_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="azure_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="azure_network_public" id="azure_network_public" ng-model="network.publicInAccount">
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="topologySelect" name="topologySelect" ng-model="network.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
                                <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
                            </select>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a id="createAwsTemplate" ng-disabled="azureNetworkForm_1.$invalid" class="btn btn-success btn-block" ng-click="createAzureNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                                    {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="azure-net-headingTwo">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#azure-net-accordion" href="#azure-net-collapseTwo" aria-expanded="false" aria-controls="azure-net-collapseTwo" ng-click="selectAzureNetworkType2()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_azure_form_type2_title}}</span>
                </a>
            </h4>
        </div>
        <div id="azure-net-collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="azure-net-headingTwo">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_azure_form_type2_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="azureNetworkForm_2">
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_2.azure_networkName.$dirty && azureNetworkForm_2.azure_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="azure_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="azure_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="azureNetworkForm_2.azure_networkName.$dirty && azureNetworkForm_2.azure_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_2.azure_networkDescription.$dirty && azureNetworkForm_2.azure_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="azure_networkDescription" ng-model="network.description" ng-maxlength="1000" id="azure_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="azureNetworkForm_2.azure_networkDescription.$dirty && azureNetworkForm_2.azure_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_2.azure_networkRGId.$dirty && azureNetworkForm_2.azure_networkRGId.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkRGId">{{msg.network_azure_form_resource_group_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="azure_networkRGId" ng-model="network.parameters.resourceGroupName" id="azure_networkRGId" placeholder="{{msg.network_azure_form_resource_group_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="azureNetworkForm_2.azure_networkRGId.$dirty && azureNetworkForm_2.azure_networkRGId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_resource_group_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_2.azure_networkVPCId.$dirty && azureNetworkForm_2.azure_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkVPCId">{{msg.network_azure_form_network_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="azure_networkVPCId" ng-model="network.parameters.networkId" id="azure_networkVPCId" placeholder="{{msg.network_azure_form_network_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="azureNetworkForm_2.azure_networkVPCId.$dirty && azureNetworkForm_2.azure_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm_2.azure_networkSubnetId.$dirty && azureNetworkForm_2.azure_networkSubnetId.$invalid }">
                        <label class="col-sm-3 control-label" for="azure_networkSubnetId">{{msg.network_azure_form_subnet_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="azure_networkSubnetId" ng-model="network.parameters.subnetId" id="azure_networkSubnetId" placeholder="{{msg.network_azure_form_subnet_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="azureNetworkForm_2.azure_networkSubnetId.$dirty && azureNetworkForm_2.azure_networkSubnetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnetid2_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="azure_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="azure_network_public" id="azure_network_public" ng-model="network.publicInAccount">
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="topologySelect" name="topologySelect" ng-model="network.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
                                <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
                            </select>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a id="createAwsTemplate" ng-disabled="azureNetworkForm_2.$invalid" class="btn btn-success btn-block" ng-click="createAzureNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                                    {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>