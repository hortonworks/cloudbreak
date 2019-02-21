package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.VIEW_DEFINITIONS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class AmbariViewProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariViewProvider.class);

    public Cluster provideViewInformation(AmbariClient ambariClient, Cluster cluster) {
        try {
            LOGGER.debug("Provide view definitions.");
            List<String> viewDefinitions = (List<String>) ambariClient.getViewDefinitions();

            Map<String, Object> obj = new Json(cluster.getAttributes()).getMap();
            if (obj == null || obj.isEmpty()) {
                obj = new HashMap<>();
            }
            obj.put(VIEW_DEFINITIONS.name(), viewDefinitions);
            cluster.setAttributes((new Json(obj)).getValue());
            return cluster;
        } catch (Exception e) {
            LOGGER.debug("Failed to provide view definitions.", e);
        }
        return cluster;
    }

}
