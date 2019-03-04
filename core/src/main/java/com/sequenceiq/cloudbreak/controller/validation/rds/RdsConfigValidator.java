package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class RdsConfigValidator {

    @Inject
    private RdsConfigService rdsConfigService;

    public void validateRdsConfigs(ClusterV4Request request, User user, Workspace workspace) {
        Map<String, Integer> typeCountMap = new HashMap<>();
        Set<String> multipleTypes = new HashSet<>();
        if (request.getDatabases() != null) {
            for (String rdsConfigName : request.getDatabases()) {
                RDSConfig rdsConfig = rdsConfigService.getByNameForWorkspace(rdsConfigName, workspace);
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (!multipleTypes.isEmpty()) {
            throw new BadRequestException("Mutliple Rds are defined for the following types: "
                    + String.join(",", multipleTypes));
        }
    }

    private void increaseCount(String type, Map<String, Integer> typeCountMap, Set<String> multipleTypes) {
        Integer count = typeCountMap.get(type);
        count = count == null ? 1 : count + 1;
        typeCountMap.put(type, count);
        if (count > 1) {
            multipleTypes.add(type);
        }
    }
}
