package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.rest.converter.AmbariConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.AmbariJson;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.StateJson;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.ClusterSecurityService;

@RestController
@RequestMapping("/clusters")
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AmbariConverter ambariConverter;
    @Autowired
    private ClusterConverter clusterConverter;
    @Autowired
    private ClusterSecurityService clusterSecurityService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> addCluster(@ModelAttribute("user") PeriscopeUser user, @RequestBody AmbariJson ambariServer) {
        MDCBuilder.buildUserMdcContext(user);
        return setCluster(user, ambariServer, null);
    }

    @RequestMapping(value = "/{clusterId}", method = RequestMethod.PUT)
    public ResponseEntity<ClusterJson> modifyCluster(@ModelAttribute("user") PeriscopeUser user,
            @RequestBody AmbariJson ambariServer, @PathVariable long clusterId) {
        MDCBuilder.buildMdcContext(user, clusterId);
        return setCluster(user, ambariServer, clusterId);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ClusterJson>> getClusters(@ModelAttribute("user") PeriscopeUser user) {
        MDCBuilder.buildUserMdcContext(user);
        List<Cluster> clusters = clusterService.findAllByUser(user);
        return new ResponseEntity<>(clusterConverter.convertAllToJson(clusters), HttpStatus.OK);
    }

    @RequestMapping(value = "/{clusterId}", method = RequestMethod.GET)
    public ResponseEntity<ClusterJson> getCluster(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId) {
        MDCBuilder.buildMdcContext(user, clusterId);
        return createClusterJsonResponse(clusterService.findOneByUser(clusterId));
    }

    @RequestMapping(value = "/{clusterId}", method = RequestMethod.DELETE)
    public ResponseEntity<ClusterJson> deleteCluster(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId) {
        MDCBuilder.buildMdcContext(user, clusterId);
        clusterService.remove(clusterId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/{clusterId}/state", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> setState(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId, @RequestBody StateJson stateJson) {
        MDCBuilder.buildMdcContext(user, clusterId);
        return createClusterJsonResponse(clusterService.setState(clusterId, stateJson.getState()));
    }

    private ResponseEntity<ClusterJson> createClusterJsonResponse(Cluster cluster) {
        return createClusterJsonResponse(cluster, HttpStatus.OK);
    }

    private ResponseEntity<ClusterJson> createClusterJsonResponse(Cluster cluster, HttpStatus status) {
        return new ResponseEntity<>(clusterConverter.convert(cluster), status);
    }

    private ResponseEntity<ClusterJson> setCluster(PeriscopeUser user, AmbariJson json, Long clusterId) {
        Ambari ambari = ambariConverter.convert(json);
        boolean access = clusterSecurityService.hasAccess(user, ambari);
        if (!access) {
            String host = ambari.getHost();
            LOGGER.info("Illegal access to Ambari cluster '{}' from user '{}'", host, user.getEmail());
            throw new AccessDeniedException(String.format("Accessing Ambari cluster '%s' is not allowed", host));
        } else {
            AmbariStack resolvedAmbari = clusterSecurityService.tryResolve(ambari);
            if (clusterId == null) {
                return createClusterJsonResponse(clusterService.create(user, resolvedAmbari), HttpStatus.CREATED);
            } else {
                return createClusterJsonResponse(clusterService.update(clusterId, resolvedAmbari), HttpStatus.OK);
            }
        }
    }

}
