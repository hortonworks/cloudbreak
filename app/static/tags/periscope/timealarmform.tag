<div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.alarmName.$dirty && timeBasedAlarmForm.alarmName.$invalid }">
  <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="alarmName" name="alarmName" placeholder="min 5 max 100 char" ng-minlength="5" ng-maxlength="100" ng-model="alarm.alarmName" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" required>
    <div class="help-block" ng-show="timeBasedAlarmForm.alarmName.$dirty && timeBasedAlarmForm.alarmName.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_name_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.alarmDesc.$dirty && timeBasedAlarmForm.alarmDesc.$invalid }">
  <label class="col-sm-3 control-label" for="alarmDesc">description</label>
  <div class="col-sm-9">
    <textarea class="form-control" id="alarmDesc" name="alarmDesc" placeholder="" rows="2" ng-model="alarm.description" ng-maxlength="1000"></textarea>
    <div class="help-block" ng-show="timeBasedAlarmForm.alarmDesc.$dirty && timeBasedAlarmForm.alarmDesc.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_description_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.timeZone.$dirty && timeBasedAlarmForm.timeZone.$invalid }">
  <label class="col-sm-3 control-label" for="alarmDesc">time zone</label>
  <div class="col-sm-9">
    <select class="form-control" id="timeZone" name="timeZone" ng-model="alarm.timeZone" required>
      <option ng-repeat="mapEntry in config.TIME_ZONES | orderBy:timeZoneOrderGetter:true" value="{{mapEntry.key}}">{{mapEntry.value}}</option>
    </select>
    <div class="help-block" ng-show="timeBasedAlarmForm.timeZone.$dirty && timeBasedAlarmForm.timeZone.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_timezone_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.cronexpression.$dirty && timeBasedAlarmForm.cronexpression.$invalid }">
  <label class="col-sm-3 control-label" for="cronexpression">cron expression</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="cronexpression" name="cronexpression" placeholder="" ng-model="alarm.cron" required>
    <div class="help-block" ng-show="timeBasedAlarmForm.cronexpression.$dirty && timeBasedAlarmForm.cronexpression.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.alarm_cron_invalid}}
    </div>
  </div>
  <!-- .col-sm-5 -->
</div>
<!-- .form-group -->
<div class="form-group" ng-class="{'has-error': imeBasedAlarmForm.email.$dirty && imeBasedAlarmForm.email.$invalid }">
  <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
  <div class="col-sm-9">
    <input type="text" class="form-control" id="email" name="email" placeholder="" ng-model="alarm.email">
    <div class="help-block" ng-show="timeBasedAlarmForm.email.$dirty && timeBasedAlarmForm.email.$invalid">
      <i class="fa fa-warning"></i> {{error_msg.email_invalid}}
    </div>
  </div>
  <!-- .col-sm-9 -->
</div>
<!-- .form-group -->
<div class="row btn-row">
  <div class="col-sm-9 col-sm-offset-3">
    <a id="createAlarm" class="btn btn-success btn-block" role="button" ng-disabled="timeBasedAlarmForm.$invalid" ng-click="createAlarm()"><i class="fa fa-plus fa-fw"></i> create alarm</a>
  </div>
</div>
