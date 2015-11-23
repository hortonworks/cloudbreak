package com.sequenceiq.cloudbreak.service.price;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class AzurePriceGenerator implements PriceGenerator {

    @Override
    public Double calculate(Template template, Long hours) {
        return 0.0;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
