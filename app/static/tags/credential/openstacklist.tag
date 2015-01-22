<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Name</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group" ng-show="credential.description">
        <label class="col-sm-3 control-label" for="openstackdescriptionfield">Description</label>

        <div class="col-sm-9">
            <p id="openstackdescriptionfield" class="form-control-static">{{credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstackuser">User</label>

        <div class="col-sm-9">
            <p id="openstackuser" class="form-control-static">{{credential.parameters.user}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstackpassword">Password</label>

      <div class="col-sm-9">
        <p id="openstackpassword" class="form-control-static">{{credential.parameters.password}}</p>
      </div>
      <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstacktenantName">TenantName</label>

      <div class="col-sm-9">
        <p id="openstacktenantName" class="form-control-static">{{credential.parameters.tenantName}}</p>
      </div>
      <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstackendpoint">Endpoint</label>

      <div class="col-sm-9">
        <p id="openstackendpoint" class="form-control-static">{{credential.parameters.endpoint}}</p>
      </div>
      <!-- .col-sm-9 -->
    </div>

</form>
