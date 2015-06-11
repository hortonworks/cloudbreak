<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group" ng-show="credential.description">
        <label class="col-sm-3 control-label" for="openstackdescriptionfield">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="openstackdescriptionfield" class="form-control-static">{{credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstacktenantName">{{msg.credential_openstack_form_tenant_label}}</label>

      <div class="col-sm-9">
        <p id="openstacktenantName" class="form-control-static">{{credential.parameters.tenantName}}</p>
      </div>
      <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstackendpoint">{{msg.credential_openstack_form_endpoint_label}}</label>

      <div class="col-sm-9">
        <p id="openstackendpoint" class="form-control-static">{{credential.parameters.endpoint}}</p>
      </div>
      <!-- .col-sm-9 -->
    </div>

</form>
