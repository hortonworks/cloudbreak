<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstackclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="openstackclusterName" class="form-control-static">{{topology.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="topology.description">
        <label class="col-sm-3 control-label" for="openstackclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="openstackclusterDesc" class="form-control-static">{{topology.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstackinstanceType">{{msg.topology_endpoint_label}}</label>

        <div class="col-sm-9">
            <p id="openstackinstanceType" class="form-control-static">{{topology.endpoint}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <label class="col-sm-3 control-label" for="emailuser">{{msg.topology_mapping_label}}</label>
    <div class="col-sm-4">
        <table class="table table-bordered table-striped responsive-utilities">
            <thead>
                <tr>
                    <th>{{msg.topology_hypervisor_table_label}}</th>
                    <th>{{msg.topology_rack_table_label}}</th>
                </tr>
            </thead>
            <tbody>

                <tr ng-repeat="(hypervisor,rack) in topology.nodes">
                    <td>{{hypervisor}}</td>
                    <td>{{rack}}</td>
                </tr>
            </tbody>
        </table>
    </div>
</form>