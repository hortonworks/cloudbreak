<div class="panel-heading">
  <h5><a data-toggle="collapse" data-parent="#scaling-list-accordion" data-target="#panel-scaling-collapse-{{policy.id}}"><i class="fa fa-expand fa-fw"></i>{{policy.name}}</a></h5>
</div>
<div id="panel-scaling-collapse-{{policy.id}}" class="panel-collapse collapse">
  <p class="btn-row-over-panel">
    <a class="btn btn-danger" role="button" id="delete-scaling-policy-btn-{{policy.id}}" data-target="#delete-scaling-policy-modal-{{policy.id}}" data-toggle="modal">
      <i class="fa fa-times fa-fw"></i><span> {{msg.periscope_resource_list_delete_label}}</span>
    </a>
  </p>
  <div class="panel-body">
    <form class="form-horizontal" role="document">
      <!-- role: 'document' - non-editable "form" -->
      <div class="form-group">
        <label class="col-sm-3 control-label" for="scaling-adjustment-type-{{policy.id}}">{{msg.periscope_policy_list_adjustment_type_label}}</label>
        <div class="col-sm-9">
          <p id="scaling-adjustment-type-{{policy.id}}" class="form-control-static">{{policy.adjustmentType}}</p>
        </div>
      </div>
      <div class="form-group">
        <label class="col-sm-3 control-label" for="scaling-adjustment-{{policy.id}}">{{msg.periscope_policy_form_scaling_adjustment_label}}</label>
        <div class="col-sm-9">
          <p id="scaling-adjustment-{{policy.id}}" class="form-control-static">{{policy.scalingAdjustment}}</p>
        </div>
      </div>
      <div class="form-group">
        <label class="col-sm-3 control-label" for="scaling-policy-hostgroup-{{policy.id}}">{{msg.periscope_policy_form_select_hostgroup_label}}</label>
        <div class="col-sm-9">
          <p id="scaling-policy-hostgroup-{{policy.id}}" class="form-control-static">{{policy.hostGroup}}</p>
        </div>
      </div>
      <div class="form-group">
        <label class="col-sm-3 control-label" for="alert-{{policy.id}}">{{msg.periscope_policy_form_select_alert_label}}</label>
        <div class="col-sm-9">
          <p id="alert-{{policy.id}}" class="form-control-static" ng-repeat="alert in alerts | filter:{id:policy.alertId}">{{alert.alertName}} (ID:{{policy.alertId}})</p>
        </div>
      </div>
    </form>
  </div>
</div>
<div class="modal fade" id="delete-scaling-policy-modal-{{policy.id}}" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
  <div class="modal-dialog modal-sm">
    <div class="modal-content">
      <!-- .modal-header -->
      <div class="modal-body">
        <p>{{msg.periscope_policy_list_delete_message_prefix_label}} <strong>{{policy.name}}</strong>?</p>
      </div>
      <div class="modal-footer">
        <div class="row">
          <div class="col-xs-6">
            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">{{msg.periscope_resource_list_cancel_label}}</button>
          </div>
          <div class="col-xs-6">
            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" ng-click="deletePolicy(policy)"><i class="fa fa-times fa-fw"></i>{{msg.periscope_resource_list_delete_label}}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
