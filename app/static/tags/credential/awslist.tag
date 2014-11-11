<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Name</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group" ng-show="credential.description">
        <label class="col-sm-3 control-label" for="awsdescriptionfield">Description</label>

        <div class="col-sm-9">
            <p id="awsdescriptionfield" class="form-control-static">{{credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="roleArn">IAM Role ARN</label>

        <div class="col-sm-9">
            <p id="roleArn" class="form-control-static">{{credential.parameters.roleArn}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>