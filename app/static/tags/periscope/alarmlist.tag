<div class="panel-heading">
  <h5><a data-toggle="collapse" data-parent="#alarm-list-accordion" data-target="#panel-alarm-collapse-{{alarm.id}}"><i class="fa fa-bell fa-fw"></i>{{alarm.alarmName}}</a></h5>
</div>
<div id="panel-alarm-collapse-{{alarm.id}}" class="panel-collapse collapse">
  <p class="btn-row-over-panel"><a class="btn btn-danger" role="button" data-target="#modal-delete-alarm-{{alarm.id}}" data-toggle="modal"><i class="fa fa-times fa-fw"></i><span> delete</span></a>
  </p>
  <div class="panel-body">
    <form class="form-horizontal" role="document">
      <!-- role: 'document' - non-editable "form" -->
      <div class="form-group">
        <label class="col-sm-3 control-label" for="alarm-description-{{alarm.id}}">description</label>
        <div class="col-sm-9">
          <p id="alarm-description-{{alarm.id}}" class="form-control-static">{{alarm.description}}</p>
        </div>
        <!-- .col-sm-9 -->
      </div>
      <!-- .form-group -->
      <div ng-show="alarm.period!=undefined">
        <div class="form-group">
          <label class="col-sm-3 control-label" for="period-{{alarm.id}}">period</label>
          <div class="col-sm-9">
            <p id="period-{{alarm.id}}" class="form-control-static">{{alarm.period}} minute(s)</p>
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .form-group -->
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}-metrics">metrics</label>
          <div class="col-sm-2">
            <p class="form-control-static">{{alarm.metric}}</p>
          </div>
          <!-- .col-sm-5 -->
          <div class="col-sm-3">
            <p class="form-control-static">{{alarm.comparisonOperator}} (comparison operator)</p>
          </div>
          <!-- .col-sm-2 -->
          <div class="col-sm-4">
            <p class="form-control-static">{{alarm.threshold}} (threshold)</p>
          </div>
          <!-- .col-sm-2 -->
        </div>
        <!-- .form-group -->
      </div>
      <div ng-show="alarm.cron!=undefined">
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}-time-zone">time zone</label>
          <div class="col-sm-9">
            <p id="alarm-{{alarm.id}}-time-zone" class="form-control-static" ng-repeat="timeZone in config.TIME_ZONES | filter:{key: alarm.timeZone}:true">{{timeZone.value}}</p>
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .form-group -->
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}-cron">cron expression</label>
          <div class="col-sm-9">
            <p id="alarm-{{alarm.id}}-cron" class="form-control-static">{{alarm.cron}}</p>
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .form-group -->
      </div>
      <div class="form-group" ng-repeat="notification in alarm.notifications">
        <label class="col-sm-3 control-label">notification ({{notification.notificationType}})</label>
        <div class="col-sm-9">
          <p ng-repeat="target in notification.target" class="form-control-static">{{target}}</p>
        </div>
        <!-- .col-sm-9 -->
      </div>
      <!-- .form-group -->
    </form>
  </div>
</div>

<div class="modal fade" id="modal-delete-alarm-{{alarm.id}}" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
  <div class="modal-dialog modal-sm">
    <div class="modal-content">
      <!-- .modal-header -->
      <div class="modal-body">
        <p>Delete alarm <strong>{{alarm.alarmName}}</strong>?</p>
      </div>
      <div class="modal-footer">
        <div class="row">
          <div class="col-xs-6">
            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
          </div>
          <div class="col-xs-6">
            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" ng-click="deleteAlarm(alarm)"><i class="fa fa-times fa-fw"></i>delete</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
