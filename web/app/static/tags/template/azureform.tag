<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tclusterName.$dirty && azureTemplateForm.azure_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tclusterName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" id="azure_tclusterName" name="azure_tclusterName" ng-model="azureTemp.name" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="azureTemplateForm.azure_tclusterName.$dirty && azureTemplateForm.azure_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tdescription.$dirty && azureTemplateForm.azure_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" id="azure_tdescription" name="azure_tdescription" ng-model="azureTemp.description" ng-maxlength="1000" placeholder="{{msg.template_form_description_placeholder}}">
        <div class="help-block" ng-show="azureTemplateForm.azure_tdescription.$dirty && azureTemplateForm.azure_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="azure_tvmType">{{msg.template_form_instance_type_label}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="azure_tvmType" ng-options="vmType.value as vmType.value for vmType in $root.params.vmTypes.AZURE" ng-model="azureTemp.instanceType" ng-change="changeInstanceType(azureTemp.instanceType, azureTemp.volumeType, 'AZURE', azureTemp)" required>
        </select>
        <div class="help-block ng-binding" ng-show="azureTemp.CPUs && azureTemp.RAMs">{{msg.template_form_vm_info | format: azureTemp.CPUs:azureTemp.RAMs}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-show="!azureTemp.parameters.managedDisk">
        <label class="col-sm-3 control-label" for="azure_tvolumetype">{{msg.template_form_volume_type_label}}</label>

        <div class="col-sm-9">
            <select class="form-control" id="azure_tvolumetype" name="azure_tvolumetype" ng-options="volumeType as $root.displayNames.getDisk('AZURE', volumeType) for volumeType in $root.params.diskTypes.AZURE | filter:filterByVolumetype" ng-model="azureTemp.volumeType" ng-change="changeInstanceType(azureTemp.instanceType, azureTemp.volumeType, 'AZURE', azureTemp)" required>
            </select>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tvolumescount.$dirty && azureTemplateForm.azure_tvolumescount.$invalid }">
        <label class="col-sm-3 control-label" for="azure_tvolumescount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <input type="number" class="form-control" id="azure_tvolumescount" name="azure_tvolumescount" ng-model="azureTemp.volumeCount" placeholder="{{msg.template_form_volume_count_placeholder | format: azureTemp.minDiskNumber:(azureTemp.maxDiskNumber)}}" min="{{azureTemp.minDiskNumber}}" max="{{azureTemp.maxDiskNumber}}" required>
            <div class="help-block" ng-show="azureTemplateForm.azure_tvolumescount.$dirty && azureTemplateForm.azure_tvolumescount.$invalid">
                <i class="fa fa-warning"></i> {{msg.volume_count_invalid | format: azureTemp.minDiskNumber:(azureTemp.maxDiskNumber)}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tvolumesize.$dirty && azureTemplateForm.azure_tvolumesize.$invalid }">
        <label class="col-sm-3 control-label" for="azure_tvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <input type="number" class="form-control" id="azure_tvolumesize" name="azure_tvolumesize" ng-model="azureTemp.volumeSize" placeholder="{{msg.template_form_volume_size_placeholder | format: azureTemp.minDiskSize:(azureTemp.maxDiskSize)}}" min="{{azureTemp.minDiskSize}}" max="{{azureTemp.maxDiskSize}}" ng-required="azureTemp.maxDiskSize !== azureTemp.minDiskSize" ng-hide="azureTemp.maxDiskSize == azureTemp.minDiskSize">
            <div class="help-block" ng-show="azureTemplateForm.azure_tvolumesize.$dirty && azureTemplateForm.azure_tvolumesize.$invalid">
                <i class="fa fa-warning"></i> {{msg.volume_size_invalid | format: azureTemp.minDiskSize:(azureTemp.maxDiskSize)}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azuremanaged">{{msg.template_form_managed}}</label>

        <div class="col-sm-9">
            <input type="checkbox" name="azuremanaged" id="azuremanaged" ng-model="azureTemp.parameters.managedDisk">
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azure_publicInAccount">{{msg.public_in_account_label}}</label>
        <div class="col-sm-9">
            <input type="checkbox" name="azure_publicInAccount" id="azure_publicInAccount" ng-model="azureTemp.public">
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
        <div class="col-sm-9">
            <select class="form-control" id="topologySelect" name="topologySelect" ng-model="azureTemp.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
                <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
            </select>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createAzureTemplate" ng-disabled="azureTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAzureTemplate()" role="button"><i
                class="fa fa-plus fa-fw"></i> {{msg.template_form_create}}</a>
        </div>
    </div>