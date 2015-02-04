package com.sequenceiq.cloudbreak.service.price;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class AzurePriceGenerator implements PriceGenerator {

    @Override
    public Double calculate(Template template, Long hours) {
        Double result = 0.0;
        AzureTemplate temp = (AzureTemplate) template;
        AzureVmType vmType = AzureVmType.valueOf(temp.getVmType());
        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
