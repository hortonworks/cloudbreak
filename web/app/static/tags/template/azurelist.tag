<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="clusterName">{{msg.name_label}}</label>

        <div class="col-sm-9">
            <p id="clusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group" ng-show="template.description">
        <label class="col-sm-3 control-label" for="clusterDesc">{{msg.description_label}}</label>

        <div class="col-sm-9">
            <p id="clusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.parameters.password.length">
        <label class="col-sm-3 control-label" for="password">{{msg.template_azure_list_azure_password}}</label>

        <div class="col-sm-9">
            <p id="password" class="form-control-static">{{template.parameters.password}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="vmType">{{msg.template_azure_list_azure_vm_type}}</label>

        <div class="col-sm-9">
            <p id="vmType" class="form-control-static">{{template.instanceType}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azurevolumetype">{{msg.template_form_volume_type_label}}</label>

        <div class="col-sm-9">
            <p id="azurevolumetype" class="form-control-static">{{$root.displayNames.getDisk('AZURE', template.volumeType)}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azuremanaged">{{msg.template_form_managed}}</label>

        <div class="col-sm-9">
            <p id="azuremanaged" class="form-control-static">{{template.parameters.managedDisk ? template.parameters.managedDisk : false}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="location">{{msg.template_form_volume_count_label}}</label>

        <div class="col-sm-9">
            <p id="volcount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="location">{{msg.template_form_volume_size_label}}</label>

        <div class="col-sm-9">
            <p id="volsize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group" ng-show="template.topologyId">
        <label class="col-sm-3 control-label" for="aws-topology">{{msg.template_form_topology_label}}</label>

        <div class="col-sm-9">
            <p id="aws-topology" class="form-control-static">{{getTopologyNameById(template.topologyId)}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>