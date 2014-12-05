<div class="form-group" ng-class="{'has-error': metricBasedAlarmForm.alarmName.$dirty && metricBasedAlarmForm.alarmName.$invalid }">
  <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="alarmName" name="alarmName" placeholder="min 5 max 100 char" ng-minlength="5" ng-maxlength="100" ng-model="alarm.alarmName" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" required>
    <div class="help-block" ng-show="metricBasedAlarmForm.alarmName.$dirty && metricBasedAlarmForm.alarmName.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_name_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{'has-error': metricBasedAlarmForm.alarmDesc.$dirty && metricBasedAlarmForm.alarmDesc.$invalid }">
  <label class="col-sm-3 control-label" for="alarmDesc">description</label>
  <div class="col-sm-9">
    <textarea class="form-control" id="alarmDesc" name="alarmDesc" placeholder="" rows="2" ng-model="alarm.description" ng-maxlength="1000"></textarea>
    <div class="help-block" ng-show="metricBasedAlarmForm.alarmDesc.$dirty && metricBasedAlarmForm.alarmDesc.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_description_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': metricBasedAlarmForm.threshold.$dirty && metricBasedAlarmForm.threshold.$invalid }">
  <label class="col-sm-3 control-label" for="metrics">metrics</label>
  <div class="col-sm-5">
    <select class="form-control" id="metrics" ng-model="alarm.metric">
      <option value="PENDING_CONTAINERS">pending containers</option>
      <option value="PENDING_APPLICATIONS">pending applications</option>
      <option value="LOST_NODES">lost nodes</option>
      <option value="UNHEALTHY_NODES">unhealthy nodes</option>
      <option value="GLOBAL_RESOURCES">global resources</option>
    </select>
  </div>
  <!-- .col-sm-5 -->
  <div class="col-sm-2">
    <select class="form-control logic-ops" id="comparisonOperator" ng-model="alarm.comparisonOperator">
      <option value="EQUALS">=</option>
      <option value="GREATER_OR_EQUAL_THAN">&gt;=</option>
      <option value="GREATER_THAN">&gt;</option>
      <option value="LESS_OR_EQUAL_THAN">&lt;=</option>
      <option value="LESS_THAN">&lt;</option>
    </select>
  </div>
  <!-- .col-sm-2 -->
  <div class="col-sm-2">
    <input type="number" class="form-control" id="threshold" name="threshold" placeholder="threshold" ng-model="alarm.threshold" required>
    <div class="help-block" ng-show="metricBasedAlarmForm.threshold.$dirty && metricBasedAlarmForm.threshold.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_threshold_invalid}}
    </div>
  </div>
  <!-- .col-sm-2 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': metricBasedAlarmForm.alarm_period.$dirty && metricBasedAlarmForm.alarm_period.$invalid }">
  <label class="col-sm-3 control-label" for="alarm-period">period</label>
  <div class="col-sm-9">
    <input type="number" class="form-control" id="alarm-period" name="alarm_period" placeholder="" ng-model="alarm.period" required>
    <div class="help-block" ng-show="metricBasedAlarmForm.alarm_period.$dirty && metricBasedAlarmForm.alarm_period.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_period_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': metricBasedAlarmForm.email.$dirty && metricBasedAlarmForm.email.$invalid }">
  <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
  <div class="col-sm-9">
    <input type="email" class="form-control" id="email" name="email" placeholder="" ng-model="alarm.email">
    <div class="help-block" ng-show="metricBasedAlarmForm.email.$dirty && metricBasedAlarmForm.email.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.email_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="row btn-row">
  <div class="col-sm-9 col-sm-offset-3">
    <a id="createAlarm" class="btn btn-success btn-block" role="button" ng-disabled="metricBasedAlarmForm.$invalid" ng-click="createAlarm()"><i class="fa fa-plus fa-fw"></i> create alarm</a>
  </div>
</div>
