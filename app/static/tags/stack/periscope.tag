
<!-- ........... ALARMS ...................................................... -->
<div ng-controller="periscopeController">
<div class="panel panel-default">
    <div class="panel-heading">
        <h5><i class="fa fa-bell-o fa-fw"></i> ALARMS</h5>
    </div><!-- .panel-heading -->
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
                    <li class="active"><a ng-click="activateMetricAlarmCreationForm(true)" role="tab" data-toggle="tab">metric based</a></li>
                    <li><a role="tab" ng-click="activateMetricAlarmCreationForm(false)" role="tab" data-toggle="tab">time based</a></li>
                  </ul>

                  <div ng-show="metricBasedAlarm">
                    <form class="form-horizontal" role="form">
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" id="alarmName" placeholder="max. 20 char" ng-model="alarm.alarmName">
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="alarmDesc">description</label>
                            <div class="col-sm-9">
                                <textarea class="form-control" id="alarmDesc" placeholder="" rows="2" ng-model="alarm.description"></textarea>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="metrics">metrics</label>
                            <div class="col-sm-5">
                                <select class="form-control" id="metrics" ng-model="alarm.metric">
                                    <option value="PENDING_CONTAINERS">pending containers</option>
                                    <option value="PENDING_APPLICATIONS">pending applications</option>
                                    <option value="LOST_NODES">lost nodes</option>
                                    <option value="UNHEALTHY_NODES">unhealthy nodes</option>
                                    <option value="GLOBAL_RESOURCES">global resources</option>
                                </select>
                            </div><!-- .col-sm-5 -->
                            <div class="col-sm-2">
                                <select class="form-control logic-ops" id="comparisonOperator" ng-model="alarm.comparisonOperator">
                                    <option value="EQUALS">=</option>
                                    <option value="GREATER_OR_EQUAL_THAN">&gt;=</option>
                                    <option value="GREATER_THAN">&gt;</option>
                                    <option value="LESS_OR_EQUAL_THAN">&lt;=</option>
                                    <option value="LESS_THAN">&lt;</option>
                                </select>
                            </div><!-- .col-sm-2 -->
                            <div class="col-sm-2">
                                <input type="number" class="form-control" id="threshold" placeholder="threshold" ng-model="alarm.threshold">
                            </div><!-- .col-sm-2 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="alarm-period">period</label>
                          <div class="col-sm-9">
                            <input type="number" class="form-control" id="alarm-period" placeholder="" ng-model="alarm.period">
                            </div><!-- .col-sm-9 -->
                          </div><!-- .form-group -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" id="email" placeholder="" ng-model="alarm.email">
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="row btn-row">
                            <div class="col-sm-9 col-sm-offset-3">
                                <a id="createAlarm" class="btn btn-success btn-block" role="button" ng-click="createAlarm()"><i class="fa fa-plus fa-fw"></i> create alarm</a>
                            </div>
                        </div>
                    </form>
                  </div>

                  <div ng-show="timeBasedAlarm">
                    <form class="form-horizontal" role="form">
                      <div class="form-group">
                        <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
                        <div class="col-sm-9">
                          <input type="text" class="form-control" id="alarmName" placeholder="max. 20 char" ng-model="alarm.alarmName">
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="alarmDesc">description</label>
                          <div class="col-sm-9">
                            <textarea class="form-control" id="alarmDesc" placeholder="" rows="2" ng-model="alarm.description"></textarea>
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="timezone">time zone</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="timezone" placeholder="" ng-model="alarm.timeZone">
                          </div><!-- .col-sm-5 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="cronexpression">cron expression</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="cronexpression" placeholder="" ng-model="alarm.cron">
                          </div><!-- .col-sm-5 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="email" placeholder="" ng-model="alarm.email">
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="row btn-row">
                          <div class="col-sm-9 col-sm-offset-3">
                            <a id="createAlarm" class="btn btn-success btn-block" role="button" ng-click="createAlarm()"><i class="fa fa-plus fa-fw"></i> create alarm</a>
                          </div>
                        </div>
                    </form>
                  </div>
                </div>
            </div>
        </div><!-- .panel -->

        <!-- ............ ALARM LIST .............................................. -->
        <div class="panel-group" id="alarm-list-accordion" >
            <!-- .............. ALARM ................................................. -->
            <div class="panel panel-default" ng-repeat="alarm in alarms">
                <div class="panel-heading">
                    <h5><a data-toggle="collapse" data-parent="#alarm-list-accordion" data-target="#panel-alarm-collapse-{{alarm.id}}"><i class="fa fa-bell fa-fw"></i>{{alarm.alarmName}}</a></h5>
                </div>
                <div id="panel-alarm-collapse-{{alarm.id}}" class="panel-collapse collapse">
                    <p class="btn-row-over-panel"><a class="btn btn-danger" role="button" data-target="#modal-delete-alarm-{{alarm.id}}" data-toggle="modal"><i class="fa fa-times fa-fw"></i><span> delete</span></a></p>
                    <div class="panel-body">
                        <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="alarm-description-{{alarm.id}}">description</label>
                                <div class="col-sm-9">
                                    <p id="alarm-description-{{alarm.id}}" class="form-control-static">{{alarm.description}}</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->
                            <div ng-show="alarm.period!=undefined">
                              <div class="form-group">
                                  <label class="col-sm-3 control-label" for="period-{{alarm.id}}">period</label>
                                  <div class="col-sm-9">
                                      <p id="period-{{alarm.id}}" class="form-control-static">{{alarm.period}} minute(s)</p>
                                  </div><!-- .col-sm-9 -->
                              </div><!-- .form-group -->
                              <div class="form-group">
                                <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}-metrics">metrics</label>
                                <div class="col-sm-2">
                                  <p class="form-control-static">{{alarm.metric}}</p>
                                </div><!-- .col-sm-5 -->
                                <div class="col-sm-3">
                                  <p class="form-control-static">{{alarm.comparisonOperator}} (comparison operator)</p>
                                </div><!-- .col-sm-2 -->
                                <div class="col-sm-4">
                                  <p class="form-control-static">{{alarm.threshold}} (threshold)</p>
                                </div><!-- .col-sm-2 -->
                              </div><!-- .form-group -->
                            </div>
                            <div ng-show="alarm.cron!=undefined">
                              <div class="form-group">
                                <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}-time-zone">time zone</label>
                                <div class="col-sm-9">
                                  <p id="alarm-{{alarm.id}}-time-zone" class="form-control-static">{{alarm.timeZone}}</p>
                                </div><!-- .col-sm-9 -->
                              </div><!-- .form-group -->
                              <div class="form-group">
                                <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}-cron">cron expression</label>
                                <div class="col-sm-9">
                                  <p id="alarm-{{alarm.id}}-cron" class="form-control-static">{{alarm.cron}}</p>
                                </div><!-- .col-sm-9 -->
                              </div><!-- .form-group -->
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="alarm-{{alarm.id}}">alarm</label>
                                <div class="col-sm-9">
                                    <p id="alarm-{{alarm.id}}" class="form-control-static">{{alarm}}</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->
                            <div class="form-group" ng-repeat="notification in alarm.notifications">
                                <label class="col-sm-3 control-label">notification ({{notification.type}})</label>
                                <div class="col-sm-9">
                                    <p ng-repeat="target in notification.target" class="form-control-static">{{target}}</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->
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
            </div><!-- .panel -->
        </div><!-- #alarm-list-accordion -->

    </div><!-- .panel-body -->
</div><!-- .panel -->


<!-- ........... SCALING ACTIONS ............................................. -->

<div class="panel panel-default">
    <div class="panel-heading">
        <h5><i class="fa fa-expand fa-fw"></i> SCALING ACTIONS</h5>
    </div><!-- .panel-heading -->
    <div class="panel-body">
        <form class="form-horizontal" role="form">
          <div class="form-group">
            <label class="col-sm-3 col-xs-12 control-label" for="cooldownTime">cooldown time</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="cooldownTime" ng-model="scalingAction.cooldown">
            </div><!-- .col-sm-2 -->
            <div class="col-sm-1 col-xs-4">
              <p class="form-control-static">minute(s)</p>
            </div><!-- .col-sm-1 -->
          </div><!-- .form-group -->

          <div class="form-group">
            <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMin">cluster size min.</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="clustersizeMin" ng-model="scalingAction.minSize">
            </div><!-- .col-sm-2 -->
          </div><!-- .form-group -->

          <div class="form-group">
            <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMax">cluster size max.</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="clustersizeMax" ng-model="scalingAction.maxSize">
            </div><!-- .col-sm-2 -->
          </div><!-- .form-group -->
        </form>
        <p class="btn-row-over-panel">
          <a class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-scaling-collapse" id="create-policy-collapse-btn">
            <i class="fa fa-plus fa-fw"></i><span> create policy</span></a>
        </p>

        <!-- ......... CREATE SCALING ACTION FORM ............................................. -->

        <div class="panel panel-default">
            <div id="panel-create-scaling-collapse" class="panel-under-btn-collapse collapse">
                <div class="panel-body">

                    <form class="form-horizontal" role="form">

                      <div class="form-group">
                          <label class="col-sm-3 control-label" for="policyName">policy name</label>
                          <div class="col-sm-9">
                              <input type="text" class="form-control" id="policyName" placeholder="max. 20 char" ng-model="scalingAction.policy.name">
                          </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="form-group">
                          <label class="col-sm-3 col-xs-12 control-label" for="scalingAdjustment">scaling adjustment</label>
                          <div class="col-sm-4 col-xs-4">
                              <input type="number" class="form-control text-right" id="scalingAdjustment" placeholder="12" ng-model="scalingAction.policy.scalingAdjustment">
                          </div><!-- .col-sm-3 -->
                          <div class="col-sm-5 col-xs-8">
                              <select class="form-control" id="scalingAdjustmentType" ng-model="scalingAction.policy.adjustmentType">
                                  <option value="NODE_COUNT">node count</option>
                                  <option value="PERCENTAGE">percentage</option>
                                  <option value="EXACT">exact</option>
                              </select>
                          </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="form-group">
                        <label class="col-sm-3 control-label" for="policyHostGroup">host group</label>
                        <div class="col-sm-9">
                          <input type="text" class="form-control" id="policyHostGroup" placeholder="max. 20 char" ng-model="scalingAction.policy.hostGroup">
                        </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="form-group">
                          <label class="col-sm-3 control-label" for="alarmForScaling">alarm</label>
                          <div class="col-sm-9">
                              <select class="form-control" id="alarmForScaling" ng-model="scalingAction.policy.alarmId" placeholder="select one">
                                  <option ng-repeat="alarm in alarms" value="{{alarm.id}}" id="alarm-option-{{alarm.id}}">{{alarm.alarmName}} (ID:{{alarm.id}})</option>
                              </select>
                          </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="row btn-row">
                          <div class="col-sm-9 col-sm-offset-3">
                              <a id="create-scaling-policy-btn" class="btn btn-success btn-block" role="button" ng-click="createPolicy()"><i class="fa fa-plus fa-fw"></i> create policy</a>
                          </div>
                      </div>

                    </form>
                </div>
            </div>
        </div><!-- .panel -->

        <!-- ............ SCALING ACTION POLICY LIST ........................................... -->

        <div class="panel-group" id="scaling-list-accordion">

            <!-- .............. SCALING ACTION POLICY .............................................. -->

            <div class="panel panel-default" ng-repeat="policy in policies.scalingPolicies">
                <div class="panel-heading">
                    <h5><a data-toggle="collapse" data-parent="#scaling-list-accordion" data-target="#panel-scaling-collapse-{{policy.id}}"><i class="fa fa-expand fa-fw"></i>{{policy.name}}</a></h5>
                </div>
                <div id="panel-scaling-collapse-{{policy.id}}" class="panel-collapse collapse">
                    <p class="btn-row-over-panel">
                      <a class="btn btn-danger" role="button" id="delete-scaling-policy-btn-{{policy.id}}" data-target="#delete-scaling-policy-modal-{{policy.id}}" data-toggle="modal">
                          <i class="fa fa-times fa-fw"></i><span> delete</span></a>
                    </p>
                    <div class="panel-body">
                      <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="scaling-adjustment-type-{{policy.id}}">adjustment type</label>
                          <div class="col-sm-9">
                            <p id="scaling-adjustment-type-{{policy.id}}" class="form-control-static">{{policy.adjustmentType}}</p>
                          </div>
                        </div>
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="scaling-adjustment-{{policy.id}}">scaling adjustment</label>
                          <div class="col-sm-9">
                            <p id="scaling-adjustment-{{policy.id}}" class="form-control-static">{{policy.scalingAdjustment}}</p>
                          </div>
                        </div>
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="scaling-policy-hostgroup-{{policy.id}}">host group</label>
                          <div class="col-sm-9">
                            <p id="scaling-policy-hostgroup-{{policy.id}}" class="form-control-static">{{policy.hostGroup}}</p>
                          </div>
                        </div>
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="alarm-{{policy.id}}">alarm</label>
                          <div class="col-sm-9">
                            <p id="alarm-{{policy.id}}" class="form-control-static" ng-repeat="alarm in alarms | filter:{id:policy.alarmId}">{{alarm.alarmName}} (ID:{{policy.alarmId}})</p>
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
                        <p>Delete policy <strong>{{policy.name}}</strong>?</p>
                      </div>
                      <div class="modal-footer">
                        <div class="row">
                          <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
                          </div>
                          <div class="col-xs-6">
                            <button type="button" class="btn btn-block btn-danger" data-dismiss="modal" ng-click="deletePolicy(policy)"><i class="fa fa-times fa-fw"></i>delete</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

            </div><!-- .panel -->
        </div><!-- #scaling-list-accordion -->

    </div><!-- .panel-body -->
</div><!-- .panel -->
</div>
