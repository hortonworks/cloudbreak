<!-- .... BLUEPRINTS PANEL ................................................. -->

<div id="panel-blueprints" ng-controller="blueprintController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="blueprints-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-blueprints-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.blueprints.length}}</span> {{msg.blueprint_manage_title}}</h4>
        </div>

        <div id="panel-blueprints-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('blueprints', userDetails.groups)">
                    <a href="" id="panel-create-blueprints-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-blueprints-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.blueprint_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-blueprints-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">
                                </div>
                            </div>

                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>

                            <form class="form-horizontal" role="form" name="blueprintForm">
                                <div ng-include src="'tags/blueprint/bpform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ BLUEPRINTS LIST ........................................... -->

                <div class="panel-group" id="blueprint-list-accordion">

                    <!-- .............. BLUEPRINTS .............................................. -->

                    <div class="panel panel-default" ng-repeat="blueprint in $root.blueprints | orderBy:'name'">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#blueprint-list-accordion" data-target="#panel-blueprint-collapse{{blueprint.id}}"><i class="fa fa-th fa-fw"></i>{{blueprint.name}}</a>
                                <i class="fa fa-users fa-lg public-account-info pull-right" ng-show="blueprint.public"></i>
                            </h5>
                        </div>
                        <div id="panel-blueprint-collapse{{blueprint.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel pull-left" ng-if="isWriteScope('blueprints', userDetails.groups)">
                                <a href="" class="btn btn-info" role="button" ng-click="copyAndEditBlueprint(blueprint)">
                                    <i class="fa fa-copy fa-fw"></i><span> {{msg.blueprint_list_copy}}</span>
                                </a>
                            </p>
                            <p class="btn-row-over-panel" ng-if="isWriteScope('blueprints', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteBlueprint(blueprint)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.blueprint_list_delete}}</span>
                                </a>
                            </p>
                            <div class="panel-body">
                                <div ng-include src="'tags/blueprint/bplist.tag'"></div>
                            </div>
                        </div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #blueprint-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>