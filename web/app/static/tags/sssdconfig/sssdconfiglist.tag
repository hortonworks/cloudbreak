<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{sssdConfig.name}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.description">
        <label class="col-sm-3 control-label" for="sssdconfigdescriptionfield">{{msg.description_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigdescriptionfield" class="form-control-static">{{sssdConfig.description}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.configuration">
        <label class="col-sm-3 control-label" for="sssdconfigtext">{{msg.sssdconfig_configuration_label}}</label>
        <div class="col-sm-9">
            <textarea name="sssdconfigtext" id="sssdconfigtext" class="form-control" ng-model="sssdConfig.configuration" rows=10 readonly="readonly"></textarea>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.providerType">
        <label class="col-sm-3 control-label" for="sssdconfigprovidertypefield">{{msg.sssdconfig_providertype_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigprovidertypefield" class="form-control-static">{{sssdConfig.providerType}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.url">
        <label class="col-sm-3 control-label" for="sssdconfigurlfield">{{msg.sssdconfig_url_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigurlfield" class="form-control-static">{{sssdConfig.url}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.schema">
        <label class="col-sm-3 control-label" for="sssdconfigschemafield">{{msg.sssdconfig_shema_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigschemafield" class="form-control-static">{{sssdConfig.schema}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.baseSearch">
        <label class="col-sm-3 control-label" for="sssdconfigbasesearchfield">{{msg.sssdconfig_basesearch_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigbasesearchfield" class="form-control-static">{{sssdConfig.baseSearch}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.tlsReqcert">
        <label class="col-sm-3 control-label" for="sssdconfigreqcertfield">{{msg.sssdconfig_reqcert_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigreqcertfield" class="form-control-static">{{sssdConfig.tlsReqcert}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.adServer">
        <label class="col-sm-3 control-label" for="sssdconfigadserverfield">{{msg.sssdconfig_adserver_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigadserverfield" class="form-control-static">{{sssdConfig.adServer}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.kerberosServer">
        <label class="col-sm-3 control-label" for="sssdconfigkerberosserverfield">{{msg.sssdconfig_kerberosserver_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigkerberosserverfield" class="form-control-static">{{sssdConfig.kerberosServer}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="sssdConfig.kerberosRealm">
        <label class="col-sm-3 control-label" for="sssdconfigkerberosrealmfield">{{msg.sssdconfig_kerberosrealm_label}}</label>
        <div class="col-sm-9">
            <p id="sssdconfigkerberosrealmfield" class="form-control-static">{{sssdConfig.kerberosRealm}}</p>
        </div>
    </div>
</form>