package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AwsInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureAvailabiltySetV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureInstanceGroupV4Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AwsInstanceGroupV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AzureAvailabiltySetV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AzureInstanceGroupV1Parameters;

@Component
public class InstanceGroupParameterConverter {
    public AwsInstanceGroupV4Parameters convert(AwsInstanceGroupV1Parameters source) {
        return new AwsInstanceGroupV4Parameters();
    }

    public AzureInstanceGroupV4Parameters convert(AzureInstanceGroupV1Parameters source) {
        AzureInstanceGroupV4Parameters response = new AzureInstanceGroupV4Parameters();
        response.setAvailabilitySet(ifNotNullF(source.getAvailabilitySet(), this::getAvailabilitySet));
        return response;
    }

    private AzureAvailabiltySetV4 getAvailabilitySet(AzureAvailabiltySetV1Parameters source) {
        AzureAvailabiltySetV4 response = new AzureAvailabiltySetV4();
        response.setFaultDomainCount(source.getFaultDomainCount());
        response.setName(source.getName());
        response.setUpdateDomainCount(source.getUpdateDomainCount());
        return response;
    }
}
