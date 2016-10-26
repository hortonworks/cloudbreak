<form class="form-horizontal" role="form" name="$parent.recipeCreationForm">

    <div class="form-group" ng-class="{ 'has-error': recipeCreationForm.recipename.$dirty && recipeCreationForm.recipename.$invalid }">
        <label class="col-sm-3 control-label" for="recipename">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <input type="text" class="form-control" name="recipename" ng-model="recipe.name" id="recipename" placeholder="{{msg.recipe_name_placeholder}}" ng-pattern="/^[a-z0-9]*[-a-z0-9]*$/" ng-maxlength="100" required>
            <div class="help-block" ng-show="recipeCreationForm.recipename.$dirty && recipeCreationForm.recipename.$invalid"><i class="fa fa-warning"></i> {{msg.recipe_name_invalid}}
            </div>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="recipedescription">{{msg.description_label}}</label>
        <div class="col-sm-9">
            <input type="text" class="form-control" name="recipedescription" ng-model="recipe.description" ng-maxlength="1000" id="recipedescription" placeholder="{{msg.recipe_description_placeholder}}">
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="recipetype">{{msg.plugins_label}}</label>
        <div class="col-sm-9">
            <div class="row">
                <div class="col-md-3">
                    <select class="form-control" id="recipetype" name="recipetype" ng-options="recipeType.key as recipeType.value for recipeType in $root.config.RECIPE_TYPE.content_types" ng-model="recipeType"></select>
                </div>
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="recipeType == 'FILE'" ng-class="{ 'has-error': !fileReadAvailable }">
        <label class="col-sm-3 control-label" for="preInstallFile" style="border-bottom: 0">{{msg.recipe_pre_install_file}}</label>
        <div class="col-sm-9">
            <input type="file" name="preInstallFile" id="preInstallFile" onchange="angular.element(this).scope().generateStoredPluginFromFile()" ng-disabled="{{!fileReadAvailable}}" />
            <div class="help-block" ng-show="!fileReadAvailable"><i class="fa fa-warning"></i> {{msg.file_upload_not_allowed}}
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="recipeType == 'FILE'" ng-class="{ 'has-error': !fileReadAvailable }">
        <label class="col-sm-3 control-label" for="postInstallFile" style="border-bottom: 0">{{msg.recipe_post_install_file}}</label>
        <div class="col-sm-9">
            <input type="file" name="postInstallFile" id="postInstallFile" onchange="angular.element(this).scope().generateStoredPluginFromFile()" ng-disabled="{{!fileReadAvailable}}" />
            <div class="help-block" ng-show="!fileReadAvailable"><i class="fa fa-warning"></i> {{msg.file_upload_not_allowed}}
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="recipeType == 'SCRIPT'">
        <label class="col-sm-3 control-label" for="preInstallScript" style="border-bottom: 0">{{msg.recipe_pre_install_script}}</label>
        <div class="col-sm-9">
            <textarea name="preInstallScript" id="preInstallScript" class="form-control" ng-model="$parent.preInstallScript" ng-change="generateStoredPluginFromText()" rows=10 ng-attr-placeholder="{{msg.recipe_pre_install_script_placeholder}}"></textarea>
        </div>
    </div>

    <div class="form-group" ng-show="recipeType == 'SCRIPT'">
        <label class="col-sm-3 control-label" for="postInstallScript" style="border-bottom: 0">{{msg.recipe_post_install_script}}</label>
        <div class="col-sm-9">
            <textarea name="postInstallScript" id="postInstallScript" class="form-control" ng-model="$parent.postInstallScript" ng-change="generateStoredPluginFromText()" rows=10 ng-attr-placeholder="{{msg.recipe_post_install_script_placeholder}}"></textarea>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="recipe_public">{{msg.public_in_account_label}}</label>
        <div class="col-sm-9">
            <input type="checkbox" name="recipe_public" id="recipe_public" ng-model="$parent.recipePublicInAccount">
        </div>
    </div>

    <div class="row btn-row">
        <div class="col-sm-9 col-sm-offset-3">
            <a id="createRecipe" class="btn btn-success btn-block" ng-disabled="recipeCreationForm.$invalid" ng-click="createRecipe()" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.recipe_form_create}}</a>
        </div>
    </div>

</form>