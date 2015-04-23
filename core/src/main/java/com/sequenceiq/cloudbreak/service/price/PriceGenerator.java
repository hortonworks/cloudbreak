package com.sequenceiq.cloudbreak.service.price;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;

public interface PriceGenerator {

    Double calculate(Template template, Long hours);

    CloudPlatform getCloudPlatform();

}
