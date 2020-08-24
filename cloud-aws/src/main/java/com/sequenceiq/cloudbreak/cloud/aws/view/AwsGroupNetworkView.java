package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;

public class AwsGroupNetworkView {
    @VisibleForTesting
    static final String SUBNET_ID = "subnetId";

    private final GroupNetwork network;

    public AwsGroupNetworkView(GroupNetwork network) {
        this.network = network;
    }

    public boolean isExistingSubnet() {
        return isNotEmpty(network.getStringParameter(SUBNET_ID));
    }

    public String getExistingSubnet() {
        return network.getStringParameter(SUBNET_ID);
    }

    public boolean isSubnetList() {
        return isExistingSubnet() && getExistingSubnet().contains(",");
    }

    public List<String> getSubnetList() {
        return isSubnetList() ? List.of(getExistingSubnet().split(",")) : (isExistingSubnet() ? List.of(getExistingSubnet()) : List.of());
    }

}
