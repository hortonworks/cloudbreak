
    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tclusterName.$dirty && awsTemplateForm.aws_tclusterName.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_tclusterName" ng-model="awsTemp.name" ng-minlength="5" ng-maxlength="100" required id="aws_tclusterName" placeholder="{{msg.name_placeholder}}">
            <div class="help-block" ng-show="awsTemplateForm.aws_tclusterName.$dirty && awsTemplateForm.aws_tclusterName.$invalid">
                <i class="fa fa-warning"></i> {{msg.template_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tdescription.$dirty && awsTemplateForm.aws_tdescription.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tdescription">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="aws_tdescription" ng-model="awsTemp.description" ng-maxlength="1000" id="aws_tdescription" placeholder="{{msg.template_form_description_placeholder}}">
            <div class="help-block" ng-show="awsTemplateForm.aws_tdescription.$dirty && awsTemplateForm.aws_tdescription.$invalid">
                <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tinstanceType">{{msg.template_form_instance_type_label}}</label>

        <div class="col-sm-9">
            <select class="form-control" id="aws_tinstanceType" name="aws_tinstanceType" ng-options="instanceType.key as instanceType.value for instanceType in $root.config.AWS.instanceType" ng-model="awsTemp.parameters.instanceType" ng-change="changeAwsInstanceType()" required>
            </select>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="aws_tvolumetype">{{msg.template_form_volume_type_label}}</label>

      <div class="col-sm-9">
        <select class="form-control" id="aws_tvolumetype" name="aws_tvolumetype" ng-options="volumeType.key as volumeType.value for volumeType in $root.config.AWS.volumeTypes | filter:filterByVolumetype" ng-model="awsTemp.parameters.volumeType" ng-change="changeAwsInstanceType()" required>
        </select>
      </div>
      <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error' : awsTemplateForm.aws_tvolumecount.$dirty && awsTemplateForm.aws_tvolumecount.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tvolumecount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <input type="number" name="aws_tvolumecount" class="form-control" ng-model="awsTemp.volumeCount" id="aws_tvolumecount" min="1" max="12"
                   placeholder="{{msg.template_form_volume_count_placeholder}}" required>

            <div class="help-block"
                 ng-show="awsTemplateForm.aws_tvolumecount.$dirty && awsTemplateForm.aws_tvolumecount.$invalid"><i class="fa fa-warning"></i>
                {{msg.volume_count_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tvolumesize.$dirty && awsTemplateForm.aws_tvolumesize.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">

            <input type="number" name="aws_tvolumesize" class="form-control" ng-model="awsTemp.volumeSize" id="aws_tvolumesize" min="10" max="1000" placeholder="{{msg.template_form_volume_size_placeholder}}"
                ng-required="awsTemp.parameters.volumeType != 'Ephemeral'" ng-hide="awsTemp.parameters.volumeType == 'Ephemeral'">

            <input type="text" class="form-control" name="aws_ephemeral_volumesize" id="aws_ephemeral_volumesize" ng-disabled="true" ng-hide="awsTemp.parameters.volumeType != 'Ephemeral'" value="{{awsInstanceType.ephemeralVolumeSize}}">
            <div class="help-block"
                 ng-show="awsTemplateForm.aws_tvolumesize.$dirty && awsTemplateForm.aws_tvolumesize.$invalid"><i class="fa fa-warning"></i>
                {{msg.volume_size_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tspotprice">{{msg.template_aws_form_spot_price_label}}</label>

        <div class="col-sm-9">
            <input type="number" name="aws_tspotprice" class="form-control" id="aws_tspotprice" ng-model="awsTemp.parameters.spotPrice" min="0.04" max="100.0">
          <div class="help-block" ng-show="awsTemplateForm.aws_tspotprice.$dirty"><i class="fa fa-warning"></i>
              {{msg.template_aws_form_spot_price_help}}
          </div>
          <div class="help-block"
               ng-show="awsTemplateForm.aws_tspotprice.$dirty && awsTemplateForm.aws_tspotprice.$invalid"><i class="fa fa-warning"></i>
              {{msg.spot_price_invalid}}
          </div>
          <!-- .col-sm-9 -->
        </div>
    </div>

    <div class="form-group" ng-hide="awsTemp.parameters.volumeType == 'Ephemeral'">
            <label class="col-sm-3 control-label" for="aws_ebsencryption">{{msg.template_aws_form_ebs_label}}</label>
            <div class="col-sm-9">
                <input type="checkbox" name="aws_ebsencryption" id="aws_ebsencryption" ng-model="awsTemp.parameters.encrypted">
            </div>
       <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
            <label class="col-sm-3 control-label" for="aws_publicinaccount">{{msg.public_in_account_label}}</label>
            <div class="col-sm-9">
                <input type="checkbox" name="aws_publicinaccount" id="aws_publicinaccount" ng-model="awsTemp.public">
            </div>
       <!-- .col-sm-9 -->
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createAwsTemplate" ng-disabled="awsTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAwsTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.template_form_create}}</a>
        </div>
    </div>
