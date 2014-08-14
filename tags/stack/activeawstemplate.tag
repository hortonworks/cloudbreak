<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsclusterName">Name</label>

        <div class="col-sm-9">
            <p id="awsclusterName" class="form-control-static">{{activeStack.template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsclusterDescription">Description</label>

        <div class="col-sm-9">
            <p id="awsclusterDescription" class="form-control-static">{{activeStack.template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsamiId">AMI</label>

        <div class="col-sm-9">
            <p id="awsamiId" class="form-control-static">{{activeStack.template.parameters.amiId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awssshLocation">SSH location</label>

        <div class="col-sm-9">
            <p id="awssshLocation" class="form-control-static">{{activeStack.template.parameters.sshLocation}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsregion">Region</label>

        <div class="col-sm-9">
            <p id="awsregion" class="form-control-static">{{getRegionName(activeStack.template.parameters.region)}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsinstanceType">Instance type</label>

        <div class="col-sm-9">
            <p id="awsinstanceType" class="form-control-static">{{activeStack.template.parameters.instanceType}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="awslvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <p id="awslvolumecount" class="form-control-static">{{activeStack.template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awslvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <p id="awslvolumesize" class="form-control-static">{{activeStack.template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awslvolumetype">Volume type</label>

        <div class="col-sm-9">
            <p id="awslvolumetype" class="form-control-static">{{activeStack.template.parameters.volumeType}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="{{template.parameters.spotPrice}}">
        <label class="col-sm-3 control-label" for="awslspotprice">Spot Price</label>

        <div class="col-sm-9">
            <p id="awslspotprice" class="form-control-static">{{activeStack.template.parameters.spotPrice}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>