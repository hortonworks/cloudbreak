<div class="form-group" ng-class="{ 'has-error': blueprintForm.name.$dirty && blueprintForm.name.$invalid }">
    <label class="col-sm-3 control-label" for="name">Name</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="name" ng-model="blueprint.name" name="name" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" ng-minlength="5" ng-maxlength="100" placeholder="min. 5 max. 100 char" required>
        <div class="help-block" ng-show="blueprintForm.name.$dirty && blueprintForm.name.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.blueprint_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="description">Description</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="description" ng-model="blueprint.description" name="description" placeholder="max. 50 char" ng-maxlength="50">
        <div class="help-block" ng-show="blueprintForm.description.$dirty && blueprintForm.description.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.blueprint_description_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-hide="blueprintForm.ambariBlueprint.$dirty && !blueprintForm.ambariBlueprint.$error.required" ng-class="{ 'has-error': blueprintForm.url.$dirty && blueprintForm.url.$invalid }">
    <label class="col-sm-3 control-label" for="url">Source URL</label>
    <div class="col-sm-9">
        <input type="url" class="form-control" id="url" ng-model="blueprint.url" name="url"  placeholder="set blueprint URL" required>
        <div class="help-block" ng-show="blueprintForm.url.$dirty && blueprintForm.url.$invalid">
            <i class="fa fa-warning"></i> {{error_msg.blueprint_url_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-hide="blueprintForm.url.$dirty && !blueprintForm.url.$error.required" ng-class="{ 'has-error': blueprintForm.ambariBlueprint.$error.validjson && blueprintForm.ambariBlueprint.$dirty }">
    <label class="col-sm-3 control-label" for="ambariBlueprint">Manual copy</label>
    <div class="col-sm-9">
        <textarea class="form-control" id="ambariBlueprint" ng-model="blueprint.ambariBlueprint" name="ambariBlueprint" placeholder="paste blueprint definition here as JSON...." validjson rows="10" required></textarea>
        <div class="help-block" ng-show="blueprintForm.ambariBlueprint.$error.validjson">
            <i class="fa fa-warning"></i> {{error_msg.blueprint_json_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
      <label class="col-sm-3 control-label" for="bpPublic">Public in account</label>
      <div class="col-sm-9">
          <input type="checkbox" name="bpPublic" id="bpPublic" ng-model="blueprint.public">
      </div>
       <!-- .col-sm-9 -->
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a href="" id="createBlueprint" ng-click="createBlueprint()"
           ng-disabled="blueprintForm.name.$invalid || (blueprintForm.url.$error.url && !blueprintForm.url.$error.required) || (blueprintForm.ambariBlueprint.$error.validjson && !blueprintForm.ambariBlueprint.$error.required) ||
           (blueprintForm.url.$error.required && blueprintForm.ambariBlueprint.$error.required)" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> create blueprint</a>
    </div>
</div>
