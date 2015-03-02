<div id="cluster-form-panel" class="col-sm-11 col-md-9 col-lg-9">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="create-cluster-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>Create cluster</h4>
            <a ng-show="!showAdvancedOptionForm" class="btn btn-success pull-right" role="button" style="right: -1px !important;left: 64%;bottom: -2px;" id="advanced-pick" ng-click="showAdvancedOption()">
              <i></i>Show Advanced Options
            </a>
            <a ng-show="showAdvancedOptionForm" class="btn btn-success pull-right" role="button" style="right: -1px !important;left: 64%;bottom: -2px;" id="advanced-pick" ng-click="showAdvancedOption()">
              <i></i>Hide Advanced Options
            </a>
        </div>
        <div id="create-cluster-panel-collapse" class="panel panel-default">
            <div class="panel-body">
                <form class="form-horizontal" role="form" name="$parent.clusterCreationForm">
                    <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterName">Cluster name</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" name="cl_clusterName" placeholder="min. 5 max. 40 char" ng-model="cluster.name"  ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="40" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{error_msg.cluster_name_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterUserName">Ambari username</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterUserName" class="form-control" id="cl_clusterUserName" name="cl_clusterUserName" placeholder="min. 5 max. 15 char" ng-model="cluster.userName"  ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="15" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid"><i class="fa fa-warning"></i> {{error_msg.ambari_user_name_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterPass">Ambari password</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterPass" class="form-control" id="cl_clusterPass" name="cl_clusterPass" placeholder="min. 5 max. 15 char" ng-model="cluster.password"  ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="15" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid"><i class="fa fa-warning"></i> {{error_msg.ambari_password_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="selectRegion">Region</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectRegion" ng-model="cluster.region" required ng-show="activeCredential.cloudPlatform == 'AWS'">
                                <option ng-repeat="region in $root.config.AWS.awsRegions" value="{{region.key}}">{{region.value}}</option>
                            </select>
                            <select class="form-control" id="selectRegion" ng-model="cluster.region" required ng-show="activeCredential.cloudPlatform == 'AZURE'">
                                <option ng-repeat="region in $root.config.AZURE.azureRegions" value="{{region.key}}">{{region.value}}</option>
                            </select>
                            <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'GCC'">
                                <option ng-repeat="region in $root.config.GCC.gccRegions" value="{{region.key}}">{{region.value}}</option>
                            </select>
                            <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
                              <option ng-repeat="region in $root.config.OPENSTACK.regions" value="{{region.key}}">{{region.value}}</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="selectAdjustment">Minimum cluster size</label>
                      <div class="col-sm-3">
                        <select class="form-control" id="bestEffort" ng-model="cluster.bestEffort" ng-change="selectedAdjustmentChange()" ng-disabled="activeCredential.cloudPlatform == 'AWS' || activeCredential.cloudPlatform == 'OPENSTACK'">
                          <option value="EXACT">exact</option>
                          <option value="BEST_EFFORT">best effort</option>
                        </select>
                      </div>
                      <div class="col-sm-6">
                        <div class="col-sm-6">
                          <div class="input-group col-sm-12" ng-show="cluster.bestEffort != 'BEST_EFFORT'">
                              <select class="form-control" id="selectAdjustment" ng-model="cluster.failurePolicy.adjustmentType" ng-disabled="activeCredential.cloudPlatform == 'AWS' || activeCredential.cloudPlatform == 'OPENSTACK'">
                                <option value="EXACT"># of nodes</option>
                                <option value="PERCENTAGE">% of nodes</option>
                              </select>
                          </div>
                        </div>
                        <div class="col-sm-6">
                          <div class="input-group" ng-show="cluster.bestEffort != 'BEST_EFFORT'">
                              <span class="input-group-addon" id="basic-addon1">=</span>
                              <input type="number" name="fthreshold" class="form-control" ng-model="cluster.failurePolicy.threshold" id="fthreshold" ng-disabled="activeCredential.cloudPlatform == 'AWS'">
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="selectBlueprint">Blueprint</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectBlueprint" ng-model="cluster.blueprintId"  required ng-change="selectedBlueprintChange()">
                                <option ng-repeat="blueprint in $root.blueprints | orderBy:'name'" data-value="{{blueprint.id}}" value="{{blueprint.id}}" id="{{blueprint.id}}">{{blueprint.name}}
                                </option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="cluster_publicInAccount">Public in account</label>
                        <div class="col-sm-9">
                             <input type="checkbox" name="cluster_publicInAccount" id="cluster_publicInAccount" ng-model="cluster.public">
                         </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="emailneeded">Email notification when cluster is provisioned</label>
                      <div class="col-sm-9">
                        <input type="checkbox" id="emailneeded" ng-model="cluster.email" name="emailneeded">
                      </div>
                    </div>
                    <div class="form-group" ng-show="cluster.instanceGroups">
                      <label class="col-sm-3 control-label" for="hostgroupconfig">Hostgroup configuration</label>
                      <div class="col-sm-8 col-sm-offset-1">
                        <div ng-repeat="instanceGroup in cluster.instanceGroups" id="hostgroupconfig">
                          <div class="row">
                            <div>
                              <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                  <h3 class="panel-title">{{instanceGroup.group}}</h3>
                                </div>
                                <div class="panel-body">
                                  <div class="form-group" name="templateNodeform{{$index}}" >
                                    <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">Group size</label>
                                    <div class="col-sm-9">
                                      <input type="number" name="templateNodeCount{{$index}}" ng-disabled="isMaster(instanceGroup.group)" class="form-control" ng-model="instanceGroup.nodeCount" id="templateNodeCount{{$index}}" min="1" max="100000" placeholder="1 - 100000" required>
                                        <div class="help-block"
                                          ng-show="clusterCreationForm.templateNodeCount{{$index}}.$dirty && clusterCreationForm.templateNodeCount{{$index}}.$invalid"><i class="fa fa-warning"></i>
                                          {{error_msg.cluster_size_invalid}}
                                        </div>
                                      </div>
                                    </div>
                                    <div class="form-group" >
                                      <label class="col-sm-3 control-label" for="templateName{{$index}}">Template</label>
                                      <div class="col-sm-9">
                                        <select class="form-control" id="template-name-{{$index}}" name="template-name-{{$index}}" ng-model="instanceGroup.templateId"
                                          ng-options="template.id as template.name for template in $root.templates | filter: {'cloudPlatform': activeCredential.cloudPlatform} | orderBy:'name'" required>
                                        </select>
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                      </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AWS'">
                      <label class="col-sm-3 control-label" for="iparams">Parameters</label>
                      <div class="col-sm-8 col-sm-offset-1">
                        <div id="paramsArrays">
                          <div class="row" ng-show="activeCredential.cloudPlatform == 'AWS'">
                            <div>
                              <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                  <h3 class="panel-title">Custom Vpc Configuration</h3>
                                </div>
                                <div class="panel-body">
                                  <div class="form-group">
                                      <label class="col-sm-3 control-label" for="vpcId">Vpc Id</label>
                                      <div class="col-sm-9">
                                        <input type="text" name="vpcId" class="form-control" id="vpcId" ng-model="cluster.parameters.vpcId" placeholder="vpc-abcd1234" ng-pattern="vpc-[a-z0-9]{8}" ng-minlength="12" ng-maxlength="12">
                                        <div class="help-block" ng-show="clusterCreationForm.vpcId.$dirty && clusterCreationForm.vpcId.$invalid">
                                          <i class="fa fa-warning"></i>Invalid Vpc id (It has to match for this pattern vpc-abcd1234)
                                        </div>
                                      </div>
                                  </div>
                                  <div class="form-group">
                                      <label class="col-sm-3 control-label" for="internetGatewayId">Internet Gateway Id</label>
                                      <div class="col-sm-9">
                                        <input type="text" name="internetGatewayId" class="form-control" id="internetGatewayId" ng-model="cluster.parameters.internetGatewayId" placeholder="igw-abcd1234" ng-pattern="igw-[a-z0-9]{8}" ng-minlength="12" ng-maxlength="12">
                                        <div class="help-block" ng-show="clusterCreationForm.internetGatewayId.$dirty && clusterCreationForm.internetGatewayId.$invalid">
                                          <i class="fa fa-warning"></i>Invalid Internet grateway id (It has to match for this pattern igw-abcd1234)
                                        </div>
                                      </div>
                                  </div>
                                  <div class="form-group">
                                      <label class="col-sm-3 control-label" for="subnetCIDR">subnetCIDR</label>
                                      <div class="col-sm-9">
                                        <input type="text" name="subnetCIDR" class="form-control" id="subnetCIDR" ng-model="cluster.parameters.subnetCIDR" placeholder="10.0.128.0/19" ng-pattern="/^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}/[0-9]{1,2}$/">
                                        <div class="help-block" ng-show="clusterCreationForm.subnetCIDR.$dirty && clusterCreationForm.subnetCIDR.$invalid">
                                          <i class="fa fa-warning"></i>Invalid Subnet CIDR (It has to match for this pattern 10.0.128.0/19)
                                        </div>
                                      </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a href="" id="createCluster" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" role="button" ng-click="createCluster()"><i class="fa fa-plus fa-fw"></i>create and start cluster</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
