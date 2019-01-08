package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class RdsConfigValidator {

    @Inject
    private RdsConfigService rdsConfigService;

    public void validateRdsConfigs(ClusterRequest request, User user, Workspace workspace) {
        Map<String, Integer> typeCountMap = new HashMap<>();
        Set<String> multipleTypes = new HashSet<>();
        if (request.getRdsConfigIds() != null) {
            for (Long rdsConfigId : request.getRdsConfigIds()) {
                RDSConfig rdsConfig = rdsConfigService.get(rdsConfigId);
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (request.getRdsConfigNames() != null) {
            for (String rdsConfigName : request.getRdsConfigNames()) {
                RDSConfig rdsConfig = rdsConfigService.getByNameForWorkspace(rdsConfigName, workspace);
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (request.getRdsConfigJsons() != null) {
            for (DatabaseV4Base rdsConfig : request.getRdsConfigJsons()) {
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (request.getAmbariDatabaseDetails() != null) {
            increaseCount(DatabaseType.AMBARI.name(), typeCountMap, multipleTypes);
        }
        if (!multipleTypes.isEmpty()) {
            throw new BadRequestException("Mutliple Rds are defined for the following types: "
                    + multipleTypes.stream().collect(Collectors.joining(",")));
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
