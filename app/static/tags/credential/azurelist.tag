<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Name</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-show="credential.description">
        <label class="col-sm-3 control-label" for="azuredescriptionfield">Description</label>

        <div class="col-sm-9">
            <p id="azuredescriptionfield" class="form-control-static">{{credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="subscriptionId">Subscription ID</label>

        <div class="col-sm-9">
            <p id="subscriptionId" class="form-control-static">{{credential.parameters.subscriptionId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="jksPassword">Password</label>

        <div class="col-sm-9">
            <p id="jksPassword" class="form-control-static">{{credential.parameters.jksPassword}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="jksFile">Certification file</label>

        <div class="col-sm-9">
            <a id="jksFile" ng-click="getAzureCertification(credential.id)" download="file.cer" class="btn btn-success btn-block" role="button"><i
                    class="fa fa-file fa-fw"></i>
                Download certificate file</a>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>