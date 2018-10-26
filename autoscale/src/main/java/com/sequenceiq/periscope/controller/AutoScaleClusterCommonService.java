package com.sequenceiq.periscope.controller;


import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.converter.AmbariConverter;
import com.sequenceiq.periscope.converter.ClusterConverter;
import com.sequenceiq.periscope.converter.ClusterRequestConverter;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.AmbariUtilService;
import com.sequenceiq.periscope.service.security.CloudbreakAuthorizationService;

@Component
public class AutoScaleClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterCommonService.class);

    @Inject
    private ClusterConverter clusterConverter;

    @Inject
    private AmbariUtilService ambariUtilService;

    @Inject
    private CloudbreakAuthorizationService cloudbreakAuthorizationService;

    @Inject
    private ClusterRequestConverter clusterRequestConverter;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private AmbariConverter ambariConverter;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    public AutoscaleClusterResponse addCluster(AutoscaleClusterRequest ambariServer) {
        MDCBuilder.buildMdcContext(ambariServer.getStackId(), "", "CLUSTER");
        return setCluster(ambariServer, null);
    }

    public AutoscaleClusterResponse modifyCluster(AutoscaleClusterRequest ambariServer, Long clusterId) {
        MDCBuilder.buildMdcContext(clusterId, "", "CLUSTER");
        return setCluster(ambariServer, clusterId);
    }

    public List<AutoscaleClusterResponse> getClusters() {
        List<Cluster> clusters = clusterService.findAllByUser(restRequestThreadLocalService.getCloudbreakUser());
        return clusterConverter.convertAllToJson(clusters);
    }

    public AutoscaleClusterResponse getCluster(Long clusterId) {
        MDCBuilder.buildMdcContext(clusterId, "", "CLUSTER");
        return createClusterJsonResponse(clusterService.findById(clusterId));
    }

    public void deleteCluster(Long clusterId) {
        MDCBuilder.buildMdcContext(clusterId, "", "CLUSTER");
        clusterService.removeById(clusterId);
    }

    public AutoscaleClusterResponse setState(Long clusterId, StateJson stateJson) {
        MDCBuilder.buildMdcContext(clusterId, "", "CLUSTER");
        Cluster cluster = clusterService.setState(clusterId, stateJson.getState());
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    public AutoscaleClusterResponse setAutoscaleState(Long clusterId, AutoscaleClusterState autoscaleState) {
        MDCBuilder.buildMdcContext(clusterId, "", "CLUSTER");
        Cluster cluster = clusterService.setAutoscaleState(clusterId, autoscaleState.isEnableAutoscaling());
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    private AutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return clusterConverter.convert(cluster);
    }

    private AutoscaleClusterResponse setCluster(AutoscaleClusterRequest json, Long clusterId) {
        Ambari ambari = ambariConverter.convert(json);
        Long stackId = Optional.ofNullable(json.getStackId()).orElseGet(() -> ambariUtilService.getStackId(ambari));
        CloudbreakUser user = restRequestThreadLocalService.getCloudbreakUser();
        cloudbreakAuthorizationService.hasAccess(stackId, user.getUserId(), user.getTenant(), PermissionType.WRITE.name());
        Cluster cluster = clusterRequestConverter.convert(json);
        ClusterPertain clusterPertain = new ClusterPertain(user.getTenant(), json.getWorkspaceId(), user.getUserId());
        if (!hasAmbariConnectionDetailsSpecified(json)) {
            AmbariStack ambariStack = new AmbariStack(ambari, stackId, null);
            cluster = clusterService.create(cluster, clusterPertain, ambariStack, PENDING);
        } else {
            AmbariStack resolvedAmbari = ambariUtilService.tryResolve(ambari);
            cluster = clusterId == null ? clusterService.create(cluster, clusterPertain, resolvedAmbari, RUNNING)
                    : clusterService.update(clusterId, resolvedAmbari, cluster.isAutoscalingEnabled());
        }
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    private boolean hasAmbariConnectionDetailsSpecified(AutoscaleClusterRequest json) {
        return !StringUtils.isEmpty(json.getHost())
                && !StringUtils.isEmpty(json.getPort());
    }

    private void createHistoryAndNotification(Cluster cluster) {
        History history;
        history = cluster.isAutoscalingEnabled()
                ? historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster)
                : historyService.createEntry(ScalingStatus.DISABLED, "Autoscaling has been disabled for the cluster.", 0, cluster);
        notificationSender.send(cluster, history);
    }
}
