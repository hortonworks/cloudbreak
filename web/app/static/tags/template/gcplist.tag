<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplclusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="gcplclusterName" class="form-control-static">{{template.name}}</p>
        </div>
    </div>
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="gcplclusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="gcplclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplgcpInstanceType">{{msg.template_form_instance_type_label}}</label>

        <div class="col-sm-9">
            <p id="gcplgcpInstanceType" class="form-control-static">{{template.instanceType}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpvolumecount">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <p id="gcpvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcpvolumesize">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <p id="gcpvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcplgcpVolumeType">{{msg.template_form_volume_type_label}}</label>

        <div class="col-sm-9">
            <p id="gcplgcpVolumeType" class="form-control-static">{{$root.displayNames.getDisk('GCP', template.volumeType)}}</p>
        </div>
    </div>
    <div class="form-group" ng-show="template.topologyId">
        <label class="col-sm-3 control-label" for="aws-topology">{{msg.template_form_topology_label}}</label>

        <div class="col-sm-9">
            <p id="aws-topology" class="form-control-static">{{getTopologyNameById(template.topologyId)}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.parameters.preemptible">
        <label class="col-sm-3 control-label" for="preemptible">{{msg.preemptible_label}}</label>

        <div class="col-sm-9">
            <i id="preemptible" class="form-control-static fa fa-check-circle fa-5" style="color: #4cb84c;"></i>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>