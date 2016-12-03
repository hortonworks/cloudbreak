</form>
<div class="panel-group" id="aws-security-accordion" role="tablist" aria-multiselectable="true">
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="aws-security-headingOne">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#aws-security-accordion" href="#aws-security-collapseOne" aria-expanded="true" aria-controls="aws-security-collapseOne" ng-click="selectSecurityGroupType1()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.security_group_form_type1_title}}</span>
                </a>
            </h4>
        </div>
        <div id="aws-security-collapseOne" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="aws-security-headingOne">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.security_group_aws_form_type1_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="awssecuritygroupForm_1">
                    <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_1.sgname.$dirty && awssecuritygroupForm_1.sgname.$invalid }">
                        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" name="sgname" ng-model="securitygroup.name" ng-minlength="5" ng-maxlength="100" required id="sgname" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="awssecuritygroupForm_1.sgname.$dirty && awssecuritygroupForm_1.sgname.$invalid">
                                <i class="fa fa-warning"></i> {{msg.securitygroup_name_invalid}}
                            </div>
                        </div>
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_1.description.$dirty && awssecuritygroupForm_1.description.$invalid }">
                        <label class="col-sm-3 control-label" for="description">{{msg.description_label}}</label>
                
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="description" ng-model="securitygroup.description" ng-maxlength="1000" id="description" placeholder="{{msg.template_form_description_placeholder}}">
                            <div class="help-block" ng-show="awssecuritygroupForm_1.description.$dirty && awssecuritygroupForm_1.description.$invalid">
                                <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_1.securitygroup_publicInAccount.$dirty && awssecuritygroupForm_1.securitygroup_publicInAccount.$invalid }">
                        <label class="col-sm-3 control-label" for="securitygroup_publicInAccount">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="securitygroup_publicInAccount" id="securitygroup_publicInAccount" ng-model="securitygroup.public">
                        </div>
                    </div>
                
                    <div class="form-group">
                        <label class="col-sm-3 control-label">{{msg.security_group_security_rules}}</label>
                        <div class="col-sm-8 col-sm-offset-1">
                            <form class="form-horizontal" role="form" name="tmpsecportForm">
                                <div class="form-group" style="padding-top: 10px;" ng-class="{ 'has-error': tmpsecportForm.cidr.$dirty && tmpsecportForm.cidr.$invalid }">
                                    <label class="col-sm-3 control-label">CIDR: </label>
                                    <div class="col-sm-9">
                                        <input type="text" class="form-control" name="cidr" ng-model="tmpsecport.cidr" placeholder="0.0.0.0/0" id="cidr" ng-pattern="/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$/">
                                        <div class="help-block" ng-show="tmpsecportForm.seccidr.$dirty && tmpsecportForm.seccidr.$invalid">
                                            <i class="fa fa-warning"></i> {{msg.invalid_cidr}}
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group" ng-class="{ 'has-error': tmpsecportForm.port.$dirty && tmpsecportForm.port.$invalid }">
                                    <label class="col-sm-3 control-label">Port: </label>
                                    <div class="col-sm-9">
                                        <input type="text" class="form-control" name="port" placeholder="4444,5555,6666" ng-model="tmpsecport.port" id="port">
                                        <div class="help-block" ng-show="tmpsecportForm.port.$dirty && tmpsecportForm.port.$invalid">
                                            <i class="fa fa-warning"></i> {{msg.invalid_port_string}}
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group" ng-class="{ 'has-error': tmpsecportForm.protocol.$dirty && tmpsecportForm.protocol.$invalid }">
                                    <label class="col-sm-3 control-label">Protocol: </label>
                                    <div class="col-sm-9">
                                        <input type="text" class="form-control" name="protocol" placeholder="tcp" ng-model="tmpsecport.protocol" id="protocol">
                                        <div class="help-block" ng-show="tmpsecportForm.protocol.$dirty && tmpsecportForm.protocol.$invalid">
                                            <i class="fa fa-warning"></i> {{msg.invalid_protocol_string}}
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <a id="createGroupProtocolTemplate" ng-disabled="tmpsecportForm.$invalid" class="btn btn-success btn-block" ng-click="addGroupProtocol()" role="button"><i class="fa fa-plus"></i> Add Rule</a>
                                </div>
                            </form>
                            <div style="padding-top: 10px;" ng-show="securitygroup.tmpsecurityRules.length > 0">
                                <table class="table table-bordered">
                                    <thead>
                                        <tr>
                                            <th>CIDR</th>
                                            <th>Port</th>
                                            <th>Protocol</th>
                                            <th>Remove</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr ng-repeat="sec in securitygroup.tmpsecurityRules">
                                            <td>{{sec.cidr}}</td>
                                            <td>{{sec.port}}</td>
                                            <td>{{sec.protocol}}</td>
                                            <td><span id="deleteGroupProtocolTemplate" class="label label-danger" ng-click="deleteGroupProtocol(sec)" role="button"><i class="fa fa-minus"></i></span></td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a id="createSecurityGroup" ng-disabled="awssecuritygroupForm_1.$invalid" class="btn btn-success btn-block" ng-click="createAwsSecurityGroup()" role="button"><i
                                     class="fa fa-plus fa-fw"></i> {{msg.securitygroup_form_create}}</a>
                        </div>
                    </div>                
                </form>
            </div>
        </div>
    </div>
    
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="aws-security-headingTwo">
            <h4 class="panel-title">
                <a class="collapsed" role="button" data-toggle="collapse" data-parent="#aws-security-accordion" href="#aws-security-collapseTwo" aria-expanded="false" aria-controls="aws-security-collapseTwo" ng-click="selectSecurityGroupType2()">
                <i class="fa fa-sitemap fa-fw"/><span style="padding-left: 10px">{{msg.security_group_form_type2_title}}</span>
                </a>
            </h4>
        </div>
        <div id="aws-security-collapseTwo" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="aws-security-headingTwo">
            <div class="panel-body">
                <div class="form-group col-sm-12">
                    <i class="fa fa-info-circle" /><small>&nbsp;{{msg.security_group_aws_form_type2_info}}</small>
                </div>
                <form class="form-horizontal" role="form" name="awssecuritygroupForm_2">
                    <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_2.sgname.$dirty && awssecuritygroupForm_2.sgname.$invalid }">
                        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" ng-pattern="/^[a-z][-a-z0-9]*[a-z0-9]$/" name="sgname" ng-model="securitygroup.name" ng-minlength="5" ng-maxlength="100" required id="sgname" placeholder="{{msg.name_placeholder}}">
                            <div class="help-block" ng-show="awssecuritygroupForm_2.sgname.$dirty && awssecuritygroupForm_2.sgname.$invalid">
                                <i class="fa fa-warning"></i> {{msg.securitygroup_name_invalid}}
                            </div>
                        </div>
                    </div>
                     <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_2.description.$dirty && awssecuritygroupForm_2.description.$invalid }">
                        <label class="col-sm-3 control-label" for="description">{{msg.description_label}}</label>
                
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="description" ng-model="securitygroup.description" ng-maxlength="1000" id="description" placeholder="{{msg.template_form_description_placeholder}}">
                            <div class="help-block" ng-show="awssecuritygroupForm_2.description.$dirty && awssecuritygroupForm_2.description.$invalid">
                                <i class="fa fa-warning"></i> {{msg.template_description_invalid}}
                            </div>
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_2.securityGroupId.$dirty && awssecuritygroupForm_2.securityGroupId.$invalid }">
                        <label class="col-sm-3 control-label" for="securityGroupId">{{msg.security_group_id_label}}</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="securityGroupId" ng-model="securitygroup.securityGroupId" required id="securityGroupId" placeholder="{{msg.security_group_id_placeholder}}" ng-pattern="/^sg-[a-zA-Z0-9]{8}$/">
                        </div>
                        <!-- .col-sm-9 -->
                    </div>
                    <div class="form-group" ng-class="{ 'has-error': awssecuritygroupForm_2.securitygroup_publicInAccount.$dirty && awssecuritygroupForm_2.securitygroup_publicInAccount.$invalid }">
                        <label class="col-sm-3 control-label" for="securitygroup_publicInAccount">{{msg.public_in_account_label}}</label>
                        <div class="col-sm-9">
                            <input type="checkbox" name="securitygroup_publicInAccount" id="securitygroup_publicInAccount" ng-model="securitygroup.public">
                        </div>
                    </div>
                    <div class="row btn-row">
                        <div class="col-sm-9 col-sm-offset-3">
                            <a id="createSecurityGroup" ng-disabled="awssecuritygroupForm_2.$invalid" class="btn btn-success btn-block" ng-click="createAwsSecurityGroup()" role="button"><i
                                     class="fa fa-plus fa-fw"></i> {{msg.securitygroup_form_create}}</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<form>