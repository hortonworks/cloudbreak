</form>
<div class="panel-group" id="openstack-net-accordion" role="tablist" aria-multiselectable="true">
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="openstack-net-headingOne">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#openstack-net-accordion" href="#openstack-net-collapseOne" aria-expanded="true" aria-controls="openstack-net-collapseOne" ng-click="selectOpenstackNetworkType1()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_openstack_form_type1_title}}</span>
                </a>
            </h4>
        </div>
        <div id="openstack-net-collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="openstack-net-headingOne">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_openstack_form_type1_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="openStackNetworkForm_1">
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_1.openstack_networkName.$dirty && openStackNetworkForm_1.openstack_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="openstack_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="openstack_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="openStackNetworkForm_1.openstack_networkName.$dirty && openStackNetworkForm_1.openstack_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_1.openstack_networkDescription.$dirty && openStackNetworkForm_1.openstack_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkDescription" ng-model="network.description" ng-maxlength="1000" id="openstack_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="openStackNetworkForm_1.openstack_networkDescription.$dirty && openStackNetworkForm_1.openstack_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_1.openstack_networkSubnet.$dirty && openStackNetworkForm_1.openstack_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkSubnet">{{msg.network_form_subnet_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" subnetrvalidation id="openstack_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" required>
                            <div class="help-block" ng-show="openStackNetworkForm_1.openstack_networkSubnet.$dirty && openStackNetworkForm_1.openstack_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_1.openstack_publicNetId.$dirty && openStackNetworkForm_1.openstack_publicNetId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_publicNetId">{{msg.network_openstack_form_public_network_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_publicNetId" ng-model="network.parameters.publicNetId" ng-minlength="10" ng-maxlength="60" id="openstack_publicNetId" placeholder="{{msg.network_openstack_form_public_network_id_placeholder}}" required>
                            <div class="help-block" ng-show="openStackNetworkForm_1.openstack_publicNetId.$dirty && openStackNetworkForm_1.openstack_publicNetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_publicnetid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="openstack_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="openstack_network_public" id="openstack_network_public" ng-model="network.publicInAccount">
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
                            <a id="createOpenStackNetwork" ng-disabled="openStackNetworkForm_1.$invalid" class="btn btn-success btn-block" ng-click="createOpenStackNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                                    {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="openstack-net-headingTwo">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#openstack-net-accordion" href="#openstack-net-collapseTwo" aria-expanded="false" aria-controls="openstack-net-collapseTwo" ng-click="selectOpenstackNetworkType2()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_openstack_form_type2_title}}</span>
                </a>
            </h4>
        </div>
        <div id="openstack-net-collapseTwo" class="panel-collapse collapse" role="tabpanel" aria-labelledby="openstack-net-headingTwo">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_openstack_form_type2_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="openStackNetworkForm_2">
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_2.openstack_networkName.$dirty && openStackNetworkForm_2.openstack_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="openstack_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="openstack_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="openStackNetworkForm_2.openstack_networkName.$dirty && openStackNetworkForm_2.openstack_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_2.openstack_networkDescription.$dirty && openStackNetworkForm_2.openstack_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkDescription" ng-model="network.description" ng-maxlength="1000" id="openstack_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="openStackNetworkForm_2.openstack_networkDescription.$dirty && openStackNetworkForm_2.openstack_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_2.openstack_networkSubnet.$dirty && openStackNetworkForm_2.openstack_networkSubnet.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkSubnet">{{msg.network_form_subnet_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" subnetrvalidation id="openstack_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" required>
                            <div class="help-block" ng-show="openStackNetworkForm_2.openstack_networkSubnet.$dirty && openStackNetworkForm_2.openstack_networkSubnet.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_2.openstack_publicNetId.$dirty && openStackNetworkForm_2.openstack_publicNetId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_publicNetId">{{msg.network_openstack_form_public_network_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_publicNetId" ng-model="network.parameters.publicNetId" ng-minlength="10" ng-maxlength="60" id="openstack_publicNetId" placeholder="{{msg.network_openstack_form_public_network_id_placeholder}}" required>
                            <div class="help-block" ng-show="openStackNetworkForm_2.openstack_publicNetId.$dirty && openStackNetworkForm_2.openstack_publicNetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_publicnetid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_2.openstack_networkVPCId.$dirty && openStackNetworkForm_2.openstack_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkVPCId">{{msg.network_openstack_form_network_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkVPCId" ng-model="network.parameters.networkId" id="openstack_networkVPCId" placeholder="{{msg.network_openstack_form_network_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="openStackNetworkForm_2.openstack_networkVPCId.$dirty && openStackNetworkForm_2.openstack_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_2.openstack_networkRouterId.$dirty && openStackNetworkForm_2.openstack_networkRouterId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkRouterId">{{msg.network_openstack_form_router_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkRouterId" ng-model="network.parameters.routerId" id="openstack_networkRouterId" placeholder="{{msg.network_openstack_form_router_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="openStackNetworkForm_2.openstack_networkRouterId.$dirty && openStackNetworkForm_2.openstack_networkRouterId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_router_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="openstack_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="openstack_network_public" id="openstack_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="openStackNetworkForm_2.$invalid" class="btn btn-success btn-block" ng-click="createOpenStackNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                                    {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="openstack-net-headingThree">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#openstack-net-accordion" href="#openstack-net-collapseThree" aria-expanded="false" aria-controls="openstack-net-collapseThree" ng-click="selectOpenstackNetworkType3()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.network_openstack_form_type3_title}}</span>
                </a>
            </h4>
        </div>
        <div id="openstack-net-collapseThree" class="panel-collapse collapse" role="tabpanel" aria-labelledby="openstack-net-headingThree">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.network_openstack_form_type3_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="openStackNetworkForm_3">
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_3.openstack_networkName.$dirty && openStackNetworkForm_3.openstack_networkName.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkName">{{msg.name_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="openstack_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="openstack_networkName" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="openStackNetworkForm_3.openstack_networkName.$dirty && openStackNetworkForm_3.openstack_networkName.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_3.openstack_networkDescription.$dirty && openStackNetworkForm_3.openstack_networkDescription.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkDescription">{{msg.description_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkDescription" ng-model="network.description" ng-maxlength="1000" id="openstack_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
                            <div class="help-block" ng-show="openStackNetworkForm_3.openstack_networkDescription.$dirty && openStackNetworkForm_3.openstack_networkDescription.$invalid">
                                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_3.openstack_publicNetId.$dirty && openStackNetworkForm_3.openstack_publicNetId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_publicNetId">{{msg.network_openstack_form_public_network_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_publicNetId" ng-model="network.parameters.publicNetId" ng-minlength="10" ng-maxlength="60" id="openstack_publicNetId" placeholder="{{msg.network_openstack_form_public_network_id_placeholder}}" required>
                            <div class="help-block" ng-show="openStackNetworkForm_3.openstack_publicNetId.$dirty && openStackNetworkForm_3.openstack_publicNetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_publicnetid_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_3.openstack_networkVPCId.$dirty && openStackNetworkForm_3.openstack_networkVPCId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkVPCId">{{msg.network_openstack_form_network_id_label}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkVPCId" ng-model="network.parameters.networkId" id="openstack_networkVPCId" placeholder="{{msg.network_openstack_form_network_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="openStackNetworkForm_3.openstack_networkVPCId.$dirty && openStackNetworkForm_3.openstack_networkVPCId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_identifier_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>

                    <div class="form-group" ng-class="{ 'has-error': openStackNetworkForm_3.openstack_networkSubnetId.$dirty && openStackNetworkForm_3.openstack_networkSubnetId.$invalid }">
                        <label class="col-sm-3 control-label" for="openstack_networkSubnetId">{{msg.network_openstack_form_subnet_id_label_optional}}</label>

                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="openstack_networkSubnetId" ng-model="network.parameters.subnetId" id="openstack_networkSubnetId" placeholder="{{msg.network_openstack_form_subnet_id_placeholder}}" ng-pattern="/^[-a-zA-Z0-9]*$/" required>
                            <div class="help-block" ng-show="openStackNetworkForm_3.openstack_networkSubnetId.$dirty && openStackNetworkForm_3.openstack_networkSubnetId.$invalid">
                                <i class="fa fa-warning"></i> {{msg.network_subnetid2_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>


                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="openstack_network_public">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="openstack_network_public" id="openstack_network_public" ng-model="network.publicInAccount">
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
                            <a id="createAwsTemplate" ng-disabled="openStackNetworkForm_3.$invalid" class="btn btn-success btn-block" ng-click="createOpenStackNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                                    {{msg.network_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<form>