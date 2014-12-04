
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
                    <form class="form-horizontal" role="form" name="metricBasedAlarmForm">
                        <div class="form-group" ng-class="{'has-error': metricBasedAlarmForm.alarmName.$dirty && metricBasedAlarmForm.alarmName.$invalid }">
                            <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" id="alarmName" name="alarmName" placeholder="min 5 max 100 char" ng-minlength="5" ng-maxlength="100" ng-model="alarm.alarmName" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" required>
                                <div class="help-block" ng-show="metricBasedAlarmForm.alarmName.$dirty && metricBasedAlarmForm.alarmName.$invalid">
                                    <i class="fa fa-warning"></i> {{error_msg.alarm_name_invalid}}
                                </div>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{'has-error': metricBasedAlarmForm.alarmDesc.$dirty && metricBasedAlarmForm.alarmDesc.$invalid }">
                            <label class="col-sm-3 control-label" for="alarmDesc">description</label>
                            <div class="col-sm-9">
                                <textarea class="form-control" id="alarmDesc" name="alarmDesc" placeholder="" rows="2" ng-model="alarm.description" ng-maxlength="1000"></textarea>
                                <div class="help-block" ng-show="metricBasedAlarmForm.alarmDesc.$dirty && metricBasedAlarmForm.alarmDesc.$invalid">
                                     <i class="fa fa-warning"></i> {{error_msg.alarm_description_invalid}}
                                </div>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
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
                                <input type="number" class="form-control" id="threshold" name="threshold" placeholder="threshold" ng-model="alarm.threshold" required>
                                <div class="help-block" ng-show="metricBasedAlarmForm.threshold.$dirty && metricBasedAlarmForm.threshold.$invalid">
                                    <i class="fa fa-warning"></i> {{error_msg.alarm_threshold_invalid}}
                                </div>
                            </div><!-- .col-sm-2 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{ 'has-error': metricBasedAlarmForm.alarm_period.$dirty && metricBasedAlarmForm.alarm_period.$invalid }">
                            <label class="col-sm-3 control-label" for="alarm-period">period</label>
                            <div class="col-sm-9">
                               <input type="number" class="form-control" id="alarm-period" name="alarm_period" placeholder="" ng-model="alarm.period" required>
                               <div class="help-block" ng-show="metricBasedAlarmForm.alarm_period.$dirty && metricBasedAlarmForm.alarm_period.$invalid">
                                  <i class="fa fa-warning"></i> {{error_msg.alarm_period_invalid}}
                               </div>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{ 'has-error': metricBasedAlarmForm.email.$dirty && metricBasedAlarmForm.email.$invalid }">
                            <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
                            <div class="col-sm-9">
                                <input type="email" class="form-control" id="email" name="email" placeholder="" ng-model="alarm.email">
                                <div class="help-block" ng-show="metricBasedAlarmForm.email.$dirty && metricBasedAlarmForm.email.$invalid">
                                     <i class="fa fa-warning"></i> {{error_msg.email_invalid}}
                                </div>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="row btn-row">
                            <div class="col-sm-9 col-sm-offset-3">
                                <a id="createAlarm" class="btn btn-success btn-block" role="button" ng-disabled="metricBasedAlarmForm.$invalid" ng-click="createAlarm()"><i class="fa fa-plus fa-fw"></i> create alarm</a>
                            </div>
                        </div>
                    </form>
                  </div>

                  <div ng-show="timeBasedAlarm">
                    <form class="form-horizontal" role="form" name="timeBasedAlarmForm">
                        <div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.alarmName.$dirty && timeBasedAlarmForm.alarmName.$invalid }">
                          <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
                          <div class="col-sm-9">
                             <input type="text" class="form-control" id="alarmName" name="alarmName" placeholder="min 5 max 100 char" ng-minlength="5" ng-maxlength="100" ng-model="alarm.alarmName" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" required>
                             <div class="help-block" ng-show="timeBasedAlarmForm.alarmName.$dirty && timeBasedAlarmForm.alarmName.$invalid">
                                  <i class="fa fa-warning"></i> {{error_msg.alarm_name_invalid}}
                             </div>
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.alarmDesc.$dirty && timeBasedAlarmForm.alarmDesc.$invalid }">
                            <label class="col-sm-3 control-label" for="alarmDesc">description</label>
                            <div class="col-sm-9">
                                <textarea class="form-control" id="alarmDesc" name="alarmDesc" placeholder="" rows="2" ng-model="alarm.description" ng-maxlength="1000"></textarea>
                                <div class="help-block" ng-show="timeBasedAlarmForm.alarmDesc.$dirty && timeBasedAlarmForm.alarmDesc.$invalid">
                                     <i class="fa fa-warning"></i> {{error_msg.alarm_description_invalid}}
                                </div>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.timeZone.$dirty && timeBasedAlarmForm.timeZone.$invalid }">
                            <label class="col-sm-3 control-label" for="alarmDesc">time zone</label>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" id="timeZone" name="timeZone" placeholder="" rows="2" ng-model="alarm.timeZone" required></input>
                                <div class="help-block" ng-show="timeBasedAlarmForm.timeZone.$dirty && timeBasedAlarmForm.timeZone.$invalid">
                                    <i class="fa fa-warning"></i> {{error_msg.alarm_timezone_invalid}}
                                </div>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{'has-error': timeBasedAlarmForm.cronexpression.$dirty && timeBasedAlarmForm.cronexpression.$invalid }">
                          <label class="col-sm-3 control-label" for="cronexpression">cron expression</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="cronexpression" name="cronexpression" placeholder="" ng-model="alarm.cron" required>
                            <div class="help-block" ng-show="timeBasedAlarmForm.cronexpression.$dirty && timeBasedAlarmForm.cronexpression.$invalid">
                                <i class="fa fa-warning"></i> {{error_msg.alarm_cron_invalid}}
                            </div>
                          </div><!-- .col-sm-5 -->
                        </div><!-- .form-group -->
                        <div class="form-group" ng-class="{'has-error': imeBasedAlarmForm.email.$dirty && imeBasedAlarmForm.email.$invalid }">
                          <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="email" name="email" placeholder="" ng-model="alarm.email">
                            <div class="help-block" ng-show="timeBasedAlarmForm.email.$dirty && timeBasedAlarmForm.email.$invalid">
                                <i class="fa fa-warning"></i> {{error_msg.email_invalid}}
                            </div>
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="row btn-row">
                          <div class="col-sm-9 col-sm-offset-3">
                            <a id="createAlarm" class="btn btn-success btn-block" role="button" ng-disabled="timeBasedAlarmForm.$invalid" ng-click="createAlarm()"><i class="fa fa-plus fa-fw"></i> create alarm</a>
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
        <form class="form-horizontal" role="form" name="scalingActionBaseForm">
          <div class="form-group" ng-class="{ 'has-error': scalingActionBaseForm.cooldownTime.$dirty && scalingActionBaseForm.cooldownTime.$invalid }">
            <label class="col-sm-3 col-xs-12 control-label" for="cooldownTime">cooldown time</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="cooldownTime" name="cooldownTime" ng-model="scalingAction.cooldown" required>
              <div class="help-block" ng-show="scalingActionBaseForm.cooldownTime.$dirty && scalingActionBaseForm.cooldownTime.$invalid">
                  <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
              </div>
            </div><!-- .col-sm-2 -->
            <div class="col-sm-1 col-xs-4">
              <p class="form-control-static">minute(s)</p>
            </div><!-- .col-sm-1 -->
          </div><!-- .form-group -->

          <div class="form-group" ng-class="{ 'has-error': scalingActionBaseForm.clustersizeMin.$dirty && scalingActionBaseForm.clustersizeMin.$invalid }">
            <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMin">cluster size min.</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="clustersizeMin" name="clustersizeMin" ng-model="scalingAction.minSize" required>
              <div class="help-block" ng-show="scalingActionBaseForm.clustersizeMin.$dirty && scalingActionBaseForm.clustersizeMin.$invalid">
                 <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
              </div>
            </div><!-- .col-sm-2 -->
          </div><!-- .form-group -->

          <div class="form-group" ng-class="{ 'has-error': scalingActionBaseForm.clustersizeMax.$dirty && scalingActionBaseForm.clustersizeMax.$invalid }">
            <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMax">cluster size max.</label>
            <div class="col-sm-2 col-xs-4">
              <input type="number" class="form-control text-right" id="clustersizeMax" name="clustersizeMax" ng-model="scalingAction.maxSize" required>
              <div class="help-block" ng-show="scalingActionBaseForm.clustersizeMax.$dirty && scalingActionBaseForm.clustersizeMax.$invalid">
                  <i class="fa fa-warning"></i> {{error_msg.scaling_policy_base_numbers}}
              </div>
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

                    <form class="form-horizontal" role="form" name="policyForm">

                      <div class="form-group" ng-class="{ 'has-error': policyForm.policyName.$dirty && policyForm.policyName.$invalid }">
                          <label class="col-sm-3 control-label" for="policyName">policy name</label>
                          <div class="col-sm-9">
                              <input type="text" class="form-control" id="policyName" name="policyName" placeholder="min 5 max 100 char" ng-minlength="5" ng-maxlength="100" ng-model="scalingAction.policy.name" required>
                              <div class="help-block" ng-show="policyForm.policyName.$dirty && policyForm.policyName.$invalid">
                                   <i class="fa fa-warning"></i> {{error_msg.scaling_policy_name_invalid}}
                              </div>
                          </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="form-group" ng-class="{ 'has-error': policyForm.scalingAdjustment.$dirty && policyForm.scalingAdjustment.$invalid }">
                          <label class="col-sm-3 col-xs-12 control-label" for="scalingAdjustment">scaling adjustment</label>
                          <div class="col-sm-4 col-xs-4">
                              <input type="number" class="form-control text-right" id="scalingAdjustment" name="scalingAdjustment" placeholder="12" ng-model="scalingAction.policy.scalingAdjustment" required>
                              <div class="help-block" ng-show="policyForm.scalingAdjustment.$dirty && policyForm.scalingAdjustment.$invalid">
                                  <i class="fa fa-warning"></i> {{error_msg.scaling_policy_adjustment_required}}
                              </div>
                          </div><!-- .col-sm-3 -->
                          <div class="col-sm-5 col-xs-8">
                              <select class="form-control" id="scalingAdjustmentType" ng-model="scalingAction.policy.adjustmentType">
                                  <option value="NODE_COUNT">node count</option>
                                  <option value="PERCENTAGE">percentage</option>
                                  <option value="EXACT">exact</option>
                              </select>
                          </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="form-group" ng-class="{ 'has-error': policyForm.policyHostGroup.$dirty && policyForm.policyHostGroup.$invalid }">
                        <label class="col-sm-3 control-label" for="policyHostGroup">host group</label>
                        <div class="col-sm-9">
                          <input type="text" class="form-control" id="policyHostGroup" name="policyHostGroup" ng-model="scalingAction.policy.hostGroup" required>
                          <div class="help-block" ng-show="policyForm.policyHostGroup.$dirty && policyForm.policyHostGroup.$invalid">
                              <i class="fa fa-warning"></i> {{error_msg.scaling_policy_host_group_invalid}}
                          </div>
                        </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="form-group" ng-class="{ 'has-error': policyForm.alarmForScaling.$dirty && policyForm.alarmForScaling.$invalid }">
                          <label class="col-sm-3 control-label" for="alarmForScaling">alarm</label>
                          <div class="col-sm-9">
                              <select class="form-control" id="alarmForScaling" name="alarmForScaling" ng-model="scalingAction.policy.alarmId" placeholder="select one" required>
                                  <option ng-repeat="alarm in alarms" value="{{alarm.id}}" id="alarm-option-{{alarm.id}}">{{alarm.alarmName}} (ID:{{alarm.id}})</option>
                              </select>
                              <div class="help-block" ng-show="policyForm.alarmForScaling.$dirty && policyForm.alarmForScaling.$invalid">
                                  <i class="fa fa-warning"></i> {{error_msg.scaling_policy_alarm_empty}}
                              </div>
                          </div><!-- .col-sm-9 -->
                      </div><!-- .form-group -->

                      <div class="row btn-row">
                          <div class="col-sm-9 col-sm-offset-3">
                              <a id="create-scaling-policy-btn" ng-disabled="policyForm.$invalid || scalingActionBaseForm.$invalid" class="btn btn-success btn-block" role="button" ng-click="createPolicy()"><i class="fa fa-plus fa-fw"></i> create policy</a>
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
