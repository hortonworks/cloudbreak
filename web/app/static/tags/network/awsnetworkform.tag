</form>
<div class="panel-group" id="aws-net-accordion" role="tablist" aria-multiselectable="true">
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="aws-net-headingOne">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#aws-net-accordion" href="#aws-net-collapseOne" aria-expanded="true" aria-controls="aws-net-collapseOne" ng-click="selectAwsNetworkType1()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_aws_form_type1_title}}</span>
                </a>
            </h4>
        </div>
        <div id="aws-net-collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="aws-net-headingOne">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_aws_form_type1_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="awsNetworkForm_1">
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_1.aws_networkName.$dirty && awsNetworkForm_1.aws_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="aws_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="awsNetworkForm_1.aws_networkName.$dirty && awsNetworkForm_1.aws_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_1.aws_networkDescription.$dirty && awsNetworkForm_1.aws_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkDescription" ng-model="network.description" ng-maxlength="1000" id="aws_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="awsNetworkForm_1.aws_networkDescription.$dirty && awsNetworkForm_1.aws_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_1.aws_networkSubnet.$dirty && awsNetworkForm_1.aws_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkSubnet">{{msg.network_form_subnet_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="aws_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" subnetrvalidation required>
                            <div class="help-block" ng-show="awsNetworkForm_1.aws_networkSubnet.$dirty && awsNetworkForm_1.aws_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="aws_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="aws_network_public" id="aws_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="awsNetworkForm_1.$invalid || !((awsNetworkForm_1.aws_networkVPCId.$viewValue.length>0 && awsNetworkForm_1.aws_networkIGWID.$viewValue.length>0) || (!network.parameters.vpcId && !network.parameters.internetGatewayId && !network.parameters.subnetId))" class="btn btn-success btn-block" ng-click="createAwsNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="aws-net-headingTwo">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#aws-net-accordion" href="#aws-net-collapseTwo" aria-expanded="false" aria-controls="aws-net-collapseTwo" ng-click="selectAwsNetworkType2()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_aws_form_type2_title}}</span>
                </a>
            </h4>
        </div>
        <div id="aws-net-collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="aws-net-headingTwo">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_aws_form_type2_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="awsNetworkForm_2">
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_2.aws_networkName.$dirty && awsNetworkForm_2.aws_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="aws_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="awsNetworkForm_2.aws_networkName.$dirty && awsNetworkForm_2.aws_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_2.aws_networkDescription.$dirty && awsNetworkForm_2.aws_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkDescription" ng-model="network.description" ng-maxlength="1000" id="aws_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="awsNetworkForm_2.aws_networkDescription.$dirty && awsNetworkForm_2.aws_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_2.aws_networkVPCId.$dirty && awsNetworkForm_2.aws_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkVPCId">{{msg.network_aws_form_vpc_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkVPCId" ng-model="network.parameters.vpcId" id="aws_networkVPCId" placeholder="{{msg.network_aws_form_vpc_id_placeholder}}" ng-pattern="/^vpc-[a-zA-Z0-9]{8}$/" required>
                            <div class="help-block" ng-show="awsNetworkForm_2.aws_networkVPCId.$dirty && awsNetworkForm_2.aws_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_vpcid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_2.aws_networkIGWID.$dirty && awsNetworkForm_2.aws_networkIGWID.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkIGWID">{{msg.network_aws_form_gateway_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkIGWID" ng-model="network.parameters.internetGatewayId" id="aws_networkIGWID" placeholder="{{msg.network_aws_form_gateway_id_placeholder}}" ng-pattern="/^igw-[a-zA-Z0-9]{8}$/" required>
                            <div class="help-block" ng-show="awsNetworkForm_2.aws_networkIGWID.$dirty && awsNetworkForm_2.aws_networkIGWID.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_internetgatewayid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_2.aws_networkSubnet.$dirty && awsNetworkForm_2.aws_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkSubnet">{{msg.network_form_subnet_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="aws_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" subnetrvalidation required>
                            <div class="help-block" ng-show="awsNetworkForm_2.aws_networkSubnet.$dirty && awsNetworkForm_2.aws_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="aws_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="aws_network_public" id="aws_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="awsNetworkForm_2.$invalid || !((awsNetworkForm_2.aws_networkVPCId.$viewValue.length>0 && awsNetworkForm_2.aws_networkIGWID.$viewValue.length>0) || (!network.parameters.vpcId && !network.parameters.internetGatewayId && !network.parameters.subnetId))" class="btn btn-success btn-block" ng-click="createAwsNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="aws-net-headingThree">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#aws-net-accordion" href="#aws-net-collapseThree" aria-expanded="false" aria-controls="aws-net-collapseThree" ng-click="selectAwsNetworkType3()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_aws_form_type3_title}}</span>
                </a>
            </h4>
        </div>
        <div id="aws-net-collapseThree" class="panel-collapse collapse" role="tabpanel" aria-labelledby="aws-net-headingThree">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_aws_form_type3_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="awsNetworkForm_3">
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_3.aws_networkName.$dirty && awsNetworkForm_3.aws_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" id="aws_networkName" placeholder="{{msg.name_placeholder}}" required>
                            <div class="help-block" ng-show="awsNetworkForm_3.aws_networkName.$dirty && awsNetworkForm_3.aws_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_3.aws_networkDescription.$dirty && awsNetworkForm_3.aws_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkDescription" ng-model="network.description" ng-maxlength="1000" id="aws_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="awsNetworkForm_3.aws_networkDescription.$dirty && awsNetworkForm_3.aws_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_3.aws_networkVPCId.$dirty && awsNetworkForm_3.aws_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkVPCId">{{msg.network_aws_form_vpc_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkVPCId" ng-model="network.parameters.vpcId" id="aws_networkVPCId" placeholder="{{msg.network_aws_form_vpc_id_placeholder}}" ng-pattern="/^vpc-[a-zA-Z0-9]{8}$/" required>
                            <div class="help-block" ng-show="awsNetworkForm_3.aws_networkVPCId.$dirty && awsNetworkForm_3.aws_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_vpcid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>



                    <div class="form-group" ng-class="{ 'has-error': awsNetworkForm_3.aws_networkSubnetId.$dirty && awsNetworkForm_3.aws_networkSubnetId.$invalid }">
                        <label class="col-sm-3 control-label" for="aws_networkSubnetId">{{msg.network_aws_form_subnet_id_label_optional}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="aws_networkSubnetId" ng-model="network.parameters.subnetId" id="aws_networkSubnetId" placeholder="{{msg.network_aws_form_subnet_id_placeholder}}" ng-pattern="/^subnet-[a-zA-Z0-9]{8}(,subnet-[a-zA-Z0-9]{8})*$/" required>
                            <div class="help-block" ng-show="awsNetworkForm_3.aws_networkSubnetId.$dirty && awsNetworkForm_3.aws_networkSubnetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnetid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="aws_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="aws_network_public" id="aws_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="awsNetworkForm_3.$invalid || !((awsNetworkForm_3.aws_networkVPCId.$viewValue.length>0) || (!network.parameters.vpcId && !network.parameters.subnetId))" class="btn btn-success btn-block" ng-click="createAwsNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
</div>
<form>