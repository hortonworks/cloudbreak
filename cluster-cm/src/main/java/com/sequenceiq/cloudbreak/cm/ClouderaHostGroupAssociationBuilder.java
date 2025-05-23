package com.sequenceiq.cloudbreak.cm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.model.ClusterHostAttributes;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.template.utils.HostGroupUtils;
import com.sequenceiq.cloudbreak.util.NullUtil;

@Service
class ClouderaHostGroupAssociationBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaHostGroupAssociationBuilder.class);

    @Inject
    private HostGroupUtils hostGroupUtils;

    Map<String, List<Map<String, String>>> buildHostGroupAssociations(Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup) {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.debug("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (Entry<HostGroup, List<InstanceMetaData>> hostGroupListEntry : instanceMetaDataByHostGroup.entrySet()) {
            if (hostGroupUtils.isNotEcsHostGroup(hostGroupListEntry.getKey().getName())) {
                List<Map<String, String>> hostInfoForHostGroup = buildHostGroupAssociation(hostGroupListEntry.getValue());
                hostGroupMappings.put(hostGroupListEntry.getKey().getName(), hostInfoForHostGroup);
            }
        }
        LOGGER.debug("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    private List<Map<String, String>> buildHostGroupAssociation(List<InstanceMetaData> instanceMetadataList) {
        List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
        for (InstanceMetaData metaData : instanceMetadataList) {
            Map<String, String> hostInfo = new HashMap<>();
            hostInfo.put(ClusterHostAttributes.FQDN, metaData.getDiscoveryFQDN());
            NullUtil.putIfPresent(hostInfo, ClusterHostAttributes.RACK_ID, metaData.getRackId());
            hostInfoForHostGroup.add(hostInfo);
        }
        return hostInfoForHostGroup;
    }

}
