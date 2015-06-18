<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstackclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="openstackclusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="openstackclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="openstackclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
      <label class="col-sm-3 control-label" for="openstackinstanceType">{{msg.template_form_instance_type_label}}</label>

      <div class="col-sm-9">
        <p id="openstackinstanceType" class="form-control-static">{{template.parameters.instanceType}}</p>
      </div>
      <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstackvolumecount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <p id="openstackvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="openstackvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <p id="openstackvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
</form>
