<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tclusterName.$dirty && azureTemplateForm.azure_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tclusterName">Name</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" id="azure_tclusterName" name="azure_tclusterName" ng-model="azure_tclusterName" ng-minlength="5" ng-maxlength="20" required placeholder="min. 5 max. 20 char">
        <div class="help-block" ng-show="azureTemplateForm.azure_tclusterName.$dirty && azureTemplateForm.azure_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tdescription.$dirty && azureTemplateForm.azure_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tdescription">Description</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" id="azure_tdescription" name="azure_tdescription" ng-model="azure_tdescription" ng-maxlength="20" placeholder="max. 20 char">
        <div class="help-block" ng-show="azureTemplateForm.azure_tdescription.$dirty && azureTemplateForm.azure_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="azure_tlocation">Location</label>

    <div class="col-sm-9">
        <select class="form-control" id="azure_tlocation">
            <option value="NORTH_EUROPE">North Europe</option>
            <option value="EAST_ASIA">East Asia</option>
            <option value="EAST_US">East US</option>
            <option value="WEST_US">West US</option>
            <option value="BRAZIL_SOUTH">Brazil South</option>
        </select>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="azure_tvmType">Instance type</label>
    <div class="col-sm-9">
        <select class="form-control" id="azure_tvmType">
            <option value="SMALL">Small</option>
            <option value="MEDIUM">Medium</option>
            <option value="LARGE">Large</option>
            <option value="EXTRA_LARGE">Extra Large</option>
        </select>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tvolumescount.$dirty && azureTemplateForm.azure_tvolumescount.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tvolumescount">Attached volumes per instance</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="azure_tvolumescount" name="azure_tvolumescount" ng-model="azure_tvolumescount"  placeholder="0" min="1">
        <div class="help-block" ng-show="azureTemplateForm.azure_tvolumescount.$dirty && azureTemplateForm.azure_tvolumescount.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_volume_count_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': azureTemplateForm.azure_tvolumesize.$dirty && azureTemplateForm.azure_tvolumesize.$invalid }">
    <label class="col-sm-3 control-label" for="azure_tvolumesize">Volume size (GB)</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="azure_tvolumesize" name="azure_tvolumesize" ng-model="azure_tvolumesize"  placeholder="0 Gb" min="10" max="1024">
        <div class="help-block" ng-show="azureTemplateForm.azure_tvolumesize.$dirty && azureTemplateForm.azure_tvolumesize.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.volume_size_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>



<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a href="#" id="createAzureTemplate" ng-disabled="azureTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAzureTemplate()" role="button"><i
                class="fa fa-plus fa-fw"></i> create template</a>
    </div>
</div>