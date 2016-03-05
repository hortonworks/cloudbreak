<!-- .... TEMPLATES PANEL ................................................. -->

<div id="panel-templates" ng-controller="sssdConfigController" class="col-md-12 col-lg-11">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="sssdconfigs-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-sssdconfigs-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.sssdConfigs.length}}</span> {{msg.sssdconfig_manage_configs}}</h4>
        </div>

        <div id="panel-sssdconfigs-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('sssdconfigs', userDetails.groups)">
                    <a href="" id="panel-create-sssdconfigs-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-sssdconfigs-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.sssdconfig_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-sssdconfigs-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div ng-include src="'tags/sssdconfig/sssdconfigform.tag'"></div>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TEMPLATE LIST ........................................... -->

                <div class="panel-group" id="sssdconfig-list-accordion">

                    <!-- .............. TEMPLATE .............................................. -->

                    <div class="panel panel-default" ng-repeat="sssdConfig in $root.sssdConfigs | orderBy:'name'">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#sssdconfig-list-accordion" data-target="#panel-sssdconfig-collapse{{sssdConfig.id}}"><i class="fa fa-puzzle-piece fa-fw"></i>{{sssdConfig.name}}</a>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="sssdConfig.public"></i>
                            </h5>
                        </div>
                        <div id="panel-sssdconfig-collapse{{sssdConfig.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('sssdconfigs', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteSssdConfig(sssdConfig)">
                                    <i class="fa fa-times fa-fw"></i><span> delete</span>
                                </a>
                            </p>

                            <div class="panel-body">
                                <div ng-include src="'tags/sssdconfig/sssdconfiglist.tag'"></div>
                            </div>

                        </div>
                    </div>
                    <!-- .panel -->
                </div>
            </div>
            <!-- .panel-body -->
        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>