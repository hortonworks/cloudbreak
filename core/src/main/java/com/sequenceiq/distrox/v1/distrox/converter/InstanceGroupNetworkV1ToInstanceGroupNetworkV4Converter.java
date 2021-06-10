package com.sequenceiq.distrox.v1.distrox.converter;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class InstanceGroupNetworkV1ToInstanceGroupNetworkV4Converter {

    @Inject
    private InstanceGroupNetworkParameterConverter instanceGroupNetworkParameterConverter;

    public InstanceGroupNetworkV4Request convertToInstanceGroupNetworkV4Request(Pair<InstanceGroupNetworkV1Request, DetailedEnvironmentResponse> network) {
        DetailedEnvironmentResponse value = network.getValue();
        EnvironmentNetworkResponse environmentNetworkResponse = null;
        if (value == null) {
            environmentNetworkResponse = new EnvironmentNetworkResponse();
        } else {
            environmentNetworkResponse = value.getNetwork();
        }
        InstanceGroupNetworkV1Request key = network.getKey();
        if (key == null) {
            key = new InstanceGroupNetworkV1Request();
        }

        InstanceGroupNetworkV4Request request = new InstanceGroupNetworkV4Request();

        if (value != null) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(value.getCloudPlatform());
            request.setCloudPlatform(cloudPlatform);
            request.setAws(
                    instanceGroupNetworkParameterConverter.convert(key.getAws(), environmentNetworkResponse, cloudPlatform));
            request.setAzure(
                    instanceGroupNetworkParameterConverter.convert(key.getAzure(), environmentNetworkResponse, cloudPlatform));
            request.setGcp(
                    instanceGroupNetworkParameterConverter.convert(key.getGcp(), environmentNetworkResponse, cloudPlatform));
            request.setYarn(
                    instanceGroupNetworkParameterConverter.convert(key.getYarn(), environmentNetworkResponse, cloudPlatform));
            request.setMock(
                    instanceGroupNetworkParameterConverter.convert(key.getMock(), environmentNetworkResponse, cloudPlatform));
            request.setOpenstack(
                    instanceGroupNetworkParameterConverter.convert(key.getOpenstack(), environmentNetworkResponse, cloudPlatform));
        }
        return request;
    }
}
