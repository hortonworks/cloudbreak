<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

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
    <div class="form-group" ng-show="credential.topologyId">
        <label class="col-sm-3 control-label" for="credential-topology">{{msg.credential_form_topology_label}}</label>

        <div class="col-sm-9">
            <p id="credential-topology" class="form-control-static">{{getTopologyNameById(credential.topologyId)}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-repeat="(key, value) in getCredentialParameters(credential)">
        <label class="col-sm-3 control-label" for="openstackendpoint">{{getParameterLabel(key)}}</label>

        <div class="col-sm-9">
            <p id="openstackendpoint" class="form-control-static">{{$root.displayNames.getPropertyName(value)}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>