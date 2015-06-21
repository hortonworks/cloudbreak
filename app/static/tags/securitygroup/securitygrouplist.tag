<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="{{securitygroup.name}}">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="{{securitygroup.name}}" class="form-control-static">{{securitygroup.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="securitygroup.description" ng-if="securitygroup.description != 'null'">
        <label class="col-sm-3 control-label" for="{{securitygroup.name}}-desc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="{{securitygroup.name}}-desc" class="form-control-static">{{securitygroup.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <div class="form-group">
            <label class="col-sm-3 control-label" for="securitygroup-securityrules">{{msg.securitygroups_rules}}</label>
            <div class="col-sm-8">
                <table class="table table-responsive table-condensed usage-inline-table" style="margin-bottom: 0px;margin-top: 10px;">
                <thead>
                  <tr>
                    <th>{{msg.securityrule_cidr}}</th>
                    <th>{{msg.securityrule_ports}}</th>
                    <th>{{msg.securityrule_protocol}}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr ng-repeat="rule in securitygroup.securityRules">
                    <td>{{rule.subnet}}</td>
                    <td><p>{{rule.ports}}</p></td>
                    <td>{{rule.protocol}}</td>
                  </tr>
                </tbody>
            </table>
            </div>
        </div>
    </div>
</form>
