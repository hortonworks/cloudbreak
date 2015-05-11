    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkName.$dirty && azureNetworkForm.azure_networkName.$invalid }">
        <label class="col-sm-3 control-label" for="azure_networkName">Name</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="azure_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="azure_networkName" placeholder="min. 5 max. 100 char">
            <div class="help-block" ng-show="azureNetworkForm.azure_networkName.$dirty && azureNetworkForm.azure_networkName.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.network_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkDescription.$dirty && azureNetworkForm.azure_networkDescription.$invalid }">
        <label class="col-sm-3 control-label" for="azure_networkDescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="azure_networkDescription" ng-model="network.description" ng-maxlength="1000" id="azure_networkDescription" placeholder="max. 1000 char">
            <div class="help-block" ng-show="azureNetworkForm.azure_networkDescription.$dirty && azureNetworkForm.azure_networkDescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.netowrk_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkSubnet.$dirty && azureNetworkForm.azure_networkSubnet.$invalid }">
        <label class="col-sm-3 control-label" for="azure_networkSubnet">Subnet (CIDR)</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="azure_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="azure_networkSubnet" placeholder="a valid CIDR notation" ng-pattern="/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$/" required>
            <div class="help-block" ng-show="azureNetworkForm.azure_networkSubnet.$dirty && azureNetworkForm.azure_networkSubnet.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.network_subnet_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>  
    <div class="form-group" ng-class="{ 'has-error': azureNetworkForm.azure_networkAddressPrefix.$dirty && azureNetworkForm.azure_networkAddressPrefix.$invalid }">
        <label class="col-sm-3 control-label" for="azure_networkAddressPrefix">Address prefix (CIDR)</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="azure_networkAddressPrefix" ng-model="network.parameters.addressPrefixCIDR" ng-maxlength="30" id="azure_networkAddressPrefix" placeholder="a valid CIDR notation" ng-pattern="/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$/" required>
            <div class="help-block" ng-show="azureNetworkForm.azure_networkAddressPrefix.$dirty && azureNetworkForm.azure_networkAddressPrefix.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.network_subnet_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
            <label class="col-sm-3 control-label" for="azure_network_public">Public in account</label>
            <div class="col-sm-9">
                <input type="checkbox" name="azure_network_public" id="azure_network_public" ng-model="network.publicInAccount">
            </div>
       <!-- .col-sm-9 -->
    </div>
    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createAwsTemplate" ng-disabled="azureNetworkForm.$invalid" class="btn btn-success btn-block" ng-click="createAzureNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                create network</a>
        </div>
    </div>
