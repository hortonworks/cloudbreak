package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.VIEW_DEFINITIONS;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;

@Service
public class AmbariViewProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariViewProvider.class);

    @Inject
    private ClusterRepository clusterRepository;

    public Cluster provideViewInformation(AmbariClient ambariClient, Cluster cluster) {
        try {
            LOGGER.info("Provide view definitions.");
            List<String> viewDefinitions = (List<String>) ambariClient.getViewDefinitions();

            Map<String, Object> obj = cluster.getAttributes().getMap();
            if (obj == null || obj.isEmpty()) {
                obj = new HashMap<>();
            }
            obj.put(VIEW_DEFINITIONS.name(), viewDefinitions);
            cluster.setAttributes(new Json(obj));
            return clusterRepository.save(cluster);
        } catch (Exception e) {
            LOGGER.warn("Failed to provide view definitions.", e);
        }
        return cluster;
    }

    public boolean isViewDefinitionNotProvided(Cluster cluster) {
        Object viewDefinitions = cluster.getAttributes().getMap().get(VIEW_DEFINITIONS.name());
        Object legacyViewDefinitions = cluster.getAttributes().getMap().get("viewDefinitions");

        if (legacyViewDefinitions != null && (viewDefinitions == null || ((Collection<String>) viewDefinitions).isEmpty())) {
            Map<String, Object> obj = cluster.getAttributes().getMap();
            if (obj == null || obj.isEmpty()) {
                obj = new HashMap<>();
            }
            obj.put(VIEW_DEFINITIONS.name(), legacyViewDefinitions);
            try {
                cluster.setAttributes(new Json(obj));
            } catch (JsonProcessingException e) {
                return true;
            }
            cluster = clusterRepository.save(cluster);
            viewDefinitions = cluster.getAttributes().getMap().get(VIEW_DEFINITIONS.name());
            return ((Collection<String>) viewDefinitions).isEmpty();
        }
        if (viewDefinitions != null) {
            return ((Collection<String>) viewDefinitions).isEmpty();
        }
        return true;
    }

}
