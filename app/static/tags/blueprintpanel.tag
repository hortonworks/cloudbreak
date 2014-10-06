<!-- .... BLUEPRINTS PANEL ................................................. -->

<div ng-controller="blueprintController" class="col-md-12 col-lg-9">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="blueprints-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
               data-target="#panel-blueprints-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.blueprints.length}}</span> manage blueprints</h4>
        </div>

        <div id="panel-blueprints-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel">
                    <a href="" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-blueprint-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> create blueprint</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-blueprint-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">
                                </div>
                            </div>

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

                    <div class="panel panel-default" ng-repeat="blueprint in $root.blueprints">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#blueprint-list-accordion" data-target="#panel-blueprint-collapse{{blueprint.id}}"><i class="fa fa-file-o fa-fw"></i>{{blueprint.name}}</a>
                            </h5>
                        </div>
                        <div id="panel-blueprint-collapse{{blueprint.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteBlueprint(blueprint)">
                                    <i class="fa fa-times fa-fw"></i><span> delete</span>
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
