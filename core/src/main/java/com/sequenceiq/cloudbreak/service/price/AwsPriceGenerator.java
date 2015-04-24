package com.sequenceiq.cloudbreak.service.price;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class AwsPriceGenerator implements PriceGenerator {

    @Override
    public Double calculate(Template template, Long hours) {
        Double result = 0.0;
        AwsTemplate temp = (AwsTemplate) template;
        AwsInstanceType vmType = temp.getInstanceType();
        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
