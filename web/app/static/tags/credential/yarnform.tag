<div class="form-group" ng-class="{ 'has-error': yarnImportStackForm.yarnStackName.$dirty && yarnImportStackForm.yarnStackName.$invalid }">
    <label class="col-sm-3 control-label" for="yarnStackName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" class="form-control" ng-model="yarnStack.name" id="yarnStackName" name="yarnStackName" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="yarnImportStackForm.yarnStackName.$dirty && yarnImportStackForm.yarnStackName.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': yarnImportStackForm.yarnStackDescription.$dirty && yarnImportStackForm.yarnStackDescription.$invalid }">
    <label class="col-sm-3 control-label" for="yarnStackDescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="yarnStack.description" id="yarnStackDescription" name="yarnStackDescription" ng-maxlength="1000" placeholder="{{msg.credential_form_description_placeholder}}">
        <div class="help-block" ng-show="yarnImportStackForm.yarnStackDescription.$dirty && yarnImportStackForm.yarnStackDescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': yarnImportStackForm.yarnEndpoint.$dirty && yarnImportStackForm.yarnEndpoint.$invalid }">
    <label class="col-sm-3 control-label" for="yarnEndpoint">{{msg.credential_yarn_form_yarn_endpoint}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="yarnEndpoint" ng-model="yarnStack.orchestrator.apiEndpoint" ng-minlength="5" required id="yarnEndpoint">
        <div class="help-block" ng-show="yarnImportStackForm.yarnEndpoint.$dirty && yarnImportStackForm.yarnEndpoint.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_yarn_endpoint_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="credPublic">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="credPublic" id="credPublic" ng-model="yarnStack.public">
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="importYarnStack" ng-disabled="yarnImportStackForm.$invalid" ng-click="importYarnStack()" class="btn btn-success btn-block" role="button"><i
                class="fa fa-plus fa-fw"></i>
            {{msg.credential_form_create}}</a>
    </div>
</div>