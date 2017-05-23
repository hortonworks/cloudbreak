<form class="form-horizontal" role="form" name="$parent.flexCreationForm">

    <div class="form-group" ng-class="{ 'has-error': flexCreationForm.flexname.$dirty && flexCreationForm.flexname.$invalid }">
        <label class="col-sm-3 control-label" for="flexname">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <input type="text" class="form-control" name="flexname" ng-model="flex.name" id="flexname" placeholder="{{msg.flex_name_placeholder}}" required ng-pattern="/^[a-zA-Z][ -_a-zA-Z0-9]*[a-zA-Z0-9]$/" ng-minlength="5" ng-maxlength="50">
            <div class="help-block" ng-show="flexCreationForm.flexname.$dirty && flexCreationForm.flexname.$invalid"><i class="fa fa-warning"></i> {{msg.flex_name_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group" ng-class="{ 'has-error': flexCreationForm.flexsubscriptionid.$dirty && flexCreationForm.flexsubscriptionid.$invalid }">
        <label class="col-sm-3 control-label" for="flexsubscriptionid">{{msg.flex_subscriptionid_label}}</label>
        <div class="col-sm-9">
            <input type="text" class="form-control" name="flexsubscriptionid" ng-model="flex.subscriptionId" id="flexsubscriptionid" placeholder="{{msg.flex_subscriptionid_placeholder}}" ng-pattern="/^FLEX-[0-9]{10}$/" ng-change="(flex.subscriptionId.length === 15) && checkReservedFlexIds()" required>
            <div class="help-block" ng-show="flexCreationForm.flexsubscriptionid.$dirty && flexCreationForm.flexsubscriptionid.$invalid"><i class="fa fa-warning"></i> {{msg.flex_subscriptionid_invalid}}
            <div class="help-block" ng-show="flexCreationForm.flexsubscriptionid.$dirty && flexCreationForm.flexsubscriptionid.$error.used"><i class="fa fa-warning"></i>  {{msg.flex_subscriptionid_invalid_used}}
            </div>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="flex_default">{{msg.flex_default_label}}</label>
        <div class="col-sm-9">
            <input type="checkbox" name="flex_default" id="flex_default" ng-model="flex.default">
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="flex_usedforcontroller">{{msg.flex_used_for_controller_label}}</label>
        <div class="col-sm-9">
            <input type="checkbox" name="flex_usedforcontroller" id="flex_usedforcontroller" ng-model="flex.usedForController">
        </div>
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createFlex" class="btn btn-success btn-block" ng-disabled="flexCreationForm.$invalid" ng-click="createFlex()" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.flex_form_create}}</a>
        </div>
    </div>

</form>
