
    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tclusterName.$dirty && awsTemplateForm.aws_tclusterName.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tclusterName">Name</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_tclusterName" ng-model="aws_tclusterName" ng-minlength="5" ng-maxlength="20" required id="aws_tclusterName" placeholder="min. 5 max. 20 char">
            <div class="help-block" ng-show="awsTemplateForm.aws_tclusterName.$dirty && awsTemplateForm.aws_tclusterName.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tdescription.$dirty && awsTemplateForm.aws_tdescription.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tdescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="aws_tdescription" ng-model="aws_tdescription" ng-maxlength="20" id="aws_tdescription" placeholder="max. 20 char">
            <div class="help-block" ng-show="awsTemplateForm.aws_tdescription.$dirty && awsTemplateForm.aws_tdescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tsshLocation.$dirty && awsTemplateForm.aws_tsshLocation.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tsshLocation">SSH location</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="aws_tsshLocation" name="aws_tsshLocation" ng-model="aws_tsshLocation" placeholder="0.0.0.0/0" required ng-pattern="/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/i">
            <div class="help-block" ng-show="awsTemplateForm.aws_tsshLocation.$dirty && awsTemplateForm.aws_tsshLocation.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_ssh_location_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tregion">Region</label>

        <div class="col-sm-9">
            <select class="form-control" id="aws_tregion">
                <option value="US_EAST_1">us-east-1</option>
                <option value="US_WEST_1">us-west-1</option>
                <option value="US_WEST_2">us-west-2</option>
                <option value="EU_WEST_1">eu-west-1</option>
                <option value="AP_SOUTHEAST_1">ap-southeast-1</option>
                <option value="AP_SOUTHEAST_2">ap-southeast-2</option>
                <option value="AP_NORTHEAST_1">ap-northeast-1</option>
                <option value="SA_EAST_1">sa-east-1</option>
            </select>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tinstanceType">Instance type</label>

        <div class="col-sm-9">
            <select class="form-control" id="aws_tinstanceType">
                <option value="T2Micro">t2.micro</option>
                <option value="T2Small">t2.small</option>
                <option value="T2Medium">t2.medium</option>
                <option value="M3Medium">m3.medium</option>
                <option value="M3Large">m3.large</option>
                <option value="M3Xlarge">m3.xlarge</option>
                <option value="M32xlarge">m3.2xlarge</option>
            </select>
        </div>
        <!-- .col-sm-9 -->

    </div>


    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a href="#" id="createAwsTemplate" ng-disabled="awsTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAwsTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
                create template</a>
        </div>
    </div>
