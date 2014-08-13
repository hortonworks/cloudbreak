<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="clusterName">name</label>

        <div class="col-sm-9">
            <p id="clusterName" class="form-control-static">{{activeStack.template.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="azureclusterDescription">Description</label>

        <div class="col-sm-9">
            <p id="azureclusterDescription" class="form-control-static">{{activeStack.template.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="vmType">VM type</label>

        <div class="col-sm-9">
            <p id="vmType" class="form-control-static">{{activeStack.template.parameters.vmType}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="imageName">Image name</label>

        <div class="col-sm-9">
            <p id="imageName" class="form-control-static">{{activeStack.template.parameters.imageName}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="location">Location</label>

        <div class="col-sm-9">
            <p id="location" class="form-control-static">{{activeStack.template.parameters.location}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="volcount">Volume count</label>

        <div class="col-sm-9">
            <p id="volcount" class="form-control-static">{{activeStack.template.volumeCount}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="volsize">Volume size</label>

        <div class="col-sm-9">
            <p id="volsize" class="form-control-static">{{activeStack.template.volumeSize}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>


</form>