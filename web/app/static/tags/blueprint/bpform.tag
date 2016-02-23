<div class="form-group" ng-class="{ 'has-error': blueprintForm.name.$dirty && blueprintForm.name.$invalid }">
    <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="name" ng-model="blueprint.name" name="name" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" ng-minlength="5" ng-maxlength="100" placeholder="{{msg.name_placeholder}}" required>
        <div class="help-block" ng-show="blueprintForm.name.$dirty && blueprintForm.name.$invalid"><i class="fa fa-warning"></i> {{msg.blueprint_name_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="description">{{msg.description_label}}</label>
    <div class="col-sm-9">
        <input type="text" class="form-control" id="description" ng-model="blueprint.description" name="description" placeholder="{{msg.blueprint_form_description_placeholder}}" ng-maxlength="50">
        <div class="help-block" ng-show="blueprintForm.description.$dirty && blueprintForm.description.$invalid"><i class="fa fa-warning"></i> {{msg.blueprint_description_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="blueprinttype">{{msg.blueprint_type}}</label>
    <div class="col-sm-9">
        <div id="blueprinttype" name="blueprinttype" class="row">
            <div class="col-md-3">
                <select class="form-control" id="blueprinttype" name="blueprinttype" ng-options="blueprintType.key as blueprintType.value for blueprintType in $root.config.BLUEPRINT_TYPE" ng-model="blueprintType"></select>
            </div>
        </div>
    </div>
</div>
<div class="form-group" ng-show="blueprintType == 'URL'" ng-class="{ 'has-error': blueprintForm.url.$dirty && blueprintForm.url.$invalid }">
    <label class="col-sm-3 control-label" for="url">{{msg.blueprint_form_source_label}}</label>
    <div class="col-sm-9">
        <input type="url" class="form-control" id="url" ng-model="blueprint.url" name="url" placeholder="{{msg.blueprint_form_source_placeholder}}" ng-required="blueprintType == 'URL'">
        <div class="help-block" ng-show="blueprintForm.url.$dirty && blueprintForm.url.$invalid">
            <i class="fa fa-warning"></i> {{msg.blueprint_url_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="blueprintType == 'TEXT'" ng-class="{ 'has-error': blueprintForm.ambariBlueprint.$error.validjson && blueprintForm.ambariBlueprint.$dirty && blueprintForm.ambariBlueprint.$error.validjson }">
    <label class="col-sm-3 control-label" for="ambariBlueprint">{{msg.blueprint_form_manual_label}}</label>
    <div class="col-sm-9">
        <textarea class="form-control" id="ambariBlueprint" ng-model="blueprint.ambariBlueprint" name="ambariBlueprint" placeholder="{{msg.blueprint_form_manual_placeholder}}" validjson rows="10" ng-required="blueprintType == 'TEXT' || blueprintType == 'FILE'"></textarea>
        <div class="help-block" ng-show="blueprintForm.ambariBlueprint.$error.validjson && blueprintForm.ambariBlueprint.$dirty">
            <i class="fa fa-warning"></i> {{msg.blueprint_json_invalid}}
        </div>
    </div>
</div>
<div class="form-group" ng-show="blueprintType == 'FILE'" ng-class="{ 'has-error': !fileReadAvailable || blueprintFileValid === false }">
    <label class="col-sm-3 control-label" for="blueprintFile" style="border-bottom: 0">{{msg.blueprint_file}}</label>
    <div class="col-sm-9">
        <input type="file" name="blueprintFile" id="blueprintFile" onchange="angular.element(this).scope().generateBlueprintFromFile()" ng-disabled="{{!fileReadAvailable}}" />
        <div class="help-block" ng-show="!fileReadAvailable">
            <i class="fa fa-warning"></i> {{msg.file_upload_not_allowed}}
        </div>
        <div class="help-block" ng-show="blueprintFileValid === false">
            <i class="fa fa-warning"></i> {{msg.blueprint_json_invalid}}
        </div>
    </div>
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="bpPublic">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="bpPublic" id="bpPublic" ng-model="blueprint.public">
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a href="" id="createBlueprint" ng-click="createBlueprint()" ng-disabled="blueprintForm.$invalid || (blueprintType == 'FILE' && blueprintFileValid !== true)" class="btn btn-success btn-block" role="button"><i class="fa fa-plus fa-fw"></i> {{msg.blueprint_form_create}}</a>
    </div>
</div>