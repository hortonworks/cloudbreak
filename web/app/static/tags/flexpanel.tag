<!-- .... FLEX PANEL ................................................. -->

<div id="panel-flex" ng-controller="flexController" class="col-md-12 col-lg-11" ng-show="smartSenseSubscription">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="flex-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-flexs-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.flexs.length}}</span> {{msg.flex_manage_flexs}}</h4>
        </div>

        <div id="panel-flexs-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel">
                    <a href="" id="panel-create-flexs-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-flexs-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.flex_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-flexs-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div ng-include src="'tags/flex/flexform.tag'"></div>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ FLEX LIST ........................................... -->

                <div class="panel-group" id="flex-list-accordion">

                    <!-- .............. FLEX .............................................. -->

                    <div class="panel panel-default" ng-repeat="flex in $root.flexs | orderBy:'name'">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#flex-list-accordion" data-target="#panel-flex-collapse{{flex.id}}"><i class="fa fa-puzzle-piece fa-fw"></i>{{flex.name}}</a>
                                <span ng-show="flex.default" class="label label-info pull-right" >{{msg.flex_default}}</span>
                                <span ng-show="flex.usedForController" class="label label-info pull-right" >{{msg.flex_used_for_controller}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="flex.public"></i>
                            </h5>
                        </div>
                        <div id="panel-flex-collapse{{flex.id}}" class="panel-collapse collapse">

                            <p ng-show="!flex.default" class="btn-row-over-panel pull-left">
                                <a href="" class="btn btn-info" role="button" ng-click="changeDefaultFlex(flex)">
                                    <i class="fa fa-copy fa-fw"></i><span> set as default</span>
                                </a>
                            </p>

                            <p ng-show="!flex.usedForController" class="btn-row-over-panel pull-left">
                                <a href="" class="btn btn-info" role="button" ng-click="changeUseForController(flex)">
                                    <i class="fa fa-copy fa-fw"></i><span> use for controller</span>
                                </a>
                            </p>

                            <p class="btn-row-over-panel">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteFlex(flex)">
                                    <i class="fa fa-times fa-fw"></i><span> delete</span>
                                </a>
                            </p>

                            <div class="panel-body">
                                <div ng-include src="'tags/flex/flexlist.tag'"></div>
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
