<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclclusterName">Name</label>

        <div class="col-sm-9">
            <p id="gcclclusterName" class="form-control-static">{{template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclclusterDesc">Description</label>

        <div class="col-sm-9">
            <p id="gcclclusterDesc" class="form-control-static">{{template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclgccZone">Zone</label>

        <div class="col-sm-9">
            <p id="gcclgccZone" class="form-control-static">{{gccRegions[template.parameters.gccZone]}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclgccInstanceType">Instance Type</label>

        <div class="col-sm-9">
            <p id="gcclgccInstanceType" class="form-control-static">{{gccInstanceTypes[template.parameters.gccInstanceType]}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="gccvolumecount">Attached volumes per instance</label>

        <div class="col-sm-9">
            <p id="gccvolumecount" class="form-control-static">{{template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gccvolumesize">Volume size (GB)</label>

        <div class="col-sm-9">
            <p id="gccvolumesize" class="form-control-static">{{template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="gcclgccVolumeType">VolumeType</label>

        <div class="col-sm-9">
            <p id="gcclgccVolumeType" class="form-control-static">{{gccDiskTypes[template.parameters.volumeType]}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

</form>