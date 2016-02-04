<div class="form-group" ng-class="{ 'has-error': gcpTemplateForm.gcp_tclusterName.$dirty && gcpTemplateForm.gcp_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_tclusterName">{{msg.name_label}}</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcp_tclusterName" ng-model="gcpTemp.name" ng-minlength="5" ng-maxlength="100" required id="gcp_tclusterName" placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="gcpTemplateForm.gcp_tclusterName.$dirty && gcpTemplateForm.gcp_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpTemplateForm.gcp_tdescription.$dirty && gcpTemplateForm.gcp_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_tdescription">{{msg.description_label}}</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" name="gcp_tdescription" ng-model="gcpTemp.description" ng-maxlength="1000" id="gcp_tdescription" placeholder="{{msg.template_form_description_placeholder}}">
        <div class="help-block" ng-show="gcpTemplateForm.gcp_tdescription.$dirty && gcpTemplateForm.gcp_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcp_tinstanceType">{{msg.template_form_instance_type_label}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcp_tinstanceType" ng-options="instanceType.value as instanceType.value for instanceType in $root.params.vmTypes.GCP" ng-model="gcpTemp.instanceType">
        </select>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcp_tvolumetype">{{msg.template_form_volume_type_label}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcp_tvolumetype" ng-options="diskType as $root.displayNames.getDisk('GCP', diskType) for diskType in $root.params.diskTypes.GCP" ng-model="gcpTemp.volumeType">
        </select>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpTemplateForm.gcp_tvolumecount.$dirty && gcpTemplateForm.gcp_tvolumecount.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_tvolumecount">{{msg.template_form_volume_count_label}}</label>
    <div class="col-sm-9">
        <input type="number" name="gcp_tvolumecount" class="form-control" id="gcp_tvolumecount" min="1" ng-model="gcpTemp.volumeCount" placeholder="{{msg.template_form_volume_count_placeholder}}" max="12" required>
        <div class="help-block" ng-show="gcpTemplateForm.gcp_tvolumecount.$dirty && gcpTemplateForm.gcp_tvolumecount.$invalid"><i class="fa fa-warning"></i> {{msg.volume_count_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gcpTemplateForm.gcp_tvolumesize.$dirty && gcpTemplateForm.gcp_tvolumesize.$invalid }">
    <label class="col-sm-3 control-label" for="gcp_tvolumesize">{{msg.template_form_volume_size_label}}</label>
    <div class="col-sm-9">
        <input type="number" name="gcp_tvolumesize" class="form-control" ng-model="gcpTemp.volumeSize" id="gcp_tvolumesize" min="10" max="1000" placeholder="{{msg.template_form_volume_size_placeholder}}" required>
        <div class="help-block" ng-show="gcpTemplateForm.gcp_tvolumesize.$dirty && gcpTemplateForm.gcp_tvolumesize.$invalid"><i class="fa fa-warning"></i> {{msg.volume_size_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcp_publicInAccount">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="gcp_publicInAccount" id="gcp_publicInAccount" ng-model="gcpTemp.public">
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="topologySelect" name="topologySelect" ng-model="gcpTemp.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'" >
            <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
        </select>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createGcpTemplate" ng-disabled="gcpTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createGcpTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
            {{msg.template_form_create}}</a>
    </div>
</div>