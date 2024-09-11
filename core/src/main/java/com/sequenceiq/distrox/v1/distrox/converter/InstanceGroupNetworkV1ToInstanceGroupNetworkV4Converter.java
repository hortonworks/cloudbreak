package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class InstanceGroupNetworkV1ToInstanceGroupNetworkV4Converter {

    @Inject
    private InstanceGroupNetworkParameterConverter instanceGroupNetworkParameterConverter;

    public InstanceGroupNetworkV4Request convertToInstanceGroupNetworkV4Request(InstanceGroupNetworkV1Request instanceGroupNetworkV1Request,
            DetailedEnvironmentResponse detailedEnvironmentResponse, NetworkV4Request stackLevelNetwork) {
        Objects.requireNonNull(detailedEnvironmentResponse);
        EnvironmentNetworkResponse environmentNetworkResponse = Optional.ofNullable(detailedEnvironmentResponse.getNetwork())
                .orElse(new EnvironmentNetworkResponse());
        if (instanceGroupNetworkV1Request == null) {
            instanceGroupNetworkV1Request = new InstanceGroupNetworkV1Request();
        }

        InstanceGroupNetworkV4Request request = new InstanceGroupNetworkV4Request();

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(detailedEnvironmentResponse.getCloudPlatform());
        request.setCloudPlatform(cloudPlatform);
        request.setAws(instanceGroupNetworkParameterConverter.convert(instanceGroupNetworkV1Request.getAws(), environmentNetworkResponse, cloudPlatform,
                stackLevelNetwork));
        request.setAzure(instanceGroupNetworkParameterConverter.convert(instanceGroupNetworkV1Request.getAzure(), environmentNetworkResponse, cloudPlatform,
                stackLevelNetwork));
        request.setGcp(instanceGroupNetworkParameterConverter.convert(instanceGroupNetworkV1Request.getGcp(), environmentNetworkResponse, cloudPlatform,
                stackLevelNetwork));
        request.setYarn(
                instanceGroupNetworkParameterConverter.convert(instanceGroupNetworkV1Request.getYarn(), environmentNetworkResponse, cloudPlatform));
        request.setMock(
                instanceGroupNetworkParameterConverter.convert(instanceGroupNetworkV1Request.getMock(), environmentNetworkResponse, cloudPlatform,
                        stackLevelNetwork));
        return request;
    }
}
