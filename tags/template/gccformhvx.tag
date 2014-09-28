<div class="form-group" ng-class="{ 'has-error': gcchvxTemplateForm.gcchvx_tclusterName.$dirty && gcchvxTemplateForm.gcchvx_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="gcchvx_tclusterName">Name</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcchvx_tclusterName" ng-model="gcchvx_tclusterName" ng-minlength="5" ng-maxlength="20" required id="gcchvx_tclusterName" placeholder="min. 5 max. 20 char">
        <div class="help-block" ng-show="gcchvxTemplateForm.gcchvx_tclusterName.$dirty && gcchvxTemplateForm.gcchvx_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': gcchvxTemplateForm.gcchvx_tdescription.$dirty && gcchvxTemplateForm.gcchvx_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="gcchvx_tdescription">Description</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="gcchvx_tdescription" ng-model="gcchvx_tdescription" ng-maxlength="20" id="gcchvx_tdescription" placeholder="max. 20 char">
        <div class="help-block" ng-show="gcchvxTemplateForm.gcchvx_tdescription.$dirty && gcchvxTemplateForm.gcchvx_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="gcchvx_tregion">Region</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcchvx_tregion">
            <option value="US_CENTRAL1_A">us-central1-a</option>
            <option value="US_CENTRAL1_B">us-central1-b</option>
            <option value="US_CENTRAL1_F">us-central1-f</option>
            <option value="EUROPE_WEST1_A">europe-west1-a</option>
            <option value="EUROPE_WEST1_B">europe-west1-b</option>
            <option value="ASIA_EAST1_A">asia-east1-a</option>
            <option value="ASIA_EAST1_B">asia-east1-b</option>
        </select>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcchvx_tinstanceType">Instance type</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcchvx_tinstanceType">
            <option value="N1_STANDARD_1">n1-standard-1</option>
            <option value="N1_STANDARD_2">n1-standard-2</option>
            <option value="N1_STANDARD_4">n1-standard-4</option>
            <option value="N1_STANDARD_8">n1-standard-8</option>
            <option value="N1_STANDARD_16">n1-standard-16</option>
            <option value="N1_HIGHMEM_2">n1-highmem-2</option>
            <option value="N1_HIGHMEM_4">n1-highmem-4</option>
            <option value="N1_HIGHMEM_8">n1-highmem-8</option>
            <option value="N1_HIGHMEM_16">n1-highmem-16</option>
            <option value="N1_HIGHCPU_2">n1-highcpu-2</option>
            <option value="N1_HIGHCPU_4">n1-highcpu-4</option>
            <option value="N1_HIGHCPU_8">n1-highcpu-8</option>
            <option value="N1_HIGHCPU_16">n1-highcpu-16</option>
        </select>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="gcchvx_tvolumecount">Attached volumes per instance</label>

    <div class="col-sm-9">
        <input type="number" name="gcchvx_tvolumecount" class="form-control" ng-model="gcchvx_tvolumecount" id="gcchvx_tvolumecount" min="1"
               required>

        <div class="help-block"
             ng-show="gcchvxTemplateForm.gcchvx_tvolumecount.$dirty && gcchvxTemplateForm.gcchvx_tvolumecount.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.volume_count_invalid}}
        </div>
    <!-- .col-sm-9 -->
  </div>
</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="gcchvx_tvolumesize">Volume size (GB)</label>

    <div class="col-sm-9">
        <input type="number" name="gcchvx_tvolumesize" class="form-control" ng-model="gcchvx_tvolumesize" id="gcchvx_tvolumesize" min="10"
               max="1024" required>

        <div class="help-block"
             ng-show="gcchvxTemplateForm.gcchvx_tvolumesize.$dirty && gcchvxTemplateForm.gcchvx_tvolumesize.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.volume_size_invalid}}
        </div>
    <!-- .col-sm-9 -->
  </div>
</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="gcchvx_containerCount">Count of container per Instance</label>

    <div class="col-sm-9">
        <input type="number" name="gcchvx_containerCount" class="form-control" ng-model="gcchvx_containerCount" id="gcchvx_containerCount" min="1"
               required>

        <div class="help-block"
             ng-show="gcchvxTemplateForm.gcchvx_containerCount.$dirty && gcchvxTemplateForm.gcchvx_containerCount.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.container_count_invalid}}
        </div>
        <!-- .col-sm-9 -->
    </div>
</div>


<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a href="#" id="createGccHvxTemplate" ng-disabled="gcchvxTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createGccHvxTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
            create template</a>
    </div>
</div>
