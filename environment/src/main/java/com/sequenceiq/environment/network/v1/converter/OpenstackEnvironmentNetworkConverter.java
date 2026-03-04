package com.sequenceiq.environment.network.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.OpenstackNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.OpenStackParams;

@Component
public class OpenstackEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {
    protected OpenstackEnvironmentNetworkConverter(EnvironmentViewConverter environmentViewConverter, EntitlementService entitlementService) {
        super(environmentViewConverter, entitlementService);
    }

    @Override
    public boolean isApplicableForDwx(CloudSubnet cloudSubnet) {
        return false;
    }

    @Override
    public boolean isApplicableForMlx(CloudSubnet cloudSubnet) {
        return false;
    }

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        OpenstackNetwork openstackNetwork = new OpenstackNetwork();
        openstackNetwork.setNetworkId(network.getOpenstack().getNetworkId());
        openstackNetwork.setRouterId(network.getOpenstack().getRouterId());
        openstackNetwork.setPublicNetId(network.getOpenstack().getPublicNetId());
        return openstackNetwork;
    }

    @Override
    NetworkDto setProviderSpecificFieldsAndReturnDto(NetworkDto.Builder builder, BaseNetwork network) {
        OpenstackNetwork openstackNetwork = (OpenstackNetwork) network;
        OpenStackParams.Builder openstackParamsBuilder = OpenStackParams.builder();
        Optional.ofNullable(openstackNetwork.getNetworkId()).ifPresent(openstackParamsBuilder::withNetworkId);
        Optional.ofNullable(openstackNetwork.getRouterId()).ifPresent(openstackParamsBuilder::withRouterId);
        Optional.ofNullable(openstackNetwork.getPublicNetId()).ifPresent(openstackParamsBuilder::withPublicNetId);
        return builder
                .withOpenstack(openstackParamsBuilder.build())
                .build();
    }

    @Override
    void setRegistrationType(BaseNetwork result, NetworkDto networkDto) {
        result.setRegistrationType(RegistrationType.EXISTING);
    }

    @Override
    public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        throw new CloudbreakRuntimeException("Creating new network is not supported on OpenStack");
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        OpenstackNetwork openstackNetwork = (OpenstackNetwork) baseNetwork;
        Map<String, Object> param = new HashMap<>();
        param.put(OpenStackConstants.NETWORK_ID, openstackNetwork.getNetworkId());
        param.put(OpenStackConstants.ROUTER_ID, openstackNetwork.getRouterId());
        param.put(OpenStackConstants.PUBLIC_NET_ID, openstackNetwork.getPublicNetId());
        return new Network(null, param);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }
}
