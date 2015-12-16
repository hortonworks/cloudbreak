package com.sequenceiq.cloudbreak.service.price;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.domain.Template;

public interface PriceGenerator {

    Double calculate(Template template, Long hours);

    Platform getCloudPlatform();

}
