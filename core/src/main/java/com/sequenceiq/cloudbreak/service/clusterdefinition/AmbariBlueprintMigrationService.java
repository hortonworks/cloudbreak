package com.sequenceiq.cloudbreak.service.clusterdefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.repository.ClusterDefinitionRepository;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class AmbariBlueprintMigrationService {
    private static final String UNKNOWN = "UNKNOWN";

    @Inject
    private ClusterDefinitionRepository clusterDefinitionRepository;

    @Inject
    private AmbariBlueprintUtils ambariBlueprintUtils;

    public void migrateBlueprints() {
        Iterable<ClusterDefinition> blueprints = clusterDefinitionRepository.findAll();
        List<ClusterDefinition> updatedClusterDefinitions = new ArrayList<>();
        for (ClusterDefinition bp : blueprints) {
            if (StringUtils.isEmpty(bp.getStackType()) || StringUtils.isEmpty(bp.getStackVersion())) {
                try {
                    JsonNode root = JsonUtil.readTree(bp.getClusterDefinitionText());
                    String stackName = ambariBlueprintUtils.getBlueprintStackName(root);
                    bp.setStackType(StringUtils.isEmpty(stackName) ? UNKNOWN : stackName);
                    String stackVersion = ambariBlueprintUtils.getBlueprintStackVersion(root);
                    bp.setStackVersion(StringUtils.isEmpty(stackVersion) ? UNKNOWN : stackVersion);
                } catch (IOException ex) {
                    bp.setStackType(UNKNOWN);
                    bp.setStackVersion(UNKNOWN);
                }
                updatedClusterDefinitions.add(bp);
            }
        }
        if (!updatedClusterDefinitions.isEmpty()) {
            clusterDefinitionRepository.saveAll(updatedClusterDefinitions);
        }
    }
}
