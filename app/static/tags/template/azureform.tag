<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tclusterName.$dirty && azureTemplateForm.azure_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tclusterName">Name</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" id="azure_tclusterName" name="azure_tclusterName" ng-model="azureTemp.name" ng-minlength="5" ng-maxlength="100" required placeholder="min. 5 max. 100 char">
        <div class="help-block" ng-show="azureTemplateForm.azure_tclusterName.$dirty && azureTemplateForm.azure_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tdescription.$dirty && azureTemplateForm.azure_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tdescription">Description</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" id="azure_tdescription" name="azure_tdescription" ng-model="azureTemp.description" ng-maxlength="1000" placeholder="max. 1000 char">
        <div class="help-block" ng-show="azureTemplateForm.azure_tdescription.$dirty && azureTemplateForm.azure_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="azure_tlocation">Location</label>

    <div class="col-sm-9">
        <select class="form-control" id="azure_tlocation" ng-options="region.key as region.value for region in $root.config.AZURE.azureRegions" ng-model="azureTemp.parameters.location" required>
        </select>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="azure_tvmType">Instance type</label>
    <div class="col-sm-9">
        <select class="form-control" id="azure_tvmType" ng-options="vmType.key as vmType.value for vmType in $root.config.AZURE.azureVmTypes" ng-model="azureTemp.parameters.vmType" required>
        </select>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tvolumescount.$dirty && azureTemplateForm.azure_tvolumescount.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tvolumescount">Attached volumes per instance</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="azure_tvolumescount" name="azure_tvolumescount" ng-model="azureTemp.volumeCount"  placeholder="1 -10" min="1" max="10" required>
        <div class="help-block" ng-show="azureTemplateForm.azure_tvolumescount.$dirty && azureTemplateForm.azure_tvolumescount.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.volume_count_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tvolumesize.$dirty && azureTemplateForm.azure_tvolumesize.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tvolumesize">Volume size (GB)</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="azure_tvolumesize" name="azure_tvolumesize" ng-model="azureTemp.volumeSize"  placeholder="10 - 1000 GB" min="10" max="1000" required>
        <div class="help-block" ng-show="azureTemplateForm.azure_tvolumesize.$dirty && azureTemplateForm.azure_tvolumesize.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.volume_size_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createAzureTemplate" ng-disabled="azureTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAzureTemplate()" role="button"><i
                class="fa fa-plus fa-fw"></i> create template</a>
    </div>
</div>
