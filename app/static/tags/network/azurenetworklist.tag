<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div ng-include src="'tags/network/commonnetworkfieldslist.tag'"></div>
    <div class="form-group" ng-if="network.parameters.resourceGroupName">
        <label class="col-sm-3 control-label" for="{{network.name}}-rg">{{msg.network_azure_form_resource_group_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-rg" class="form-control-static">{{network.parameters.resourceGroupName}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-if="network.parameters.networkId">
        <label class="col-sm-3 control-label" for="{{network.name}}-nid">{{msg.network_azure_form_network_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-nid" class="form-control-static">{{network.parameters.networkId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group" ng-if="network.parameters.subnetId">
        <label class="col-sm-3 control-label" for="{{network.name}}-sid">{{msg.network_azure_form_subnet_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-sid" class="form-control-static">{{network.parameters.subnetId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>
