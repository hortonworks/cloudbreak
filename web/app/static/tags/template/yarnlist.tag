<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="yarnclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="yarnclusterName" class="form-control-static">{{constraint.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="constraint.description">
        <label class="col-sm-3 control-label" for="yarnclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="yarnclusterDesc" class="form-control-static">{{constraint.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="yarncpu">{{msg.template_form_cpu_label}}</label>

        <div class="col-sm-9">
            <p id="yarncpu" class="form-control-static">{{constraint.cpu}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="yarnmemory">{{msg.template_form_memory_label}}</label>

        <div class="col-sm-9">
            <p id="yarnmemory" class="form-control-static">{{constraint.memory}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!--<div class="form-group">
        <label class="col-sm-3 control-label" for="yarndisk">{{msg.template_form_disk_label}}</label>

        <div class="col-sm-9">
            <p id="yarndisk" class="form-control-static">{{constraint.disk}}</p>
        </div>
    </div>-->

</form>
