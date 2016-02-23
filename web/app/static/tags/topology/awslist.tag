<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="awsclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="awsclusterName" class="form-control-static">{{topology.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="topology.description">
        <label class="col-sm-3 control-label" for="awsclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="awsclusterDesc" class="form-control-static">{{topology.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>