<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="importedStackName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="importedStackName" class="form-control-static">{{importedStack.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="importedStack.description">
        <label class="col-sm-3 control-label" for="importedStackDescription">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="importedStackDescription" class="form-control-static">{{importedStack.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="orchestratorEndpoint">{{msg.credential_mesos_form_marathon_endpoint}}</label>

        <div class="col-sm-9">
            <p id="orchestratorEndpoint" class="form-control-static">{{importedStack.orchestrator.apiEndpoint}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>