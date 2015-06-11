
    <div class="form-group" ng-class="{ 'has-error': TemplateForm.openstack_tclusterName.$dirty && openstackTemplateForm.openstack_tclusterName.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="openstack_tclusterName" ng-model="openstackTemp.name" ng-minlength="5" ng-maxlength="100" required id="openstack_tclusterName" placeholder="{{msg.name_placeholder}}">
            <div class="help-block" ng-show="openstackTemplateForm.openstack_tclusterName.$dirty && openstackTemplateForm.openstack_tclusterName.$invalid">
                <i class="fa fa-warning"></i> {{msg.template_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': openstackTemplateForm.openstack_tdescription.$dirty && openstackTemplateForm.openstack_tdescription.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tdescription">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="openstack_tdescription" ng-model="openstackTemp.description" ng-maxlength="1000" id="openstack_tdescription" placeholder="{{msg.template_form_description_placeholder}}">
            <div class="help-block" ng-show="openstackTemplateForm.openstack_tdescription.$dirty && openstackTemplateForm.openstack_tdescription.$invalid">
                <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstack_tinstanceType">{{msg.template_form_instance_type_label}}</label>

        <div class="col-sm-9">
          <input type="text" name="openstack_tinstancetype" class="form-control" ng-model="openstackTemp.parameters.instanceType" id="openstack_tinstancetype"
            placeholder="custom instance type name" required>

            <div class="help-block"
              ng-show="openstackTemplateForm.openstack_tinstancetype.$dirty && openstackTemplateForm.openstack_tinstancetype.$invalid"><i class="fa fa-warning"></i>
              {{msg.openstack_template_instancetype_invalid}}
            </div>
            <!-- .col-sm-9 -->
          </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-class="{ 'has-error' : openstackTemplateForm.openstack_tvolumecount.$dirty && openstackTemplateForm.openstack_tvolumecount.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tvolumecount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <input type="number" name="openstack_tvolumecount" class="form-control" ng-model="openstackTemp.volumeCount" id="openstack_tvolumecount" min="1" max="12"
                   placeholder="{{msg.template_form_volume_count_placeholder}}" required>

            <div class="help-block"
                 ng-show="openstackTemplateForm.openstack_tvolumecount.$dirty && openstackTemplateForm.openstack_tvolumecount.$invalid"><i class="fa fa-warning"></i>
                {{msg.volume_count_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': openstackTemplateForm.openstack_tvolumesize.$dirty && openstackTemplateForm.openstack_tvolumesize.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <input type="number" name="openstack_tvolumesize" class="form-control" ng-model="openstackTemp.volumeSize" id="openstack_tvolumesize" min="10"
                   max="1000" placeholder="{{msg.template_form_volume_size_placeholder}}" required>

            <div class="help-block"
                 ng-show="openstackTemplateForm.openstack_tvolumesize.$dirty && openstackTemplateForm.openstack_tvolumesize.$invalid"><i class="fa fa-warning"></i>
                {{msg.volume_size_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group">
            <label class="col-sm-3 control-label" for="openstack_publicinaccount">{{msg.public_in_account_label}}</label>
            <div class="col-sm-9">
                <input type="checkbox" name="openstack_publicinaccount" id="openstack_publicinaccount" ng-model="openstackTemp.public">
            </div>
       <!-- .col-sm-9 -->
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createopenstackTemplate" ng-disabled="openstackTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createOpenstackTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.template_form_create}}</a>
        </div>
    </div>
