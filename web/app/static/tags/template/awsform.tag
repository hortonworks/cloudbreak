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
        <select class="form-control" id="aws_tinstanceType" name="aws_tinstanceType" ng-options="instanceType.value as instanceType.value for instanceType in $root.params.vmTypes.AWS" ng-model="awsTemp.instanceType" ng-change="changeInstanceType(awsTemp.instanceType, awsTemp.volumeType, 'AWS', awsTemp, true)" required>
        </select>
        <div class="help-block ng-binding" ng-show="awsTemp.CPUs && awsTemp.RAMs">{{msg.template_form_vm_info | format: awsTemp.CPUs:awsTemp.RAMs}}</div>
    </div>

    <!-- .col-sm-9 -->

</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="aws_tvolumetype">{{msg.template_form_volume_type_label}}</label>

    <div class="col-sm-9">
        <select class="form-control" id="aws_tvolumetype" name="aws_tvolumetype" ng-options="volumeType as $root.displayNames.getDisk('AWS', volumeType) for volumeType in $root.params.diskTypes.AWS | filter:filterByVolumetype" ng-model="awsTemp.volumeType" ng-change="changeInstanceType(awsTemp.instanceType, awsTemp.volumeType, 'AWS', awsTemp)" required>
        </select>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-class="{ 'has-error' : awsTemplateForm.aws_tvolumecount.$dirty && awsTemplateForm.aws_tvolumecount.$invalid }">
    <label class="col-sm-3 control-label" for="aws_tvolumecount">{{msg.template_form_volume_count_label}}</label>

    <div class="col-sm-9">
        <input type="number" name="aws_tvolumecount" class="form-control" ng-model="awsTemp.volumeCount" id="aws_tvolumecount" min="{{awsTemp.minDiskNumber}}" max="{{awsTemp.maxDiskNumber}}" placeholder="{{msg.template_form_volume_count_placeholder | format: awsTemp.minDiskNumber:(awsTemp.maxDiskNumber)}}" required>

        <div class="help-block" ng-show="awsTemplateForm.aws_tvolumecount.$dirty && awsTemplateForm.aws_tvolumecount.$invalid"><i class="fa fa-warning"></i> {{msg.volume_count_invalid | format: awsTemp.minDiskNumber:(awsTemp.maxDiskNumber)}}
        </div>
        <!-- .col-sm-9 -->
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tvolumesize.$dirty && awsTemplateForm.aws_tvolumesize.$invalid }">
    <label class="col-sm-3 control-label" for="aws_tvolumesize">{{msg.template_form_volume_size_label}}</label>

    <div class="col-sm-9">
        <input type="number" name="aws_tvolumesize" class="form-control" ng-model="awsTemp.volumeSize" id="aws_tvolumesize" min="{{awsTemp.minDiskSize}}" max="{{awsTemp.maxDiskSize}}" placeholder="{{msg.template_form_volume_size_placeholder | format: awsTemp.minDiskSize:(awsTemp.maxDiskSize)}}" ng-required="awsTemp.maxDiskSize !== awsTemp.minDiskSize" ng-hide="awsTemp.maxDiskSize == awsTemp.minDiskSize">

        <input type="text" class="form-control" name="aws_ephemeral_volumesize" id="aws_ephemeral_volumesize" ng-disabled="true" ng-hide="awsTemp.volumeType !== 'ephemeral'" value="{{awsTemp.maxDiskSize}}">
        <div class="help-block" ng-show="awsTemplateForm.aws_tvolumesize.$dirty && awsTemplateForm.aws_tvolumesize.$invalid"><i class="fa fa-warning"></i> {{msg.volume_size_invalid | format: awsTemp.minDiskSize:(awsTemp.maxDiskSize)}}
        </div>
        <!-- .col-sm-9 -->
    </div>
</div>

<div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tspotprice.$dirty && awsTemplateForm.aws_tspotprice.$invalid }">
    <label class="col-sm-3 control-label" for="aws_tspotprice">{{msg.template_aws_form_spot_price_label}}</label>

    <div class="col-sm-9">
        <input type="number" name="aws_tspotprice" class="form-control" id="aws_tspotprice" ng-model="awsTemp.parameters.spotPrice" min="0.04" max="100.0">
        <div class="help-block" ng-show="awsTemplateForm.aws_tspotprice.$dirty && !awsTemplateForm.aws_tspotprice.$invalid"><i class="fa"></i> {{msg.template_aws_form_spot_price_help}}
        </div>
        <div class="help-block" ng-show="awsTemplateForm.aws_tspotprice.$dirty && awsTemplateForm.aws_tspotprice.$invalid"><i class="fa fa-warning"></i> {{msg.spot_price_invalid}}
        </div>
        <!-- .col-sm-9 -->
    </div>
</div>

<div class="form-group" ng-hide="awsTemp.volumeType == 'ephemeral'">
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

<div class="form-group">
    <label class="col-sm-3 control-label" for="topologySelect">{{msg.credential_select_topology}}</label>
    <div class="col-sm-9">
        <select class="form-control" id="topologySelect" name="topologySelect" ng-model="awsTemp.topologyId" ng-options="topology.id as topology.name for topology in $root.topologies | filter: filterByCloudPlatform | orderBy:'name'">
            <option value="">-- {{msg.credential_select_topology.toLowerCase()}} --</option>
        </select>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createAwsTemplate" ng-disabled="awsTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAwsTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.template_form_create}}</a>
    </div>
</div>