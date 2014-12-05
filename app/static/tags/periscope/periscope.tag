<!-- ........... ALARMS ...................................................... -->
<div ng-controller="periscopeController">
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
      <h5><i class="fa fa-expand fa-fw"></i> SCALING ACTIONS</h5>
    </div>
    <!-- .panel-heading -->
    <div class="panel-body">
      <form class="form-horizontal" role="form" name="scalingActionBaseForm">
        <div class="form-group" ng-class="{ 'has-error': scalingActionBaseForm.cooldownTime.$dirty && scalingActionBaseForm.cooldownTime.$invalid }">
          <label class="col-sm-3 col-xs-12 control-label" for="cooldownTime">cooldown time</label>
          <div class="col-sm-2 col-xs-4">
            <input type="number" class="form-control text-right" id="cooldownTime" name="cooldownTime" ng-model="scalingAction.cooldown" required>
            <div class="help-block" ng-show="scalingActionBaseForm.cooldownTime.$dirty && scalingActionBaseForm.cooldownTime.$invalid">
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

        <div class="form-group" ng-class="{ 'has-error': scalingActionBaseForm.clustersizeMin.$dirty && scalingActionBaseForm.clustersizeMin.$invalid }">
          <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMin">cluster size min.</label>
          <div class="col-sm-2 col-xs-4">
            <input type="number" class="form-control text-right" id="clustersizeMin" name="clustersizeMin" ng-model="scalingAction.minSize" required>
            <div class="help-block" ng-show="scalingActionBaseForm.clustersizeMin.$dirty && scalingActionBaseForm.clustersizeMin.$invalid">
              <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
            </div>
          </div>
          <!-- .col-sm-2 -->
        </div>
        <!-- .form-group -->

        <div class="form-group" ng-class="{ 'has-error': scalingActionBaseForm.clustersizeMax.$dirty && scalingActionBaseForm.clustersizeMax.$invalid }">
          <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMax">cluster size max.</label>
          <div class="col-sm-2 col-xs-4">
            <input type="number" class="form-control text-right" id="clustersizeMax" name="clustersizeMax" ng-model="scalingAction.maxSize" required>
            <div class="help-block" ng-show="scalingActionBaseForm.clustersizeMax.$dirty && scalingActionBaseForm.clustersizeMax.$invalid">
              <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
            </div>
          </div>
          <!-- .col-sm-2 -->
        </div>
        <!-- .form-group -->
      </form>


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

        <div class="panel panel-default" ng-repeat="policy in policies.scalingPolicies">
          <div ng-include="'tags/periscope/policylist.tag'"></div>
        </div>
        <!-- .panel -->
      </div>
      <!-- #scaling-list-accordion -->

    </div>
    <!-- .panel-body -->
  </div>
  <!-- .panel -->
</div>
