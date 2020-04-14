package com.sequenceiq.freeipa.service.freeipa;

import java.io.Serializable;
import java.util.Comparator;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

public class PrimaryGatewayFirstThenSortByFqdnComparator implements Comparator<InstanceMetaData>, Serializable {

    @Override
    public int compare(InstanceMetaData l, InstanceMetaData r) {
        if (l.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY) {
            return -1;
        }
        if (r.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY) {
            return 1;
        }
        return Comparator.comparing(InstanceMetaData::getDiscoveryFQDN).compare(l, r);
    }
}
