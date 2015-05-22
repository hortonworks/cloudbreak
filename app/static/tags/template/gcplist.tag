<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplclusterName">Name</label>

        <div class="col-sm-9">
            <p id="gcplclusterName" class="form-control-static">{{template.name}}</p>
        </div>
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="gcplclusterDesc">Description</label>

        <div class="col-sm-9">
            <p id="gcplclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplgcpInstanceType">Instance Type</label>

        <div class="col-sm-9">
            <p id="gcplgcpInstanceType" class="form-control-static" ng-repeat="item in $root.config.GCP.gcpInstanceTypes | filter:{key: template.parameters.gcpInstanceType}">{{item.value}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <p id="gcpvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <p id="gcpvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplgcpVolumeType">VolumeType</label>

        <div class="col-sm-9">
            <p id="gcplgcpVolumeType" class="form-control-static" ng-repeat="item in $root.config.GCP.gcpDiskTypes | filter:{key: template.parameters.volumeType}">{{item.value}}</p>
        </div>
    </div>

</form>