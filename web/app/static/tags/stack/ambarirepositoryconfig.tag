<div class="form-group" name="ambari_stack1">
    <label class="col-sm-3 control-label" for="ambari_stack">{{msg.cluster_form_ambari_repo_stack_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_stack" class="form-control" ng-model="cluster.ambariStackDetails.stack" id="ambari_stack" placeholder="{{msg.cluster_form_ambari_repo_stack_placeholder}}">

    </div>
</div>
<div class="form-group" name="ambari_version1">
    <label class="col-sm-3 control-label" for="ambari_version">{{msg.cluster_form_ambari_repo_version_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_version" class="form-control" ng-model="cluster.ambariStackDetails.version" id="ambari_version" placeholder="{{msg.cluster_form_ambari_repo_version_placeholder}}">

    </div>
</div>
<div class="form-group" name="ambari_os1">
    <label class="col-sm-3 control-label" for="ambari_os">{{msg.cluster_form_ambari_repo_os_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="ambari_os" name="ambari_os" ng-model="cluster.ambariStackDetails.os">
            <option style="display:none" value="">select a type</option>
            <option value="redhat7">redhat7</option>
            <option value="redhat6">redhat6</option>
        </select>
    </div>
</div>
<div class="form-group" name="ambari_stackRepoId1" ng-class="{ 'has-error': clusterCreationForm.ambari_stackRepoId.$dirty && clusterCreationForm.ambari_stackRepoId.$invalid }">
    <label class="col-sm-3 control-label" for="ambari_stackRepoId">{{msg.cluster_form_ambari_repo_stack_repoid_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_stackRepoId" class="form-control" ng-model="cluster.ambariStackDetails.stackRepoId" id="ambari_stackRepoId" placeholder="{{msg.cluster_form_ambari_repo_stack_repoid_placeholder}}" ng-pattern="/^(HDP-[\d\W]*)$/">
        <div class="help-block" ng-show="clusterCreationForm.ambari_stackRepoId.$dirty && clusterCreationForm.ambari_stackRepoId.$invalid"><i class="fa fa-warning"></i> Should be a valid Repo Id like HDP-2.5
        </div>
    </div>
</div>
<div class="form-group" name="ambari_stackBaseURL1">
    <label class="col-sm-3 control-label" for="ambari_stackBaseURL">{{msg.cluster_form_ambari_repo_baseurl_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_stackBaseURL" class="form-control" ng-model="cluster.ambariStackDetails.stackBaseURL" id="ambari_stackBaseURL" placeholder="{{msg.cluster_form_ambari_repo_baseurl_placeholder}}">

    </div>
</div>
<div class="form-group" name="ambari_utilsRepoId1">
    <label class="col-sm-3 control-label" for="ambari_utilsRepoId">{{msg.cluster_form_ambari_repo_utils_repoid_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_utilsRepoId" class="form-control" ng-model="cluster.ambariStackDetails.utilsRepoId" id="ambari_utilsRepoId" placeholder="{{msg.cluster_form_ambari_repo_utils_repoid_placeholder}}">

    </div>
</div>
<div class="form-group" name="ambari_utilsBaseURL1">
    <label class="col-sm-3 control-label" for="ambari_utilsBaseURL">{{msg.cluster_form_ambari_repo_utils_baseurl_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_utilsBaseURL" class="form-control" ng-model="cluster.ambariStackDetails.utilsBaseURL" id="ambari_utilsBaseURL" placeholder="{{msg.cluster_form_ambari_repo_utils_baseurl_placeholder}}">
    </div>
</div>
<div class="form-group" name="cluster_verify1">
    <label class="col-sm-3 control-label" for="cluster_verify">{{msg.cluster_form_ambari_repo_verify_label}}</label>
    <div class="col-sm-8">
        <input type="checkbox" name="cluster_verify" id="cluster_verify" ng-click="toggleAmbariStackDetailsVerify()">
    </div>
</div>
<div class="form-group">
    <div class="col-sm-11">

        <div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-default" ng-click="activeStack === undefined ? showWizardActualElement('configureFailureAction') : showWizardActualElement('configureHostGroups')">
                    <i class="fa fa-angle-double-left"></i> {{activeStack === undefined ? msg.cluster_form_ambari_failure_tag : msg.cluster_form_ambari_blueprint_tag}}
                </button>
            </div>
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group" ng-hide="clusterCreationForm.$invalid">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureAmbariDatabase')">{{msg.cluster_form_ambari_database_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
        </div>
    </div>
</div>