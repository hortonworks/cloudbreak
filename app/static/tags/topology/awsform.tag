<div class="form-group" ng-class="{ 'has-error': awsTopologyForm.aws_tclusterName.$dirty && awsTopologyForm.aws_tclusterName.$invalid }">
    <label class="col-sm-3 control-label" for="aws_tclusterName">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_tclusterName" ng-model="topologyTemp.name" ng-minlength="5" ng-maxlength="100" required id="aws_tclusterName" placeholder="{{msg.name_placeholder}}">
        <div class="help-block" ng-show="awsTopologyForm.aws_tclusterName.$dirty && awsTopologyForm.aws_tclusterName.$invalid">
            <i class="fa fa-warning"></i> {{msg.topology_name_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->

</div>

<div class="form-group" ng-class="{ 'has-error': awsTopologyForm.aws_tdescription.$dirty && awsTopologyForm.aws_tdescription.$invalid }">
    <label class="col-sm-3 control-label" for="aws_tdescription">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <input type="text" class="form-control" name="aws_tdescription" ng-model="topologyTemp.description" ng-maxlength="1000" id="aws_tdescription" placeholder="{{msg.topology_form_description_placeholder}}">
        <div class="help-block" ng-show="awsTopologyForm.aws_tdescription.$dirty && awsTopologyForm.aws_tdescription.$invalid">
            <i class="fa fa-warning"></i> {{msg.topology_description_invalid}}
        </div>
    </div>
    <!-- .col-sm-9 -->
</div>

<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="createAwsTopology" ng-disabled="awsTopologyForm.aws_tclusterName.$invalid" class="btn btn-success btn-block" ng-click="createTopology('AWS')" role="button"><i class="fa fa-plus fa-fw"></i>
                {{modify ? msg.topology_form_modify : msg.topology_form_create}}</a>
    </div>
</div>
<div class="row btn-row">
    <div class="col-sm-9 col-sm-offset-3">
        <a id="cancelAwsTopology" ng-show="modify" class="btn btn-warning btn-block" ng-click="cancelModify()" role="button" data-toggle="collapse" data-target="#panel-create-topologies-collapse"><i class="fa fa-plus fa-fw"></i>
                {{msg.topology_form_cancel}}</a>
    </div>
</div>