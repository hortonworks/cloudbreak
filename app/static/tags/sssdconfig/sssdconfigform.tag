<form class="form-horizontal" role="form" name="$parent.sssdConfigCreationForm">

    <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigname.$dirty && sssdConfigCreationForm.sssdconfigname.$invalid }">
        <label class="col-sm-3 control-label" for="sssdconfigname">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <input type="text" class="form-control" name="sssdconfigname" ng-model="sssdConfig.name" id="sssdconfigname" placeholder="{{msg.sssdconfig_name_placeholder}}" ng-pattern="/^[a-z0-9]*[-a-z0-9]*$/" ng-maxlength="100" required>
            <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigname.$dirty && sssdConfigCreationForm.sssdconfigname.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_name_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigdescription.$dirty && sssdConfigCreationForm.sssdconfigdescription.$invalid }">
        <label class="col-sm-3 control-label" for="sssdconfigdescription">{{msg.description_label}}</label>
        <div class="col-sm-9">
            <input type="text" class="form-control" name="sssdconfigdescription" ng-model="sssdConfig.description" ng-maxlength="1000" id="sssdconfigdescription" placeholder="{{msg.sssdconfig_description_placeholder}}">
            <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigdescription.$dirty && sssdConfigCreationForm.sssdconfigdescription.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_description_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="sssdconfigtype">{{msg.sssdconfig_configuration_type_label}}</label>
        <div class="col-sm-9">
            <div class="row">
                <div class="col-md-3">
                    <select class="form-control" id="sssdconfigtype" name="sssdconfigtype" ng-options="configType.key as configType.value for configType in $root.config.SSSDCONFIG_TYPE.content_types" ng-model="$parent.sssdConfigType" ng-change="changeContentType()"></select>
                </div>
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfigType == 'FILE'" ng-class="{ 'has-error': !fileReadAvailable }">
        <label class="col-sm-3 control-label" for="sssdconfigfile" style="border-bottom: 0">{{msg.sssdconfig_config_file_label}}</label>
        <div class="col-sm-9">
            <input type="file" name="sssdconfigfile" id="sssdconfigfile" onchange="angular.element(this).scope().generateSssdConfigFromFile()" ng-disabled="{{!fileReadAvailable}}" />
            <div class="help-block" ng-show="!fileReadAvailable"><i class="fa fa-warning"></i> {{msg.file_upload_not_allowed}}
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfigType == 'TEXT'" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigtext.$dirty && sssdConfigCreationForm.sssdconfigtext.$invalid }">
        <label class="col-sm-3 control-label" for="sssdconfigtext" style="border-bottom: 0">{{msg.sssdconfig_configuration_label}}</label>
        <div class="col-sm-9">
            <textarea name="sssdconfigtext" id="sssdconfigtext" class="form-control" ng-model="sssdConfig.configuration" ng-required="sssdConfigType != 'PARAMS'" ng-minlength="50" ng-maxlength="1000" rows=10 placeholder="{{msg.sssdconfig_configuration_placeholder}}"></textarea>
            <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigtext.$dirty && sssdConfigCreationForm.sssdconfigtext.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_configuration_invalid}}</div>
        </div>
    </div>

    <div ng-show="sssdConfigType == 'PARAMS'">
        <div class="form-group">
            <label class="col-sm-3 control-label" for="sssdconfigprovidertype">{{msg.sssdconfig_providertype_label}}</label>
            <div class="col-sm-9">
                <div class="row">
                    <div class="col-md-3">
                        <select class="form-control" id="sssdconfigprovidertype" name="sssdconfigprovidertype" ng-options="providerType.key as providerType.value for providerType in $root.config.SSSDCONFIG_TYPE.provider_types" ng-model="sssdConfig.providerType" ng-required="sssdConfigType == 'PARAMS'"></select>
                    </div>
                </div>
            </div>
        </div>

        <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigurl.$dirty && sssdConfigCreationForm.sssdconfigurl.$invalid }">
            <label class="col-sm-3 control-label" for="sssdconfigurl" style="border-bottom: 0">{{msg.sssdconfig_url_label}}</label>
            <div class="col-sm-9">
                <input type="text" class="form-control" name="sssdconfigurl" id="sssdconfigurl" ng-model="sssdConfig.url" ng-minlength="10" ng-maxlength="255" placeholder="{{msg.sssdconfig_url_placeholder}}" ng-pattern="/^(ldap|ldaps|ad):\/\/.*/" ng-required="sssdConfigType == 'PARAMS'">
                <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigurl.$dirty && sssdConfigCreationForm.sssdconfigurl.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_url_invalid}}</div>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label" for="sssdconfigschematype">{{msg.sssdconfig_shema_label}}</label>
            <div class="col-sm-9">
                <div class="row">
                    <div class="col-md-3">
                        <select class="form-control" id="sssdconfigschematype" name="sssdconfigschematype" ng-options="schemaType.key as schemaType.value for schemaType in $root.config.SSSDCONFIG_TYPE.schema_types" ng-model="sssdConfig.schema" ng-required="sssdConfigType == 'PARAMS'"></select>
                    </div>
                </div>
            </div>
        </div>

        <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigbasesearch.$dirty && sssdConfigCreationForm.sssdconfigbasesearch.$invalid }">
            <label class="col-sm-3 control-label" for="sssdconfigbasesearch" style="border-bottom: 0">{{msg.sssdconfig_basesearch_label}}</label>
            <div class="col-sm-9">
                <input type="text" class="form-control" name="sssdconfigbasesearch" id="sssdconfigbasesearch" ng-model="sssdConfig.baseSearch" ng-minlength="10" ng-maxlength="255" placeholder="{{msg.sssdconfig_basesearch_placeholder}}" ng-required="sssdConfigType == 'PARAMS'">
                <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigbasesearch.$dirty && sssdConfigCreationForm.sssdconfigbasesearch.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_basesearch_invalid}}</div>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label" for="sssdconfigtlstype">{{msg.sssdconfig_reqcert_label}}</label>
            <div class="col-sm-9">
                <div class="row">
                    <div class="col-md-3">
                        <select class="form-control" id="sssdconfigtlstype" name="sssdconfigtlstype" ng-options="tlsType.key as tlsType.value for tlsType in $root.config.SSSDCONFIG_TYPE.tls_types" ng-model="sssdConfig.tlsReqcert" ng-required="sssdConfigType == 'PARAMS'"></select>
                    </div>
                </div>
            </div>
        </div>

        <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigadserver.$dirty && sssdConfigCreationForm.sssdconfigadserver.$invalid }">
            <label class="col-sm-3 control-label" for="sssdconfigadserver" style="border-bottom: 0">{{msg.sssdconfig_adserver_label}}</label>
            <div class="col-sm-9">
                <input type="text" class="form-control" name="sssdconfigadserver" id="sssdconfigadserver" ng-model="sssdConfig.adServer" ng-maxlength="255" placeholder="{{msg.sssdconfig_adserver_placeholder}}">
                <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigadserver.$dirty && sssdConfigCreationForm.sssdconfigadserver.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_adserver_invalid}}</div>
            </div>
        </div>

        <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigkerberosserver.$dirty && sssdConfigCreationForm.sssdconfigkerberosserver.$invalid }">
            <label class="col-sm-3 control-label" for="sssdconfigkerberosserver" style="border-bottom: 0">{{msg.sssdconfig_kerberosserver_label}}</label>
            <div class="col-sm-9">
                <input type="text" class="form-control" name="sssdconfigkerberosserver" id="sssdconfigkerberosserver" ng-model="sssdConfig.kerberosServer" ng-maxlength="255" placeholder="{{msg.sssdconfig_kerberosserver_placeholder}}">
                <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigkerberosserver.$dirty && sssdConfigCreationForm.sssdconfigkerberosserver.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_kerberosserver_invalid}}</div>
            </div>
        </div>

        <div class="form-group" ng-class="{ 'has-error': sssdConfigCreationForm.sssdconfigkerberosrealm.$dirty && sssdConfigCreationForm.sssdconfigkerberosrealm.$invalid }">
            <label class="col-sm-3 control-label" for="sssdconfigkerberosrealm" style="border-bottom: 0">{{msg.sssdconfig_kerberosrealm_label}}</label>
            <div class="col-sm-9">
                <input type="text" class="form-control" name="sssdconfigkerberosrealm" id="sssdconfigkerberosrealm" ng-model="sssdConfig.kerberosRealm" ng-maxlength="255" placeholder="{{msg.sssdconfig_kerberosrealm_placeholder}}">
                <div class="help-block" ng-show="sssdConfigCreationForm.sssdconfigkerberosrealm.$dirty && sssdConfigCreationForm.sssdconfigkerberosrealm.$invalid"><i class="fa fa-warning"></i> {{msg.sssdconfig_kerberosrealm_invalid}}</div>
            </div>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="sssdconfig_public">{{msg.public_in_account_label}}</label>
        <div class="col-sm-9">
            <input type="checkbox" name="sssdconfig_public" id="sssdconfig_public" ng-model="$parent.sssdConfigPublicInAccount">
        </div>
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createsssdconfig" class="btn btn-success btn-block" ng-disabled="sssdConfigCreationForm.$invalid" ng-click="createSssdConfig()" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.sssdconfig_form_create}}</a>
        </div>
    </div>

</form>