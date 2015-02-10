<div id="panel-account" class="col-md-12 col-lg-12" ng-controller="accountuserController">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a id="account-btn" data-target="#panel-account-collapse" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4> account details</h4>
        </div>

        <div id="panel-account-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <div class="panel-group" id="account-list-accordion">

                    <!-- ............. ACCOUNT DETAILS ............................................... -->

                        <form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="accountUsername">Username</label>

                                <div class="col-sm-9">
                                    <p name="accountUsername" class="form-control-static">{{userDetails.userName}}</p>
                                </div>
                                <!-- .col-sm-9 -->
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="accountGivenName">First name</label>

                                <div class="col-sm-9">
                                    <p name="accountGivenName" class="form-control-static">{{userDetails.givenName}}</p>
                                </div>
                                <!-- .col-sm-9 -->
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="accountFamilyName">Last name</label>

                                <div class="col-sm-9">
                                    <p name="accountFamilyName" class="form-control-static">{{userDetails.familyName}}</p>
                                </div>
                                <!-- .col-sm-9 -->
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="accountCompanyName">Company</label>

                                <div class="col-sm-9">
                                    <p name="accountCompanyName" class="form-control-static">{{userDetails.company}}</p>
                                </div>
                                <!-- .col-sm-9 -->
                            </div>
                            <div class="form-group">
                                <label class="col-sm-3 control-label" for="accountCompanyOwner">Company admin</label>

                                <div class="col-sm-9">
                                    <p name="accountCompanyOwner" class="form-control-static">{{userDetails.companyOwner}}</p>
                                </div>
                                <!-- .col-sm-9 -->
                            </div>
                        </form>

                </div><!-- #account-list-accordion -->

            </div><!-- .panel-body -->

        </div><!-- .panel-collapse -->
    </div><!-- .panel -->

</div><!-- .col- -->