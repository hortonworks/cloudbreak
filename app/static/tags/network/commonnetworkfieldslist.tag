<div class="form-group">
    <label class="col-sm-3 control-label" for="{{network.name}}">{{msg.name_label}}</label>

    <div class="col-sm-9">
        <p id="{{network.name}}" class="form-control-static">{{network.name}}</p>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group" ng-show="network.description" ng-if="network.description != 'null'">
    <label class="col-sm-3 control-label" for="{{network.name}}-desc">{{msg.description_label}}</label>

    <div class="col-sm-9">
        <p id="{{network.name}}-desc" class="form-control-static">{{network.description}}</p>
    </div>
    <!-- .col-sm-9 -->
</div>
<div class="form-group">
    <label class="col-sm-3 control-label" for="{{network.name}}-subnet">{{msg.network_form_subnet_label}}</label>

    <div class="col-sm-9">
        <p id="{{network.name}}-subnet" class="form-control-static">{{network.subnetCIDR}}</p>
    </div>
    <!-- .col-sm-9 -->
</div>
