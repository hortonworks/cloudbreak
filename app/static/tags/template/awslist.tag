<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsclusterName">Name</label>

        <div class="col-sm-9">
            <p id="awsclusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="awsclusterDesc">Description</label>

        <div class="col-sm-9">
            <p id="awsclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsamiId">AMI</label>

        <div class="col-sm-9">
            <p id="awsamiId" class="form-control-static">{{template.parameters.amiId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awssshLocation">SSH location</label>

        <div class="col-sm-9">
            <p id="awssshLocation" class="form-control-static">{{template.parameters.sshLocation}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsregion">Region</label>

        <div class="col-sm-9">
            <p id="awsregion" class="form-control-static" ng-repeat="item in $root.config.AWS.awsRegions | filter:{key: template.parameters.region}">{{item.value}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsinstanceType">Instance type</label>

        <div class="col-sm-9">
            <p id="awsinstanceType" class="form-control-static">{{template.parameters.instanceType}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <p id="awsvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <p id="awsvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsvolumetype">Volume type</label>

        <div class="col-sm-9">
            <p id="awsvolumetype" class="form-control-static" ng-repeat="item in $root.config.AWS.volumeTypes | filter:{key: template.parameters.volumeType}">{{item.value}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="{{template.parameters.spotPrice}}">
        <label class="col-sm-3 control-label" for="awsspotprice">Spot Price</label>

        <div class="col-sm-9">
            <p id="awsspotprice" class="form-control-static">{{template.parameters.spotPrice}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>
