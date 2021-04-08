package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import org.springframework.stereotype.Service;

@Service
public class AzureMarketplaceImageProviderService {

    public AzureMarketplaceImage get(String imageName) {
        return imageName.contains("freeipa")
                ? new AzureMarketplaceImage("cloudera", "cb-cdh-test-131", "freeipa", "1.0.0")
                : new AzureMarketplaceImage("cloudera", "cb-cdh-test-131", "runtime-all", "");
    }
}
