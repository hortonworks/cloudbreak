<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="awsclusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="awsclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="awsclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsinstanceType">{{msg.template_form_instance_type_label}}</label>

        <div class="col-sm-9">
            <p id="awsinstanceType" class="form-control-static">{{template.parameters.instanceType}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsvolumetype">{{msg.template_form_volume_type_label}}</label>

        <div class="col-sm-9">
            <p id="awsvolumetype" class="form-control-static" ng-repeat="item in $root.config.AWS.volumeTypes | filter:{key: template.parameters.volumeType}">{{item.value}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsvolumecount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <p id="awsvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-hide="template.parameters.volumeType == 'Ephemeral'">
        <label class="col-sm-3 control-label" for="awsvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <p id="awsvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsebsencrypt">{{msg.template_aws_form_ebs_label}}</label>

        <div class="col-sm-9">
            <i id="awsebsencrypt" ng-show="template.parameters.encrypted" class="form-control-static fa fa-check-circle fa-5" style="color: #4cb84c;"></i>
            <i id="awsebsencrypt" ng-show="!template.parameters.encrypted" class="form-control-static fa fa-times-circle fa-5" style="color: #f9332f;"></i>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="{{template.parameters.spotPrice}}">
        <label class="col-sm-3 control-label" for="awsspotprice">{{msg.template_aws_form_spot_price_label}}</label>

        <div class="col-sm-9">
            <p id="awsspotprice" class="form-control-static" >{{template.parameters.spotPrice}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>
