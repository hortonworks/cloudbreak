package com.sequenceiq.cloudbreak.cm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
public class ClouderaHostGroupAssociationBuilder {

    public static final String FQDN = "fqdn";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaHostGroupAssociationBuilder.class);

    public Map<String, List<Map<String, String>>> buildHostGroupAssociations(Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup) {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.debug("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (Map.Entry<HostGroup, List<InstanceMetaData>> hostGroupListEntry : instanceMetaDataByHostGroup.entrySet()) {
            List<Map<String, String>> hostInfoForHostGroup = buildHostGroupAssociation(hostGroupListEntry.getValue());
            hostGroupMappings.put(hostGroupListEntry.getKey().getName(), hostInfoForHostGroup);
        }
        LOGGER.debug("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    public List<Map<String, String>> buildHostGroupAssociation(List<InstanceMetaData> instanceMetadataList) {
        List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
        for (InstanceMetaData metaData : instanceMetadataList) {
            Map<String, String> hostInfo = new HashMap<>();
            hostInfo.put(FQDN, metaData.getDiscoveryFQDN());
            hostInfoForHostGroup.add(hostInfo);
        }
        return hostInfoForHostGroup;
    }

}
