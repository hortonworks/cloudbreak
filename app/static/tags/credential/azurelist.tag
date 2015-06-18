<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

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
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="jksFile">{{msg.credential_azure_list_cert_label}}</label>

        <div class="col-sm-9" id="jksFile" >
           <div class="col-sm-11">
             <a ng-href="credentials/certificate/{{credential.id}}" class="btn btn-success btn-block" role="button"><i
                     class="fa fa-file fa-fw"></i>
                 {{msg.credential_azure_list_cert_download_label}}</a>
           </div>
           <div class="col-sm-1">
             <a ng-click=refreshCertificateFile(credential.id) class="btn btn-info btn-block" role="button">
               <i class="fa fa-refresh fa-2"></i></a>
           </div>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>
