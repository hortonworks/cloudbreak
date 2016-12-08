<!-- .... SECURITY GROUP PANEL ................................................. -->

<div id="panel-securitygroup" ng-controller="securityGroupController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="securitygroup-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-securitygroup-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.securitygroups.length}}</span> {{msg.securitygroup_manage_title}}</h4>
        </div>

        <div id="panel-securitygroup-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <!-- ............ CREATE FORM ............................................. -->

                <p class="btn-row-over-panel" ng-if="isWriteScope('securitygroups', userDetails.groups)">
                    <a href="" id="panel-create-securitygroup-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-securitygroup-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.securitygroup_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-securitygroup-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                         <div class="row " style="padding-bottom: 10px">
                                <div class="btn-segmented-control" id="providerSelector2">
                                    <div class="btn-group btn-group-justified">
                                        <a id="awsSecurityGroupChange" ng-if="isVisible('AWS')" type="button" ng-class="{'btn':true, 'btn-info':awsSecurityGroup, 'btn-default':!awsSecurityGroup}" role="button" ng-click="createAwsSecurityGroupRequest()">{{msg.aws_label}}</a>
                                        <a id="azureSecurityGroupChange" ng-if="isVisible('AZURE_RM')" ng-class="{'btn':true, 'btn-info':azureSecurityGroup, 'btn-default':!azureSecurityGroup}" role="button" ng-click="createAzureSecurityGroupRequest()">{{msg.azure_label}}</a>
                                    </div>
                                    <div class="btn-group btn-group-justified" ng-if="isVisible('GCP') || isVisible('OPENSTACK')">
                                        <a id="gcpSecurityGroupChange" ng-if="isVisible('GCP')" ng-class="{'btn':true, 'btn-info':gcpSecurityGroup, 'btn-default':!gcpSecurityGroup}" role="button" ng-click="createGcpSecurityGroupRequest()">{{msg.gcp_label}}</a>
                                        <a id="openstackSecurityGroupChange" ng-if="isVisible('OPENSTACK')" ng-class="{'btn':true, 'btn-info':openstackSecurityGroup, 'btn-default':!openstackSecurityGroup}" role="button" ng-click="createOpenstackSecurityGroupRequest()">{{msg.openstack_label}}</a>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="alert alert-danger" role="alert" ng-show="showAlert" ng-click="unShowErrorMessageAlert()">{{alertMessage}}</div>
                            
                            <form class="form-horizontal" role="form" name="azureSecurityGroupForm" ng-show="azureSecurityGroup && isVisible('AZURE_RM')">
                                <div ng-include src="'tags/securitygroup/azuresecuritygroupform.tag'"></div>
                            </form>
                            
                            <form class="form-horizontal" role="form" name="awsSecurityGroupForm" ng-show="awsSecurityGroup && isVisible('AWS')">
                                <div ng-include src="'tags/securitygroup/awssecuritygroupform.tag'"></div>
                            </form>
                            
                            <form class="form-horizontal" role="form" name="gcpSecurityGroupForm" ng-show="gcpSecurityGroup && isVisible('GCP')" >
                                <div ng-include src="'tags/securitygroup/gcpsecuritygroupform.tag'"></div>
                            </form>
                            
                            <form class="form-horizontal" role="form" name="openstackSecurityGroupForm" ng-show="openstackSecurityGroup && isVisible('OPENSTACK')" >
                                <div ng-include src="'tags/securitygroup/openstacksecuritygroupform.tag'"></div>
                            </form>
                            
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ SECURITY GROUP LIST ........................................... -->

                <div class="panel-group" id="securitygroup-list-accordion">

                    <!-- .............. SECURITY GROUP .............................................. -->

                    <div class="panel panel-default" ng-repeat="securitygroup in $root.securitygroups | orderBy:'name'">


                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#securitygroup-list-accordion" data-target="#panel-securitygroup-collapse{{securitygroup.id}}"><i class="fa fa-file-o fa-fw"></i>{{securitygroup.name}}</a>
                                <span class="label label-info pull-right" >{{securitygroup.cloudPlatform}}</span>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="securitygroup.publicInAccount"></i>
                            </h5>
                        </div>
                        <div id="panel-securitygroup-collapse{{securitygroup.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('securitygroups', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteSecurityGroup(securitygroup)">
                                    <i class="fa fa-times fa-fw"></i><span> {{msg.securitygroup_list_delete}}</span>
                                </a>
                            </p>

                            <div ng-include src="'tags/securitygroup/securitygrouplist.tag'"></div>

                        </div>
                    </div>
                    <!-- .panel -->
                </div>
                <!-- #securitygroup-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>