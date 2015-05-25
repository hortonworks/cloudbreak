<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpclname">Name</label>

        <div class="col-sm-9">
            <p id="gcpclname" class="form-control-static">{{credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-show="credential.description">
        <label class="col-sm-3 control-label" for="gcpcldescriptionfield">Description</label>

        <div class="col-sm-9">
            <p id="gcpcldescriptionfield" class="form-control-static">{{credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpclsubscriptionId">Service Account Email Address</label>

        <div class="col-sm-9">
            <p id="gcpclsubscriptionId" class="form-control-static">{{credential.parameters.serviceAccountId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplprojectId">Project Id</label>

        <div class="col-sm-9">
            <p id="gcplprojectId" class="form-control-static">{{credential.parameters.projectId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>
