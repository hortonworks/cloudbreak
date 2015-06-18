<div id="panel-events" class="col-md-12 col-lg-11" ng-controller="eventController">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="events-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
               data-target="#panel-events-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4>{{msg.events_form_title}}</h4>
        </div>

        <div id="panel-events-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <h5>
                    <i class="fa fa-filter fa-fw"></i>
                    {{msg.usage_events_form_filter_label}}</h5>

                <form class="row row-filter">
                    <div class="col-xs-6 col-sm-3 col-md-2">
                        <label for="cloudProvider">{{msg.usage_events_form_provider_label}}</label>

                        <div>
                            <select class="form-control input-sm" id="cloudProvider" ng-model="localFilter.cloud">
                                <option>{{msg.usage_events_form_all_label}}</option>
                                <option value="AWS">{{msg.usage_events_form_provider_amazon_label}}</option>
                                <option value="AZURE">{{msg.usage_events_form_provider_microsoft_label}}</option>
                                <option value="GCP">{{msg.usage_events_form_provider_google_label}}</option>
                            </select>
                        </div>
                    </div>
                    <div class="col-xs-6 col-sm-3 col-md-4">
                        <label for="user">{{msg.usage_events_form_user_label}}</label>

                        <div>
                            <div class="input-group">
                                        <span class="input-group-addon">
                                            <i class="fa fa-search"></i>
                                        </span>
                                <input class="form-control input-sm" type="text" ng-model="localFilter.user" id="user">
                            </div>
                        </div>
                    </div>
                    <div class="col-xs-6 col-sm-3 col-md-4">
                        <label for="user">{{msg.events_form_event_type_label}}</label>

                        <div>
                            <select class="form-control input-sm" id="eventType" ng-model="localFilter.eventType">
                                <option>{{msg.usage_events_form_all_label}}</option>

                                <option ng-repeat="(key, value) in $root.config.EVENT_TYPE"  value="{{key}}">{{value}}</option>
                            </select>
                        </div>
                    </div>



                    <div class="col-xs-6 col-sm-3 col-md-2">
                        <a id="btnGenReport" ng-click="loadEvents()" class="btn btn-success btn-block" role="button">
                            <i class="fa fa-fw fa-refresh"></i>
                            <!-- <i class="fa fa-circle-o-notch fa-spin fa-fw"></i> --> </a>
                    </div>

                </form>
                <!-- .row -->

                <div class="table-responsive" ng-show="(events.length != 0) && events">
                    <table class="table table-report table-sortable-cols table-with-pagination ">
                        <thead>
                        <tr>
                            <th>{{msg.usage_events_list_cloud_label}}</th>
                            <th>{{msg.usage_events_form_user_label}}</th>
                            <th>{{msg.events_list_type_label}}</th>
                            <th>{{msg.events_list_timestamp_label}}</th>
                            <th style="width: 25%;">{{msg.events_list_message_label}}</th>
                        </tr>
                        </thead>
                        <tbody>
                            <tr ng-repeat="event in events| filter:eventFilterFunction">
                                <td>{{event.cloud == "GCP" ? "GCP" : event.cloud}}</td>
                                <td>{{event.owner}}</td>
                                <td>{{$root.config.EVENT_TYPE[event.eventType]}}</td>
                                <td>{{event.eventTimestamp}}</td>
                                <td style="width: 25%;">{{event.eventMessage}}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

            </div>

        </div>
    </div>
</div>
