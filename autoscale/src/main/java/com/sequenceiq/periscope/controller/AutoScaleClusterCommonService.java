package com.sequenceiq.periscope.controller;


import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.converter.AmbariConverter;
import com.sequenceiq.periscope.converter.ClusterConverter;
import com.sequenceiq.periscope.converter.ClusterRequestConverter;
import com.sequenceiq.periscope.service.AuthenticatedUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.ClusterSecurityService;

@Component
public class AutoScaleClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterCommonService.class);

    @Inject
    private ClusterConverter clusterConverter;

    @Inject
    private ClusterSecurityService clusterSecurityService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

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
    private TransactionService transactionService;

    public AutoscaleClusterResponse addCluster(AutoscaleClusterRequest ambariServer) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildUserMdcContext(user);
        return setCluster(user, ambariServer, null);
    }

    public AutoscaleClusterResponse modifyCluster(AutoscaleClusterRequest ambariServer, Long clusterId) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        return setCluster(user, ambariServer, clusterId);
    }

    public List<AutoscaleClusterResponse> getClusters() {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildUserMdcContext(user);
        List<Cluster> clusters = clusterService.findAllByUser(user);
        return clusterConverter.convertAllToJson(clusters);
    }

    public AutoscaleClusterResponse getCluster(Long clusterId) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        try {
            return transactionService.required(() -> createClusterJsonResponse(clusterService.findById(clusterId)));
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }
    }

    public void deleteCluster(Long clusterId) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        clusterService.removeById(clusterId);
    }

    public AutoscaleClusterResponse setState(Long clusterId, StateJson stateJson) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        Cluster cluster = clusterService.setState(clusterId, stateJson.getState());
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    public AutoscaleClusterResponse setAutoscaleState(Long clusterId, AutoscaleClusterState autoscaleState) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        Cluster cluster = clusterService.setAutoscaleState(clusterId, autoscaleState.isEnableAutoscaling());
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    private AutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return clusterConverter.convert(cluster);
    }

    private AutoscaleClusterResponse setCluster(PeriscopeUser user, AutoscaleClusterRequest json, Long clusterId) {
        Ambari ambari = ambariConverter.convert(json);
        Long stackId = json.getStackId();
        boolean access = clusterSecurityService.hasAccess(user, ambari, stackId);
        if (!access) {
            String host = ambari.getHost();
            LOGGER.info("Illegal access to Ambari cluster '{}' from user '{}'", host, user.getEmail());
            throw new AccessDeniedException(String.format("Accessing Ambari cluster '%s' is not allowed", host));
        } else {
            Cluster cluster = clusterRequestConverter.convert(json);
            if (!hasAmbariConnectionDetailsSpecified(json)) {
                AmbariStack ambariStack = new AmbariStack(ambari, stackId, null);
                cluster = clusterService.create(cluster, user, ambariStack, PENDING);
            } else {
                AmbariStack resolvedAmbari = clusterSecurityService.tryResolve(ambari);
                if (clusterId == null) {
                    LOGGER.info("Creating cluster as clusterId is null for user [id: {}]: {}", user.getId(), cluster);
                    clusterService.create(cluster, user, resolvedAmbari, RUNNING);
                } else {
                    LOGGER.info("Updating cluster [id: {}] for user [id: {}]: {}", clusterId, user.getId(), cluster);
                    clusterService.update(clusterId, resolvedAmbari, cluster.isAutoscalingEnabled());
                }
            }
            createHistoryAndNotification(cluster);
            return createClusterJsonResponse(cluster);
        }
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
        notificationSender.send(history);
    }
}
