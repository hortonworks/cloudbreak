<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="gcplclusterName" class="form-control-static">{{template.name}}</p>
        </div>
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="gcplclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="gcplclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplgcpInstanceType">{{msg.template_form_instance_type_label}}</label>

        <div class="col-sm-9">
            <p id="gcplgcpInstanceType" class="form-control-static" ng-repeat="item in $root.config.GCP.gcpInstanceTypes | filter:{key: template.parameters.gcpInstanceType}">{{item.value}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpvolumecount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <p id="gcpvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <p id="gcpvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplgcpVolumeType">{{msg.template_form_volume_type_label}}</label>

        <div class="col-sm-9">
            <p id="gcplgcpVolumeType" class="form-control-static" ng-repeat="item in $root.config.GCP.gcpDiskTypes | filter:{key: template.parameters.volumeType}">{{item.value}}</p>
        </div>
    </div>

</form>
