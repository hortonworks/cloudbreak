package com.sequenceiq.cloudbreak.service.price;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class AwsPriceGenerator implements PriceGenerator {

    @Override
    public Double calculate(Template template, Long hours) {
        return 0.0;
    }

    @Override
    public Double calculate(String instanceType, Long hours) {
        return 0.0;
    }

    @Override
    public Platform getCloudPlatform() {
        return Platform.platform(CloudConstants.AWS);
    }
}
