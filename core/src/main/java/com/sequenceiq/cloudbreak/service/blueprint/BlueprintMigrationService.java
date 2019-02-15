package com.sequenceiq.cloudbreak.service.blueprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintMigrationService {
    private static final String UNKNOWN = "UNKNOWN";

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintUtils blueprintUtils;

    public void migrateBlueprints() {
        Iterable<Blueprint> blueprints = blueprintRepository.findAll();
        List<Blueprint> updatedBlueprints = new ArrayList<>();
        for (Blueprint bp : blueprints) {
            if (StringUtils.isEmpty(bp.getStackType()) || StringUtils.isEmpty(bp.getStackVersion())) {
                try {
                    JsonNode root = JsonUtil.readTree(bp.getBlueprintText());
                    String stackName = blueprintUtils.getBlueprintStackName(root);
                    bp.setStackType(StringUtils.isEmpty(stackName) ? UNKNOWN : stackName);
                    String stackVersion = blueprintUtils.getBlueprintStackVersion(root);
                    bp.setStackVersion(StringUtils.isEmpty(stackVersion) ? UNKNOWN : stackVersion);
                } catch (IOException ex) {
                    bp.setStackType(UNKNOWN);
                    bp.setStackVersion(UNKNOWN);
                }
                updatedBlueprints.add(bp);
            }
        }
        if (!updatedBlueprints.isEmpty()) {
            blueprintRepository.saveAll(updatedBlueprints);
        }
    }
}
