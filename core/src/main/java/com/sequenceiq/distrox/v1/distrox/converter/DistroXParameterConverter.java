package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;

@Component
public class DistroXParameterConverter {

    public AzureStackV4Parameters convert(AzureDistroXV1Parameters source) {
        AzureStackV4Parameters response = new AzureStackV4Parameters();
        response.setEncryptStorage(source.isEncryptStorage());
        response.setResourceGroupName(source.getResourceGroupName());
        return response;
    }

    public AwsStackV4Parameters convert(AwsDistroXV1Parameters source) {
        return new AwsStackV4Parameters();
    }
}
