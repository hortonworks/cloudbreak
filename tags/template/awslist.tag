<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsclusterName">Name</label>

        <div class="col-sm-9">
            <p id="awsclusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
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
            <p id="awsregion" class="form-control-static">{{template.parameters.region}}</p>
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
</form>