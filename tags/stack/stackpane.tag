<h4><a href="#" class="btn btn-cluster btn-block" role="button" ng-click="changeActiveStack(stack.id)">{{stack.name}}<i
        class="fa fa-angle-right fa-25x"></i></a></h4>
<dl class="mod-uptime">
    <dt>uptime</dt>
    <dd class="big-numeral">{{stack.cluster.hoursUp}}<sup>h</sup>{{stack.cluster.minutesUp}}<sup>m</sup></dd>
</dl>
<dl class="mod-nodes">
    <dt>nodes</dt>
    <dd class="big-numeral">{{stack.nodeCount}}</dd>
</dl>
<!--TODO-->
<div class="mod-LED">
    <span ng-class="getLedStyle(stack.status)" title="{{getTitleStatus(stack.status)}}"></span>
</div>
<a href="#" class="mod-start-stop btn btn-cluster btn-block" role="button" title="stop cluster"><i class="fa fa-lg" ng-class="getButtonStyle(stack.status)"></i></a>