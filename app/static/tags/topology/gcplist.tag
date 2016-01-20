<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="gcpclusterName" class="form-control-static">{{topology.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="topology.description">
        <label class="col-sm-3 control-label" for="gcpclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="gcpclusterDesc" class="form-control-static">{{topology.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>