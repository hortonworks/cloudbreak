package com.sequenceiq.cloudbreak.cm.config.upgrade;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
public class RuntimeUpgradeServiceConfigPatcherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeUpgradeServiceConfigPatcherService.class);

    @Inject
    private List<RuntimeUpgradeServiceConfigPatcher> patchers;

    public void patchServiceConfigs(ApiClient apiClient, StackDtoDelegate stack, Set<ClouderaManagerProduct> targetProducts) {
        patchers.forEach(patcher -> {
            String patcherName = patcher.getClass().getSimpleName();
            if (patcher.isPatchNeeded(stack, targetProducts)) {
                LOGGER.info("Applying runtime upgrade service config patcher {}", patcherName);
                patcher.patchServiceConfig(apiClient, stack, targetProducts);
            } else {
                LOGGER.debug("Runtime upgrade service config patcher {} is not needed", patcherName);
            }
        });
    }
}
