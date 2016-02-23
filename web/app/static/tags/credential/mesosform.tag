<div class="form-group" ng-class="{ 'has-error': mesosImportStackForm.mesosStackName.$dirty && mesosImportStackForm.mesosStackName.$invalid }">
    <label class="col-sm-3 control-label" for="mesosStackName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" class="form-control" ng-model="mesosStack.name" id="mesosStackName" name="mesosStackName" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="mesosImportStackForm.mesosStackName.$dirty && mesosImportStackForm.mesosStackName.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': mesosImportStackForm.mesosStackDescription.$dirty && mesosImportStackForm.mesosStackDescription.$invalid }">
    <label class="col-sm-3 control-label" for="mesosStackDescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-model="mesosStack.description" id="mesosStackDescription" name="mesosStackDescription" ng-maxlength="1000" placeholder="{{msg.credential_form_description_placeholder}}">
        <div class="help-block" ng-show="mesosImportStackForm.mesosStackDescription.$dirty && mesosImportStackForm.mesosStackDescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<!-- .form-group -->
<div class="form-group" ng-class="{ 'has-error': mesosImportStackForm.marathonEndpoint.$dirty && mesosImportStackForm.marathonEndpoint.$invalid }">
    <label class="col-sm-3 control-label" for="marathonEndpoint">{{msg.credential_mesos_form_marathon_endpoint}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="marathonEndpoint" ng-model="mesosStack.orchestrator.apiEndpoint" ng-minlength="5" required id="marathonEndpoint">
        <div class="help-block" ng-show="mesosImportStackForm.marathonEndpoint.$dirty && mesosImportStackForm.marathonEndpoint.$invalid">
            <i class="fa fa-warning"></i> {{msg.credential_marathon_endpoint_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="credPublic">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="credPublic" id="credPublic" ng-model="mesosStack.public">
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="importMesosStack" ng-disabled="mesosImportStackForm.$invalid" ng-click="importMesosStack()" class="btn btn-success btn-block" role="button"><i
                class="fa fa-plus fa-fw"></i>
            {{msg.credential_form_create}}</a>
    </div>
</div>