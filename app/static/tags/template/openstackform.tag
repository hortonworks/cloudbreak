
    <div class="form-group" ng-class="{ 'has-error': TemplateForm.openstack_tclusterName.$dirty && openstackTemplateForm.openstack_tclusterName.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tclusterName">Name</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="openstack_tclusterName" ng-model="openstackTemp.name" ng-minlength="5" ng-maxlength="100" required id="openstack_tclusterName" placeholder="min. 5 max. 100 char">
            <div class="help-block" ng-show="openstackTemplateForm.openstack_tclusterName.$dirty && openstackTemplateForm.openstack_tclusterName.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': openstackTemplateForm.openstack_tdescription.$dirty && openstackTemplateForm.openstack_tdescription.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tdescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="openstack_tdescription" ng-model="openstackTemp.description" ng-maxlength="1000" id="openstack_tdescription" placeholder="max. 1000 char">
            <div class="help-block" ng-show="openstackTemplateForm.openstack_tdescription.$dirty && openstackTemplateForm.openstack_tdescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstack_tinstanceType">Instance type</label>

        <div class="col-sm-9">
          <input type="text" name="openstack_tinstancetype" class="form-control" ng-model="openstackTemp.parameters.instanceType" id="openstack_tinstancetype"
            placeholder="custom instance type name" required>

            <div class="help-block"
              ng-show="openstackTemplateForm.openstack_tinstancetype.$dirty && openstackTemplateForm.openstack_tinstancetype.$invalid"><i class="fa fa-warning"></i>
              {{error_msg.openstack_template_instancetype_invalid}}
            </div>
            <!-- .col-sm-9 -->
          </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstack_tpublicNetId">Public Net Id</label>

      <div class="col-sm-9">
        <input type="text" name="openstack_tpublicNetId" class="form-control" ng-model="openstackTemp.parameters.publicNetId" id="openstack_tpublicNetId"
          placeholder="public inet id" required>

          <div class="help-block"
            ng-show="openstackTemplateForm.openstack_tpublicNetId.$dirty && openstackTemplateForm.openstack_tpublicNetId.$invalid"><i class="fa fa-warning"></i>
            {{error_msg.openstack_template_inet_invalid}}
          </div>
          <!-- .col-sm-9 -->
        </div>
        <!-- .col-sm-9 -->

      </div>

    <div class="form-group" ng-class="{ 'has-error' : openstackTemplateForm.openstack_tvolumecount.$dirty && openstackTemplateForm.openstack_tvolumecount.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <input type="number" name="openstack_tvolumecount" class="form-control" ng-model="openstackTemp.volumeCount" id="openstack_tvolumecount" min="1" max="10"
                   placeholder="1 -10" required>

            <div class="help-block"
                 ng-show="openstackTemplateForm.openstack_tvolumecount.$dirty && openstackTemplateForm.openstack_tvolumecount.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.volume_count_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': openstackTemplateForm.openstack_tvolumesize.$dirty && openstackTemplateForm.openstack_tvolumesize.$invalid }">
        <label class="col-sm-3 control-label" for="openstack_tvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <input type="number" name="openstack_tvolumesize" class="form-control" ng-model="openstackTemp.volumeSize" id="openstack_tvolumesize" min="10"
                   max="1000" placeholder="10 - 1000 GB" required>

            <div class="help-block"
                 ng-show="openstackTemplateForm.openstack_tvolumesize.$dirty && openstackTemplateForm.openstack_tvolumesize.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.volume_size_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group">
            <label class="col-sm-3 control-label" for="openstack_publicinaccount">Public in account</label>
            <div class="col-sm-9">
                <input type="checkbox" name="openstack_publicinaccount" id="openstack_publicinaccount" ng-model="openstackTemp.public">
            </div>
       <!-- .col-sm-9 -->
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createopenstackTemplate" ng-disabled="openstackTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createOpenstackTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
                create template</a>
        </div>
    </div>
