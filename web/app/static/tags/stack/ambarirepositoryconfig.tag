<div class="form-group" name="ambari_repo_version_fg">
    <label class="col-sm-3 control-label" for="ambari_repo_version">{{msg.cluster_form_ambari_version_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_repo_version" class="form-control" ng-model="cluster.ambariRepoDetailsJson.version" id="ambari_repo_version" placeholder="{{msg.cluster_form_ambari_version_placeholder}}">

    </div>
</div>

<div class="form-group" name="ambari_repo_fg">
    <label class="col-sm-3 control-label" for="ambari_repo_baseurl">{{msg.cluster_form_ambari_repo_baseurl_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_repo_baseurl" class="form-control" ng-model="cluster.ambariRepoDetailsJson.baseUrl" id="ambari_repo_baseurl" placeholder="{{msg.cluster_form_ambari_repo_baseurl_placeholder}}">

    </div>
</div>

<div class="form-group" name="ambari_repo_gpgkeyurl_fg">
    <label class="col-sm-3 control-label" for="ambari_repo_gpgkeyurl">{{msg.cluster_form_ambari_repo_gpgkeyurl_label}}</label>
    <div class="col-sm-8">
        <input type="string" name="ambari_repo_gpgkeyurl" class="form-control" ng-model="cluster.ambariRepoDetailsJson.gpgKeyUrl" id="ambari_repo_gpgkeyurl" placeholder="{{msg.cluster_form_ambari_repo_gpgkeyurl_placeholder}}">

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
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureHdpRepository')">{{msg.cluster_form_ambari_hdprepo_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
        </div>
    </div>
</div>