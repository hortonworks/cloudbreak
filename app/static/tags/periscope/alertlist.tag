<div class="panel-heading">
  <h5><a data-toggle="collapse" data-parent="#alert-list-accordion" data-target="#panel-alert-collapse-{{alert.id}}"><i class="fa fa-bell fa-fw"></i>{{alert.alertName}}</a></h5>
</div>
<div id="panel-alert-collapse-{{alert.id}}" class="panel-collapse collapse">
  <p class="btn-row-over-panel"><a class="btn btn-danger" role="button" data-target="#modal-delete-alert-{{alert.id}}" data-toggle="modal"><i class="fa fa-times fa-fw"></i><span> delete</span></a>
  </p>
  <div class="panel-body">
    <form class="form-horizontal" role="document">
      <!-- role: 'document' - non-editable "form" -->
      <div class="form-group">
        <label class="col-sm-3 control-label" for="alert-description-{{alert.id}}">description</label>
        <div class="col-sm-9">
          <p id="alert-description-{{alert.id}}" class="form-control-static">{{alert.description}} ({{alert.id}})</p>
        </div>
        <!-- .col-sm-9 -->
      </div>
      <!-- .form-group -->
      <div ng-show="alert.period!=undefined">
        <div class="form-group">
          <label class="col-sm-3 control-label" for="period-{{alert.id}}">period</label>
          <div class="col-sm-9">
            <p id="period-{{alert.id}}" class="form-control-static">{{alert.period}} minute(s)</p>
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .form-group -->
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alert-{{alert.id}}-metrics">metric</label>
          <div class="col-sm-6">
            <p class="form-control-static">{{alert.alertDefinition}}</p>
          </div>
          <!-- .col-sm-5 -->
        </div>
        <!-- .form-group -->
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alert-{{alert.id}}-desiredstate">desired state</label>
          <!-- .col-sm-5 -->
          <div class="col-sm-3">
            <p class="form-control-static">{{alert.alertState}}</p>
          </div>
        </div>
        <!-- .form-group -->
      </div>
      <div ng-show="alert.cron!=undefined">
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alert-{{alert.id}}-time-zone">time zone</label>
          <div class="col-sm-9">
            <p id="alert-{{alert.id}}-time-zone" class="form-control-static" ng-repeat="timeZone in config.TIME_ZONES | filter:{key: alert.timeZone}:true">{{timeZone.value}}</p>
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .form-group -->
        <div class="form-group">
          <label class="col-sm-3 control-label" for="alert-{{alert.id}}-cron">cron expression</label>
          <div class="col-sm-9">
            <p id="alert-{{alert.id}}-cron" class="form-control-static">{{alert.cron}}</p>
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .form-group -->
      </div>
    </form>
  </div>
</div>

<div class="modal fade" id="modal-delete-alert-{{alert.id}}" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
  <div class="modal-dialog modal-sm">
    <div class="modal-content">
      <!-- .modal-header -->
      <div class="modal-body">
        <p>Delete alert <strong>{{alert.alertName}}</strong>?</p>
      </div>
      <div class="modal-footer">
        <div class="row">
          <div class="col-xs-6">
            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
          </div>
          <div class="col-xs-6">
            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" ng-click="deleteAlarm(alert)"><i class="fa fa-times fa-fw"></i>delete</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
