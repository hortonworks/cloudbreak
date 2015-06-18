    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm.gcp_networkName.$dirty && gcpNetworkForm.gcp_networkName.$invalid }">
        <label class="col-sm-3 control-label" for="gcp_networkName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" name="gcp_networkName" ng-model="network.name" ng-minlength="5" ng-maxlength="100" required id="gcp_networkName" placeholder="{{msg.name_placeholder}}">
            <div class="help-block" ng-show="gcpNetworkForm.gcp_networkName.$dirty && gcpNetworkForm.gcp_networkName.$invalid">
                <i class="fa fa-warning"></i> {{msg.network_name_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm.gcp_networkDescription.$dirty && gcpNetworkForm.gcp_networkDescription.$invalid }">
        <label class="col-sm-3 control-label" for="gcp_networkDescription">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="gcp_networkDescription" ng-model="network.description" ng-maxlength="1000" id="gcp_networkDescription" placeholder="{{msg.network_form_description_placeholder}}">
            <div class="help-block" ng-show="gcpNetworkForm.gcp_networkDescription.$dirty && gcpNetworkForm.gcp_networkDescription.$invalid">
                <i class="fa fa-warning"></i> {{msg.netowrk_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-class="{ 'has-error': gcpNetworkForm.gcp_networkSubnet.$dirty && gcpNetworkForm.gcp_networkSubnet.$invalid }">
        <label class="col-sm-3 control-label" for="gcp_networkSubnet">{{msg.network_form_subnet_label}}</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" name="gcp_networkSubnet" ng-model="network.subnetCIDR" ng-maxlength="30" id="gcp_networkSubnet" placeholder="{{msg.network_form_subnet_placeholder}}" ng-pattern="/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$/" required>
            <div class="help-block" ng-show="gcpNetworkForm.gcp_networkSubnet.$dirty && gcpNetworkForm.gcp_networkSubnet.$invalid">
                <i class="fa fa-warning"></i> {{msg.network_subnet_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
            <label class="col-sm-3 control-label" for="gcp_network_public">{{msg.public_in_account_label}}</label>
            <div class="col-sm-9">
                <input type="checkbox" name="gcp_network_public" id="gcp_network_public" ng-model="network.publicInAccount">
            </div>
       <!-- .col-sm-9 -->
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createAwsTemplate" ng-disabled="gcpNetworkForm.$invalid" class="btn btn-success btn-block" ng-click="createGcpNetwork()" role="button"><i class="fa fa-plus fa-fw"></i>
                {{msg.network_form_create}}</a>
        </div>
    </div>
