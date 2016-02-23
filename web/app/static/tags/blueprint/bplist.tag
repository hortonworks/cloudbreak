<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.blueprint_list_name_label}}</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{blueprint.blueprintName}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{blueprint.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="source">{{msg.blueprint_list_source_label}}</label>

        <div class="col-sm-9">
            <ul class="nav nav-tabs" role="tablist" id="myTab">
                <li role="presentation" ng-class="{ 'active': jsonBp }">
                    <a id="original-tab" role="tab" data-toggle="tab" aria-controls="original" aria-expanded="true" ng-click="changeViewJsonBp()">{{msg.blueprint_raw_view}}</a>
                </li>
                <li role="presentation" ng-class="{ 'active': serviceBp }">
                    <a id="format-tab" role="tab" data-toggle="tab" aria-controls="format" aria-expanded="true" ng-click="changeViewServiceBp()">{{msg.blueprint_list_view}}</a>
                </li>
            </ul>
            <div class="tab-content" id="original" ng-show="jsonBp">
                <pre id="source" class="form-control-static blueprint-source">
{{blueprint.ambariBlueprint | json}}
                    </pre>
            </div>
            <div class="tab-content" id="format" ng-show="serviceBp">
                <div class="panel panel-default" style="margin: 10px;">
                    <div class="panel-heading">
                        <h3 class="panel-title">{{msg.bleprint_info_title}}</h3>
                    </div>
                    <div class="panel-body">
                        <p>{{msg.blueprint_info_blueprint_name}} <a class="label label-default">{{blueprint.ambariBlueprint.Blueprints.blueprint_name}}</a></p>
                        <p>{{msg.blueprint_info_stack_name}} <a class="label label-default" href="http://hortonworks.com/hdp/">{{blueprint.ambariBlueprint.Blueprints.stack_name}}</a></p>
                        <p>{{msg.blueprint_info_stack_version}} <a class="label label-default" href="http://hortonworks.com/hdp/">{{blueprint.ambariBlueprint.Blueprints.stack_version}}</a></p>
                    </div>
                </div>
                <div class="host-group-table row" ng-repeat="hostgroup in blueprint.ambariBlueprint.host_groups track by $index" ng-if="$index % 4 == 0">
                    <div class="col-md-3" ng-repeat="i in [$index, $index + 1, $index + 2, $index + 3]" id="hostgroupconfig" ng-if="blueprint.ambariBlueprint.host_groups[i] != null">
                        <div class="list-group">
                            <a href="" class="list-group-item active" style="text-decoration: none;    font-size: 15px;">
                          {{blueprint.ambariBlueprint.host_groups[i].name}}
                        </a>
                            <a href="" ng-repeat="component in blueprint.ambariBlueprint.host_groups[i].components" class="list-group-item" style="text-decoration: none;    font-size: 15px;">{{component.name}}</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->

</form>