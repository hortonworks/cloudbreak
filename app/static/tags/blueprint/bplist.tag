
<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Blueprint name</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{blueprint.blueprintName}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Description</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{blueprint.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="source">Source</label>

        <div class="col-sm-9">
            <ul class="nav nav-tabs" role="tablist" id="myTab">
                <li role="presentation" ng-class="{ 'active': jsonBp }">
                    <a id="original-tab" role="tab" data-toggle="tab" aria-controls="original" aria-expanded="true" ng-click="changeViewJsonBp()">raw view</a>
                </li>
                <li role="presentation" ng-class="{ 'active': serviceBp }">
                    <a id="format-tab" role="tab" data-toggle="tab" aria-controls="format" aria-expanded="true" ng-click="changeViewServiceBp()">list view</a>
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
                            <h3 class="panel-title">Basic info</h3>
                        </div>
                        <div class="panel-body">
                            <p>blueprint name: <a class="label label-default">{{blueprint.ambariBlueprint.Blueprints.blueprint_name}}</a></p>
                            <p>stack name: <a class="label label-default" href="http://hortonworks.com/hdp/">{{blueprint.ambariBlueprint.Blueprints.stack_name}}</a></p>
                            <p>stack version: <a class="label label-default" href="http://hortonworks.com/hdp/">{{blueprint.ambariBlueprint.Blueprints.stack_version}}</a></p>
                        </div>
                    </div>
                        <div ng-repeat="hostgroup in blueprint.ambariBlueprint.host_groups" id="hostgroupconfig" class="col-sm-3">
                            <div class="" style="margin-left: 10px; margin-bottom: 10px; margin-right: 10px; background-color: #fff;   border: 1px solid #ddd;transition: border .2s ease-in-out;padding: 20px;padding-top: 0px;">
                             <div class="thumbnail">
                                  <div class="caption">
                                    <h3>{{hostgroup.name}}</h3>
                                  </div>
                                  <div class="body">
                                    <p ng-repeat="component in hostgroup.components"><span class="label label-default">{{component.name}}</span></p>
                                  </div>
                             </div>
                           </div>
                        </div>
                </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->

</form>