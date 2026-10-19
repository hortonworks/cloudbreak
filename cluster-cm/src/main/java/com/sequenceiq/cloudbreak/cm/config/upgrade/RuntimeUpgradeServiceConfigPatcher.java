package com.sequenceiq.cloudbreak.cm.config.upgrade;

import java.util.Set;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

/**
 * Patches service configuration in Cloudera Manager during the runtime upgrade, before the new parcels are downloaded and activated,
 * when the target product versions require it. {@link RuntimeUpgradeServiceConfigPatcherService} runs the implementations.
 * The configs are applied by the restart that the runtime upgrade performs anyway. Implementations must be idempotent.
 */
public interface RuntimeUpgradeServiceConfigPatcher {

    boolean isPatchNeeded(StackDtoDelegate stack, Set<ClouderaManagerProduct> targetProducts);

    void patchServiceConfig(ApiClient apiClient, StackDtoDelegate stack, Set<ClouderaManagerProduct> targetProducts);
}
