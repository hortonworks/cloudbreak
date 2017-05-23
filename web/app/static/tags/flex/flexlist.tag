<form class="form-horizontal" role="document">
    <!-- role: 'document' - non-editable "form" -->

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">{{msg.name_label}}</label>
        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{flex.name}}</p>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="flexsubscriptionid">{{msg.flex_subscriptionid_label}}</label>
        <div class="col-sm-9">
            <p id="flexsubscriptionid" class="form-control-static">{{flex.subscriptionId}}</p>
        </div>
    </div>

</form>
