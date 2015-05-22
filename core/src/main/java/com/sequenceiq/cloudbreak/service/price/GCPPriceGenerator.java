package com.sequenceiq.cloudbreak.service.price;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.domain.GcpInstanceType;

@Component
public class GCPPriceGenerator implements PriceGenerator {

    @Override
    public Double calculate(Template template, Long hours) {
        Double result = 0.0;
        GcpTemplate temp = (GcpTemplate) template;
        GcpInstanceType vmType = temp.getGcpInstanceType();

        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}
