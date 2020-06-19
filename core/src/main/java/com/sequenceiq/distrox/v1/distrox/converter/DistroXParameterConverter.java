package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.YarnDistroXV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;

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

    public AzureDistroXV1Parameters convert(AzureStackV4Parameters source) {
        AzureDistroXV1Parameters response = new AzureDistroXV1Parameters();
        response.setEncryptStorage(source.isEncryptStorage());
        response.setResourceGroupName(source.getResourceGroupName());
        return response;
    }

    public AwsDistroXV1Parameters convert(AwsStackV4Parameters source) {
        return new AwsDistroXV1Parameters();
    }

    public YarnStackV4Parameters convert(YarnDistroXV1Parameters source) {
        YarnStackV4Parameters response = new YarnStackV4Parameters();
        response.setYarnQueue(source.getYarnQueue());
        response.setLifetime(source.getLifetime());
        return response;
    }

    public YarnDistroXV1Parameters convert(YarnStackV4Parameters source) {
        YarnDistroXV1Parameters response = new YarnDistroXV1Parameters();
        response.setYarnQueue(source.getYarnQueue());
        response.setLifetime(source.getLifetime());
        return response;
    }

    public YarnStackV4Parameters convert(EnvironmentNetworkYarnParams environmentNetworkYarnParams) {
        YarnStackV4Parameters response = new YarnStackV4Parameters();
        response.setYarnQueue(environmentNetworkYarnParams.getQueue());
        response.setLifetime(environmentNetworkYarnParams.getLifetime());
        return response;
    }
}
