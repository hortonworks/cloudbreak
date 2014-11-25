<div id="panel-users" class="col-md-12 col-lg-12" ng-controller="accountuserController">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a id="users-btn" data-target="#panel-users-collapse" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">3</span> manage users</h4>
        </div>

        <div id="panel-users-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel"><a class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-invite-users-collapse"><i class="fa fa-plus fa-fw"></i><span> invite new user</span></a></p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default" >
                    <div  class="panel-under-btn-collapse" class="panel-under-btn-collapse collapse">
                        <div class="panel-body">

                            <form class="form-horizontal" role="form" id="inviteForm">

                                <div class="form-group" >
                                    <label class="col-sm-3 control-label" for="emailNewUser">email address</label>
                                    <div class="col-sm-9">
                                        <input type="text" class="form-control" ng-model="invite.mail" id="emailNewUser" type="email" placeholder="" required>
                                        <div class="help-block" ng-show="invite.mail.$dirty && invite.mail.$invalid">
                                            <i class="fa fa-warning"></i> {{error_msg.credential_ssh_key_invalid}}
                                        </div>
                                    </div>

                                </div><!-- .form-group -->

                                <div class="row btn-row">
                                    <div class="col-sm-9 col-sm-offset-3">
                                        <a id="inviteUser" class="btn btn-success btn-block" ng-click="inviteUser()" role="button"><i class="fa fa-plus fa-fw"></i> invite user</a>
                                    </div>
                                </div>

                            </form>
                        </div>
                    </div>
                </div><!-- .panel -->

                <!-- ............ USER LIST ........................................... -->

                <div class="panel-group" id="user-list-accordion">

                    <h5><i class="fa fa-circle fa-fw"></i> active users</h5>

                    <!-- ............. USER ............................................... -->

                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h5><a data-toggle="collapse" data-parent="#user-list-accordion" data-target="#panel-user-collapse01"><i class="fa fa-user fa-fw"></i>Joe Sixpack</a></h5>
                        </div>
                        <div id="panel-user-collapse01" class="panel-collapse collapse">

                            <p class="btn-row-over-panel"><a class="btn btn-danger" role="button"><i class="fa fa-circle-o fa-fw"></i><span> deactivate user</span></a></p>

                            <div class="panel-body">


                                <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter2">email address</label>
                                        <div class="col-sm-9">
                                            <p id="parameter2" class="form-control-static">joe.sixpack@company.com</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter3">last session</label>
                                        <div class="col-sm-9">
                                            <p id="parameter3" class="form-control-static">18:12 05/08/2014</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->

                                </form>
                            </div>
                        </div>
                    </div><!-- .panel -->

                    <h5><i class="fa fa-circle-o fa-fw"></i> deactivated users</h5>

                    <!-- ............. USER ............................................... -->

                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h5><a data-toggle="collapse" data-parent="#user-list-accordion" data-target="#panel-user-collapse02"><i class="fa fa-user fa-fw"></i>Dick Head</a></h5>
                        </div>
                        <div id="panel-user-collapse02" class="panel-collapse collapse">

                            <p class="btn-row-over-panel"><a class="btn btn-success" role="button"><i class="fa fa-circle fa-fw"></i><span> activate user</span></a></p>

                            <div class="panel-body">


                                <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter2">email address</label>
                                        <div class="col-sm-9">
                                            <p id="parameter2" class="form-control-static">richard.head@company.com</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter3">last session</label>
                                        <div class="col-sm-9">
                                            <p id="parameter3" class="form-control-static">10:53 29/07/2014</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->

                                </form>
                            </div>
                        </div>
                    </div><!-- .panel -->

                    <!-- ............. USER ............................................... -->

                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h5><a data-toggle="collapse" data-parent="#user-list-accordion" data-target="#panel-user-collapse03"><i class="fa fa-user fa-fw"></i>Kolompár Csubakka</a></h5>
                        </div>
                        <div id="panel-user-collapse03" class="panel-collapse collapse">

                            <p class="btn-row-over-panel"><a class="btn btn-success" role="button"><i class="fa fa-circle fa-fw"></i><span> activate user</span></a></p>

                            <div class="panel-body">


                                <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter2">email address</label>
                                        <div class="col-sm-9">
                                            <p id="parameter2" class="form-control-static">kolompar.csubakka@company.com</p>
                                        </div><!-- .col-sm-9 -->
                                    </div><!-- .form-group -->
                                    <div class="form-group">
                                        <label class="col-sm-3 control-label" for="parameter3">last session</label>
                                        <div class="col-sm-9">
                                            <p id="parameter3" class="form-control-static">22:31 25/07/2014</p>
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