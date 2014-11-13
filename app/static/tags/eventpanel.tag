<div id="panel-events" class="col-md-12" ng-controller="eventController">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="events-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
               data-target="#panel-events-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4>events report</h4>
        </div>

        <div id="panel-events-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <h5>
                    <i class="fa fa-filter fa-fw"></i>
                    filters</h5>

                <form class="row row-filter">

                    <div class="col-xs-6 col-sm-3 col-md-3">
                        <a id="btnClearFilters" class="btn btn-default btn-block" ng-click="clearFilter()" role="button">
                            <i class="fa fa-times fa-fw"></i>
                            clear filters</a>
                        <a id="btnGenReport" ng-click="loadEvents()" class="btn btn-success btn-block" role="button">
                            <i class="fa fa-table fa-fw"></i>
                            <!-- <i class="fa fa-circle-o-notch fa-spin fa-fw"></i> --> generate report</a>
                    </div>

                </form>
                <!-- .row -->

                <div class="table-responsive" ng-show="($root.usages.length != 0) && $root.usages">
                    <table class="table table-report table-sortable-cols table-with-pagination ">
                        <thead>
                        <tr>
                            <!-- <th></th> -->
                            <th>cloud</th>
                            <th>user</th>
                            <th>state</th>
                            <th>region</th>
                            <th>instance type</th>
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
                            <tr ng-repeat="event in events">
                                <td ng-if="$index == 0" rowspan="{{azureresults.length}}">{{usage.cloud}}</td>
                                <td>{{usage.owner}}</td>
                                <td>terminated</td>
                                <td>{{usage.zone}}</td>
                                <td>{{usage.machineType}}</td>
                                <td class="text-right">{{usage.runningHours}} hrs</td>
                                <td class="text-right">{{usage.money}} $</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

            </div>

        </div>
    </div>
</div>