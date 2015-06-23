<div id="panel-users" class="col-md-12 col-lg-11" ng-show="user.admin">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a id="users-btn" data-target="#panel-users-collapse" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span style="margin-left: 4px"   class="badge pull-right">{{$root.accountUsers.length-1 == -1 ? 0 : $root.accountUsers.length-1}}</span><span ng-click="getUsers()" class="badge pull-right"><i class="fa fa-refresh"></i> </span> {{msg.users_manage_title}}</h4>
        </div>

        <div id="panel-users-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel"><a id="inviteCollapse" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-users-collapse"><i class="fa fa-plus fa-fw"></i><span> {{msg.users_form_invite_label}}</span></a></p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default" >
                    <div id="panel-create-users-collapse" class="panel-under-btn-collapse collapse">
                        <div class="panel-body">

                            <form class="form-horizontal" role="form" name="$parent.inviteForm">

                                <div class="form-group" ng-class="{ 'has-error': inviteForm.emailNewUser.$dirty && inviteForm.emailNewUser.$invalid }">
                                    <label class="col-sm-3 control-label" for="emailNewUser">{{msg.users_form_email_label}}</label>
                                    <div class="col-sm-9">
                                        <input class="form-control" ng-model="invite.mail" id="emailNewUser" name="emailNewUser" type="email" placeholder="" required>
                                        <div class="help-block" ng-show="inviteForm.emailNewUser.$dirty && inviteForm.emailNewUser.$invalid">
                                            <i class="fa fa-warning"></i> {{msg.email_invalid}}
                                        </div>
                                    </div>

                                </div><!-- .form-group -->

                                <div class="row btn-row">
                                    <div class="col-sm-9 col-sm-offset-3">
                                        <a id="inviteUser" class="btn btn-success btn-block" ng-disabled="inviteForm.$invalid" ng-click="inviteUser()" role="button"><i class="fa fa-plus fa-fw"></i> {{msg.users_form_invite_label}}</a>
                                    </div>
                                </div>

                            </form>
                        </div>
                    </div>
                </div><!-- .panel -->

                <!-- ............ USER LIST ........................................... -->

                <div class="panel-group" id="user-list-accordion">

                    <h5><i class="fa fa-circle fa-fw"></i> {{msg.users_list_active_users}}</h5>

                    <!-- ............. USER ............................................... -->

                    <div class="panel panel-default" ng-show="user.email !== actualuser.username" ng-repeat="actualuser in $root.accountUsers|filter: { active: true } ">
                        <div class="panel-heading">
                            <h5><a data-toggle="collapse" data-parent="#user-list-accordion" data-target="#panel-user-collapse{{actualuser.idx}}"><i class="fa fa-user fa-fw"></i>{{actualuser.username}}</a></h5>
                        </div>
                        <div id="panel-user-collapse{{actualuser.idx}}" class="panel-collapse collapse">
                            <p class="btn-row-over-panel pull-right"><a class="btn btn-warning" ng-click="activateUser(false, actualuser.username)" role="button"><i class="fa fa-circle-o fa-fw"></i><span> {{msg.users_form_deactivate_command_label}}</span></a></p>
                            <p class="btn-row-over-panel pull-right" ng-hide="actualuser.admin"><a class="btn btn-info" ng-click="makeAdmin(actualuser.id, actualuser.username, actualuser.idx)" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.users_form_make_admin_command_label}}</a></p>
                            <p class="btn-row-over-panel pull-right" ng-hide="actualuser.admin"><a class="btn btn-danger" ng-click="removeUser(actualuser.username, actualuser.id)" role="button"><i class="fa fa-minus fa-fw"></i>{{msg.users_form_delete_user_command_label}}</a></p>
                            <div class="panel-body">
                                <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter">{{msg.users_form_email_label}}</label>
                                        <div class="col-sm-9">
                                            <p id="parameter" class="form-control-static">{{actualuser.username}}</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->
                                </form>
                            </div>
                        </div>
                    </div><!-- .panel -->

                    <h5><i class="fa fa-circle-o fa-fw"></i> deactivated users</h5>

                    <!-- ............. USER ............................................... -->

                    <div class="panel panel-default" ng-repeat="actualuser in $root.accountUsers|filter: { active: false } ">
                        <div class="panel-heading">
                            <h5><a data-toggle="collapse" data-parent="#user-list-accordion" data-target="#panel-user-collapse{{actualuser.idx}}"><i class="fa fa-user fa-fw"></i>{{actualuser.username}}</a></h5>
                        </div>
                        <div id="panel-user-collapse{{actualuser.idx}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel pull-right"><a class="btn btn-info" role="button" ng-click="activateUser(true, actualuser.username)"><i class="fa fa-circle-o fa-fw"></i><span> {{msg.users_form_activate_command_label}}</span></a></p>
                            <p class="btn-row-over-panel pull-right" ng-hide="actualuser.admin"><a class="btn btn-danger" ng-click="removeUser(actualuser.username, actualuser.id)" role="button"><i class="fa fa-minus fa-fw"></i>{{msg.users_form_delete_user_command_label}}</a></p>
                            <div class="panel-body">


                                <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter">{{msg.users_form_email_label}}</label>
                                        <div class="col-sm-9">
                                            <p id="parameter" class="form-control-static">{{actualuser.username}}</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->
                                </form>
                            </div>
                        </div>
                    </div><!-- .panel -->


                </div><!-- #credential-list-accordion -->

            </div><!-- .panel-body -->

        </div><!-- .panel-collapse -->
    </div><!-- .panel -->

</div><!-- .col- -->
