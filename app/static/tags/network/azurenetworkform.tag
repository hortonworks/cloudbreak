<div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkName.$dirty && azureNetworkForm.azure_networkName.$invalid }">
    <label class="col-sm-3 control-label" for="azure_networkName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="azure_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="azure_networkName" placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="azureNetworkForm.azure_networkName.$dirty && azureNetworkForm.azure_networkName.$invalid">
            <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkDescription.$dirty && azureNetworkForm.azure_networkDescription.$invalid }">
    <label class="col-sm-3 control-label" for="azure_networkDescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="azure_networkDescription" ng-model="network.description" ng-maxlength="1000" id="azure_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
        <div class="help-block" ng-show="azureNetworkForm.azure_networkDescription.$dirty && azureNetworkForm.azure_networkDescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkSubnet.$dirty && azureNetworkForm.azure_networkSubnet.$invalid }">
    <label class="col-sm-3 control-label" for="azure_networkSubnet">{{msg.network_form_subnet_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="azure_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="azure_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" ng-pattern="/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$/" required>
        <div class="help-block" ng-show="azureNetworkForm.azure_networkSubnet.$dirty && azureNetworkForm.azure_networkSubnet.$invalid">
            <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group">
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azure_networkRGId">{{msg.network_azure_form_custom_vpc_label}}</label>
        <div class="col-sm-9" />
    </div>
    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkRGId.$dirty && azureNetworkForm.azure_networkRGId.$invalid }">
        <label class="col-sm-3 col-sm-offset-2 control-label" for="azure_networkRGId">{{msg.network_azure_form_resource_group_id_label}}</label>

        <div class="col-sm-7">
            <input type="text" class="form-control" name="azure_networkRGId" ng-model="network.parameters.resourceGroupName" id="azure_networkRGId" placeholder="{{msg.network_azure_form_resource_group_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/">
            <div class="help-block" ng-show="azureNetworkForm.azure_networkRGId.$dirty && azureNetworkForm.azure_networkRGId.$invalid">
                <i class="fa fa-warning"></i> {{msg.network_resource_group_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkVPCId.$dirty && azureNetworkForm.azure_networkVPCId.$invalid }">
        <label class="col-sm-3 col-sm-offset-2 control-label" for="azure_networkVPCId">{{msg.network_azure_form_network_id_label}}</label>

        <div class="col-sm-7">
            <input type="text" class="form-control" name="azure_networkVPCId" ng-model="network.parameters.networkId" id="azure_networkVPCId" placeholder="{{msg.network_azure_form_network_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/">
            <div class="help-block" ng-show="azureNetworkForm.azure_networkVPCId.$dirty && azureNetworkForm.azure_networkVPCId.$invalid">
                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkSubnetId.$dirty && azureNetworkForm.azure_networkSubnetId.$invalid }">
        <label class="col-sm-3 col-sm-offset-2 control-label" for="azure_networkSubnetId">{{msg.network_azure_form_subnet_id_label}}</label>

        <div class="col-sm-7">
            <input type="text" class="form-control" name="azure_networkSubnetId" ng-model="network.parameters.subnetId" id="azure_networkSubnetId" placeholder="{{msg.network_azure_form_subnet_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/">
            <div class="help-block" ng-show="azureNetworkForm.azure_networkSubnetId.$dirty && azureNetworkForm.azure_networkSubnetId.$invalid">
                <i class="fa fa-warning"></i> {{msg.network_subnetid2_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
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
        <a id="createAwsTemplate" ng-disabled="azureNetworkForm.$invalid || !((azureNetworkForm.azure_networkSubnetId.$viewValue.length>0 && azureNetworkForm.azure_networkVPCId.$viewValue.length>0 && azureNetworkForm.azure_networkRGId.$viewValue.length>0) || (!network.parameters.resourceGroupName && !network.parameters.networkId && !network.parameters.subnetId))" class="btn btn-success btn-block" ng-click="createAzureNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.network_form_create}}</a>
    </div>
</div>
