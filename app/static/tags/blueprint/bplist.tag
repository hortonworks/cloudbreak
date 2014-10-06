
<form class="form-horizontal" role="document">

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Blueprint name</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{blueprint.blueprintName}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="name">Description</label>

        <div class="col-sm-9">
            <p id="name" class="form-control-static">{{blueprint.description}}</p>
        </div>
        <!-- .col-sm-9 -->
    </div>

    <div class="form-group">
        <label class="col-sm-3 control-label" for="source">Source</label>

        <div class="col-sm-9">
           <pre id="source" class="form-control-static blueprint-source">
{{blueprint.ambariBlueprint | json}}
           </pre>
        </div>
        <!-- .col-sm-9 -->
    </div>
    <!-- .form-group -->

</form>