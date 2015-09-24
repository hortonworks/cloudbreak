<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-show="credential.description">
        <label class="col-sm-3 control-label" for="azuredescriptionfield">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="azuredescriptionfield" class="form-control-static">{{credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="subscriptionId">{{msg.credential_azure_form_subscription_id_label}}</label>

        <div class="col-sm-9">
            <p id="subscriptionId" class="form-control-static">{{credential.parameters.subscriptionId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="accesKey">{{msg.credential_azure_rm_form_access_key_id_label}}</label>

        <div class="col-sm-9">
            <p id="accesKey" class="form-control-static">{{credential.parameters.accesKey}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="secretKey">{{msg.credential_azure_rm_form_secret_key_id_label}}</label>

        <div class="col-sm-9">
            <p id="secretKey" class="form-control-static">{{credential.parameters.secretKey}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="tenantId">{{msg.credential_azure_rm_form_tenant_id_label}}</label>

        <div class="col-sm-9">
            <p id="tenantId" class="form-control-static">{{credential.parameters.tenantId}}</p>
        </div>
    </div>

</form>