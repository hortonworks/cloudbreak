package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.rdsconfig.LegacyRdsConfigService;

@Component
public class RdsConfigValidator {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private LegacyRdsConfigService rdsConfigService;

    public void validateRdsConfigs(ClusterRequest request) {
        Map<String, Integer> typeCountMap = new HashMap<>();
        Set<String> multipleTypes = new HashSet<>();
        if (request.getRdsConfigIds() != null) {
            for (Long rdsConfigId : request.getRdsConfigIds()) {
                RDSConfig rdsConfig = rdsConfigService.getByIdFromAnyAvailableOrganization(rdsConfigId);
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (request.getRdsConfigNames() != null) {
            for (String rdsConfigName : request.getRdsConfigNames()) {
                Long organizationId = restRequestThreadLocalService.getRequestedOrgId();

                RDSConfig rdsConfig = organizationId != null
                        ? rdsConfigService.getByNameForOrganizationId(rdsConfigName, organizationId)
                        : rdsConfigService.getByNameFromUsersDefaultOrganization(rdsConfigName);
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (request.getRdsConfigJsons() != null) {
            for (RDSConfigJson rdsConfig : request.getRdsConfigJsons()) {
                increaseCount(rdsConfig.getType(), typeCountMap, multipleTypes);
            }
        }
        if (request.getAmbariDatabaseDetails() != null) {
            increaseCount(RdsType.AMBARI.name(), typeCountMap, multipleTypes);
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
