package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.PUBLIC_NET_ID;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.ROUTER_ID;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkOpenstackParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class OpenstackEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    public Network convertToLegacyNetwork(EnvironmentNetworkResponse source, String availabilityZone) {
        Network result = super.convertToLegacyNetwork(source, availabilityZone);
        if (result.getAttributes() != null) {
            String subnetId = (String) result.getAttributes().getMap().get(NetworkConstants.SUBNET_ID);
            if (StringUtils.isNotEmpty(subnetId) && source.getSubnetMetas() != null) {
                CloudSubnet cloudSubnet = source.getSubnetMetas().get(subnetId);
                if (cloudSubnet != null && StringUtils.isNotEmpty(cloudSubnet.getCidr())) {
                    result.setSubnetCIDR(cloudSubnet.getCidr());
                }
            }
        }
        return result;
    }

    @Override
    Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source) {
        EnvironmentNetworkOpenstackParams openstack = source.getOpenstack();
        Map<String, Object> result = new HashMap<>();
        if (!Strings.isNullOrEmpty(openstack.getNetworkId())) {
            result.put(NETWORK_ID, openstack.getNetworkId());
        }
        if (openstack.getPublicNetId() != null) {
            result.put(PUBLIC_NET_ID, openstack.getPublicNetId());
        }
        if (openstack.getRouterId() != null) {
            result.put(ROUTER_ID, openstack.getRouterId());
        }
        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }
}
