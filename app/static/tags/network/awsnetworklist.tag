<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div ng-include src="'tags/network/commonnetworkfieldslist.tag'"></div>
    <div class="form-group" ng-if="network.parameters.vpcId">
        <label class="col-sm-3 control-label" for="{{network.name}}-vpcid">{{msg.network_aws_form_vpc_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-vpcid" class="form-control-static">{{network.parameters.vpcId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-if="network.parameters.internetGatewayId">
        <label class="col-sm-3 control-label" for="{{network.name}}-gatewayid">{{msg.network_aws_form_gateway_id_label}}</label>

        <div class="col-sm-9">
            <p id="{{network.name}}-gatewayid" class="form-control-static">{{network.parameters.internetGatewayId}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>
