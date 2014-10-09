<div id="cluster-form-panel" ng-controller="clusterController" class="col-sm-11 col-md-9 col-lg-9">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="create-cluster-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>Create cluster</h4>
        </div>
        <div id="create-cluster-panel-collapse" class="panel panel-default">
            <div class="panel-body">
                <form class="form-horizontal" role="form" name="clusterCreationForm">
                    <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterName">Cluster name</label>
                        <div class="col-sm-9">
                            <input type="text" name="cl_clusterName" class="form-control" id="cl_clusterName" name="cl_clusterName" placeholder="min. 5 max. 20 char" ng-model="cluster.name"  ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" ng-minlength="5" ng-maxlength="20" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterName.$dirty && clusterCreationForm.cl_clusterName.$invalid"><i class="fa fa-warning"></i> {{error_msg.cluster_name_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': clusterCreationForm.cl_clusterSize.$dirty && clusterCreationForm.cl_clusterSize.$invalid }">
                        <label class="col-sm-3 control-label" for="cl_clusterSize">Cluster size</label>
                        <div class="col-sm-9">
                            <input type="number" name="cl_clusterSize" class="form-control" ng-model="cluster.nodeCount" id="cl_clusterSize" min="1"   max="99" placeholder="1 - 99" required>
                            <div class="help-block"
                                 ng-show="clusterCreationForm.cl_clusterSize.$dirty && clusterCreationForm.cl_clusterSize.$invalid"><i class="fa fa-warning"></i>
                                {{error_msg.cluster_size_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="selectTemplate">Template</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectTemplate" ng-model="cluster.templateId" required >
                                <option ng-repeat="template in $root.templates | orderBy:'name'" data-value="{{template.id}}" value="{{template.id}}" id="{{template.id}}" ng-if="template.cloudPlatform == activeCredential.cloudPlatform">{{template.name}}
                                </option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-3 control-label" for="selectBlueprint">Blueprint</label>
                        <div class="col-sm-9">
                            <select class="form-control" id="selectBlueprint" ng-model="cluster.blueprintId"  required >
                                <option ng-repeat="blueprint in $root.blueprints | orderBy:'name'" data-value="{{blueprint.id}}" value="{{blueprint.id}}" id="{{blueprint.id}}">{{blueprint.name}}
                                </option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group" >
                        <label class="col-sm-3 control-label" for="emailneeded">Email notification when cluster is provisioned</label>
                        <div class="col-sm-9">
                            <input type="checkbox" id="emailneeded" ng-model="cluster.email" name="emailneeded">
                        </div>
                    </div>
                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a href="" id="createCluster" class="btn btn-success btn-block" ng-disabled="clusterCreationForm.$invalid" role="button" ng-click="createCluster()"><i class="fa fa-plus fa-fw"></i>create and start cluster</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
