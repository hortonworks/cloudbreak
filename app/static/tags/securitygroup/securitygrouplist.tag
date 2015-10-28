<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 col-sm-offset-1 control-label" for="{{securitygroup.name}}">{{msg.name_label}}</label>

        <div class="col-sm-7">
            <p id="{{securitygroup.name}}" class="form-control-static">{{securitygroup.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="securitygroup.description" ng-if="securitygroup.description != 'null'">
        <label class="col-sm-3 col-sm-offset-1 control-label" for="{{securitygroup.name}}-desc">{{msg.description_label}}</label>

        <div class="col-sm-7">
            <p id="{{securitygroup.name}}-desc" class="form-control-static">{{securitygroup.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 col-sm-offset-1 control-label" for="securitygroup-securityrules">{{msg.securitygroups_rules}}</label>
        <div class="col-sm-6" id="securitygroup-securityrules">
            <table data-toggle="table" class="table security-table table-bordered">
                <thead>
                    <tr>
                        <th>CIDR</th>
                        <th>port</th>
                        <th>protocol</th>
                    </tr>
                </thead>
                <tbody ng-repeat="rule in changeRule(securitygroup.securityRules)">
                    <tr ng-repeat="port1 in rule.portarray">
                        <td>{{rule.subnet}}</td>
                        <td>{{port1}}</td>
                        <td>{{rule.protocol}}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</form>