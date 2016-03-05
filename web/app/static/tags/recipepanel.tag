<!-- .... TEMPLATES PANEL ................................................. -->

<div id="panel-templates" ng-controller="recipeController" class="col-md-12 col-lg-11">

    <div class="panel panel-default">
        <div class="panel-heading panel-heading-nav">
            <a href="" id="recipes-btn" class="btn btn-info btn-fa-2x" role="button" data-toggle="collapse" data-target="#panel-recipes-collapse"><i class="fa fa-angle-down fa-2x fa-fw-forced"></i></a>
            <h4><span class="badge pull-right">{{$root.recipes.length}}</span> manage recipes</h4>
        </div>

        <div id="panel-recipes-collapse" class="panel-btn-in-header-collapse collapse">
            <div class="panel-body">

                <p class="btn-row-over-panel" ng-if="isWriteScope('recipes', userDetails.groups)">
                    <a href="" id="panel-create-recipes-collapse-btn" class="btn btn-success" role="button" data-toggle="collapse" data-target="#panel-create-recipes-collapse">
                        <i class="fa fa-plus fa-fw"></i><span> {{msg.recipe_form_create}}</span>
                    </a>
                </p>

                <!-- ............ CREATE FORM ............................................. -->

                <div class="panel panel-default">
                    <div id="panel-create-recipes-collapse" class="panel-collapse panel-under-btn-collapse collapse">
                        <div class="panel-body">
                            <div ng-include src="'tags/recipe/recipeform.tag'"></div>
                        </div>
                    </div>
                </div>
                <!-- .panel -->

                <!-- ............ TEMPLATE LIST ........................................... -->

                <div class="panel-group" id="recipe-list-accordion">

                    <!-- .............. TEMPLATE .............................................. -->

                    <div class="panel panel-default" ng-repeat="recipe in $root.recipes | orderBy:'name'">

                        <div class="panel-heading">
                            <h5>
                                <a href="" data-toggle="collapse" data-parent="#recipe-list-accordion" data-target="#panel-recipe-collapse{{recipe.id}}"><i class="fa fa-puzzle-piece fa-fw"></i>{{recipe.name}}</a>
                                <i class="fa fa-users fa-lg public-account-info pull-right" style="padding-right: 5px" ng-show="recipe.public"></i>
                            </h5>
                        </div>
                        <div id="panel-recipe-collapse{{recipe.id}}" class="panel-collapse collapse">

                            <p class="btn-row-over-panel" ng-if="isWriteScope('recipes', userDetails.groups)">
                                <a href="" class="btn btn-danger" role="button" ng-click="deleteRecipe(recipe)">
                                    <i class="fa fa-times fa-fw"></i><span> delete</span>
                                </a>
                            </p>

                            <div class="panel-body">
                                <div ng-include src="'tags/recipe/recipelist.tag'"></div>
                            </div>

                        </div>
                    </div>
                    <!-- .panel -->
                </div>
            </div>
            <!-- .panel-body -->
        </div>
        <!-- .panel-collapse -->
    </div>
    <!-- .panel -->
</div>