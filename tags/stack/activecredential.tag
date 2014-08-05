<form class="form-horizontal" role="document"><!-- role: 'document' - non-editable "form" -->

    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="cred_name">Name</label>

        <div class="col-sm-9">
            <p id="cred_name" class="form-control-static">{{activeStack.credential.name}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <div class="form-group">
        <label class="col-sm-3 control-label" for="cred_desc">Description</label>

        <div class="col-sm-9">
            <p id="cred_desc" class="form-control-static">{{activeStack.credential.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <!-- .form-group -->
    <div class="form-group">
        <label class="col-sm-3 control-label" for="cred_cloudPlatform">Cloud platform</label>

        <div class="col-sm-9">
            <p id="cred_cloudPlatform" class="form-control-static">{{activeStack.credential.cloudPlatform}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>


</form>