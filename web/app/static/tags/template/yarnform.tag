<div class="form-group" ng-class="{ 'has-error': yarnTemplateForm.yarn_constraintName.$dirty && yarnTemplateForm.yarn_constraintName.$invalid }">
    <label class="col-sm-3 control-label" for="yarn_constraintName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" class="form-control" id="yarn_constraintName" name="yarn_constraintName" ng-model="yarnTemp.name" ng-minlength="5" ng-maxlength="100" required placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="yarnTemplateForm.yarn_constraintName.$dirty && yarnTemplateForm.yarn_constraintName.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': yarnTemplateForm.yarn_tdescription.$dirty && yarnTemplateForm.yarn_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="yarn_tdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="yarn_tdescription" ng-model="yarnTemp.description" ng-maxlength="1000" id="yarn_tdescription" placeholder="{{msg.template_form_description_placeholder}}">
        <div class="help-block" ng-show="yarnTemplateForm.yarn_tdescription.$dirty && yarnTemplateForm.yarn_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>
<div class="form-group" ng-class="{ 'has-error' : yarnTemplateForm.yarn_tcpu.$dirty && yarnTemplateForm.yarn_tcpu.$invalid }">
    <label class="col-sm-3 control-label" for="yarn_tcpu">{{msg.template_form_cpu_label}}</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="yarn_tcpu" name="yarn_tcpu" ng-model="yarnTemp.cpu" min="0.1" max="64.0" required>
        <div class="help-block" ng-show="yarnTemplateForm.yarn_tcpu.$dirty && yarnTemplateForm.yarn_tcpu.$invalid"><i class="fa fa-warning"></i> {{msg.cpu_invalid}}
        </div>

    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-class="{ 'has-error' : yarnTemplateForm.yarn_tmemory.$dirty && yarnTemplateForm.yarn_tmemory.$invalid }">
    <label class="col-sm-3 control-label" for="yarn_tmemory">{{msg.template_form_memory_label}}</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="yarn_tmemory" name="yarn_tmemory" ng-model="yarnTemp.memory" min="16" max="131072" required>
        <div class="help-block" ng-show="yarnTemplateForm.yarn_tmemory.$dirty && yarnTemplateForm.yarn_tmemory.$invalid"><i class="fa fa-warning"></i> {{msg.memory_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>
<!--<div class="form-group" ng-class="{ 'has-error' : yarnTemplateForm.yarn_tdisk.$dirty && yarnTemplateForm.yarn_tdisk.$invalid }">
    <label class="col-sm-3 control-label" for="yarn_tdisk">{{msg.template_form_disk_label}}</label>

    <div class="col-sm-9">
        <input type="number" class="form-control" id="yarn_tdisk" name="yarn_tdisk" ng-model="yarnTemp.disk" min="10" max="1000" required>
        <div class="help-block" ng-show="yarnTemplateForm.yarn_tdisk.$dirty && yarnTemplateForm.yarn_tdisk.$invalid"><i class="fa fa-warning"></i> {{msg.disk_invalid}}
        </div>
    </div>
</div>-->

<div class="form-group">
    <label class="col-sm-3 control-label" for="yarn_publicinaccount">{{msg.public_in_account_label}}</label>
    <div class="col-sm-9">
        <input type="checkbox" name="yarn_publicinaccount" id="yarn_publicinaccount" ng-model="yarnTemp.public">
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createYarnTemplate" ng-disabled="yarnTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createYarnTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.template_form_create}}</a>
    </div>
</div>
