<div id="cluster-form-panel" class="col-sm-11 col-md-11 col-lg-11">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="create-cluster-back-btn" class="btn btn-info btn-fa-2x" role="button"><i class="fa fa-angle-left fa-2x fa-fw-forced"></i></a>
            <h4>{{msg.cluster_form_title}}</h4>
            <a ng-show="!showAdvancedOptionForm" class="btn btn-success pull-right" role="button" style="right: -1px !important;left: 64%;bottom: -2px;" id="advanced-pick" ng-click="showAdvancedOption()">
                <i></i>{{msg.cluster_form_show_advanced}}
            </a>
            <a ng-show="showAdvancedOptionForm" class="btn btn-success pull-right" role="button" style="right: -1px !important;left: 64%;bottom: -2px;" id="advanced-pick" ng-click="showAdvancedOption()">
                <i></i>{{msg.cluster_form_hide_advanced}}
            </a>
        </div>
        <div id="create-cluster-panel-collapse" class="panel panel-default" style="margin-bottom: 0px;">
            <div class="btn-segmented-control" id="providerSelector2">

                <div class="panel-body">
                    <div class="btn-group btn-group-justified" role="group" style="padding-top: 20px" aria-label="...">
                        <a type="button" ng-class="{'btn-info': configureCluster, 'btn-default': !configureCluster}" class="btn btn-info ng-binding wizard-button" role="presentation" ng-click="showWizardActualElement('configureCluster')">Configure Cluster</a>
                        <a type="button" ng-class="{'btn-info': configureSecurity, 'btn-default': !configureSecurity}" ng-disabled="!cluster.name || !cluster.region" class="btn ng-binding wizard-button" role="presentation" ng-click="showWizardActualElement('configureSecurity')">Configure Network and Security</a>
                        <a type="button" ng-class="{'btn-info': configureHostGroups, 'btn-default': !configureHostGroups}" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId" class="btn ng-binding wizard-button" role="presentation" ng-click="showWizardActualElement('configureHostGroups')">Configure Host Groups</a>
                        <a type="button" ng-class="{'btn-info': configureFileSystem, 'btn-default': !configureFileSystem}" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || !cluster.blueprintId" class="btn ng-binding wizard-button" role="presentation" ng-click="showWizardActualElement('configureFileSystem')" ng-show="activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM' || activeCredential.cloudPlatform == 'GCP'">Configure File System</a>
                        <a type="button" ng-class="{'btn-info': configureFailureAction, 'btn-default': !configureFailureAction}" class="btn ng-binding wizard-button" role="presentation" ng-click="showWizardActualElement('configureFailureAction')" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || !cluster.blueprintId" ng-show="showAdvancedOptionForm">Configure Failure Action</a>
                        <a type="button" ng-class="{'btn-info': configureAmbariRepository, 'btn-default': !configureAmbariRepository}" class="btn ng-binding wizard-button" role="presentation" ng-click="showWizardActualElement('configureAmbariRepository')" ng-disabled="!cluster.name || !cluster.region || !cluster.securityGroupId || !cluster.networkId || !cluster.blueprintId" ng-show="showAdvancedOptionForm">Configure Ambari Repository</a>
                        <a type="button" ng-class="{'btn-info': configureReview, 'btn-default': !configureReview}" class="btn ng-binding" role="presentation wizard-button" ng-click="showWizardActualElement('configureReview')" ng-hide="clusterCreationForm.$invalid">Review and Launch</a>
                    </div>
                </div>

                <form class="form-horizontal container" role="form" name="$parent.clusterCreationForm" style="padding-top: 20px;    padding-bottom: 20px;">

                    <div id="configure_cluster" ng-show="configureCluster" ng-include src="'tags/stack/configurecluster.tag'">
                    </div>
                    <div id="configure_security_group" ng-show="configureSecurity" ng-include src="'tags/stack/configuresecuritygroup.tag'">
                    </div>
                    <div id="ambari_repository_config" ng-show="showAdvancedOptionForm && configureAmbariRepository" ng-include src="'tags/stack/ambarirepositoryconfig.tag'">
                    </div>
                    <div id="configure_failure_action" ng-show="configureFailureAction" ng-include src="'tags/stack/configurefailureaction.tag'">
                    </div>
                    <div id="configure_filesystem" ng-show="configureFileSystem && (activeCredential.cloudPlatform == 'AZURE' || activeCredential.cloudPlatform == 'AZURE_RM' || activeCredential.cloudPlatform == 'GCP')" ng-include src="'tags/stack/configurefilesystem.tag'">
                    </div>
                    <div id="configure_host_groups" ng-show="configureHostGroups" ng-include src="'tags/stack/configurehostgroups.tag'">
                    </div>
                    <div id="configure_review" ng-show="configureReview" ng-include src="'tags/stack/configurereview.tag'">
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
$(document).ready(function() { $('#rootwizard').bootstrapWizard(); });