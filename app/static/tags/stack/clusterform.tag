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
        <div id="create-cluster-panel-collapse" class="panel panel-default" style="margin-bottom: 0px;">
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
                            <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'GCP'">
                                <option ng-repeat="region in $root.config.GCP.gcpRegions" value="{{region.key}}">{{region.value}}</option>
                            </select>
                            <select class="form-control" id="selectRegion" ng-model="cluster.region" ng-show="activeCredential.cloudPlatform == 'OPENSTACK'">
                              <option ng-repeat="region in $root.config.OPENSTACK.regions" value="{{region.key}}">{{region.value}}</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="selectClusterNetwork">Network</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectClusterNetwork" ng-model="cluster.networkId" required>
                                <option ng-repeat="network in $root.networks | filter:{cloudPlatform: activeCredential.cloudPlatform} | orderBy:'name'" value="{{network.id}}">{{network.name}}</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="consulServerCount">Consul server count</label>
                      <div class="col-sm-3">
                        <input class="form-control" type="number" id="consulServerCount" ng-model="cluster.consulServerCount">
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
                            <select class="form-control" id="selectBlueprint" ng-model="cluster.blueprintId" required ng-change="selectedBlueprintChange()" ng-options="blueprint.id as blueprint.name for blueprint in $root.blueprints | orderBy:'name'">
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
                        <label class="col-sm-3 control-label" for="cluster_validateBlueprint">Validate blueprint</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="cluster_validateBlueprint" id="cluster_validateBlueprint" ng-model="cluster.validateBlueprint">
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="emailneeded">Email notification when cluster is provisioned</label>
                      <div class="col-sm-9">
                        <input type="checkbox" id="emailneeded" ng-model="cluster.email" name="emailneeded">
                      </div>
                    </div>
                    <div id="ambariconfig" class="form-group" ng-show="showAdvancedOptionForm">
                       <label class="col-sm-3 control-label" for="ambarirepoconfig1">Ambari Repository config</label>
                       <div class="col-sm-9" id="ambarirepoconfig1" style="padding: 0px;padding-left: 10px;">
                             <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                   <h3 class="panel-title">configurations</h3>
                                </div>
                                <div class="panel-body">
                                   <div class="form-group" name="ambari_stack1" >
                                      <label class="col-sm-3 control-label" for="ambari_stack">Stack</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_stack" class="form-control" ng-model="cluster.ambariStackDetails.stack" id="ambari_stack" placeholder="HDP">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_version1" >
                                      <label class="col-sm-3 control-label" for="ambari_version">Version</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_version" class="form-control" ng-model="cluster.ambariStackDetails.version" id="ambari_version" placeholder="2.2">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_os1" >
                                      <label class="col-sm-3 control-label" for="ambari_os">Os</label>
                                      <div class="col-sm-9">
                                         <input type="string" disabled name="ambari_os" class="form-control" ng-model="cluster.ambariStackDetails.os" id="ambari_os" placeholder="redhat6">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_stackRepoId1" >
                                      <label class="col-sm-3 control-label" for="ambari_stackRepoId">Stack Repo Id</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_stackRepoId" class="form-control" ng-model="cluster.ambariStackDetails.stackRepoId" id="ambari_stackRepoId" placeholder="HDP-2.2">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_stackBaseURL1" >
                                      <label class="col-sm-3 control-label" for="ambari_stackBaseURL">Base Url</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_stackBaseURL" class="form-control" ng-model="cluster.ambariStackDetails.stackBaseURL" id="ambari_stackBaseURL" placeholder="http://public-repo-1.hortonworks.com/HDP/centos6/2.x/GA/2.2.0.0">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_utilsRepoId1" >
                                      <label class="col-sm-3 control-label" for="ambari_utilsRepoId">Utils Repo Id</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_utilsRepoId" class="form-control" ng-model="cluster.ambariStackDetails.utilsRepoId" id="ambari_utilsRepoId" placeholder="HDP-UTILS-1.1.0.20">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_utilsBaseURL1" >
                                      <label class="col-sm-3 control-label" for="ambari_utilsBaseURL">Utils Base Url</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_utilsBaseURL" class="form-control" ng-model="cluster.ambariStackDetails.utilsBaseURL" id="ambari_utilsBaseURL" placeholder="http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6">
                                      </div>
                                   </div>
                                   <div class="form-group" name="cluster_verify1" >
                                        <label class="col-sm-3 control-label" for="cluster_verify">Verify</label>
                                        <div class="col-sm-9">
                                            <input type="checkbox" name="cluster_verify" id="cluster_verify" ng-model="cluster.ambariStackDetails.verify">
                                        </div>
                                   </div>
                                </div>
                             </div>
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
                                      <input type="number" name="templateNodeCount{{$index}}" ng-disabled="instanceGroup.type=='GATEWAY'"  class="form-control" ng-model="instanceGroup.nodeCount" id="templateNodeCount{{$index}}" min="1" max="100000" placeholder="1 - 100000" required>
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
