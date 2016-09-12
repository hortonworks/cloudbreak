</form>
<div class="panel-group" id="gcp-net-accordion" role="tablist" aria-multiselectable="true">
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="gcp-net-headingOne">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#gcp-net-accordion" href="#gcp-net-collapseOne" aria-expanded="true" aria-controls="gcp-net-collapseOne" ng-click="selectGcpNetworkType1()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_gcp_form_type1_title}}</span>
                </a>
            </h4>
        </div>
        <div id="gcp-net-collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="gcp-net-headingOne">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_gcp_form_type1_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="gcpNetworkForm_1">
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_1.gcp_networkName.$dirty && gcpNetworkForm_1.gcp_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkName">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcp_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="gcp_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_1.gcp_networkName.$dirty && gcpNetworkForm_1.gcp_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_1.gcp_networkDescription.$dirty && gcpNetworkForm_1.gcp_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkDescription">{{msg.description_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkDescription" ng-model="network.description" ng-maxlength="1000" id="gcp_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_1.gcp_networkDescription.$dirty && gcpNetworkForm_1.gcp_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_1.gcp_networkSubnet.$dirty && gcpNetworkForm_1.gcp_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkSubnet">{{msg.network_form_subnet_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="gcp_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" subnetrvalidation required>
                            <div class="help-block" ng-show="gcpNetworkForm_1.gcp_networkSubnet.$dirty && gcpNetworkForm_1.gcp_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="gcp_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="gcp_network_public" id="gcp_network_public" ng-model="network.publicInAccount">
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
                            <a id="createGcpNetwork_1" ng-disabled="gcpNetworkForm_1.$invalid" class="btn btn-success btn-block" ng-click="createGcpNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                            {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="gcp-net-headingTwo">
            <div class="panel-title">
                <a role="button" data-toggle="collapse" data-parent="#gcp-net-accordion" href="#gcp-net-collapseTwo" aria-expanded="false" aria-controls="gcp-net-collapseTwo" ng-click="selectGcpNetworkType2()">
                    <i class="fa fa-sitemap fa-fw" /><span style="padding-left: 10px">{{msg.network_gcp_form_type2_title}}</span>
                </a>
            </div>
        </div>
        <div id="gcp-net-collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="gcp-net-headingTwo">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_gcp_form_type2_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="gcpNetworkForm_2">
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_2.gcp_networkName.$dirty && gcpNetworkForm_2.gcp_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkName">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcp_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="gcp_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_2.gcp_networkName.$dirty && gcpNetworkForm_2.gcp_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_2.gcp_networkDescription.$dirty && gcpNetworkForm_2.gcp_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkDescription">{{msg.description_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkDescription" ng-model="network.description" ng-maxlength="1000" id="gcp_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_2.gcp_networkDescription.$dirty && gcpNetworkForm_2.gcp_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_2.gcp_networkVPCId.$dirty && gcpNetworkForm_2.gcp_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkVPCId">{{msg.network_gcp_form_network_id_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkVPCId" ng-model="network.parameters.networkId" id="gcp_networkVPCId" placeholder="{{msg.network_gcp_form_custom_network_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="gcpNetworkForm_2.gcp_networkVPCId.$dirty && gcpNetworkForm_2.gcp_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_2.gcp_networkSubnet.$dirty && gcpNetworkForm_2.gcp_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkSubnet">{{msg.network_form_subnet_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="gcp_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" subnetrvalidation required>
                            <div class="help-block" ng-show="gcpNetworkForm_2.gcp_networkSubnet.$dirty && gcpNetworkForm_2.gcp_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="gcp_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="gcp_network_public" id="gcp_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="gcpNetworkForm_2.$invalid" class="btn btn-success btn-block" ng-click="createGcpNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                            {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="gcp-net-headingThree">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#gcp-net-accordion" href="#gcp-net-collapseThree" aria-expanded="false" aria-controls="gcp-net-collapseThree" ng-click="selectGcpNetworkType3()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_gcp_form_type3_title}}</span>
                </a>
            </h4>
        </div>
        <div id="gcp-net-collapseThree" class="panel-collapse collapse" role="tabpanel" aria-labelledby="gcp-net-headingThree">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_gcp_form_type3_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="gcpNetworkForm_3">
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_3.gcp_networkName.$dirty && gcpNetworkForm_3.gcp_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkName">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcp_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="gcp_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_3.gcp_networkName.$dirty && gcpNetworkForm_3.gcp_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_3.gcp_networkDescription.$dirty && gcpNetworkForm_3.gcp_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkDescription">{{msg.description_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkDescription" ng-model="network.description" ng-maxlength="1000" id="gcp_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_3.gcp_networkDescription.$dirty && gcpNetworkForm_3.gcp_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_3.gcp_networkVPCId.$dirty && gcpNetworkForm_3.gcp_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkVPCId">{{msg.network_gcp_form_network_id_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkVPCId" ng-model="network.parameters.networkId" id="gcp_networkVPCId" placeholder="{{msg.network_gcp_form_custom_network_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="gcpNetworkForm_3.gcp_networkVPCId.$dirty && gcpNetworkForm_3.gcp_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_3.gcp_networkSubnetId.$dirty && gcpNetworkForm_3.gcp_networkSubnetId.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkSubnetId">{{msg.network_gcp_form_subnet_id_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkSubnetId" ng-model="network.parameters.subnetId" id="gcp_networkSubnetId" placeholder="{{msg.network_gcp_form_custom_subnet_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="gcpNetworkForm_3.gcp_networkSubnetId.$dirty && gcpNetworkForm_3.gcp_networkSubnetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.subnet_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="no_public_ip">{{msg.network_dont_create_publicip}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="no_public_ip" id="no_public_ip" ng-model="network.parameters.noPublicIp">
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="no_firewall_rules">{{msg.network_dont_create_firewall_rules}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="no_firewall_rules" id="no_firewall_rules" ng-model="network.parameters.noFirewallRules">
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="gcp_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="gcp_network_public" id="gcp_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="gcpNetworkForm_3.$invalid" class="btn btn-success btn-block" ng-click="createGcpNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                            {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="gcp-net-headingFour">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#gcp-net-accordion" href="#gcp-net-collapseFour" aria-expanded="false" aria-controls="gcp-net-collapseFour" ng-click="selectGcpNetworkType4()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_gcp_form_type4_title}}</span>
                </a>
            </h4>
        </div>
        <div id="gcp-net-collapseFour" class="panel-collapse collapse" role="tabpanel" aria-labelledby="gcp-net-headingFour">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_gcp_form_type4_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="gcpNetworkForm_4">
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_4.gcp_networkName.$dirty && gcpNetworkForm_4.gcp_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkName">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcp_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="gcp_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_4.gcp_networkName.$dirty && gcpNetworkForm_4.gcp_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_4.gcp_networkDescription.$dirty && gcpNetworkForm_4.gcp_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkDescription">{{msg.description_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkDescription" ng-model="network.description" ng-maxlength="1000" id="gcp_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="gcpNetworkForm_4.gcp_networkDescription.$dirty && gcpNetworkForm_4.gcp_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm_4.gcp_networkVPCId.$dirty && gcpNetworkForm_4.gcp_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="gcp_networkVPCId">{{msg.network_gcp_form_network_id_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="gcp_networkVPCId" ng-model="network.parameters.networkId" id="gcp_networkVPCId" placeholder="{{msg.network_gcp_form_custom_network_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="gcpNetworkForm_4.gcp_networkVPCId.$dirty && gcpNetworkForm_4.gcp_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="gcp_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="gcp_network_public" id="gcp_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="gcpNetworkForm_4.$invalid" class="btn btn-success btn-block" ng-click="createGcpNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                            {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<form>