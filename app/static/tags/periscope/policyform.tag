<div class="form-group" ng-class="{ 'has-error': policyForm.policyName.$dirty && policyForm.policyName.$invalid }">
  <label class="col-sm-3 control-label" for="policyName">{{msg.periscope_policy_form_name_label}}</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="policyName" name="policyName" placeholder="{{msg.periscope_resource_form_name_placeholder}}" ng-minlength="5" ng-maxlength="100" ng-model="scalingAction.policy.name" required>
    <div class="help-block" ng-show="policyForm.policyName.$dirty && policyForm.policyName.$invalid">
      <i class="fa fa-warning"></i> {{msg.scaling_policy_name_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="form-group" ng-class="{ 'has-error': policyForm.scalingAdjustment.$dirty && policyForm.scalingAdjustment.$invalid }">
  <label class="col-sm-3 col-xs-12 control-label" for="scalingAdjustment">{{msg.periscope_policy_form_scaling_adjustment_label}}</label>
  <div class="col-sm-4 col-xs-4">
    <input type="number" class="form-control text-right" id="scalingAdjustment" name="scalingAdjustment" placeholder="{{msg.periscope_policy_form_scaling_adjustment_placeholder}}" ng-model="scalingAction.policy.scalingAdjustment" required>
    <div class="help-block" ng-show="policyForm.scalingAdjustment.$dirty && policyForm.scalingAdjustment.$invalid">
      <i class="fa fa-warning"></i> {{msg.scaling_policy_adjustment_required}}
    </div>
  </div>
  <!-- .col-sm-3 -->
  <div class="col-sm-5 col-xs-8">
    <select class="form-control" id="scalingAdjustmentType" ng-model="scalingAction.policy.adjustmentType">
      <option value="NODE_COUNT">{{msg.periscope_policy_form_adjustment_type_node_count_label}}</option>
      <option value="PERCENTAGE">{{msg.periscope_policy_form_adjustment_type_percentage_label}}</option>
      <option value="EXACT">{{msg.periscope_policy_form_adjustment_type_exact_label}}</option>
    </select>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="form-group" ng-class="{ 'has-error': policyForm.policyHostGroup.$dirty && policyForm.policyHostGroup.$invalid }">
  <label class="col-sm-3 control-label" for="policyHostGroup">{{msg.periscope_policy_form_select_hostgroup_label}}</label>
  <div class="col-sm-9">
    <select class="form-control" id="policyHostGroup" name="policyHostGroup" placeholder="{{msg.periscope_policy_form_select_placeholder}}" ng-model="scalingAction.policy.hostGroup" required>
      <option ng-repeat="hostGroup in activeClusterBlueprint.ambariBlueprint.host_groups" value="{{hostGroup.name}}" id="host-group-{{hostGroup.name}}">{{hostGroup.name}}</option>
    </select>
    <div class="help-block" ng-show="policyForm.policyHostGroup.$dirty && policyForm.policyHostGroup.$invalid">
      <i class="fa fa-warning"></i> {{msg.scaling_policy_host_group_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="form-group" ng-class="{ 'has-error': policyForm.alertForScaling.$dirty && policyForm.alertForScaling.$invalid }">
  <label class="col-sm-3 control-label" for="alertForScaling">{{msg.periscope_policy_form_select_alert_label}}</label>
  <div class="col-sm-9">
    <select class="form-control" id="alertForScaling" name="alertForScaling" ng-model="scalingAction.policy.alertId" placeholder="{{msg.periscope_policy_form_select_placeholder}}" required>
      <option ng-repeat="alert in alerts" value="{{alert.id}}" id="alert-option-{{alert.id}}">{{alert.alertName}} (ID:{{alert.id}})</option>
    </select>
    <div class="help-block" ng-show="policyForm.alertForScaling.$dirty && policyForm.alertForScaling.$invalid">
      <i class="fa fa-warning"></i> {{msg.scaling_policy_alert_empty}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->

<div class="row btn-row">
  <div class="col-sm-9 col-sm-offset-3">
    <a id="create-scaling-policy-btn" ng-disabled="policyForm.$invalid" class="btn btn-success btn-block" role="button" ng-click="createPolicy()"><i class="fa fa-plus fa-fw"></i> {{msg.periscope_policy_form_create}}</a>
  </div>
</div>
