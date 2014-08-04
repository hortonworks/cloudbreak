<form class="form-horizontal" role="form" name="blueprintForm">

    <div class="form-group" ng-class="{ 'has-error': blueprintForm.bluePrintName.$dirty && blueprintForm.bluePrintName.$invalid }">
        <label class="col-sm-3 control-label" for="bluePrintName">Name</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="bluePrintName" ng-model="bluePrintName" name="bluePrintName"
                   ng-pattern="/^[a-zA-Z][-a-zA-Z0-9]*$/" placeholder="set blueprint name" required>
            <div class="help-block" ng-show="blueprintForm.bluePrintName.$dirty && blueprintForm.bluePrintName.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.blueprint_name_invalid}}
            </div>
        </div>


        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->


    <div class="form-group">
        <label class="col-sm-3 control-label" for="bluePrintDescription">Description</label>

        <div class="col-sm-9">
            <input type="text" class="form-control" id="bluePrintDescription" ng-model="bluePrintDescription" name="bluePrintDescription"
                   placeholder="description" ng-maxlength="20">
            <div class="help-block" ng-show="blueprintForm.bluePrintDescription.$dirty && blueprintForm.bluePrintDescription.$invalid"><i class="fa fa-warning"></i>
                {{error_msg.blueprint_description_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->

    <div class="form-group" ng-hide="blueprintForm.bluePrintText.$dirty && !blueprintForm.bluePrintText.$error.required" ng-class="{ 'has-error': blueprintForm.blueprintUrl.$dirty && blueprintForm.blueprintUrl.$invalid }">
        <label class="col-sm-3 control-label" for="blueprintUrl">Source URL</label>

        <div class="col-sm-9">
            <input type="url" class="form-control" id="blueprintUrl" ng-model="blueprintUrl" name="blueprintUrl"
                   placeholder="set blueprint URL" required>

            <div class="help-block" ng-show="blueprintForm.blueprintUrl.$dirty && blueprintForm.blueprintUrl.$invalid">
                <i class="fa fa-warning"></i> {{error_msg.blueprint_url_invalid}}
            </div>
        </div>


        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->

    <div class="form-group" ng-hide="blueprintForm.blueprintUrl.$dirty && !blueprintForm.blueprintUrl.$error.required" ng-class="{ 'has-error': blueprintForm.bluePrintText.$error.validjson && blueprintForm.bluePrintText.$dirty }">
        <label class="col-sm-3 control-label" for="bluePrintText">Manual copy</label>

        <div class="col-sm-9">
            <textarea class="form-control" id="bluePrintText" ng-model="bluePrintText" name="bluePrintText" placeholder="paste blueprint text here" validjson rows="10" required></textarea>

            <div class="help-block" ng-show="blueprintForm.bluePrintText.$error.validjson">
                <i class="fa fa-warning"></i> {{error_msg.blueprint_json_invalid}}
            </div>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->


    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a href="#" id="createBlueprint" ng-click="createBlueprint()"
               ng-disabled="blueprintForm.bluePrintName.$invalid ||
                                           (blueprintForm.blueprintUrl.$error.url && !blueprintForm.blueprintUrl.$error.required) ||
                                           blueprintForm.bluePrintText.$error.validjson && !blueprintForm.bluePrintText.$error.required" class="btn btn-success btn-block" role="button"><i
                    class="fa fa-plus fa-fw"></i> create
                blueprint</a>
        </div>
    </div>

</form>