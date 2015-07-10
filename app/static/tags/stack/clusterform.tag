<div id="cluster-form-panel" class="col-sm-11 col-md-9 col-lg-9">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="create-cluster-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>{{msg.cluster_form_title}}</h4>
            <a ng-show="!showAdvancedOptionForm" class="btn btn-success pull-right" role="button" style="right: -1px !important;left: 64%;bottom: -2px;" id="advanced-pick" ng-click="showAdvancedOption()">
              <i></i>{{msg.cluster_form_show_advanced}}
            </a>
            <a ng-show="showAdvancedOptionForm" class="btn btn-success pull-right" role="button" style="right: -1px !important;left: 64%;bottom: -2px;" id="advanced-pick" ng-click="showAdvancedOption()">
              <i></i>{{msg.cluster_form_hide_advanced}}
            </a>
        </div>
        <div id="create-cluster-panel-collapse" class="panel panel-default" style="margin-bottom: 0px;">
            <div class="panel-body">
                <form class="form-horizontal" role="form" name="$parent.clusterCreationForm">
                    <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterName">{{msg.cluster_form_name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" name="cl_clusterName" placeholder="{{msg.cluster_form_name_placeholder}}" ng-model="cluster.name"  ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="40" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_name_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterUserName">{{msg.cluster_form_ambari_user_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterUserName" class="form-control" id="cl_clusterUserName" name="cl_clusterUserName" placeholder="{{msg.cluster_form_ambari_user_placeholder}}" ng-model="cluster.userName"  ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="15" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterUserName.$dirty && clusterCreationForm.cl_clusterUserName.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_user_name_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm" ng-class="{ 'has-error': clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterPass">{{msg.cluster_form_ambari_password_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterPass" class="form-control" id="cl_clusterPass" name="cl_clusterPass" placeholder="{{msg.cluster_form_ambari_password_placeholder}}" ng-model="cluster.password" ng-minlength="5" ng-maxlength="50" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterPass.$dirty && clusterCreationForm.cl_clusterPass.$invalid"><i class="fa fa-warning"></i> {{msg.ambari_password_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="selectRegion">{{msg.cluster_form_region_label}}</label>
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
                        <label class="col-sm-3 control-label" for="selectClusterNetwork">{{msg.cluster_form_network_label}}</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectClusterNetwork" ng-model="cluster.networkId" required>
                                <option ng-repeat="network in $root.networks | filter:{cloudPlatform: activeCredential.cloudPlatform} | orderBy:'name'" value="{{network.id}}">{{network.name}}</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="select-cluster-securitygroup">{{msg.cluster_form_securitygroup_label}}</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="select-cluster-securitygroup" ng-model="cluster.securityGroupId" required>
                                <option ng-repeat="securitygroup in $root.securitygroups | orderBy:'name'" value="{{securitygroup.id}}">{{securitygroup.name}}</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="consulServerCount">{{msg.cluster_form_consul_server_count_label}}</label>
                      <div class="col-sm-3">
                        <input class="form-control" type="number" id="consulServerCount" ng-model="cluster.consulServerCount">
                      </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AZURE'">
                      <label class="col-sm-3 control-label" for="diskPerStorageAccount">{{msg.cluster_form_disk_per_storage_label}}</label>
                      <div class="col-sm-3">
                        <input class="form-control" type="number" id="diskPerStorageAccount" name="diskPerStorageAccount" ng-model="cluster.parameters.diskPerStorage" min="1" max="10000">
                      </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                        <label class="col-sm-3 control-label" for="onFailureConfig">{{msg.cluster_form_onfailure_label}}</label>
                        <div class="col-sm-3">
                            <select class="form-control" id="onFailureConfig" ng-model="cluster.onFailureAction">
                                <option value="DO_NOTHING">{{msg.cluster_form_onfailure_donothing}}</option>
                                <option value="ROLLBACK">{{msg.cluster_form_onfailure_rollback}}</option>
                            </select>
                        </div>
                     </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="selectAdjustment">{{msg.cluster_form_adjustment_min_label}}</label>
                      <div class="col-sm-3">
                        <select class="form-control" id="bestEffort" ng-model="cluster.bestEffort" ng-change="selectedAdjustmentChange()" ng-disabled="activeCredential.cloudPlatform == 'AWS' || activeCredential.cloudPlatform == 'OPENSTACK'">
                          <option value="EXACT">{{msg.cluster_form_adjustment_exact_label}}</option>
                          <option value="BEST_EFFORT">{{msg.cluster_form_adjustment_best_effort_label}}</option>
                        </select>
                      </div>

                      <div class="col-sm-6">
                        <div class="col-sm-6">
                          <div class="input-group col-sm-12" ng-show="cluster.bestEffort != 'BEST_EFFORT'">
                              <select class="form-control" id="selectAdjustment" ng-model="cluster.failurePolicy.adjustmentType" ng-disabled="activeCredential.cloudPlatform == 'AWS' || activeCredential.cloudPlatform == 'OPENSTACK'">
                                <option value="EXACT">{{msg.cluster_form_adjustment_exact_number_label}}</option>
                                <option value="PERCENTAGE">{{msg.cluster_form_adjustment_exact_percentage_label}}</option>
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
                        <label class="col-sm-3 control-label" for="selectBlueprint">{{msg.cluster_form_blueprint_label}}</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectBlueprint" ng-model="cluster.blueprintId" required ng-change="selectedBlueprintChange()" ng-options="blueprint.id as blueprint.name for blueprint in $root.blueprints | orderBy:'name'">
                            </select>
                        </div>
                    </div>
                    <div class="form-group" ng-show="cluster.instanceGroups">
                      <label class="col-sm-3 control-label" for="hostgroupconfig">{{msg.cluster_form_hostgroup_label}}</label>
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
                                    <label class="col-sm-3 control-label" for="templateNodeCount{{$index}}">{{msg.cluster_form_hostgroup_group_size_label}}</label>
                                    <div class="col-sm-9">
                                      <input type="number" name="templateNodeCount{{$index}}" ng-disabled="instanceGroup.type=='GATEWAY'"  class="form-control" ng-model="instanceGroup.nodeCount" id="templateNodeCount{{$index}}" min="1" max="100000" placeholder="1 - 100000" required>
                                        <div class="help-block"
                                          ng-show="clusterCreationForm.templateNodeCount{{$index}}.$dirty && clusterCreationForm.templateNodeCount{{$index}}.$invalid"><i class="fa fa-warning"></i>
                                          {{msg.cluster_size_invalid}}
                                        </div>
                                      </div>
                                    </div>
                                    <div class="form-group" >
                                      <label class="col-sm-3 control-label" for="templateName{{$index}}">{{msg.cluster_form_hostgroup_template_label}}</label>
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
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="cluster_publicInAccount">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                             <input type="checkbox" name="cluster_publicInAccount" id="cluster_publicInAccount" ng-model="cluster.public">
                         </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                        <label class="col-sm-3 control-label" for="cluster_validateBlueprint">{{msg.cluster_form_blueprint_validate_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="cluster_validateBlueprint" id="cluster_validateBlueprint" ng-model="cluster.validateBlueprint">
                        </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm">
                      <label class="col-sm-3 control-label" for="emailneeded">{{msg.cluster_form_email_label}}</label>
                      <div class="col-sm-9">
                        <input type="checkbox" id="emailneeded" ng-model="cluster.email" name="emailneeded">
                      </div>
                    </div>
                    <div class="form-group" ng-show="showAdvancedOptionForm && activeCredential.cloudPlatform == 'AWS'">
                      <label class="col-sm-3 control-label" for="awsDedicatedInstancesRequested">{{msg.cluster_form_dedicated_label}}</label>
                      <div class="col-sm-9">
                        <input type="checkbox" id="awsDedicatedInstancesRequested" ng-model="cluster.parameters.dedicatedInstances" name="awsDedicatedInstancesRequested">
                      </div>
                    </div>
                    <div id="ambariconfig" class="form-group" ng-show="showAdvancedOptionForm">
                       <label class="col-sm-3 control-label" for="ambarirepoconfig1">{{msg.cluster_form_ambari_repo_label}}</label>
                       <div class="col-sm-9" id="ambarirepoconfig1" style="padding: 0px;padding-left: 10px;">
                             <div class="panel panel-default" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                <div class="panel-heading" style="border-top-left-radius: 0.5em; border-top-right-radius: 0.5em;">
                                   <h3 class="panel-title">{{msg.cluster_form_ambari_repo_config_label}}</h3>
                                </div>
                                <div class="panel-body">
                                   <div class="form-group" name="ambari_stack1" >
                                      <label class="col-sm-3 control-label" for="ambari_stack">{{msg.cluster_form_ambari_repo_stack_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_stack" class="form-control" ng-model="cluster.ambariStackDetails.stack" id="ambari_stack" placeholder="{{msg.cluster_form_ambari_repo_stack_placeholder}}">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_version1" >
                                      <label class="col-sm-3 control-label" for="ambari_version">{{msg.cluster_form_ambari_repo_version_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_version" class="form-control" ng-model="cluster.ambariStackDetails.version" id="ambari_version" placeholder="{{msg.cluster_form_ambari_repo_version_placeholder}}">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_os1" >
                                      <label class="col-sm-3 control-label" for="ambari_os">{{msg.cluster_form_ambari_repo_os_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" disabled name="ambari_os" class="form-control" ng-model="cluster.ambariStackDetails.os" id="ambari_os" placeholder="{{msg.cluster_form_ambari_repo_os_placeholder}}">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_stackRepoId1" >
                                      <label class="col-sm-3 control-label" for="ambari_stackRepoId">{{msg.cluster_form_ambari_repo_stack_repoid_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_stackRepoId" class="form-control" ng-model="cluster.ambariStackDetails.stackRepoId" id="ambari_stackRepoId" placeholder="{{msg.cluster_form_ambari_repo_stack_repoid_placeholder}}">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_stackBaseURL1" >
                                      <label class="col-sm-3 control-label" for="ambari_stackBaseURL">{{msg.cluster_form_ambari_repo_baseurl_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_stackBaseURL" class="form-control" ng-model="cluster.ambariStackDetails.stackBaseURL" id="ambari_stackBaseURL" placeholder="{{msg.cluster_form_ambari_repo_baseurl_placeholder}}">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_utilsRepoId1" >
                                      <label class="col-sm-3 control-label" for="ambari_utilsRepoId">{{msg.cluster_form_ambari_repo_utils_repoid_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_utilsRepoId" class="form-control" ng-model="cluster.ambariStackDetails.utilsRepoId" id="ambari_utilsRepoId" placeholder="{{msg.cluster_form_ambari_repo_utils_repoid_placeholder}}">

                                      </div>
                                   </div>
                                   <div class="form-group" name="ambari_utilsBaseURL1" >
                                      <label class="col-sm-3 control-label" for="ambari_utilsBaseURL">{{msg.cluster_form_ambari_repo_utils_baseurl_label}}</label>
                                      <div class="col-sm-9">
                                         <input type="string" name="ambari_utilsBaseURL" class="form-control" ng-model="cluster.ambariStackDetails.utilsBaseURL" id="ambari_utilsBaseURL" placeholder="{{msg.cluster_form_ambari_repo_utils_baseurl_placeholder}}">
                                      </div>
                                   </div>
                                   <div class="form-group" name="cluster_verify1" >
                                        <label class="col-sm-3 control-label" for="cluster_verify">{{msg.cluster_form_ambari_repo_verify_label}}</label>
                                        <div class="col-sm-9">
                                            <input type="checkbox" name="cluster_verify" id="cluster_verify" ng-model="cluster.ambariStackDetails.verify">
                                        </div>
                                   </div>
                                </div>
                             </div>
                       </div>
                    </div>
                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a href="" id="createCluster" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" role="button" ng-click="createCluster()"><i class="fa fa-plus fa-fw"></i>{{msg.cluster_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
