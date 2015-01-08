<div id="panel-usages" class="col-md-12" ng-controller="usageController">
  <div class="panel panel-default">
    <!-- <div class="panel-heading panel-heading-nav">
        <a href="" id="usages-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
           data-target="#panel-usages-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
        <h4>cost explorer</h4>
    </div> -->

    <div id="panel-usages-collapse" class="">
      <!-- <div id="panel-usages-collapse" class="panel-btn-in-header-collapse collapse"> -->


      <div class="panel-body">

        <h5><i class="fa fa-filter fa-fw"></i> filters</h5>

        <form class="row row-filter" name="usageFilterForm">
          <div class="col-xs-6 col-sm-2 col-md-2">
            <label for="startDate">start date</label>
            <div ng-class="{ 'has-error': usageFilterForm.startDate.$invalid }">
              <div class="input-group date" id="datePickerStart" data-date-format="YYYY-MM-DD">
                <input type="date" class="form-control input-sm" id="startDate" ng-model="usageFilter.startDate" name="startDate" startdatevalidation="endDate">
                <span class="input-group-btn">
                    <button class="btn btn-default btn-sm" type="button">
                        <i class="fa fa-calendar"></i>
                    </button>
                </span>
              </div>
              <div class="help-block" ng-show="usageFilterForm.startDate.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.usage_startdate_invalid}}
              </div>
            </div>
          </div>

          <div class="col-xs-6 col-sm-2 col-md-2">
            <label for="endDate">end date</label>
            <div ng-class="{ 'has-error': usageFilterForm.endDate.$invalid }">
              <div class="input-group date" id="datePickerEnd" data-date-format="YYYY-MM-DD">
                <input type="date" class="form-control input-sm" id="endDate" ng-model="usageFilter.endDate" name="endDate" enddatevalidation>
                <span class="input-group-btn">
                    <button class="btn btn-default btn-sm" type="button">
                      <i class="fa fa-calendar"></i>
                    </button>
                  </span>
              </div>
              <div class="help-block" ng-show="usageFilterForm.endDate.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.usage_enddate_invalid}}
              </div>
            </div>
          </div>

          <div class="col-xs-6 col-sm-2 col-md-2">
            <label for="user">user</label>
            <div>
              <div class="input-group">
                <!--span class="input-group-addon">
                    <i class="fa fa-search"></i>
                </span>
                <input class="form-control input-sm" type="text" ng-model="usageFilter.user" id="user"-->
                <select class="form-control input-sm" id="cloudProvider" ng-model="usageFilter.user">
                  <option default value="all">all</option>
                  <option ng-repeat="user in $root.accountUsers" value="{{user.id}}">{{user.username}}</option>
                </select>
              </div>
            </div>
          </div>

          <div class="col-xs-6 col-sm-2 col-md-2">
            <label for="cloudProvider">cloud provider</label>

            <div>
              <select class="form-control input-sm" id="cloudProvider" ng-model="usageFilter.cloud" ng-change="selectRegionsByProvider()">
                <option>all</option>
                <option value="AWS">Amazon EC2</option>
                <option value="AZURE">Microsoft Azure</option>
                <option value="GCC">Google Cloud Compute</option>
              </select>
            </div>
          </div>
          <div class="col-xs-6 col-sm-2 col-md-2">
            <label for="region">region</label>
            <div>
              <select class="form-control input-sm" id="region" ng-model="usageFilter.zone" ng-change="selectProviderByRegion()">
                <option value="all">all</option>
                <option ng-repeat="region in regions" value="{{region.key}}">{{region.value}}</option>
              </select>
            </div>
          </div>

          <div class="col-xs-6 col-sm-2 col-md-2">
            <a id="btnClearFilters" class="btn btn-danger btn-block" ng-click="clearFilter()" role="button">
              <i class="fa fa-eraser fa-fw"></i>clear filters</a>
            <a id="btnGenReport" ng-click="loadUsages()" class="btn btn-success btn-block" role="button" ng-disabled="usageFilterForm.startDate.$invalid || usageFilterForm.endDate.$invalid">
              <i class="fa fa-table fa-fw"></i>
              <!-- <i class="fa fa-circle-o-notch fa-spin fa-fw"></i> -->generate report</a>
          </div>

        </form>
        <!-- .row -->

        <div class="table-responsive" ng-show="(usages.length != 0) && usages">
          <table class="table table-report table-sortable-cols table-with-pagination ">
            <thead>
              <tr>
                <!-- <th></th> -->
                <th>cloud</th>
                <th>stack name</th>
                <th>user</th>
                <th>region</th>
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
              <tr ng-repeat="usage in gccSum.items">
                <td ng-if="$index == 0" rowspan="{{gccSum.items.length}}">{{usage.cloud}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td><p id="awsregion" class="form-control-static" ng-repeat="item in $root.config.GCC.gccRegions | filter:{key: usage.zone}">{{item.value}}</p></td>
                <td class="text-right">{{usage.instanceHours}} hrs</td>
                <td class="text-right">{{usage.money}} $</td>
              </tr>
              <tr class="row-summa" ng-show="usages && gccSum.items.length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{gccSum.fullHours}} hrs</td>
                <td class="text-right">$ {{gccSum.fullMoney}}</td>
              </tr>

              <tr ng-repeat="usage in awsSum.items">
                <td ng-if="$index == 0" rowspan="{{awsSum.items.length}}">{{usage.cloud}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td><p id="awsregion" class="form-control-static" ng-repeat="item in $root.config.AWS.awsRegions | filter:{key: usage.zone}">{{item.value}}</p></td>
                <td class="text-right">{{usage.instanceHours}} hrs</td>
                <td class="text-right">{{usage.money}} $</td>
              </tr>

              <tr class="row-summa" ng-show="usages && awsSum.items.length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{awsSum.fullHours}} hrs</td>
                <td class="text-right">$ {{awsSum.fullMoney}}</td>
              </tr>

              <tr ng-repeat="usage in azureSum.items">
                <td ng-if="$index == 0" rowspan="{{azureSum.items.length}}">{{usage.cloud}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td><p id="awsregion" class="form-control-static" ng-repeat="item in $root.config.AZURE.azureRegions | filter:{key: usage.zone}">{{item.value}}</p></td>
                <td class="text-right">{{usage.instanceHours}} hrs</td>
                <td class="text-right">{{usage.money}} $</td>
              </tr>

              <tr class="row-summa" ng-show="usages && azureSum.items.length != 0">
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
