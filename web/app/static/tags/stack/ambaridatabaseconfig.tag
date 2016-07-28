<div class="form-group" name="ambari_database1">
    <label class="col-sm-3 control-label" for="ambari_db_vendor">{{msg.cluster_form_ambari_db_vendor_label}}</label>
    <div class="col-sm-8">
        <select class="form-control" id="ambari_db_vendor" name="ambari_db_vendor" ng-options="dbVendor as $root.displayNames.getPropertyName('database', dbVendor) for dbVendor in $root.settings.database.vendor" ng-model="cluster.ambariDatabaseDetails.vendor">
            <option/>
        </select>
        <div class="help-block" ng-show="cluster.ambariDatabaseDetails.vendor == 'MSSQL' || cluster.ambariDatabaseDetails.vendor == 'ORACLE' || cluster.ambariDatabaseDetails.vendor == 'SQLANYWHERE'">
            <i class="fa fa-warning"></i> {{msg.cluster_form_ambari_db_not_oob_warning}}
        </div>
    </div>
</div>

<div ng-show="cluster.ambariDatabaseDetails.vendor">
    <div class="form-group" name="ambari_database2" ng-class="{ 'has-error': $parent.clusterCreationForm.ambari_db_host.$dirty && $parent.clusterCreationForm.ambari_db_host.$invalid }">
        <label class="col-sm-3 control-label" for="ambari_db_host">{{msg.cluster_form_ambari_db_host_label}}</label>
        <div class="col-sm-8">
            <input type="string" name="ambari_db_host" class="form-control" ng-model="cluster.ambariDatabaseDetails.host" id="ambari_db_host" placeholder="{{msg.cluster_form_ambari_db_host_placeholder}}" ng-required="cluster.ambariDatabaseDetails.vendor" ng-pattern="/^[a-zA-Z0-9]([a-zA-Z0-9-\.]+)$/">
            <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_db_host.$dirty && $parent.clusterCreationForm.ambari_db_host.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_form_ambari_db_host_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group" name="ambari_database3" ng-class="{ 'has-error': $parent.clusterCreationForm.ambari_db_port.$dirty && $parent.clusterCreationForm.ambari_db_port.$invalid }">
        <label class="col-sm-3 control-label" for="ambari_db_port">{{msg.cluster_form_ambari_db_port_label}}</label>
        <div class="col-sm-8">
            <input type="number" name="ambari_db_port" class="form-control" ng-model="cluster.ambariDatabaseDetails.port" id="ambari_db_port" placeholder="{{ $root.displayNames.getPropertyName('databasePorts', cluster.ambariDatabaseDetails.vendor) }}" ng-required="cluster.ambariDatabaseDetails.vendor" min="1" max="65535">
            <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_db_port.$dirty && $parent.clusterCreationForm.ambari_db_port.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_form_ambari_db_port_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group" name="ambari_database4" ng-class="{ 'has-error': $parent.clusterCreationForm.ambari_db_name.$dirty && $parent.clusterCreationForm.ambari_db_name.$invalid }">
        <label class="col-sm-3 control-label" for="ambari_db_name">{{msg.cluster_form_ambari_db_name_label}}</label>
        <div class="col-sm-8">
            <input type="string" name="ambari_db_name" class="form-control" ng-model="cluster.ambariDatabaseDetails.name" id="ambari_db_name" ng-required="cluster.ambariDatabaseDetails.vendor" ng-pattern="/^[^']+$/">
            <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_db_name.$dirty && $parent.clusterCreationForm.ambari_db_name.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_form_ambari_db_name_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group" name="ambari_database5" ng-class="{ 'has-error': $parent.clusterCreationForm.ambari_db_username.$dirty && $parent.clusterCreationForm.ambari_db_username.$invalid }">
        <label class="col-sm-3 control-label" for="ambari_db_username">{{msg.cluster_form_ambari_db_username_label}}</label>
        <div class="col-sm-8">
            <input type="string" name="ambari_db_username" class="form-control" ng-model="cluster.ambariDatabaseDetails.userName" id="ambari_db_username" ng-required="cluster.ambariDatabaseDetails.vendor" ng-pattern="/^[^']+$/">
            <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_db_username.$dirty && $parent.clusterCreationForm.ambari_db_username.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_form_ambari_db_username_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group" name="ambari_database6" ng-class="{ 'has-error': $parent.clusterCreationForm.ambari_db_password.$dirty && $parent.clusterCreationForm.ambari_db_password.$invalid }">
        <label class="col-sm-3 control-label" for="ambari_db_password">{{msg.cluster_form_ambari_db_password_label}}</label>
        <div class="col-sm-8">
            <input type="string" name="ambari_db_password" class="form-control" ng-model="cluster.ambariDatabaseDetails.password" id="ambari_db_password" ng-required="cluster.ambariDatabaseDetails.vendor" ng-pattern="/^[^']+$/">
            <div class="help-block" ng-show="$parent.clusterCreationForm.ambari_db_password.$dirty && $parent.clusterCreationForm.ambari_db_password.$invalid"><i class="fa fa-warning"></i> {{msg.cluster_form_ambari_db_password_invalid}}
            </div>
        </div>
    </div>
</div>

<div class="form-group">
    <div class="col-sm-11">

        <div class="btn-group btn-group-justified" role="group" style="padding-top: 40px" aria-label="...">
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureAmbariRepository')">
                    <i class="fa fa-angle-double-left"></i> {{msg.cluster_form_ambari_hdprepo_tag}}
                </button>
            </div>
            <div class="btn-group" role="group" style="opacity: 0;">
                <button type="button" class="btn btn-sm btn-default"></button>
            </div>
            <div class="btn-group" role="group" ng-hide="clusterCreationForm.$invalid">
                <button type="button" class="btn btn-sm btn-default" ng-click="showWizardActualElement('configureReview')">{{msg.cluster_form_ambari_launch_tag}} <i class="fa fa-angle-double-right"></i></button>
            </div>
        </div>
    </div>
</div>