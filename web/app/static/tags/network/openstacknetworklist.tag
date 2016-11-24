<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div ng-include src="'tags/network/commonnetworkfieldslist.tag'"></div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="{{network.name}}-publicnetid">{{msg.network_openstack_form_public_network_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-publicnetid" class="form-control-static">{{network.parameters.publicNetId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-if="network.parameters.networkId">
        <label class="col-sm-3 control-label" for="{{network.name}}-nid">{{msg.network_openstack_form_network_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-nid" class="form-control-static">{{network.parameters.networkId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-if="network.parameters.routerId">
        <label class="col-sm-3 control-label" for="{{network.name}}-rid">{{msg.network_openstack_form_router_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-rid" class="form-control-static">{{network.parameters.routerId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-if="network.parameters.subnetId">
        <label class="col-sm-3 control-label" for="{{network.name}}-subnetid">{{msg.network_openstack_form_subnet_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-subnetid" class="form-control-static">{{network.parameters.subnetId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-if="network.parameters.networkingOption">
        <label class="col-sm-3 control-label" for="{{network.name}}-networkingOption">{{msg.network_openstack_form_network_option_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-networkingOption" class="form-control-static">{{network.parameters.networkingOption}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>