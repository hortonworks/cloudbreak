package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;

@Service
public class AmbariViewProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariViewProvider.class);
    private static final String VIEW_DEFINITIONS = "viewDefinitions";

    @Inject
    private ClusterRepository clusterRepository;

    public Cluster provideViewInformation(AmbariClient ambariClient, Cluster cluster) {
        try {
            LOGGER.info("Provide view definitions.");
            Map<String, String> viewDefinitions = (Map<String, String>) ambariClient.getViewDefinitions();

            Map<String, Object> obj = new HashMap<>();
            obj.put(VIEW_DEFINITIONS, viewDefinitions);
            cluster.setAttributes(new Json(obj));
            return clusterRepository.save(cluster);
        } catch (Exception e) {
            LOGGER.error("Provide view definitions is failed.", e);
        }
        return cluster;
    }

    public boolean isViewDefinitionNotProvided(Cluster cluster) {
        return ((Map<String, Object>) cluster.getAttributes().getMap().get(VIEW_DEFINITIONS)).entrySet().isEmpty();
    }

}
