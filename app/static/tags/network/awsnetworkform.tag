    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm.aws_networkName.$dirty && awsNetworkForm.aws_networkName.$invalid }">
        <label class="col-sm-3 control-label" for="aws_networkName">Name</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="aws_networkName" placeholder="min. 5 max. 100 char">
            <div class="help-block" ng-show="awsNetworkForm.aws_networkName.$dirty && awsNetworkForm.aws_networkName.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.network_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm.aws_networkDescription.$dirty && awsNetworkForm.aws_networkDescription.$invalid }">
        <label class="col-sm-3 control-label" for="aws_networkDescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="aws_networkDescription" ng-model="network.description" ng-maxlength="1000" id="aws_networkDescription" placeholder="max. 1000 char">
            <div class="help-block" ng-show="awsNetworkForm.aws_networkDescription.$dirty && awsNetworkForm.aws_networkDescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.netowrk_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm.aws_networkSubnet.$dirty && awsNetworkForm.aws_networkSubnet.$invalid }">
        <label class="col-sm-3 control-label" for="aws_networkSubnet">Subnet (CIDR)</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="aws_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="aws_networkSubnet" placeholder="a valid CIDR notation" ng-pattern="/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$/" required>
            <div class="help-block" ng-show="awsNetworkForm.aws_networkSubnet.$dirty && awsNetworkForm.aws_networkSubnet.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.network_subnet_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>


    <div class="form-group">
        <div class="form-group">
            <label class="col-sm-3 control-label" for="aws_networkVPCId">Custom VPC configuration (optional)</label>
            <div class="col-sm-9" />
        </div>
        <div class="form-group" ng-class="{ 'has-error': awsNetworkForm.aws_networkVPCId.$dirty && awsNetworkForm.aws_networkVPCId.$invalid }">
            <label class="col-sm-3 col-sm-offset-2 control-label" for="aws_networkVPCId">Virtual Private Cloud Identifier (VPC ID)</label>

            <div class="col-sm-7">
                <input type="text" class="form-control" name="aws_networkVPCId" ng-model="network.parameters.vpcId" ng-maxlength="30" id="aws_networkVPCId" placeholder="vpc-xxxxxxxx" ng-pattern="/vpc-[a-zA-Z0-9]{8}/">
                <div class="help-block" ng-show="awsNetworkForm.aws_networkVPCId.$dirty && awsNetworkForm.aws_networkVPCId.$invalid">
                    <i class="fa fa-warning"></i> {{error_msg.network_vpcid_invalid}}
                </div>
            </div>
            <!-- .col-sm-9 -->
        </div>

        <div class="form-group" ng-class="{ 'has-error': awsNetworkForm.aws_networkIGWID.$dirty && awsNetworkForm.aws_networkIGWID.$invalid }">
            <label class="col-sm-3 col-sm-offset-2 control-label" for="aws_networkIGWID">Internet Gateway Identifier</label>

            <div class="col-sm-7">
                <input type="text" class="form-control" name="aws_networkIGWID" ng-model="network.parameters.internetGatewayId" ng-maxlength="30" id="aws_networkIGWID" placeholder="igw-xxxxxxxx" ng-pattern="/igw-[a-zA-Z0-9]{8}/">
                <div class="help-block" ng-show="awsNetworkForm.aws_networkIGWID.$dirty && awsNetworkForm.aws_networkIGWID.$invalid">
                    <i class="fa fa-warning"></i> {{error_msg.network_internetgatewayid_invalid}}
                </div>
            </div>
            <!-- .col-sm-9 -->
        </div>
    </div>

    <div class="form-group">
            <label class="col-sm-3 control-label" for="aws_network_public">Public in account</label>
            <div class="col-sm-9">
                <input type="checkbox" name="aws_network_public" id="aws_network_public" ng-model="network.publicInAccount">
            </div>
       <!-- .col-sm-9 -->
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createAwsTemplate" ng-disabled="awsNetworkForm.$invalid || !((awsNetworkForm.aws_networkVPCId.$viewValue.length>0 && awsNetworkForm.aws_networkIGWID.$viewValue.length>0) || (!network.parameters.vpcId && !network.parameters.internetGatewayId))" class="btn btn-success btn-block" ng-click="createAwsNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                create network</a>
        </div>
    </div>
