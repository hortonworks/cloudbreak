<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{recipe.name}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="recipe.recipeType">
        <label class="col-sm-3 control-label" for="recipetypefield">{{msg.recipe_type_label}}</label>
        <div class="col-sm-9">
            <p id="recipetypefield" class="form-control-static">{{recipe.recipeType}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="recipe.description">
        <label class="col-sm-3 control-label" for="recipedescriptionfield">{{msg.description_label}}</label>
        <div class="col-sm-9">
            <p id="recipedescriptionfield" class="form-control-static">{{recipe.description}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="recipe.content">
        <label class="col-sm-3 control-label" for="recipecontentfield">{{msg.content_label}}</label>
        <div class="col-sm-9">
            <textarea disabled style="width: 100%; height: 200px;">{{recipe.decodedContent}}</textarea>
        </div>
    </div>

</form>