<div class="form-group" ng-class="{'has-error': metricBasedAlertForm.alertName.$dirty && metricBasedAlertForm.alertName.$invalid }">
  <label class="col-sm-3 control-label" for="alertName">{{msg.periscope_alert_form_name_label}}</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="alertName" name="alertName" placeholder="{{msg.periscope_resource_form_name_placeholder}}" ng-minlength="5" ng-maxlength="100" ng-model="alert.alertName" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" required>
    <div class="help-block" ng-show="metricBasedAlertForm.alertName.$dirty && metricBasedAlertForm.alertName.$invalid">
      <i class="fa fa-warning"></i> {{msg.alert_name_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{'has-error': metricBasedAlertForm.alertDesc.$dirty && metricBasedAlertForm.alertDesc.$invalid }">
  <label class="col-sm-3 control-label" for="alertDesc">{{msg.periscope_alert_list_description_label}}</label>
  <div class="col-sm-9">
    <textarea class="form-control" id="alertDesc" name="alertDesc" placeholder="" rows="2" ng-model="alert.description" ng-maxlength="1000"></textarea>
    <div class="help-block" ng-show="metricBasedAlertForm.alertDesc.$dirty && metricBasedAlertForm.alertDesc.$invalid">
      <i class="fa fa-warning"></i> {{msg.alert_description_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': metricBasedAlertForm.threshold.$dirty && metricBasedAlertForm.threshold.$invalid }">
  <label class="col-sm-3 control-label" for="alertDefinitions">{{msg.periscope_alert_list_metric_label}} - {{msg.periscope_alert_list_desired_state_label}}</label>
  <div class="col-sm-5">
    <select class="form-control" id="alertDefinitions" ng-model="alert.alertDefinition">
      <option ng-repeat="alertDef in alertDefinitions" value="{{alertDef.name}}">{{alertDef.label}}</option>
    </select>
  </div>
  <!-- .col-sm-5 -->
  <div class="col-sm-2">
    <select class="form-control logic-ops" id="alertState" ng-model="alert.alertState">
      <option value="OK">OK</option>
      <option value="WARN">WARN</option>
      <option value="CRITICAL">CRITICAL</option>
    </select>
  </div>
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': metricBasedAlertForm.alert_period.$dirty && metricBasedAlertForm.alert_period.$invalid }">
  <label class="col-sm-3 control-label" for="alert-period">{{msg.periscope_alert_list_period_label}}</label>
  <div class="col-sm-9">
    <input type="number" class="form-control" id="alert-period" name="alert_period" placeholder="" ng-model="alert.period" required>
    <div class="help-block" ng-show="metricBasedAlertForm.alert_period.$dirty && metricBasedAlertForm.alert_period.$invalid">
      <i class="fa fa-warning"></i> {{msg.alert_period_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="row btn-row">
  <div class="col-sm-9 col-sm-offset-3">
    <a id="createAlert" class="btn btn-success btn-block" role="button" ng-disabled="metricBasedAlertForm.$invalid" ng-click="createAlert()"><i class="fa fa-plus fa-fw"></i> {{msg.periscope_alert_form_create}}</a>
  </div>
</div>
