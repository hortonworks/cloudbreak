package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.ClusterEndpoint;
import com.sequenceiq.periscope.api.model.AmbariJson;
import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.rest.converter.AmbariConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.service.AuthenticatedUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.ClusterSecurityService;

@Component
public class ClusterController implements ClusterEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private AmbariConverter ambariConverter;

    @Autowired
    private ClusterConverter clusterConverter;

    @Autowired
    private ClusterSecurityService clusterSecurityService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public ClusterJson addCluster(AmbariJson ambariServer) {
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildUserMdcContext(user);
        return setCluster(user, ambariServer, null);
    }

    @Override
    public ClusterJson modifyCluster(AmbariJson ambariServer, Long clusterId) {
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
        return createClusterJsonResponse(clusterService.setState(clusterId, stateJson.getState()));
    }

    private ClusterJson createClusterJsonResponse(Cluster cluster) {
        return clusterConverter.convert(cluster);
    }

    private ClusterJson setCluster(PeriscopeUser user, AmbariJson json, Long clusterId) {
        Ambari ambari = ambariConverter.convert(json);
        boolean access = clusterSecurityService.hasAccess(user, ambari, json.getStackId());
        if (!access) {
            String host = ambari.getHost();
            LOGGER.info("Illegal access to Ambari cluster '{}' from user '{}'", host, user.getEmail());
            throw new AccessDeniedException(String.format("Accessing Ambari cluster '%s' is not allowed", host));
        } else {
            if (json.getStackId() != null && ClusterState.PENDING.equals(json.getClusterState())) {
                AmbariStack ambariStack = new AmbariStack(ambari, json.getStackId(), null);
                return createClusterJsonResponse(clusterService.create(user, ambariStack, json.getClusterState()));
            }
            AmbariStack resolvedAmbari = clusterSecurityService.tryResolve(ambari);
            if (clusterId == null) {
                return createClusterJsonResponse(clusterService.create(user, resolvedAmbari, null));
            } else {
                return createClusterJsonResponse(clusterService.update(clusterId, resolvedAmbari));
            }
        }
    }

}
