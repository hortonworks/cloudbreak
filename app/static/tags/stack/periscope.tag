
<!-- ........... ALARMS ...................................................... -->

<div class="panel panel-default" ng-controller="periscopeController">
    <div class="panel-heading">
        <h5><i class="fa fa-bell-o fa-fw"></i> ALARMS</h5>
    </div><!-- .panel-heading -->
    <div class="panel-body">

        <p class="btn-row-over-panel">
            <a class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-alarm-collapse">
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
                                <input type="text" class="form-control" id="alarmName" placeholder="max. 20 char">
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="alarmDesc">description</label>
                            <div class="col-sm-9">
                                <textarea class="form-control" id="alarmDesc" placeholder="" rows="2"></textarea>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="metrics">metrics</label>
                            <div class="col-sm-5">
                                <select class="form-control" id="metrics">
                                    <option>pending containers</option>
                                    <option>pending applications</option>
                                    <option>lost nodes</option>
                                    <option>unhealthy nodes</option>
                                    <option>global resources</option>
                                </select>
                            </div><!-- .col-sm-5 -->
                            <div class="col-sm-2">
                                <select class="form-control logic-ops" id="comparisonOperator">
                                    <option>=</option>
                                    <option>&gt;=</option>
                                    <option>&gt;</option>
                                    <option>&lt;=</option>
                                    <option>&lt;</option>
                                </select>
                            </div><!-- .col-sm-2 -->
                            <div class="col-sm-2">
                                <input type="number" class="form-control" id="threshold" placeholder="threshold">
                            </div><!-- .col-sm-2 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" id="email" placeholder="">
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="row btn-row">
                            <div class="col-sm-9 col-sm-offset-3">
                                <a id="createAlarm" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> create alarm</a>
                            </div>
                        </div>
                    </form>
                  </div>

                  <div ng-show="timeBasedAlarm">
                    <form class="form-horizontal" role="form">
                      <div class="form-group">
                        <label class="col-sm-3 control-label" for="alarmName">alarm name</label>
                        <div class="col-sm-9">
                          <input type="text" class="form-control" id="alarmName" placeholder="max. 20 char">
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="alarmDesc">description</label>
                          <div class="col-sm-9">
                            <textarea class="form-control" id="alarmDesc" placeholder="" rows="2"></textarea>
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="metrics">time zone</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="email" placeholder="">
                          </div><!-- .col-sm-5 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="metrics">cron expression</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="email" placeholder="">
                          </div><!-- .col-sm-5 -->
                        </div><!-- .form-group -->
                        <div class="form-group">
                          <label class="col-sm-3 control-label" for="email">notification email (optional)</label>
                          <div class="col-sm-9">
                            <input type="text" class="form-control" id="email" placeholder="">
                          </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->
                        <div class="row btn-row">
                          <div class="col-sm-9 col-sm-offset-3">
                            <a id="createAlarm" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> create alarm</a>
                          </div>
                        </div>
                    </form>
                  </div>
                </div>
            </div>
        </div><!-- .panel -->

        <!-- ............ ALARM LIST .............................................. -->
        <div class="panel-group" id="alarm-list-accordion">
            <!-- .............. ALARM ................................................. -->
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h5><a data-toggle="collapse" data-parent="#alarm-list-accordion" data-target="#panel-alarm-collapse01"><i class="fa fa-bell fa-fw"></i>pendingContainerHigh</a></h5>
                </div>
                <div id="panel-alarm-collapse01" class="panel-collapse collapse">

                    <p class="btn-row-over-panel"><a class="btn btn-danger" role="button"><i class="fa fa-times fa-fw"></i><span> delete</span></a></p>

                    <div class="panel-body">

                        <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="description01">description</label>
                                <div class="col-sm-9">
                                    <p id="description01" class="form-control-static">Number of pending containers is high</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->

                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="period01">period</label>
                                <div class="col-sm-9">
                                    <p id="period01" class="form-control-static">1 minute(s)</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->

                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="metrics01">metrics</label>
                                <div class="col-sm-9">
                                    <p id="metrics01" class="form-control-static">pending containers &gt; 10</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->

                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="email01">notification email</label>
                                <div class="col-sm-9">
                                    <p id="email01" class="form-control-static">n/a</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->

                        </form>
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
                    <input type="number" class="form-control text-right" id="cooldownTime" placeholder="">
                </div><!-- .col-sm-2 -->
                <div class="col-sm-1 col-xs-4">
                    <p class="form-control-static">minute(s)</p>
                </div><!-- .col-sm-1 -->
            </div><!-- .form-group -->

            <div class="form-group">
                <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMin">cluster size min.</label>
                <div class="col-sm-2 col-xs-4">
                    <input type="number" class="form-control text-right" id="clustersizeMin" placeholder="">
                </div><!-- .col-sm-2 -->
            </div><!-- .form-group -->

            <div class="form-group">
                <label class="col-sm-3 col-xs-12 control-label" for="clustersizeMax">cluster size max.</label>
                <div class="col-sm-2 col-xs-4">
                    <input type="number" class="form-control text-right" id="clustersizeMax" placeholder="">
                </div><!-- .col-sm-2 -->
            </div><!-- .form-group -->

        </form>


        <p class="btn-row-over-panel">
            <a class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-scaling-collapse">
                <i class="fa fa-plus fa-fw"></i><span> create policy</span>
            </a>
        </p>


        <!-- ......... CREATE SCALING ACTION FORM ............................................. -->

        <div class="panel panel-default">
            <div id="panel-create-scaling-collapse" class="panel-under-btn-collapse collapse">
                <div class="panel-body">

                    <form class="form-horizontal" role="form">


                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="policyName">policy name</label>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" id="policyName" placeholder="max. 20 char">
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->

                        <div class="form-group">
                            <label class="col-sm-3 col-xs-12 control-label" for="scalingAdjustment">scaling adjustment</label>
                            <div class="col-sm-4 col-xs-4">
                                <input type="text" class="form-control text-right" id="scalingAdjustment" placeholder="">
                            </div><!-- .col-sm-3 -->
                            <div class="col-sm-5 col-xs-8">
                                <select class="form-control" id="scalingAdjustmentType">
                                    <option>– select adj. type –</option>
                                    <option>node number</option>
                                    <option>%</option>
                                </select>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->

                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="alarm">alarm</label>
                            <div class="col-sm-9">
                                <select class="form-control" id="alarm">
                                    <option>– select alarm –</option>
                                    <option>pendingContainerHigh</option>
                                    <option>freeGlobalResourcesRateLow</option>
                                </select>
                            </div><!-- .col-sm-9 -->
                        </div><!-- .form-group -->



                        <div class="row btn-row">
                            <div class="col-sm-9 col-sm-offset-3">
                                <a id="createPolicy" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> create policy</a>
                            </div>
                        </div>

                    </form>
                </div>
            </div>
        </div><!-- .panel -->

        <!-- ............ SCALING ACTION POLICY LIST ........................................... -->

        <div class="panel-group" id="scaling-list-accordion">


            <!-- .............. SCALING ACTION POLICY .............................................. -->

            <div class="panel panel-default">
                <div class="panel-heading">
                    <h5><a data-toggle="collapse" data-parent="#scaling-list-accordion" data-target="#panel-scaling-collapse01"><i class="fa fa-expand fa-fw"></i>downScaleWhenHighResource</a></h5>
                </div>
                <div id="panel-scaling-collapse01" class="panel-collapse collapse">

                    <p class="btn-row-over-panel"><a class="btn btn-danger" role="button"><i class="fa fa-times fa-fw"></i><span> delete</span></a></p>

                    <div class="panel-body">

                        <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="scalingAdjustment01">scaling adjustment</label>
                                <div class="col-sm-9">
                                    <p id="scalingAdjustment01" class="form-control-static">2 node(s)</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="alarm01">alarm</label>
                                <div class="col-sm-9">
                                    <p id="alarm01" class="form-control-static">freeGlobalResourcesRateLow</p>
                                </div><!-- .col-sm-9 -->
                            </div><!-- .form-group -->
                        </form>
                    </div>
                </div>
            </div><!-- .panel -->
        </div><!-- #scaling-list-accordion -->

    </div><!-- .panel-body -->
</div><!-- .panel -->
