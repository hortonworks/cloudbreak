<div class="form-group" ng-class="{ 'has-error': openstackTopologyForm.openstack_tclusterName.$dirty && openstackTopologyForm.openstack_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_tclusterName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="openstack_tclusterName" ng-model="openstackTemp.name" ng-minlength="5" ng-maxlength="100" required id="openstack_tclusterName" placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="openstackTopologyForm.openstack_tclusterName.$dirty && openstackTopologyForm.openstack_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{msg.topology_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': openstackTopologyForm.openstack_tdescription.$dirty && openstackTopologyForm.openstack_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_tdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="openstack_tdescription" ng-model="openstackTemp.description" ng-maxlength="1000" id="openstack_tdescription" placeholder="{{msg.topology_form_description_placeholder}}">
        <div class="help-block" ng-show="openstackTopologyForm.openstack_tdescription.$dirty && openstackTopologyForm.openstack_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.topology_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group" ng-class="{ 'has-error' : openstackTopologyForm.openstack_tendpoint.$dirty && openstackTopologyForm.openstack_tendpoint.$invalid }">
    <label class="col-sm-3 control-label" for="openstack_tendpoint">{{msg.topology_endpoint_label}}</label>

    <div class="col-sm-9">
        <input type="text" name="openstack_tendpoint" class="form-control" ng-model="openstackTemp.endpoint" id="openstack_tendpoint" placeholder="endpoint">
        <div class="help-block" ng-show="openstackTopologyForm.openstack_tendpoint.$dirty && openstackTopologyForm.openstack_tendpoint.$invalid"><i class="fa fa-warning"></i>{{msg.topology_endpoint_invalid}}
        </div>
        <!-- .col-sm-9 -->
    </div>
</div>

<div ng-show="!modify" class="form-group" ng-class="{ 'has-error': !fileReadAvailable }">
    <label class="col-sm-3 control-label" for="mappingFile">{{msg.topology_upload_mapping}}</label>
    <div class="col-sm-9">
        <input style="opacity: 0; height: 0px;" type="file" name="mappingFile" id="mappingFile" onchange="angular.element(this).scope().generateMappingFromFile()" ng-disabled="{{!fileReadAvailable}}" title="please choose a mapping file" />
        <span class="btn btn-info" id="mappingFileButton" onclick="document.getElementById('mappingFile').click();">{{msg.topology_form_select_file}}<span>
        <div class="help-block" ng-show="!fileReadAvailable"><i class="fa fa-warning"></i> {{msg.file_upload_not_allowed}}
        </div>
    </div>
</div>

<div class="form-group">
    <label class="col-sm-3 control-label">{{msg.topology_mapping_label}}</label>
    <div class="col-sm-8 col-sm-offset-1">
        <ng-form class="form-horizontal" role="form" name="topologyMappingForm">
            <div class="form-group" ng-class="{ 'has-error': topologyMappingForm.hypervisor.$dirty && topologyMappingForm.hypervisor.$invalid }">
                <label class="col-sm-3 control-label">{{msg.topology_hypervisor_label}}</label>
                <div class="col-sm-9">
                    <input type="text" class="form-control" name="hypervisor" placeholder="" ng-model="tmpMapping.hypervisor" id="hypervisor" ng-minlength="2" ng-pattern="/^[^\s]*$/" ng-trim="false" required>
                    <div class="help-block" ng-show="topologyMappingForm.hypervisor.$dirty && topologyMappingForm.hypervisor.$invalid">
                        <i class="fa fa-warning"></i> {{msg.topology_hypervisor_invalid}}
                    </div>
                </div>
            </div>
            <div class="form-group" ng-class="{ 'has-error': topologyMappingForm.rack.$dirty && topologyMappingForm.rack.$invalid }">
                <label class="col-sm-3 control-label">{{msg.topology_rack_label}}</label>
                <div class="col-sm-9">
                    <input type="text" class="form-control" name="rack" placeholder="" ng-model="tmpMapping.rack" id="rack" ng-minlength="2" ng-pattern="/^[^\s]*$/" ng-trim="false" required>
                    <div class="help-block" ng-show="topologyMappingForm.rack.$dirty && topologyMappingForm.rack.$invalid">
                        <i class="fa fa-warning"></i> {{msg.topology_rack_invalid}}
                    </div>
                </div>
            </div>
            <div class="form-group">
                <a id="createMapping" ng-disabled="topologyMappingForm.$invalid" class="btn btn-success btn-block" ng-click="addMapping(topologyMappingForm)" role="button"><i class="fa fa-plus"></i> {{msg.topology_form_add_mapping}}</a>
            </div>
        </ng-form>
        <div style="padding-top: 10px;">
            <table class="table table-bordered">
                <thead>
                    <tr>
                        <th>{{msg.topology_hypervisor_table_label}}</th>
                        <th>{{msg.topology_rack_table_label}}</th>
                        <th>{{msg.topology_remove_table_label}}</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="(hypervisor, rack) in openstackTemp.nodes">
                        <td>{{hypervisor}}</td>
                        <td>{{rack}}</td>
                        <td><span id="deleteMapping" class="label label-danger" ng-click="deleteMapping(hypervisor)" role="button"><i class="fa fa-minus"></i></span></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createOpenstackTopology" ng-disabled="openstackTopologyForm.openstack_tclusterName.$invalid" class="btn btn-success btn-block" ng-click="createOpenstackTopology()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{modify ? msg.topology_form_modify : msg.topology_form_create}}</a>
    </div>
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="cancelOpenstackTopology" ng-show="modify" class="btn btn-warning btn-block" ng-click="cancelModify()" role="button" data-toggle="collapse" data-target="#panel-create-topologies-collapse"><i class="fa fa-plus fa-fw"></i>
                {{msg.topology_form_cancel}}</a>
    </div>
</div>