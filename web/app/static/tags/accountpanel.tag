<div id="panel-account" class="col-md-12 col-lg-11">
    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a id="account-btn" data-target="#panel-account-collapse" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4> {{msg.account_details_manage_title}}</h4>
        </div>

        <div id="panel-account-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <div class="panel-group" id="account-list-accordion">

                    <!-- ............. ACCOUNT DETAILS ............................................... -->

                    <form class="form-horizontal" role="document">
                        <!-- role: 'document' - non-editable "form" -->
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="accountUsername">{{msg.account_details_username_label}}</label>

                            <div class="col-sm-9">
                                <p name="accountUsername" class="form-control-static">{{userDetails.userName}}</p>
                            </div>
                            <!-- .col-sm-9 -->
                        </div>
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="accountGivenName">{{msg.account_details_fistname_label}}</label>

                            <div class="col-sm-9">
                                <p name="accountGivenName" class="form-control-static">{{userDetails.givenName}}</p>
                            </div>
                            <!-- .col-sm-9 -->
                        </div>
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="accountFamilyName">{{msg.account_details_lastname_label}}</label>

                            <div class="col-sm-9">
                                <p name="accountFamilyName" class="form-control-static">{{userDetails.familyName}}</p>
                            </div>
                            <!-- .col-sm-9 -->
                        </div>
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="accountCompanyName">{{msg.account_details_company_label}}</label>

                            <div class="col-sm-9">
                                <p name="accountCompanyName" class="form-control-static">{{userDetails.company}}</p>
                            </div>
                            <!-- .col-sm-9 -->
                        </div>
                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="accountCompanyOwner">{{msg.account_details_company_admin_label}}</label>
                            <div class="col-sm-9">
                                <p name="accountCompanyOwner" class="form-control-static">{{userDetails.companyOwner}}</p>
                            </div>
                            <!-- .col-sm-9 -->
                        </div>

                        <div class="form-group">
                            <label class="col-sm-3 control-label" for="scopes">{{msg.account_details_scopes}}</label>
                            <div class="col-sm-4">
                                <table class="table table-bordered table-striped responsive-utilities">
                                    <thead>
                                        <tr>
                                            <th>{{msg.account_details_ability}}</th>
                                            <th>{{msg.account_details_create}}</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <th scope="row">{{msg.account_details_blueprints}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="bpch1" ng-checked="isWriteScope('blueprints', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_recipes}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="recch1" ng-checked="isWriteScope('recipes', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_templates}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="tmch1" ng-checked="isWriteScope('templates', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_credentials}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="crch1" ng-checked="isWriteScope('credentials', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_stacks}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="stch1" ng-checked="isWriteScope('stacks', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_networks}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="nwcrch1" ng-checked="isWriteScope('networks', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_securitygroups}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="sgstch1" ng-checked="isWriteScope('securitygroups', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_sssdconfigs}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="sssdch1" ng-checked="isWriteScope('sssdconfigs', userDetails.groups)">
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">{{msg.account_details_cloudplatforms}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" disabled name="pch1" ng-checked="isWriteScope('platforms', userDetails.groups)">
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                        <div ng-init="fetchIsPlatformSelectionDisabled()" ng-hide="isPlatformSelectionDisabled" class="">
                            <label class="col-sm-3 control-label" for="scopes">{{msg.account_details_cloudplatforms}}</label>
                            <div class="col-sm-4">
                                <table class="table table-bordered table-striped responsive-utilities">
                                    <thead>
                                        <tr>
                                            <th>{{msg.account_details_ability}}</th>
                                            <th>{{msg.account_details_use}}</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr ng-repeat="(platform, variants) in $root.params.platformVariants">
                                            <th scope="row">{{platform}}</th>
                                            <td class="is-visible">
                                                <input type="checkbox" name="bpch1" id="bpch1" ng-checked="isChecked(platform)" ng-disabled="!user.admin || ($root.params.platforms.length == 1 && isChecked(platform))" ng-click="changePlatformVisibility(platform)">
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>

                    </form>

                </div>
                <!-- #account-list-accordion -->

            </div>
            <!-- .panel-body -->

        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->

</div>
<!-- .col- -->