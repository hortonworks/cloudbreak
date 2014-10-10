<div ng-controller="clusterController"><!-- this slide is active at start -->

    <div class="row cluster-block collapse in">
        <div class="isotope-wrapper">

            <div class="cluster" id="cluster-{{cluster.id}}" ng-repeat="cluster in $root.clusters">
                <h4>
                    <a href="" class="btn btn-cluster btn-block" role="button" ng-click="changeActiveCluster(cluster.id)">{{cluster.name}}<i class="fa fa-angle-right fa-25x"></i></a>
                </h4>
                <dl class="mod-uptime">
                    <dt>uptime</dt>
                    <dd class="big-numeral">{{cluster.hoursUp}}<sup>h</sup>{{cluster.minutesUp}}<sup>m</sup></dd>
                </dl>
                <dl class="mod-nodes">
                    <dt>nodes</dt>
                    <dd class="big-numeral">{{cluster.nodeCount}}</dd>
                </dl>
                <!--TODO-->
                <div class="mod-LED">
                    <span ng-class="ledStyles[cluster.status]" title="titleStatus[cluster.status]"></span>
                </div>
                <a href="" class="mod-start-stop btn btn-cluster btn-block" role="button" ng-click="requestStatusChange(cluster)"><i class="fa fa-lg" ng-class="buttonStyles[cluster.status]"></i></a>
            </div>

        </div>
        <!-- .isotope-wrapper -->
    </div>
    <!-- .cluster-block -->

</div>
