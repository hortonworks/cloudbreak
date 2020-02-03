package com.sequenceiq.cloudbreak.orchestrator.salt.grain;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class GrainUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrainUploader.class);

    @Inject
    private SaltCommandRunner saltCommandRunner;

    public void uploadGrains(Set<Node> allNodes, List<GrainProperties> grainPropertiesList, ExitCriteriaModel exitModel, SaltConnector sc,
            ExitCriteria exitCriteria) throws Exception {
        if (!grainPropertiesList.isEmpty()) {
            for (GrainProperties grainProperties : grainPropertiesList) {
                handleGrainProperties(allNodes, exitModel, sc, exitCriteria, grainProperties);
            }
        }
    }

    private void handleGrainProperties(Set<Node> allNodes, ExitCriteriaModel exitModel, SaltConnector sc, ExitCriteria exitCriteria, GrainProperties properties)
            throws Exception {
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = properties.getHostsPerGrainMap();
        for (Entry<Entry<String, String>, Collection<String>> grainKeyValuesForHosts : hostsPerGrainMap.entrySet()) {
            LOGGER.debug("upload grains [{}] to hosts {} ", grainKeyValuesForHosts.getKey(), grainKeyValuesForHosts.getValue());
            addGrainToHosts(allNodes, exitModel, sc, exitCriteria, grainKeyValuesForHosts);
        }
    }

    private void addGrainToHosts(Set<Node> allNodes, ExitCriteriaModel exitModel, SaltConnector sc, ExitCriteria exitCriteria,
            Entry<Entry<String, String>, Collection<String>> grainForHosts) throws Exception {
        saltCommandRunner.runSaltCommand(sc, new GrainAddRunner(new HashSet<>(grainForHosts.getValue()), allNodes, grainForHosts.getKey().getKey(),
                        grainForHosts.getKey().getValue()), exitModel, exitCriteria);
    }
}
