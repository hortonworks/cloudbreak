<!-- .... TEMPLATES PANEL ................................................. -->

<div id="panel-templates" ng-controller="templateController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="templates-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-templates-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.templates.length}}</span> {{msg.template_manage_title}}</h4>
        </div>

        <div id="panel-templates-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('templates', userDetails.groups)">
                    <a href="" id="panel-create-templates-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-templates-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.template_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-templates-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">
                                    <div class="btn-group btn-group-justified">
                                        <a id="awsTemplateChange" ng-if="isVisible('AWS')" type="button" ng-class="{'btn':true, 'btn-info':awsTemplate, 'btn-default':!awsTemplate}" role="button" ng-click="createAwsTemplateRequest()">{{msg.aws_label}}</a>
                                        <a id="azureTemplateChange" ng-if="isVisible('AZURE_RM')" ng-class="{'btn':true, 'btn-info':azureTemplate, 'btn-default':!azureTemplate}" role="button" ng-click="createAzureTemplateRequest()">{{msg.azure_label}}</a>
                                        <a id="mesosTemplateChange" ng-if="isVisible('BYOS')" ng-class="{'btn':true, 'btn-info':mesosTemplate, 'btn-default':!mesosTemplate}" role="button" ng-click="createMesosTemplateRequest()">{{msg.mesos_label}}</a>
                                    </div>
                                    <div class="btn-group btn-group-justified">
                                        <a id="gcpTemplateChange" ng-if="isVisible('GCP')" ng-class="{'btn':true, 'btn-info':gcpTemplate, 'btn-default':!gcpTemplate}" role="button" ng-click="createGcpTemplateRequest()">{{msg.gcp_label}}</a>
                                        <a id="openstackTemplateChange" ng-if="isVisible('OPENSTACK')" ng-class="{'btn':true, 'btn-info':openstackTemplate, 'btn-default':!openstackTemplate}" role="button" ng-click="createOpenstackTemplateRequest()">{{msg.openstack_label}}</a>
                                    </div>
                                </div>
                            </div>

                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>

                            <form class="form-horizontal" role="form" name="azureTemplateForm" ng-show="azureTemplate && isVisible('AZURE_RM')">
                                <div ng-include src="'tags/template/azureform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="awsTemplate && isVisible('AWS')" name="awsTemplateForm">
                                <div ng-include src="'tags/template/awsform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="gcpTemplate && isVisible('GCP')" name="gcpTemplateForm" ng-show="gcpTemplate">
                                <div ng-include src="'tags/template/gcpform.tag'"></div>
                            </form>
                            <form class="form-horizontal" role="form" ng-show="openstackTemplate && isVisible('OPENSTACK')" name="openstackTemplateForm" ng-show="openstackTemplate">
                                <div ng-include src="'tags/template/openstackform.tag'"></div>
                            </form>
                            <form class="form-horizontal" role="form" name="mesosTemplateForm" ng-show="mesosTemplate && isVisible('BYOS')">
                                <div ng-include src="'tags/template/mesosform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TEMPLATE LIST ........................................... -->

                <div class="panel-group" id="templete-list-accordion">

                    <!-- .............. TEMPLATE .............................................. -->

                    <div class="panel panel-default" ng-repeat="template in $root.templates | filter:filterByVisiblePlatform | orderBy:['cloudPlatform', 'name']">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#templete-list-accordion" data-target="#panel-template-collapse{{template.id}}"><i class="fa fa-file-o fa-fw"></i>{{template.name}}</a>
                                <span class="label label-info pull-right" >{{template.cloudPlatform}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="template.public"></i>
                            </h5>
                        </div>
                        <div id="panel-template-collapse{{template.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('templates', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteTemplate(template)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.template_list_delete}}</span>
                                </a>
                            </p>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'AZURE_RM' ">
                                <div ng-include src="'tags/template/azurelist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'GCP' ">
                                <div ng-include src="'tags/template/gcplist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'AWS' ">
                                <div ng-include src="'tags/template/awslist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'OPENSTACK' ">
                                <div ng-include src="'tags/template/openstacklist.tag'"></div>
                            </div>

                        </div>
                    </div>
                    <div class="panel panel-default" ng-repeat="constraint in $root.constraints | filter:filterByVisiblePlatform | orderBy:['name']">
                        <div class="panel-heading">
                            <h5>
                                    <a href="" data-toggle="collapse" data-parent="#constraint-list-accordion" data-target="#panel-constraint-collapse{{constraint.id}}"><i class="fa fa-file-o fa-fw"></i>{{constraint.name}}</a>
                                    <span class="label label-info pull-right" >MESOS</span>
                                    <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="constraint.public"></i>
                                </h5>
                        </div>
                        <div id="panel-constraint-collapse{{constraint.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteConstraint(constraint)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.template_list_delete}}</span>
                                </a>
                            </p>

                            <div class="panel-body">
                                <div ng-include src="'tags/template/mesoslist.tag'"></div>
                            </div>
                        </div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #template-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>