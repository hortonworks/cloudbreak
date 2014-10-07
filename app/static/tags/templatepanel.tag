<!-- .... TEMPLATES PANEL ................................................. -->

<div id="panel-templates" ng-controller="templateController" class="col-md-12 col-lg-9">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="templates-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"
               data-target="#panel-templates-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.templates.length}}</span> manage templates</h4>
        </div>

        <div id="panel-templates-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel">
                    <a href="" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-templates-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> create template</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-templates-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">
                                    <div class="btn-group btn-group-justified">
                                        <a id="awsTemplateChange" type="button" class="btn btn-info" role="button" ng-click="createAwsTemplateRequest()">Aws</a>
                                        <a id="azureTemplateChange" class="btn btn-default" role="button" ng-click="createAzureTemplateRequest()">Azure</a>
                                        <a id="gccTemplateChange" class="btn btn-default" role="button" ng-click="createGccTemplateRequest()">Gcc</a>
                                    </div>
                                </div>
                            </div>

                            <form class="form-horizontal" role="form" name="azureTemplateForm" ng-show="azureTemplate">
                                <div ng-include src="'tags/template/azureform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="awsTemplate" name="awsTemplateForm">
                                <div ng-include src="'tags/template/awsform.tag'"></div>
                            </form>

                            <form class="form-horizontal" role="form" ng-show="gccTemplate" name="gccTemplateForm" ng-show="gccTemplate">
                                <div ng-include src="'tags/template/gccform.tag'"></div>
                            </form>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TEMPLATE LIST ........................................... -->

                <div class="panel-group" id="templete-list-accordion">

                    <!-- .............. TEMPLATE .............................................. -->

                    <div class="panel panel-default" ng-repeat="template in $root.templates">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#templete-list-accordion" data-target="#panel-template-collapse{{template.id}}"><i class="fa fa-file-o fa-fw"></i>{{template.name}}</a>
                            </h5>
                        </div>
                        <div id="panel-template-collapse{{template.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteTemplate(template)">
                                    <i class="fa fa-times fa-fw"></i><span> delete</span>
                                </a>
                            </p>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'AZURE' ">
                                <div ng-include src="'tags/template/azurelist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'GCC' ">
                                <div ng-include src="'tags/template/gcclist.tag'"></div>
                            </div>

                            <div class="panel-body" ng-if="template.cloudPlatform == 'AWS' ">
                                <div ng-include src="'tags/template/awslist.tag'"></div>
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
