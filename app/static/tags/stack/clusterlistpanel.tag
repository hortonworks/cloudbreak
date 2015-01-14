<div><!-- this slide is active at start -->

    <div class="row cluster-block collapse in">
        <div class="isotope-wrapper">

            <div class="cluster" id="cluster-{{cluster.id}}" ng-repeat="cluster in $root.clusters">
                <h4>
                    <a href="" class="btn btn-cluster btn-block" role="button" ng-click="changeActiveCluster(cluster.id)">{{cluster.name}}<i class="fa fa-angle-right fa-25x"></i></a>
                </h4>
                <dl class="row" style="padding-bottom: 4px;">
                  <div class="col-md-4">
                    <h6 style="margin: 0px;">
                      <span class="label label-info">{{cluster.cloudPlatform}}</span>
                    </h6>
                  </div>
                  <div class="col-md-4">
                    <h4>
                      <i class="fa fa-users fa-md public-account-info" ng-show="cluster.public"></i>
                    </h4>
                  </div>
                </dl>
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
                <a href="" ng-disabled="cluster.cloudPlatform === 'GCC' || !(cluster.status === 'STOPPED' || cluster.status === 'AVAILABLE')"
                    ng-click="selectCluster(cluster)" class="mod-start-stop btn btn-cluster btn-block" role="button" data-toggle="modal" data-target="#modal-cluster-status-change"><i class="fa fa-lg" ng-class="buttonStyles[cluster.status]"></i></a>
            </div>

        </div>
        <!-- .isotope-wrapper -->
    </div>
    <!-- .cluster-block -->

</div>

<div class="modal fade" id="modal-cluster-status-change" tabindex="-1" role="dialog" aria-labelledby="modal01-title" aria-hidden="true">
    <div class="modal-dialog modal-sm">
       <div class="modal-content">
          <!-- .modal-header -->
          <div class="modal-body">
               <div ng-show="selectedCluster.status === 'STOPPED'"><p>Start cluster <strong>{{selectedCluster.name}}</strong> and its stack?</p></div>
               <div ng-show="selectedCluster.status === 'AVAILABLE'"><p>Stop cluster <strong>{{selectedCluster.name}}</strong> and its stack?</p></div>
          </div>
          <div class="modal-footer">
             <div class="row">
                 <div class="col-xs-6">
                    <button type="button" class="btn btn-block btn-default" data-dismiss="modal">cancel</button>
                 </div>
                 <div class="col-xs-6">
                    <div ng-show="selectedCluster.status === 'STOPPED'"><button ng-show="selectedCluster.status === 'STOPPED'" type="button" class="btn btn-block btn-success" data-dismiss="modal" id="reqStatChangeBtn" ng-click="requestStatusChange(selectedCluster)"><i class="fa fa-play fa-fw"></i>start</button></div>
                    <div ng-show="selectedCluster.status === 'AVAILABLE'"><button ng-show="selectedCluster.status === 'AVAILABLE'" type="button" class="btn btn-block btn-warning" data-dismiss="modal" id="reqStatChangeBtn" ng-click="requestStatusChange(selectedCluster)"><i class="fa fa-pause fa-fw"></i>stop</button></div>
                 </div>
             </div>
          </div>
       </div>
    </div>
</div>
