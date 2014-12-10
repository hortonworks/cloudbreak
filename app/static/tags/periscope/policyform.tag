<div class="form-group" ng-class="{ 'has-error': policyForm.policyName.$dirty && policyForm.policyName.$invalid }">
  <label class="col-sm-3 control-label" for="policyName">policy name</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="policyName" name="policyName" placeholder="min 5 max 100 char" ng-minlength="5" ng-maxlength="100" ng-model="scalingAction.policy.name" required>
    <div class="help-block" ng-show="policyForm.policyName.$dirty && policyForm.policyName.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.scaling_policy_name_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="form-group" ng-class="{ 'has-error': policyForm.scalingAdjustment.$dirty && policyForm.scalingAdjustment.$invalid }">
  <label class="col-sm-3 col-xs-12 control-label" for="scalingAdjustment">scaling adjustment</label>
  <div class="col-sm-4 col-xs-4">
    <input type="number" class="form-control text-right" id="scalingAdjustment" name="scalingAdjustment" placeholder="12" ng-model="scalingAction.policy.scalingAdjustment" required>
    <div class="help-block" ng-show="policyForm.scalingAdjustment.$dirty && policyForm.scalingAdjustment.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.scaling_policy_adjustment_required}}
    </div>
  </div>
  <!-- .col-sm-3 -->
  <div class="col-sm-5 col-xs-8">
    <select class="form-control" id="scalingAdjustmentType" ng-model="scalingAction.policy.adjustmentType">
      <option value="NODE_COUNT">node count</option>
      <option value="PERCENTAGE">percentage</option>
      <option value="EXACT">exact</option>
    </select>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="form-group" ng-class="{ 'has-error': policyForm.policyHostGroup.$dirty && policyForm.policyHostGroup.$invalid }">
  <label class="col-sm-3 control-label" for="policyHostGroup">host group</label>
  <div class="col-sm-9">
    <select class="form-control" id="policyHostGroup" name="policyHostGroup" placeholder="select one" ng-model="scalingAction.policy.hostGroup" required>
      <option ng-repeat="hostGroup in activeClusterBlueprint.ambariBlueprint.host_groups" value="{{hostGroup.name}}" id="host-group-{{hostGroup.name}}">{{hostGroup.name}}</option>
    </select>
    <div class="help-block" ng-show="policyForm.policyHostGroup.$dirty && policyForm.policyHostGroup.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.scaling_policy_host_group_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="form-group" ng-class="{ 'has-error': policyForm.alarmForScaling.$dirty && policyForm.alarmForScaling.$invalid }">
  <label class="col-sm-3 control-label" for="alarmForScaling">alarm</label>
  <div class="col-sm-9">
    <select class="form-control" id="alarmForScaling" name="alarmForScaling" ng-model="scalingAction.policy.alarmId" placeholder="select one" required>
      <option ng-repeat="alarm in alarms" value="{{alarm.id}}" id="alarm-option-{{alarm.id}}">{{alarm.alarmName}} (ID:{{alarm.id}})</option>
    </select>
    <div class="help-block" ng-show="policyForm.alarmForScaling.$dirty && policyForm.alarmForScaling.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.scaling_policy_alarm_empty}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="row btn-row">
  <div class="col-sm-9 col-sm-offset-3">
    <a id="create-scaling-policy-btn" ng-disabled="policyForm.$invalid" class="btn btn-success btn-block" role="button" ng-click="createPolicy()"><i class="fa fa-plus fa-fw"></i> create policy</a>
  </div>
</div>
