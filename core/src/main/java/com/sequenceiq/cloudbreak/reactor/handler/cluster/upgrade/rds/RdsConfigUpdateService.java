package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class RdsConfigUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigUpdateService.class);

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private Set<AbstractRdsConfigProvider> rdsConfigProviders;

    public void updateRdsConnectionUserName(StackDto stackDto) {
        // TODO Currently we update all the rds configs for the cluster because of customer pressure
        // TODO In the followup CB-22393 it will be refactored to update only the CM config here and CM services config after the CM is successfully restarted
        LOGGER.debug("Updating rds config connections to the upgraded database in cloudbreak database.");
        boolean dataHubCluster = StackType.WORKLOAD.equals(stackDto.getStack().getType());
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(stackDto.getCluster().getId())
                .stream()
                .filter(filterRdsConfigsToUpdate(dataHubCluster))
                .collect(Collectors.toSet());
        rdsConfigs.forEach(rdsConfig -> {
            String originalUserName = rdsConfig.getConnectionUserName();
            String newUserName = originalUserName.split("@")[0];
            LOGGER.debug("Updating rds config connection {}'s user name from {} to {}", rdsConfig.getName(),
                    originalUserName, newUserName);
            rdsConfig.setConnectionUserName(newUserName);
        });
        rdsConfigService.pureSaveAll(rdsConfigs);
    }

    private Predicate<RDSConfig> filterRdsConfigsToUpdate(boolean dataHubCluster) {
        return rdsCfg -> hasMatchingRdsConfigProvider(rdsCfg) && configUpdateRequired(rdsCfg, dataHubCluster);
    }

    private boolean configUpdateRequired(RDSConfig rdsCfg, boolean dataHubCluster) {
        return !(dataHubCluster && rdsConfigService.getClustersUsingResource(rdsCfg).size() > 1);
    }

    private boolean hasMatchingRdsConfigProvider(RDSConfig rdsCfg) {
        return rdsConfigProviders.stream().anyMatch(configProvider -> isMatchRdsTypeWithString(configProvider.getRdsType(), rdsCfg));
    }

    private boolean isMatchRdsTypeWithString(DatabaseType rdsType, RDSConfig rdsConfig) {
        return rdsType.name().equals(rdsConfig.getType());
    }
}
