<!-- ........... ALARMS ...................................................... -->
<div ng-controller="periscopeController">
  <div class="panel">
    <div class="row btn-row">
      <div class="col-sm-3 col-sm-offset-9">
        <a id="update-scaling-configuration-btn" class="btn btn-primary btn-block" role="button" ng-show="!autoScalingSLAPoliciesEnabled" ng-click="enableAutoScaling()">
          <i class="fa fa-power-off fa-fw"></i> enable</a>
        <a id="update-scaling-configuration-btn" class="btn btn-danger btn-block" role="button" ng-show="autoScalingSLAPoliciesEnabled" ng-click="disableAutoScaling()">
          <i class="fa fa-power-off fa-fw"></i> disable</a>
      </div>
    </div>
  </div>
  <div ng-show="autoScalingSLAPoliciesEnabled">
    <div class="panel panel-default">
      <div class="panel-heading">
        <h5><i class="fa fa-bell-o fa-fw"></i> ALARMS</h5>
      </div>
      <!-- .panel-heading -->
      <div class="panel-body">

        <p class="btn-row-over-panel">
          <a class="btn btn-success" id="panel-create-periscope-alarm-btn" role="button" data-toggle="collapse" data-target="#panel-create-alarm-collapse">
            <i class="fa fa-plus fa-fw"></i><span> create alarm</span>
          </a>
        </p>
        <!-- ......... CREATE ALARM FORM ............................................. -->
        <div class="panel panel-default">
          <div id="panel-create-alarm-collapse" class="panel-under-btn-collapse collapse">
            <div class="panel-body">
              <ul class="nav nav-pills nav-justified" role="tablist" style="padding-bottom:10px">
                <li class="active"><a ng-click="activateMetricAlarmCreationForm(true)" role="tab" data-toggle="tab">metric based</a>
                </li>
                <li><a role="tab" ng-click="activateMetricAlarmCreationForm(false)" role="tab" data-toggle="tab">time based</a>
                </li>
              </ul>

              <div ng-show="metricBasedAlarm">
                <form class="form-horizontal" role="form" name="metricBasedAlarmForm">
                  <div ng-include="'tags/periscope/metricalarmform.tag'"></div>
                </form>
              </div>

              <div ng-show="timeBasedAlarm">
                <form class="form-horizontal" role="form" name="timeBasedAlarmForm">
                  <div ng-include="'tags/periscope/timealarmform.tag'"></div>
                </form>
              </div>

            </div>
          </div>
        </div>
        <!-- .panel -->

        <!-- ............ ALARM LIST .............................................. -->
        <div class="panel-group" id="alarm-list-accordion">
          <!-- .............. ALARM ................................................. -->
          <div class="panel panel-default" ng-repeat="alarm in alarms">
            <div ng-include="'tags/periscope/alarmlist.tag'"></div>
          </div>
          <!-- .panel -->
        </div>
        <!-- #alarm-list-accordion -->

      </div>
      <!-- .panel-body -->
    </div>
    <!-- .panel -->


    <!-- ........... SCALING ACTIONS ............................................. -->

    <div class="panel panel-default">
      <div class="panel-heading">
        <h5><i class="fa fa-expand fa-fw"></i> SCALING POLICIES</h5>
      </div>
      <!-- .panel-heading -->
      <div class="panel-body">
        <!-- ......... CREATE SCALING POLICY FORM ............................................. -->
        <p class="btn-row-over-panel">
          <a class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-scaling-collapse" id="create-policy-collapse-btn">
            <i class="fa fa-plus fa-fw"></i><span> create policy</span>
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
        <h5><i class="fa fa-cog fa-fw"></i> CLUSTER SCALING CONFIGURATIONS</h5>
      </div>
      <!-- .panel-heading -->
      <div class="panel-body">
        <form class="form-horizontal" role="form" name="scalingConfigurationForm">
          <div class="form-group" ng-class="{ 'has-error': scalingConfigurationForm.cooldownTime.$dirty && scalingConfigurationForm.cooldownTime.$invalid }">
            <label class="col-sm-3 col-xs-12 control-label" for="cooldownTime">cooldown time</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="cooldownTime" name="cooldownTime" ng-model="scalingConfiguration.cooldown" required>
              <div class="help-block" ng-show="scalingConfigurationForm.cooldownTime.$dirty && scalingConfigurationForm.cooldownTime.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
              </div>
            </div>
            <!-- .col-sm-2 -->
            <div class="col-sm-1 col-xs-4">
              <p class="form-control-static">minute(s)</p>
            </div>
            <!-- .col-sm-1 -->
          </div>
          <!-- .form-group -->

          <div class="form-group" ng-class="{ 'has-error': scalingConfigurationForm.clustersizeMin.$dirty && scalingConfigurationForm.clustersizeMin.$invalid }">
            <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMin">cluster size min.</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="clustersizeMin" name="clustersizeMin" ng-model="scalingConfiguration.minSize" required>
              <div class="help-block" ng-show="scalingConfigurationForm.clustersizeMin.$dirty && scalingConfigurationForm.clustersizeMin.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
              </div>
            </div>
            <!-- .col-sm-2 -->
          </div>
          <!-- .form-group -->

          <div class="form-group" ng-class="{ 'has-error': scalingConfigurationForm.clustersizeMax.$dirty && scalingConfigurationForm.clustersizeMax.$invalid }">
            <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMax">cluster size max.</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="clustersizeMax" name="clustersizeMax" ng-model="scalingConfiguration.maxSize" required>
              <div class="help-block" ng-show="scalingConfigurationForm.clustersizeMax.$dirty && scalingConfigurationForm.clustersizeMax.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
              </div>
            </div>
            <!-- .col-sm-2 -->
          </div>
          <!-- .form-group -->
          <div class="row btn-row">
            <div class="col-sm-3 col-sm-offset-3">
              <a id="update-scaling-configuration-btn" ng-disabled="scalingConfigurationForm.$invalid" class="btn btn-success btn-block" role="button" ng-click="updateScalingConfiguration()"><i class="fa fa-plus fa-fw"></i> update cluster configuration</a>
            </div>
          </div>
        </form>
      </div>
      <!-- .panel-body -->
    </div>
    <!-- .panel -->
  </div>
</div>
