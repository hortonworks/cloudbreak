
    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tclusterName.$dirty && awsTemplateForm.aws_tclusterName.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tclusterName">Name</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="aws_tclusterName" ng-model="awsTemp.name" ng-minlength="5" ng-maxlength="20" required id="aws_tclusterName" placeholder="min. 5 max. 20 char">
            <div class="help-block" ng-show="awsTemplateForm.aws_tclusterName.$dirty && awsTemplateForm.aws_tclusterName.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tdescription.$dirty && awsTemplateForm.aws_tdescription.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tdescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="aws_tdescription" ng-model="awsTemp.description" ng-maxlength="20" id="aws_tdescription" placeholder="max. 20 char">
            <div class="help-block" ng-show="awsTemplateForm.aws_tdescription.$dirty && awsTemplateForm.aws_tdescription.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tsshLocation.$dirty && awsTemplateForm.aws_tsshLocation.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tsshLocation">SSH location</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="aws_tsshLocation" name="aws_tsshLocation" ng-model="awsTemp.parameters.sshLocation" placeholder="0.0.0.0/0" required ng-pattern="/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/i">
            <div class="help-block" ng-show="awsTemplateForm.aws_tsshLocation.$dirty && awsTemplateForm.aws_tsshLocation.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.template_ssh_location_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tregion">Region</label>

        <div class="col-sm-9">
            <select class="form-control" id="aws_tregion" ng-model="awsTemp.parameters.region" required>
                <option value="US_EAST_1">US East(N. Virginia)</option>
                <option value="US_WEST_1">US West (Oregon)</option>
                <option value="US_WEST_2">US West (N. California)</option>
                <option value="EU_WEST_1">EU (Ireland)</option>
                <option value="AP_SOUTHEAST_1">Asia Pacific (Singapore)</option>
                <option value="AP_SOUTHEAST_2">Asia Pacific (Sydney)</option>
                <option value="AP_NORTHEAST_1">Asia Pacific (Tokyo)</option>
                <option value="SA_EAST_1">South America (São Paulo)</option>
            </select>
        </div>
        <!-- .col-sm-9 -->

    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tinstanceType">Instance type</label>

        <div class="col-sm-9">
            <select class="form-control" id="aws_tinstanceType" ng-model="awsTemp.parameters.instanceType" required>
                <option value="T2Micro">T2Micro</option>
                <option value="T2Small">T2Small</option>
                <option value="T2Medium">T2Medium</option>
                <option value="M3Medium">M3Medium</option>
                <option value="M3Large">M3Large</option>
                <option value="M3Xlarge">M3Xlarge</option>
                <option value="M32xlarge">M32xlarge</option>
            </select>
        </div>
        <!-- .col-sm-9 -->

    </div>

    <div class="form-group" ng-class="{ 'has-error' : awsTemplateForm.aws_tvolumecount.$dirty && awsTemplateForm.aws_tvolumecount.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <input type="number" name="aws_tvolumecount" class="form-control" ng-model="awsTemp.volumeCount" id="aws_tvolumecount" min="1" max="10"
                   required>

            <div class="help-block"
                 ng-show="awsTemplateForm.aws_tvolumecount.$dirty && awsTemplateForm.aws_tvolumecount.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.volume_count_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': awsTemplateForm.aws_tvolumesize.$dirty && awsTemplateForm.aws_tvolumesize.$invalid }">
        <label class="col-sm-3 control-label" for="aws_tvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <input type="number" name="aws_tvolumesize" class="form-control" ng-model="awsTemp.volumeSize" id="aws_tvolumesize" min="10"
                   max="1024" placeholder="0 GB" required>

            <div class="help-block"
                 ng-show="awsTemplateForm.aws_tvolumesize.$dirty && awsTemplateForm.aws_tvolumesize.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.volume_size_invalid}}
            </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tvolumetype">Volume type</label>

        <div class="col-sm-9">
            <select class="form-control" id="aws_tvolumetype" ng-model="awsTemp.parameters.volumeType" required>
                <option value="Gp2">SSD</option>
                <option value="Standard">Magnetic</option>
            </select>
        </div>
        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="aws_tspotprice">Spot price (USD)</label>

        <div class="col-sm-9">
            <input type="number" name="aws_tspotprice" class="form-control" id="aws_tspotprice" ng-model="awsTemp.parameters.spotPrice" min="0.1" max="100.0">
          <div class="help-block" ng-show="awsTemplateForm.aws_tspotprice.$dirty"><i class="fa fa-warning"></i>
              If you enter a spot price it cannot be guaranteed when your instances will be able to start.
          </div>
          <div class="help-block"
               ng-show="awsTemplateForm.aws_tspotprice.$dirty && awsTemplateForm.aws_tspotprice.$invalid"><i class="fa fa-warning"></i>
              {{error_msg.spot_price_invalid}}
          </div>
        </div>

        <!-- .col-sm-9 -->
      </div>
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a href="" id="createAwsTemplate" ng-disabled="awsTemplateForm.$invalid" class="btn btn-success btn-block" ng-click="createAwsTemplate()" role="button"><i class="fa fa-plus fa-fw"></i>
                create template</a>
        </div>
    </div>
