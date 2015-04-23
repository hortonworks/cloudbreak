package com.sequenceiq.cloudbreak.service.price;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccInstanceType;

@Component
public class GCPPriceGenerator implements PriceGenerator {

    @Override
    public Double calculate(Template template, Long hours) {
        Double result = 0.0;
        GccTemplate temp = (GccTemplate) template;
        GccInstanceType vmType = temp.getGccInstanceType();

        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }
}
