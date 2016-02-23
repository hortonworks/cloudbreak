<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{recipe.name}}</p>
        </div>
    </div>

    <div class="form-group" ng-show="recipe.description">
        <label class="col-sm-3 control-label" for="recipedescriptionfield">{{msg.description_label}}</label>
        <div class="col-sm-9">
            <p id="recipedescriptionfield" class="form-control-static">{{recipe.description}}</p>
        </div>
    </div>

    <div class="form-group" ng-hide="isEmpty(recipe.properties)">
        <label class="col-sm-3 control-label" for="recipeproperties">{{msg.properties_label}}</label>
        <div class="col-sm-9">
            <table id="recipeproperties" class="table table-report table-sortable-cols table-with-pagination table-condensed form-control-static" style="table-layout: fixed; margin-top: 0px;background-color: transparent;">
                <thead>
                    <tr>
                        <th class="text-center" style='width: 50%'>{{msg.name_label}}</th>
                        <th class="text-center">{{msg.value_label}}</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="(key, value) in recipe.properties | orderBy:'key'">
                        <td class="text-center" style="overflow: auto;">{{key}}</td>
                        <td class="text-center" style="overflow: auto;"><span class="label label-default">{{value}}</span></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="form-group" ng-show="recipe.plugins">
        <label class="col-sm-3 control-label" for="recipeplugins">{{msg.plugins_label}}</label>
        <div class="col-sm-7" style="width: 75%">
            <table id="recipeplugins" class="table table-report table-sortable-cols table-with-pagination table-condensed form-control-static" style="margin-top: 0px;background-color: transparent;">
                <thead>
                    <tr>
                        <th class="text-center">{{msg.content_label}}</th>
                        <th class="text-center">{{msg.exec_type_label}}</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="(key, value) in recipe.plugins">
                        <td data-title="'pluginkey'" class="col-md-4 text-center">
                            <ul ng-if="recipe.pluginContents[key]" class="nav">
                                <li ng-repeat="(file, content) in recipe.pluginContents[key]">
                                    {{file}}
                                    <br />
                                    <textarea disabled style="width: 100%; height: 200px;">{{content}}</textarea>
                                </li>
                            </ul>
                            <a ng-if="!recipe.pluginContents[key]" href="{{key}}" target="_blank">{{key}}</a>
                        </td>
                        <td data-title="'pluginValue'" class="col-md-3 text-center" style="width: 1%;">{{value}}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

</form>