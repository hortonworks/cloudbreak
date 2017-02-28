<!-- .... CREDENTIALS PANEL ................................................ -->

<div id="panel-credentials" ng-controller="credentialController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="credentials-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-credentials-collapse">
                <i class="fa fa-angle-down fa-2x fa-fw-forced"></i>
            </a>
            <h4>
                <span class="badge pull-right">{{countList($root.credentials) + ($root.importedStacks | filter:filterByImportedStack).length}}</span> {{msg.credential_manage_title}}
            </h4>
        </div>

        <div id="panel-credentials-collapse" class="panel-collapse panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('credentials', userDetails.groups)">
                    <a href="" id="panel-create-credentials-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-credentials-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.credential_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-credentials-collapse" class="panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector1">
                                    <div class="btn-group btn-group-justified">
                                        <a id="awsChange" ng-if="isVisible('AWS')" type="button" ng-class="{'btn':true, 'btn-info':awsCredential, 'btn-default':!awsCredential}" ng-click="createAwsCredentialRequest()">{{msg.aws_label}}</a>
                                        <a id="azureChange" ng-if="isVisible('AZURE')" type="button" ng-class="{'btn':true,
    'btn-info':azureCredential, 'btn-default':!azureCredential}" ng-click="createAzureCredentialRequest()">{{msg.azure_label}}</a>
                                        <a id="mesosChange" ng-if="isVisible('BYOS')" type="button" ng-class="{'btn':true, 'btn-info':mesosCredential, 'btn-default':!mesosCredential}" ng-click="importMesosStackRequest()">{{msg.mesos_label}}</a>
                                        <a id="yarnChange" ng-if="isVisible('BYOS')" type="button" ng-class="{'btn':true, 'btn-info':yarnCredential, 'btn-default':!yarnCredential}" ng-click="importYarnCredentialRequest()">{{msg.yarn_label}}</a>
                                    </div>
                                    <div class="btn-group btn-group-justified" ng-if="isVisible('GCP') || isVisible('OPENSTACK')">
                                        <a id="gcpChange" ng-if="isVisible('GCP')" type="button" ng-class="{'btn':true, 'btn-info':gcpCredential, 'btn-default':!gcpCredential}" ng-click="createGcpCredentialRequest()">{{msg.gcp_label}}</a>
                                        <a id="openstackChange" ng-if="isVisible('OPENSTACK')" type="button" ng-class="{'btn':true, 'btn-info':openstackCredential, 'btn-default':!openstackCredential}" ng-click="createOpenstackCredentialRequest()">{{msg.openstack_label}}</a>
                                    </div>
                                </div>
                            </div>
                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>

                            <form class="form-horizontal" role="form" name="awsCredentialForm" ng-show="awsCredential && isVisible('AWS')" name="awsCredentialForm">
                                <div ng-include src="'tags/credential/awsform.tag'"></div>
                            </form>

                            <azure-credential ng-show="azureCredential && isVisible('AZURE')"></azure-credential>

                            <form class="form-horizontal" role="form" name="gcpCredentialForm" ng-show="gcpCredential && isVisible('GCP')">
                                <div ng-include src="'tags/credential/gcpform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" name="openstackCredentialForm" ng-show="openstackCredential && isVisible('OPENSTACK')">
                                <div ng-include src="'tags/credential/openstackform.tag'"></div>
                            </form>
                            <form class="form-horizontal" role="form" name="mesosImportStackForm" ng-show="mesosCredential && isVisible('BYOS')">
                                <div ng-include src="'tags/credential/mesosform.tag'"></div>
                            </form>
                            <form class="form-horizontal" role="form" name="yarnImportStackForm" ng-show="yarnCredential && isVisible('BYOS')">
                                <div ng-include src="'tags/credential/yarnform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ CREDENTIAL LIST ........................................... -->

                <div class="panel-group" id="credential-list-accordion">

                    <!-- ............. CREDENTIAL ............................................... -->

                    <div class="panel panel-default" ng-repeat="credential in $root.credentials  | filter:filterByVisiblePlatform | orderBy:['cloudPlatform', 'name']">
                        <div class="panel-heading">
                            <h5><a href="" data-toggle="collapse" data-parent="#credential-list-accordion"
                                   data-target="#panel-credential-collapse{{credential.id}}"><i class="fa fa-tag fa-fw"></i>{{credential.name}}</a>
                                <span class="label label-info pull-right" >{{credential.cloudPlatform == "BYOS" ? credential.parameters.type : credential.cloudPlatform}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="credential.public"></i>

                            </h5>
                        </div>
                        <div id="panel-credential-collapse{{credential.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('credentials', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteCredential(credential)">
                                    <i class="fa fa-times fa-fw"></i>
                                    <span> {{msg.credential_list_delete}}</span>
                                </a>
                            </p>

                            <div class="panel-body" ng-if="credential.cloudPlatform == 'AZURE' ">
                                <div ng-include src="'tags/credential/azurelist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="credential.cloudPlatform == 'GCP' ">
                                <div ng-include src="'tags/credential/gcplist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="credential.cloudPlatform == 'AWS' ">
                                <div ng-include src="'tags/credential/awslist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="credential.cloudPlatform == 'OPENSTACK' ">
                                <div ng-include src="'tags/credential/openstacklist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="credential.cloudPlatform == 'BYOS' && credential.parameters.type == 'YARN'">
                                <div ng-include src="'tags/credential/yarnlist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="credential.cloudPlatform == 'BYOS' && credential.parameters.type == 'MARATHON'">
                                <div ng-include src="'tags/credential/marathonlist.tag'"></div>
                            </div>

                        </div>
                    </div>
                    <!-- .panel -->


                </div>
                <!-- #credential-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->

</div>
<!-- .col- -->