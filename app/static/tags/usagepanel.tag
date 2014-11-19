<div id="panel-usages" class="col-md-12" ng-controller="usageController">
<div class="panel panel-default">
    <div class="panel-heading panel-heading-nav">
        <a href="" id="usages-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
           data-target="#panel-usages-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
        <h4>usages report</h4>
    </div>

    <div id="panel-usages-collapse" class="panel-btn-in-header-collapse collapse">
    <div class="panel-body">

    <h5>
        <i class="fa fa-filter fa-fw"></i>
        filters</h5>

    <form class="row row-filter">
        <div class="col-xs-6 col-sm-3 col-md-3">
            <label for="startDate">start date</label>

            <div>
                <div class="input-group date" id="datePickerStart" data-date-format="MM.DD.YYYY">
                    <input type="date" class="form-control input-sm" id="startDate" ng-model="localFilter.since">
                                        <span class="input-group-btn">
                                            <button class="btn btn-default btn-sm" type="button">
                                                <i class="fa fa-calendar"></i>
                                            </button>
                                        </span>
                </div>
            </div>
        </div>

        <div class="col-xs-6 col-sm-3 col-md-3">
            <label for="user">user</label>

            <div>
                <div class="input-group">
                                        <span class="input-group-addon">
                                            <i class="fa fa-search"></i>
                                        </span>
                    <input class="form-control input-sm" type="text" ng-model="localFilter.user" id="user">
                </div>
            </div>
        </div>
        <div class="col-xs-6 col-sm-3 col-md-3">
            <label for="clusterState">cluster state</label>

            <div>
                <select class="form-control input-sm" id="clusterState">
                    <option>any</option>
                </select>
            </div>
        </div>

        <div class="col-xs-6 col-sm-3 col-md-3 form-inline">
            <label for="runningTime">running time</label>

            <div id="runningTime">
                <div class="form-group">
                    <select class="form-control input-sm">
                        <option>=</option>
                    </select>
                </div>
                <div class="form-group">
                    <input class="form-control input-sm" type="text" ng-model="localFilter.hours"></div>
            </div>
        </div>

        <div class="col-xs-6 col-sm-3 col-md-3">
            <label for="cloudProvider">cloud provider</label>

            <div>
                <select class="form-control input-sm" id="cloudProvider" ng-model="localFilter.cloud">
                    <option>all</option>
                    <option value="AWS">Amazon EC2</option>
                    <option value="AZURE">Microsoft Azure</option>
                    <option value="GCC">Google Cloud Compute</option>
                </select>
            </div>
        </div>
        <div class="col-xs-6 col-sm-3 col-md-3">
            <label for="region">region</label>

            <div>
                <select class="form-control input-sm" id="region" ng-model="localFilter.zone">
                    <option>all</option>
                    <option ng-repeat="region in $root.config.AWS.awsRegions" value="{{region.key}}">{{region.value}}</option>
                    <option ng-repeat="region in $root.config.AZURE.azureRegions" value="{{region.key}}">{{region.value}}</option>
                    <option ng-repeat="region in $root.config.GCC.gccRegions" value="{{region.key}}">{{region.value}}</option>
                </select>
            </div>
        </div>
        <div class="col-xs-6 col-sm-3 col-md-3">
            <label for="instanceType">instance type</label>

            <div>
                <select class="form-control input-sm" id="instanceType" ng-model="localFilter.vmtype">
                    <option>any</option>
                    <option ng-repeat="instanceType in $root.config.AWS.instanceType" value="{{instanceType.key}}">{{instanceType.value}}</option>
                    <option ng-repeat="vmType in $root.config.AZURE.azureVmTypes" value="{{vmType.key}}">{{vmType.value}}</option>
                    <option ng-repeat="instanceType in $root.config.GCC.gccInstanceTypes" value="{{instanceType.key}}">{{instanceType.value}}</option>
                </select>
            </div>
        </div>

        <div class="col-xs-6 col-sm-3 col-md-3">
            <a id="btnClearFilters" class="btn btn-default btn-block" ng-click="clearFilter()" role="button">
                <i class="fa fa-times fa-fw"></i>
                clear filters</a>
            <a id="btnGenReport" ng-click="loadUsages()" class="btn btn-success btn-block" role="button">
                <i class="fa fa-table fa-fw"></i>
                <!-- <i class="fa fa-circle-o-notch fa-spin fa-fw"></i> --> generate report</a>
        </div>

    </form>
    <!-- .row -->

    <div class="table-responsive" ng-show="(usages.length != 0) && usages">
        <table class="table table-report table-sortable-cols table-with-pagination ">
            <thead>
            <tr>
                <!-- <th></th> -->
                <th>cloud</th>
                <th>user</th>
                <th>stack name</th>
                <th>status</th>
                <th class="text-right">
                    <a title="sort by">running time
                        <i class="fa fa-sort"></i>
                    </a>
                </th>
                <th class="text-right">
                    <a title="sort by" class="active">estimated costs
                        <i class="fa fa-sort-down"></i>
                    </a>
                </th>
            </tr>
            </thead>
            <tbody>
            <tr ng-repeat="usage in usages| filter:gccFilterFunction as gccresults">
                <td ng-if="$index == 0" rowspan="{{gccresults.length}}">{{usage.cloud}}</td>
                <td>{{usage.username}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.stackStatus}}</td>
                <td class="text-right">{{usage.instanceHours}} hrs</td>
                <td class="text-right">{{usage.money}} $</td>
            </tr>
            <tr class="row-summa" ng-show="usages && (usages | filter:gccFilterFunction).length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{gccSum.fullHours}} hrs</td>
                <td class="text-right">$ {{gccSum.fullMoney}}</td>
            </tr>

            <tr ng-repeat="usage in usages| filter:awsFilterFunction as awsresults">
                <td ng-if="$index == 0" rowspan="{{awsresults.length}}">{{usage.cloud}}</td>
                <td>{{usage.username}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.stackStatus}}</td>
                <td class="text-right">{{usage.instanceHours}} hrs</td>
                <td class="text-right">{{usage.money}} $</td>
            </tr>

            <tr class="row-summa" ng-show="usages && (usages | filter:awsFilterFunction).length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{awsSum.fullHours}} hrs</td>
                <td class="text-right">$ {{awsSum.fullMoney}}</td>
            </tr>

            <tr ng-repeat="usage in usages| filter:azureFilterFunction as azureresults">
                <td ng-if="$index == 0" rowspan="{{azureresults.length}}">{{usage.cloud}}</td>
                <td>{{usage.username}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.stackStatus}}</td>
                <td class="text-right">{{usage.instanceHours}} hrs</td>
                <td class="text-right">{{usage.money}} $</td>
            </tr>

            <tr class="row-summa" ng-show="usages && (usages | filter:azureFilterFunction).length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{azureSum.fullHours}} hrs</td>
                <td class="text-right">$ {{azureSum.fullMoney}}</td>
            </tr>
            </tbody>
        </table>
    </div>
    <!-- .table-responsive -->

    </div>
    <!-- .panel-body -->

    </div>
    <!-- .panel-collapse -->
</div>
</div>