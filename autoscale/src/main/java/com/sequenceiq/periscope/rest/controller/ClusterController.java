package com.sequenceiq.periscope.rest.controller;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.periscope.api.endpoint.ClusterEndpoint;
import com.sequenceiq.periscope.api.model.ClusterAutoscaleState;
import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.api.model.ClusterRequestJson;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.rest.converter.AmbariConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.converter.ClusterRequestConverter;
import com.sequenceiq.periscope.service.AuthenticatedUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.ClusterSecurityService;

@Component
public class ClusterController implements ClusterEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariConverter ambariConverter;

    @Inject
    private ClusterConverter clusterConverter;

    @Inject
    private ClusterSecurityService clusterSecurityService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private ClusterRequestConverter clusterRequestConverter;

    @Override
    public ClusterJson addCluster(ClusterRequestJson ambariServer) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildUserMdcContext(user);
        return setCluster(user, ambariServer, null);
    }

    @Override
    public ClusterJson modifyCluster(ClusterRequestJson ambariServer, Long clusterId) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        return setCluster(user, ambariServer, clusterId);
    }

    @Override
    public List<ClusterJson> getClusters() {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildUserMdcContext(user);
        List<Cluster> clusters = clusterService.findAllByUser(user);
        return clusterConverter.convertAllToJson(clusters);
    }

    @Override
    public ClusterJson getCluster(Long clusterId) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        return createClusterJsonResponse(clusterService.findOneById(clusterId));
    }

    @Override
    public void deleteCluster(Long clusterId) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        clusterService.removeOne(clusterId);
    }

    @Override
    public ClusterJson setState(Long clusterId, StateJson stateJson) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        Cluster cluster = clusterService.setState(clusterId, stateJson.getState());
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    @Override
    public ClusterJson setAutoscaleState(Long clusterId, ClusterAutoscaleState autoscaleState) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        Cluster cluster = clusterService.setAutoscaleState(clusterId, autoscaleState.isEnableAutoscaling());
        createHistoryAndNotification(cluster);
        return createClusterJsonResponse(cluster);
    }

    private ClusterJson createClusterJsonResponse(Cluster cluster) {
        return clusterConverter.convert(cluster);
    }

    private ClusterJson setCluster(PeriscopeUser user, ClusterRequestJson json, Long clusterId) {
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
                    cluster = clusterService.create(cluster, user, resolvedAmbari, RUNNING);
                } else {
                    cluster = clusterService.update(clusterId, resolvedAmbari, cluster.isAutoscalingEnabled());
                }
            }
            createHistoryAndNotification(cluster);
            return createClusterJsonResponse(cluster);
        }
    }

    private boolean hasAmbariConnectionDetailsSpecified(ClusterRequestJson json) {
        return !StringUtils.isEmpty(json.getHost())
                && !StringUtils.isEmpty(json.getPort());
    }

    private void createHistoryAndNotification(Cluster cluster) {
        History history;
        if (cluster.isAutoscalingEnabled()) {
            history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
        } else {
            history = historyService.createEntry(ScalingStatus.DISABLED, "Autoscaling has been disabled for the cluster.", 0, cluster);
        }
        notificationSender.send(history);
    }

}
