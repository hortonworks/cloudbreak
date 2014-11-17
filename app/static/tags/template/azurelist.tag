<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="clusterName">Name</label>

        <div class="col-sm-9">
            <p id="clusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="clusterDesc">Description</label>

        <div class="col-sm-9">
            <p id="clusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.parameters.password.length">
        <label class="col-sm-3 control-label" for="password">Password</label>

        <div class="col-sm-9">
            <p id="password" class="form-control-static">{{template.parameters.password}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="username">Username</label>

        <div class="col-sm-9">
            <p id="username" class="form-control-static">ubuntu</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="vmType">VM type</label>

        <div class="col-sm-9">
            <p id="vmType" class="form-control-static" ng-repeat="item in $root.config.AZURE.azureVmTypes | filter:{key: template.parameters.vmType}">{{item.value}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="imageName">Image name</label>

        <div class="col-sm-9">
            <p id="imageName" class="form-control-static">{{template.parameters.imageName}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="location">Location</label>

        <div class="col-sm-9">
            <p id="location" class="form-control-static" ng-repeat="item in $root.config.AZURE.azureRegions | filter:{key: template.parameters.location}">{{item.value}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="location">Attached volumes per instance</label>

        <div class="col-sm-9">
            <p id="volcount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="location">Volume size (GB)</label>

        <div class="col-sm-9">
            <p id="volsize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>
