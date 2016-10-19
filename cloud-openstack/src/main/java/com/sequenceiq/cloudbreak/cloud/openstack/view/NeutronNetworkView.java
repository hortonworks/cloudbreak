package com.sequenceiq.cloudbreak.cloud.openstack.view;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.ROUTER_ID;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.SUBNET_ID;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;

public class NeutronNetworkView {

    private Network network;

    public NeutronNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnetCIDR() {
        return network.getSubnet().getCidr();
    }

    public boolean isAssignFloatingIp() {
        return isNoneEmpty(getPublicNetId());
    }

    public boolean isExistingNetwork() {
        return isNoneEmpty(getCustomNetworkId());
    }

    public boolean isExistingSubnet() {
        return isNoneEmpty(getCustomSubnetId());
    }

    public String getCustomNetworkId() {
        return network.getStringParameter(NETWORK_ID);
    }

    public String getCustomRouterId() {
        return network.getStringParameter(ROUTER_ID);
    }

    public String getCustomSubnetId() {
        return network.getStringParameter(SUBNET_ID);
    }

    public String getPublicNetId() {
        return network.getStringParameter(OpenStackConstants.PUBLIC_NET_ID);
    }

    public boolean isProviderNetwork() {
        String networkingOption = network.getStringParameter(OpenStackConstants.NETWORKING_OPTION);
        return NetworkingOptions.PROVIDER.getValue().equals(networkingOption);
    }

    private enum NetworkingOptions {

        PROVIDER("provider"), SELF_SERVICE("self-service");

        private String value;

        NetworkingOptions(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}


