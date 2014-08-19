<form class="form-horizontal" role="form" name="clusterCreationForm">

    <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
        <label class="col-sm-3 control-label" for="cl_clusterName">Cluster name</label>

        <div class="col-sm-9">
            <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" name="cl_clusterName" placeholder="min. 5 max. 20 char" ng-model="cl_clusterName"
                   ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" ng-minlength="5" ng-maxlength="20" required>

            <div class="help-block"
                 ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{error_msg.cluster_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterSize.$dirty && clusterCreationForm.cl_clusterSize.$invalid }">
        <label class="col-sm-3 control-label" for="cl_clusterSize">Cluster size</label>

        <div class="col-sm-9">
            <input type="number" name="cl_clusterSize" class="form-control" ng-model="cl_clusterSize" id="cl_clusterSize" min="1"
                   max="99" required>

            <div class="help-block"
                 ng-show="clusterCreationForm.cl_clusterSize.$dirty && clusterCreationForm.cl_clusterSize.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.cluster_size_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="selectTemplate">Template</label>

        <div class="col-sm-9">
            <select class="form-control" id="selectTemplate" ng-model="selectTemplate" required >
                <option data-value="{{template.id}}" value="{{template.id}}" id="{{template.id}}" ng-if="template.cloudPlatform == activeCredential.cloudPlatform"  ng-repeat="template in templates" >{{template.name}}
                </option>
            </select>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="selectBlueprint">Blueprint</label>

        <div class="col-sm-9">
            <select class="form-control" id="selectBlueprint" ng-model="selectBlueprint"  required >
                <option ng-repeat="blueprint in blueprints" data-value="{{blueprint.id}}" value="{{blueprint.id}}" id="{{blueprint.id}}">{{blueprint.name}}
                </option>
            </select>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->

    <div class="form-group" >
        <label class="col-sm-3 control-label" for="emailneeded">Email notification when cluster is provisioned</label>

        <div class="col-sm-9">
            <input type="checkbox" class="" id="emailneeded" ng-model="emailneeded" name="emailneeded">

        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a href="#" id="createCluster" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" role="button" ng-click="createStack()"><i
                    class="fa fa-plus fa-fw"></i>create and start
                cluster</a>
        </div>
    </div>

</form>