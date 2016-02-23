<div class="form-group" ng-class="{ 'has-error': mesosTemplateForm.mesos_constraintName.$dirty && mesosTemplateForm.mesos_constraintName.$invalid }">
    <label class="col-sm-3 control-label" for="mesos_constraintName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="mesos_constraintName" ng-model="mesosTemp.name" ng-minlength="5" ng-maxlength="100" required id="mesos_constraintName" placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="mesosTemplateForm.mesos_constraintName.$dirty && mesosTemplateForm.mesos_constraintName.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': mesosTemplateForm.mesos_tdescription.$dirty && mesosTemplateForm.mesos_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="mesos_tdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="mesos_tdescription" ng-model="mesosTemp.description" ng-maxlength="1000" id="mesos_tdescription" placeholder="{{msg.template_form_description_placeholder}}">
        <div class="help-block" ng-show="mesosTemplateForm.mesos_tdescription.$dirty && mesosTemplateForm.mesos_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error' : mesosTemplateForm.mesos_tcpu.$dirty && mesosTemplateForm.mesos_tcpu.$invalid }">
    <label class="col-sm-3 control-label" for="mesos_tcpu">{{msg.template_form_cpu_label}}</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="mesos_tcpu" name="mesos_tcpu" ng-model="mesosTemp.cpu" min="0.1" max="64.0" required>
        <div class="help-block" ng-show="mesosTemplateForm.mesos_tcpu.$dirty && mesosTemplateForm.mesos_tcpu.$invalid"><i class="fa fa-warning"></i> {{msg.cpu_invalid}}
        </div>

    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-class="{ 'has-error' : mesosTemplateForm.mesos_tmemory.$dirty && mesosTemplateForm.mesos_tmemory.$invalid }">
    <label class="col-sm-3 control-label" for="mesos_tmemory">{{msg.template_form_memory_label}}</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="mesos_tmemory" name="mesos_tmemory" ng-model="mesosTemp.memory" min="16" max="131072" required>
        <div class="help-block" ng-show="mesosTemplateForm.mesos_tmemory.$dirty && mesosTemplateForm.mesos_tmemory.$invalid"><i class="fa fa-warning"></i> {{msg.memory_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-class="{ 'has-error' : mesosTemplateForm.mesos_tdisk.$dirty && mesosTemplateForm.mesos_tdisk.$invalid }">
    <label class="col-sm-3 control-label" for="mesos_tdisk">{{msg.template_form_disk_label}}</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="mesos_tdisk" name="mesos_tdisk" ng-model="mesosTemp.disk" min="10" max="1000" required>
        <div class="help-block" ng-show="mesosTemplateForm.mesos_tdisk.$dirty && mesosTemplateForm.mesos_tdisk.$invalid"><i class="fa fa-warning"></i> {{msg.disk_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="form-group">
    <label class="col-sm-3 control-label" for="mesos_publicinaccount">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="mesos_publicinaccount" id="mesos_publicinaccount" ng-model="mesosTemp.public">
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createMesosTemplate" ng-disabled="mesosTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createMesosTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.constraint_form_create}}</a>
    </div>
</div>