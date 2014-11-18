<div class="form-group" ng-class="{ 'has-error': gccTemplateForm.gcc_tclusterName.$dirty && gccTemplateForm.gcc_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="gcc_tclusterName">Name</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcc_tclusterName" ng-model="gccTemp.name" ng-minlength="5" ng-maxlength="100" required id="gcc_tclusterName" placeholder="min. 5 max. 100 char">
        <div class="help-block" ng-show="gccTemplateForm.gcc_tclusterName.$dirty && gccTemplateForm.gcc_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-class="{ 'has-error': gccTemplateForm.gcc_tdescription.$dirty && gccTemplateForm.gcc_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="gcc_tdescription">Description</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" name="gcc_tdescription" ng-model="gccTemp.description" ng-maxlength="1000" id="gcc_tdescription" placeholder="max. 1000 char">
        <div class="help-block" ng-show="gccTemplateForm.gcc_tdescription.$dirty && gccTemplateForm.gcc_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcc_tregion">Region</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcc_tregion" ng-model="gccTemp.parameters.gccZone">
            <option ng-repeat="region in $root.config.GCC.gccRegions" value="{{region.key}}">{{region.value}}</option>
        </select>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcc_tinstanceType">Instance type</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcc_tinstanceType" ng-model="gccTemp.parameters.gccInstanceType">
            <option ng-repeat="instanceType in $root.config.GCC.gccInstanceTypes" value="{{instanceType.key}}">{{instanceType.value}}</option>
        </select>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcc_tvolumecount">Attached volumes per instance</label>
    <div class="col-sm-9">
        <input type="number" name="gcc_tvolumecount" class="form-control" id="gcc_tvolumecount" min="1" ng-model="gccTemp.volumeCount" placeholder="1 -10" max="10" required>
        <div class="help-block"  ng-show="gccTemplateForm.gcc_tvolumecount.$dirty && gccTemplateForm.gcc_tvolumecount.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.volume_count_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcc_tvolumesize">Volume size (GB)</label>
    <div class="col-sm-9">
        <input type="number" name="gcc_tvolumesize" class="form-control" ng-model="gccTemp.volumeSize" id="gcc_tvolumesize" min="10" max="1024" placeholder="10 - 1024 GB" required>
        <div class="help-block"
             ng-show="gccTemplateForm.gcc_tvolumesize.$dirty && gccTemplateForm.gcc_tvolumesize.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.volume_size_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="gcc_tvolumetype">Volume type</label>
    <div class="col-sm-9">
        <select class="form-control" id="gcc_tvolumetype" ng-model="gccTemp.parameters.volumeType">
            <option ng-repeat="diskType in $root.config.GCC.gccDiskTypes" value="{{diskType.key}}">{{diskType.value}}</option>
        </select>
    </div>
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createGccTemplate" ng-disabled="gccTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createGccTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
            create template</a>
    </div>
</div>
