<div id="panel-usages" class="col-md-12 col-lg-11" ng-controller="usageController">
  <div class="panel panel-default">
    <div class="panel-heading panel-heading-nav">
        <a href="" id="usages-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
           data-target="#panel-usages-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
        <h4>{{msg.usage_manage_title}}</h4>
    </div>

    <div id="panel-usages-collapse" class="panel-btn-in-header-collapse collapse" style="background-color: white;">


      <div class="panel-body">

        <h5><i class="fa fa-filter fa-fw"></i> {{msg.usage_events_form_filter_label}}</h5>

        <form class="row row-filter" name="usageFilterForm">
          <div class="col-xs-6 col-sm-4 col-md-2">
            <label for="startDate">{{msg.usage_form_start_date_label}}</label>
            <div ng-class="{ 'has-error': usageFilterForm.startDate.$invalid }">
              <div class="input-group date" id="datePickerStart" data-date-format="YYYY-MM-DD">
                <input type="date" class="form-control input-sm datepickerclass" id="startDate" ng-model="usageFilter.startDate" name="startDate" startdatevalidation="endDate">
                <span class="input-group-btn">
                    <button class="btn btn-default btn-sm" type="button">
                        <i class="fa fa-calendar"></i>
                    </button>
                </span>
              </div>
              <div class="help-block" ng-show="usageFilterForm.startDate.$invalid">
                <i class="fa fa-warning"></i> {{msg.usage_startdate_invalid}}
              </div>
            </div>
          </div>

          <div class="col-xs-6 col-sm-4 col-md-2">
            <label for="endDate">{{msg.usage_form_end_date_label}}</label>
            <div ng-class="{ 'has-error': usageFilterForm.endDate.$invalid }">
              <div class="input-group date" id="datePickerEnd" data-date-format="YYYY-MM-DD">
                <input type="date" class="form-control input-sm datepickerclass" id="endDate" ng-model="usageFilter.endDate" name="endDate" enddatevalidation>
                <span class="input-group-btn">
                    <button class="btn btn-default btn-sm" type="button">
                      <i class="fa fa-calendar"></i>
                    </button>
                  </span>
              </div>
              <div class="help-block" ng-show="usageFilterForm.endDate.$invalid">
                <i class="fa fa-warning"></i> {{msg.usage_enddate_invalid}}
              </div>
            </div>
          </div>

          <div class="col-xs-6 col-sm-4 col-md-2" ng-show="user.admin">
            <label for="user">{{msg.usage_events_form_user_label}}</label>
            <div>
              <div class="input-group">
                <select class="form-control input-sm" id="cloudProvider" ng-model="usageFilter.user">
                  <option default value="all">{{msg.usage_events_form_all_label}}</option>
                  <option ng-repeat="u in $root.accountUsers" value="{{u.id}}">{{u.username}}</option>
                </select>
              </div>
            </div>
          </div>

          <div class="col-xs-6 col-sm-4 col-md-2">
            <label for="cloudProvider">{{msg.usage_events_form_provider_label}}</label>

            <div>
              <select class="form-control input-sm" id="cloudProvider" ng-model="usageFilter.provider" ng-change="selectRegionsByProvider()">
                <option>{{msg.usage_events_form_all_label}}</option>
                <option value="AWS">{{msg.usage_events_form_provider_amazon_label}}</option>
                <option value="AZURE">{{msg.usage_events_form_provider_microsoft_label}}</option>
                <option value="GCP">{{msg.usage_events_form_provider_google_label}}</option>
                <option value="OPENSTACK">{{msg.usage_events_form_provider_openstack_label}}</option>
              </select>
            </div>
          </div>
          <div class="col-xs-6 col-sm-4 col-md-2">
            <label for="region">{{msg.usage_form_region_label}}</label>
            <div>
              <select class="form-control input-sm" id="region" ng-model="usageFilter.region" ng-change="selectProviderByRegion()">
                <option value="all">{{msg.usage_events_form_all_label}}</option>
                <option ng-repeat="region in regions" value="{{region.key}}">{{region.value}}</option>
              </select>
            </div>
          </div>

          <div class="col-xs-6 col-sm-4 col-md-2">
            <a id="btnClearFilters" class="btn btn-danger btn-block" ng-click="clearFilter()" role="button">
              <i class="fa fa-eraser fa-fw"></i>{{msg.usage_form_filter_clear_label}}</a>
            <a id="btnGenReport" ng-click="loadUsages()" class="btn btn-success btn-block" role="button" ng-disabled="usageFilterForm.startDate.$invalid || usageFilterForm.endDate.$invalid">
              <i class="fa fa-table fa-fw"></i>{{msg.usage_form_filter_generate_label}}</a>
          </div>

        </form>
        <!-- .row -->

        <div class="table-responsive" ng-show="(usages.length != 0) && usages">
          <table class="table table-report table-sortable-cols table-with-pagination">
            <thead>
              <tr>
                <th>{{msg.usage_events_list_cloud_label}}</th>
                <th>
                  <a title="sort by" ng-click="reverse=!reverse;orderUsagesBy('stackName',reverse)">{{msg.usage_list_sort_stack_name_label}}
                    <i class="fa fa-sort"></i>
                  </a>
                </th>
                <th>{{msg.usage_events_form_user_label}}</th>
                <th>{{msg.usage_form_region_label}}</th>
                <th></th>
                <th class="text-right">
                  <a title="sort by" ng-click="reverse=!reverse;orderUsagesBy('instanceHours',reverse)">{{msg.usage_list_sort_running_time_label}}
                        <i class="fa fa-sort"></i>
                    </a>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr ng-repeat="usage in gcpSum.items">
                <td ng-if="$index == 0" rowspan="{{gcpSum.items.length}}">{{msg.gcp_label}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td><p id="awsregion" ng-repeat="item in $root.config.GCP.gcpRegions | filter:{key: usage.region}">{{item.value}}</p></td>
                <td>
                  <table class="table usage-inline-table" style="background-color: #FFFFFF; margin-bottom: 0px;">
                    <thead>
                      <tr>
                        <th>{{msg.usage_list_instancegroup_hostgroup_label}}</th>
                        <th>{{msg.usage_list_instancegroup_instance_type_label}}</th>
                        <th>{{msg.usage_list_instancegroup_hours_label}}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr ng-repeat="group in usage.instanceGroups | orderBy:'name'">
                        <td>{{group.name}}</td>
                        <td><p ng-repeat="item in $root.config.GCP.gcpInstanceTypes | filter:{key: group.instanceType}">{{item.value}}</p></td>
                        <td>{{group.hours}}</td>
                      </tr>
                    </tbody>
                  </table>
                </td>
                <td class="text-right">{{usage.instanceHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>
              <tr class="row-summa" ng-show="usages && gcpSum.items.length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{gcpSum.fullHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>

              <tr ng-repeat="usage in awsSum.items">
                <td ng-if="$index == 0" rowspan="{{awsSum.items.length}}">{{usage.provider}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td><p ng-repeat="item in $root.config.AWS.awsRegions | filter:{key: usage.region}">{{item.value}}</p></td>
                <td>
                  <table class="table usage-inline-table" style="background-color: #FFFFFF; margin-bottom: 0px;">
                    <thead>
                      <tr>
                        <th>{{msg.usage_list_instancegroup_hostgroup_label}}</th>
                        <th>{{msg.usage_list_instancegroup_instance_type_label}}</th>
                        <th>{{msg.usage_list_instancegroup_hours_label}}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr ng-repeat="group in usage.instanceGroups | orderBy:'name'">
                        <td>{{group.name}}</td>
                        <td><p ng-repeat="item in $root.config.AWS.instanceType | filter:{key: group.instanceType}">{{item.value}}</p></td>
                        <td>{{group.hours}}</td>
                      </tr>
                    </tbody>
                  </table>
                </td>
                <td class="text-right">{{usage.instanceHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>
              <tr class="row-summa" ng-show="usages && awsSum.items.length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{awsSum.fullHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>

              <tr ng-repeat="usage in azureSum.items">
                <td ng-if="$index == 0" rowspan="{{azureSum.items.length}}">{{usage.provider}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td><p ng-repeat="item in $root.config.AZURE.azureRegions | filter:{key: usage.region}">{{item.value}}</p></td>
                <td>
                  <table class="table usage-inline-table" style="background-color: #FFFFFF; margin-bottom: 0px;">
                    <thead>
                      <tr>
                        <th>{{msg.usage_list_instancegroup_hostgroup_label}}</th>
                        <th>{{msg.usage_list_instancegroup_instance_type_label}}</th>
                        <th>{{msg.usage_list_instancegroup_hours_label}}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr ng-repeat="group in usage.instanceGroups | orderBy:'name'">
                        <td>{{group.name}}</td>
                        <td><p ng-repeat="item in $root.config.AZURE.azureVmTypes | filter:{key: group.instanceType}">{{item.value}}</p></td>
                        <td>{{group.hours}}</td>
                      </tr>
                    </tbody>
                  </table>
                </td>
                <td class="text-right">{{usage.instanceHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>
              <tr class="row-summa" ng-show="usages && azureSum.items.length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{azureSum.fullHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>

              <tr ng-repeat="usage in openstackSum.items">
                <td ng-if="$index == 0" rowspan="{{openstackSum.items.length}}">{{usage.provider}}</td>
                <td>{{usage.stackName}}</td>
                <td>{{usage.username}}</td>
                <td></td>
                <td>
                  <table class="table usage-inline-table" style="background-color: #FFFFFF; margin-bottom: 0px;">
                    <thead>
                      <tr>
                        <th>{{msg.usage_list_instancegroup_hostgroup_label}}</th>
                        <th>{{msg.usage_list_instancegroup_instance_type_label}}</th>
                        <th>{{msg.usage_list_instancegroup_hours_label}}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr ng-repeat="group in usage.instanceGroups | orderBy:'name'">
                        <td>{{group.name}}</td>
                        <td>{{group.instanceType}}</td>
                        <td>{{group.hours}}</td>
                      </tr>
                    </tbody>
                  </table>
                </td>
                <td class="text-right">{{usage.instanceHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>
              <tr class="row-summa" ng-show="usages && openstackSum.items.length != 0">
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td class="text-right">{{openstackSum.fullHours}} {{msg.usage_hours_suffix_label}}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- .table-responsive -->

        <div class="row" usagecharts></div>

      </div>
      <!-- .panel-body -->

    </div>
    <!-- .panel-collapse -->
  </div>
</div>
