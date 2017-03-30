<!-- ........... ALARMS ...................................................... -->
<div ng-controller="periscopeController">
    <div class="panel">
        <div class="row btn-row">
            <div class="col-sm-3 col-sm-offset-9">
                <a id="enable-scaling-configuration-btn" class="btn btn-primary btn-block" role="button" ng-show="!autoScalingSLAPoliciesEnabled" ng-click="enableAutoScaling()">
                    <i class="fa fa-power-off fa-fw"></i> {{msg.periscope_enable_label}}</a>
                <a id="disable-scaling-configuration-btn" class="btn btn-danger btn-block" role="button" ng-show="autoScalingSLAPoliciesEnabled" ng-click="disableAutoScaling()">
                    <i class="fa fa-power-off fa-fw"></i> {{msg.periscope_disable_label}}</a>
            </div>
        </div>
    </div>
    <div ng-show="autoScalingSLAPoliciesEnabled">
        <div class="panel panel-default">
            <div class="panel-heading">
                <h5><i class="fa fa-bell-o fa-fw"></i> {{msg.periscope_alerts_label}}</h5>
            </div>
            <!-- .panel-heading -->
            <div class="panel-body">

                <p class="btn-row-over-panel">
                    <a class="btn btn-success" id="panel-create-periscope-alert-btn" role="button" data-toggle="collapse" data-target="#panel-create-alert-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.periscope_alert_form_create}}</span>
                    </a>
                </p>
                <!-- ......... CREATE ALARM FORM ............................................. -->
                <div class="panel panel-default">
                    <div id="panel-create-alert-collapse" class="panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <ul class="nav nav-pills nav-justified" role="tablist" style="padding-bottom:10px">
                                <li class="active"><a ng-click="activateMetricAlertCreationForm(true)" role="tab" data-toggle="tab">{{msg.periscope_alert_metric_based_label}}</a>
                                </li>
                                <li><a role="tab" ng-click="activateMetricAlertCreationForm(false)" role="tab" data-toggle="tab">{{msg.periscope_alert_time_based_label}}</a>
                                </li>
                            </ul>

                            <div ng-show="metricBasedAlarm">
                                <form class="form-horizontal" role="form" name="metricBasedAlertForm">
                                    <div ng-include="'tags/periscope/metricalertform.tag'"></div>
                                </form>
                            </div>

                            <div ng-show="timeBasedAlarm">
                                <form class="form-horizontal" role="form" name="timeBasedAlertForm">
                                    <div ng-include="'tags/periscope/timealertform.tag'"></div>
                                </form>
                            </div>

                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ ALARM LIST .............................................. -->
                <div class="panel-group" id="alert-list-accordion">
                    <!-- .............. ALARM ................................................. -->
                    <div class="panel panel-default" ng-repeat="alert in alerts">
                        <div ng-include="'tags/periscope/alertlist.tag'"></div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #alert-list-accordion -->

            </div>
            <!-- .panel-body -->
        </div>
        <!-- .panel -->


        <!-- ........... SCALING ACTIONS ............................................. -->

        <div class="panel panel-default">
            <div class="panel-heading">
                <h5><i class="fa fa-expand fa-fw"></i> {{msg.periscope_policies_label}}</h5>
            </div>
            <!-- .panel-heading -->
            <div class="panel-body">
                <!-- ......... CREATE SCALING POLICY FORM ............................................. -->
                <p class="btn-row-over-panel">
                    <a class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-scaling-collapse" id="create-policy-collapse-btn">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.periscope_policy_form_create}}</span>
                    </a>
                </p>

                <div class="panel panel-default">
                    <div id="panel-create-scaling-collapse" class="panel-under-btn-collapse collapse">
                        <div class="panel-body">

                            <form class="form-horizontal" role="form" name="policyForm">
                                <div ng-include="'tags/periscope/policyform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ SCALING ACTION POLICY LIST ........................................... -->

                <div class="panel-group" id="scaling-list-accordion">

                    <!-- .............. SCALING ACTION POLICY .............................................. -->

                    <div class="panel panel-default" ng-repeat="policy in policies">
                        <div ng-include="'tags/periscope/policylist.tag'"></div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #scaling-list-accordion -->

            </div>
            <!-- .panel-body -->
        </div>
        <!-- .panel -->
        <div class="panel panel-default">
            <div class="panel-heading">
                <h5><i class="fa fa-cog fa-fw"></i> {{msg.periscope_scaling_configuration_form_title}}</h5>
            </div>
            <!-- .panel-heading -->
            <div class="panel-body">
                <form class="form-horizontal" role="form" name="scalingConfigurationForm">
                    <div class="form-group" ng-class="{ 'has-error': scalingConfigurationForm.cooldownTime.$dirty && scalingConfigurationForm.cooldownTime.$invalid }">
                        <label class="col-sm-3 col-xs-12 control-label" for="cooldownTime">{{msg.periscope_scaling_configuration_form_cooldown_label}}</label>
                        <div class="col-sm-2 col-xs-4">
                            <input type="number" class="form-control text-right" id="cooldownTime" name="cooldownTime" ng-model="scalingConfiguration.cooldown" required>
                            <div class="help-block" ng-show="scalingConfigurationForm.cooldownTime.$dirty && scalingConfigurationForm.cooldownTime.$invalid">
                                <i class="fa fa-warning"></i> {{msg.scaling_policy_base_numbers}}
                            </div>
                        </div>
                        <!-- .col-sm-2 -->
                        <div class="col-sm-1 col-xs-4">
                            <p class="form-control-static">{{msg.periscope_scaling_configuration_form_cooldown_suffix_label}}</p>
                        </div>
                        <!-- .col-sm-1 -->
                    </div>
                    <!-- .form-group -->

                    <div class="form-group" ng-class="{ 'has-error': scalingConfigurationForm.clustersizeMin.$dirty && scalingConfigurationForm.clustersizeMin.$invalid }">
                        <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMin">{{msg.periscope_scaling_configuration_form_min_cluster_size_label}}</label>
                        <div class="col-sm-2 col-xs-4">
                            <input type="number" class="form-control text-right" id="clustersizeMin" name="clustersizeMin" ng-model="scalingConfiguration.minSize" required>
                            <div class="help-block" ng-show="scalingConfigurationForm.clustersizeMin.$dirty && scalingConfigurationForm.clustersizeMin.$invalid">
                                <i class="fa fa-warning"></i> {{msg.scaling_policy_base_numbers}}
                            </div>
                        </div>
                        <!-- .col-sm-2 -->
                    </div>
                    <!-- .form-group -->

                    <div class="form-group" ng-class="{ 'has-error': scalingConfigurationForm.clustersizeMax.$dirty && scalingConfigurationForm.clustersizeMax.$invalid }">
                        <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMax">{{msg.periscope_scaling_configuration_form_max_cluster_size_label}}</label>
                        <div class="col-sm-2 col-xs-4">
                            <input type="number" class="form-control text-right" id="clustersizeMax" name="clustersizeMax" ng-model="scalingConfiguration.maxSize" required>
                            <div class="help-block" ng-show="scalingConfigurationForm.clustersizeMax.$dirty && scalingConfigurationForm.clustersizeMax.$invalid">
                                <i class="fa fa-warning"></i> {{msg.scaling_policy_base_numbers}}
                            </div>
                        </div>
                        <!-- .col-sm-2 -->
                    </div>
                    <!-- .form-group -->
                    <div class="row btn-row">
                        <div class="col-sm-3 col-sm-offset-3">
                            <a id="update-scaling-configuration-btn" ng-disabled="scalingConfigurationForm.$invalid" class="btn btn-success btn-block" role="button" ng-click="updateScalingConfiguration()"><i class="fa fa-plus fa-fw"></i> {{msg.periscope_scaling_configuration_form_update_label}}</a>
                        </div>
                    </div>
                </form>
            </div>
            <!-- .panel-body -->
        </div>
        <!-- .panel -->

        <div class="panel panel-default" ng-show="scalingHistory && scalingHistory.length !== 0">
            <div class="panel-heading">
                <h5><i class="fa fa-history fa-fw"></i> {{msg.periscope_scaling_history_title}}</h5>
            </div>
            <!-- .panel-heading -->
            <div class="panel-body pagination">
                <select name="itemsPerPageSelector" class="form-control pull-right" style="width: auto" data-live-search="true" ng-model="pagination.itemsPerPage">
                    <option selected value="10">10</option>
                    <option value="25">25</option>
                    <option value="50">50</option>
                    <option value="100">100</option>
                </select>
                <table id="metadataTable" class="table table-report table-sortable-cols table-with-pagination table-condensed" style="background-color: transparent;">
                    <thead>
                        <tr>
                            <th class="text-center">{{msg.periscope_scaling_history_trigger_time_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_node_count_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_adjustment_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_adjustment_type_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_status_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_reason_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_alert_type_label}}</th>
                            <th class="text-center">{{msg.periscope_scaling_history_hostgroup_label}}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr ng-repeat="actual in filteredScalingHistory|orderBy:eventTimestampAsFloat" ng-class="{ 'danger':  actual.scalingStatus == 'FAILED'}">
                            <td data-title="'timestamp'" class="col-sm-2 text-center">{{actual.timestamp}}</td>
                            <td data-title="'originalNodeCount'" class="col-sm-1 text-center">{{actual.originalNodeCount}}</td>
                            <td data-title="'adjustment'" class="col-sm-1 text-center">{{actual.adjustment}}</td>
                            <td data-title="'adjustmentType'" class="col-sm-1 text-center">{{actual.adjustmentType}}</td>
                            <td data-title="'scalingStatus'" class="col-sm-1 text-center">{{actual.scalingStatus}}</td>
                            <td data-title="'statusReason'" class="col-sm-4">{{actual.statusReason}}</td>
                            <td data-title="'alertType'" class="col-sm-1 text-center">{{actual.alertType}}</td>
                            <td data-title="'hostgroup'" class="col-sm-2 text-center"><span class="label label-default">{{actual.hostGroup}}</span></td>
                        </tr>
                    </tbody>
                </table>
                <pagination boundary-links="true" total-items="pagination.totalItems" items-per-page="pagination.itemsPerPage" ng-model="pagination.currentPage" max-size="10"></pagination>
            </div>
        </div>
    </div>
</div>