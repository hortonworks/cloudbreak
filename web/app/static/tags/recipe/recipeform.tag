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
        <label class="col-sm-3 control-label" for="recipeType">{{msg.recipe_execution_type_label}}</label>
        <div class="col-sm-9">
            <div class="row">
                <div class="col-md-3">
                    <select class="form-control" id="recipeType" name="recipeType" ng-options="recipeType.key as recipeType.value for recipeType in $root.config.RECIPE_EXECUTION_TYPE.execution_types" ng-model="recipe.recipeType"></select>
                </div>
            </div>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="recipeContentType">{{msg.plugins_label}}</label>
        <div class="col-sm-9">
            <div class="row">
                <div class="col-md-3">
                    <select class="form-control" id="recipeContentType" name="recipeContentType" ng-options="recipeContentType.key as recipeContentType.value for recipeContentType in $root.config.RECIPE_CONTENT_TYPE.content_types" ng-model="$parent.recipeContentType"></select>
                </div>
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="recipeContentType == 'FILE'" ng-class="{ 'has-error': !fileReadAvailable }">
        <label class="col-sm-3 control-label" for="recipeFile" style="border-bottom: 0">{{msg.recipe_file}}</label>
        <div class="col-sm-9">
            <input type="file" name="recipeFile" id="recipeFile" onchange="angular.element(this).scope().generateStoredContentFromFile()" ng-disabled="{{!fileReadAvailable}}" />
            <div class="help-block" ng-show="!fileReadAvailable"><i class="fa fa-warning"></i> {{msg.file_upload_not_allowed}}
            </div>
        </div>
    </div>

    <div class="form-group" ng-show="recipeContentType == 'SCRIPT'">
        <label class="col-sm-3 control-label" for="recipeScript" style="border-bottom: 0">{{msg.recipe_script}}</label>
        <div class="col-sm-9">
            <textarea name="recipeScript" id="recipeScript" class="form-control" ng-model="$parent.recipeScript" rows=10 ng-attr-placeholder="{{msg.recipe_script_placeholder}}"></textarea>
        </div>
    </div>

    <div class="form-group" ng-show="recipeContentType == 'URL'">
            <label class="col-sm-3 control-label" for="recipeUrl" style="border-bottom: 0">{{msg.recipe_url}}</label>
            <div class="col-sm-9">
                <input type="text" class="form-control" name="recipeUrl" ng-model="recipe.uri" placeholder="{{msg.recipe_url_placeholder}}" >
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
            <a id="createRecipe" class="btn btn-success btn-block" ng-disabled="recipeCreationForm.$invalid || !validateRecipe()" ng-click="createRecipe()" role="button"><i class="fa fa-plus fa-fw"></i>{{msg.recipe_form_create}}</a>
        </div>
    </div>

</form>