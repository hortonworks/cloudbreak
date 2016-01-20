<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="mesosclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="mesosclusterName" class="form-control-static">{{constraint.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="constraint.description">
        <label class="col-sm-3 control-label" for="mesosclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="mesosclusterDesc" class="form-control-static">{{constraint.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="mesoscpu">{{msg.template_form_cpu_label}}</label>

        <div class="col-sm-9">
            <p id="mesoscpu" class="form-control-static">{{constraint.cpu}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="mesosmemory">{{msg.template_form_memory_label}}</label>

        <div class="col-sm-9">
            <p id="mesosmemory" class="form-control-static">{{constraint.memory}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="mesosdisk">{{msg.template_form_disk_label}}</label>

        <div class="col-sm-9">
            <p id="mesosdisk" class="form-control-static">{{constraint.disk}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>