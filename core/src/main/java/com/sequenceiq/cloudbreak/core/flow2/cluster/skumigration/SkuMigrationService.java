package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.LOAD_BALANCER_SKU_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.STANDARD_SKU_MIGRATION_PARAMETER;
import static com.sequenceiq.common.api.type.LoadBalancerSku.STANDARD;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackParametersService;

@Service
public class SkuMigrationService {

    public static final String MIGRATED = "migrated";

    private static final Logger LOGGER = LoggerFactory.getLogger(SkuMigrationService.class);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private StackParametersService stackParametersService;

    @Value("${cb.upscale.sku.migration.enabled:false}")
    private boolean upscaleSkuMigrationEnabled;

    @Value("${cb.repair.sku.migration.enabled:false}")
    private boolean repairSkuMigrationEnabled;

    public void updateSkuToStandard(Long stackId, Set<LoadBalancer> loadBalancers) {
        if (!loadBalancers.isEmpty()) {
            loadBalancers.forEach(loadBalancer -> {
                loadBalancer.setSku(STANDARD);
            });
            loadBalancerPersistenceService.saveAll(loadBalancers);
            stackParametersService.setStackParameter(stackId, LOAD_BALANCER_SKU_PARAMETER, STANDARD.name());
        }
    }

    public void setSkuMigrationParameter(Long stackId) {
        stackParametersService.setStackParameter(stackId, STANDARD_SKU_MIGRATION_PARAMETER, MIGRATED);
    }

    public boolean isMigrationNecessary(StackDto stackDto) {
        if (CloudPlatform.AZURE.name().equalsIgnoreCase(stackDto.getCloudPlatform())) {
            String standardSkuMigrationParameter = stackDto.getParameters().get(STANDARD_SKU_MIGRATION_PARAMETER);
            LOGGER.info("Standard sku migration parameter: {}", standardSkuMigrationParameter);
            boolean multiAz = stackDto.getStack().isMultiAz();
            boolean noPublicIp = stackDto.getNetwork().getAttributes().getBoolean(AzureConstants.NO_PUBLIC_IP) != null ?
                    stackDto.getNetwork().getAttributes().getBoolean(AzureConstants.NO_PUBLIC_IP) : false;
            Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackDto.getId());
            boolean notStandardSkuLB = loadBalancers.stream().anyMatch(loadBalancer -> !STANDARD.equals(loadBalancer.getSku()));
            LOGGER.info("Load balancers for stack: {}. Not standard load balancer found: {}. multiAz: {}, noPublicIp: {}", loadBalancers, notStandardSkuLB,
                    multiAz, noPublicIp);
            return standardSkuMigrationParameter == null && (notStandardSkuLB || (!noPublicIp && !multiAz));
        } else {
            return false;
        }
    }

    public boolean isUpscaleSkuMigrationEnabled() {
        return upscaleSkuMigrationEnabled;
    }

    public boolean isRepairSkuMigrationEnabled() {
        return repairSkuMigrationEnabled;
    }
}
