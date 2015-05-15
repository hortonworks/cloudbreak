<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclclusterName">Name</label>

        <div class="col-sm-9">
            <p id="gcclclusterName" class="form-control-static">{{template.name}}</p>
        </div>
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="gcclclusterDesc">Description</label>

        <div class="col-sm-9">
            <p id="gcclclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclgccInstanceType">Instance Type</label>

        <div class="col-sm-9">
            <p id="gcclgccInstanceType" class="form-control-static" ng-repeat="item in $root.config.GCC.gccInstanceTypes | filter:{key: template.parameters.gccInstanceType}">{{item.value}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gccvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <p id="gccvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gccvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <p id="gccvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclgccVolumeType">VolumeType</label>

        <div class="col-sm-9">
            <p id="gcclgccVolumeType" class="form-control-static" ng-repeat="item in $root.config.GCC.gccDiskTypes | filter:{key: template.parameters.volumeType}">{{item.value}}</p>
        </div>
    </div>

</form>