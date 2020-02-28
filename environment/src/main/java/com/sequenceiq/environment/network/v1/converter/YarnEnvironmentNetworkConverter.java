package com.sequenceiq.environment.network.v1.converter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dao.domain.YarnNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

@Component
public class YarnEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Value("${cb.yarn.networkCidr}")
    private String yarnNetworkCidr;

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        YarnNetwork yarnNetwork = new YarnNetwork();
        yarnNetwork.setNetworkCidr(yarnNetworkCidr);
        if (network.getYarn() != null) {
            yarnNetwork.setQueue(network.getYarn().getQueue());
        }
        return yarnNetwork;
    }

    @Override
    public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        YarnNetwork yarnNetwork = (YarnNetwork) baseNetwork;
        Map<String, Object> properties = createdCloudNetwork.getProperties();
        yarnNetwork.setQueue((String) properties.getOrDefault("queue", null));
        return yarnNetwork;
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        YarnNetwork yarnNetwork = (YarnNetwork) network;
        return builder.withYarn(
                YarnParams.YarnParamsBuilder
                    .anYarnParams()
                    .withQueue(yarnNetwork.getQueue())
                    .build())
                .build();
    }

    @Override
    void setRegistrationType(BaseNetwork result, NetworkDto networkDto) {
        result.setRegistrationType(RegistrationType.CREATE_NEW);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

}
