package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class ClouderaHostGroupAssociationBuilder {

    public static final String FQDN = "fqdn";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaHostGroupAssociationBuilder.class);

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public Map<String, List<Map<String, String>>> buildHostGroupAssociations(Iterable<HostGroup> hostGroups) {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.debug("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            List<Map<String, String>> hostInfoForHostGroup = buildHostGroupAssociation(hostGroup);
            hostGroupMappings.put(hostGroup.getName(), hostInfoForHostGroup);
        }
        LOGGER.debug("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    public List<Map<String, String>> buildHostGroupAssociation(HostGroup hostGroup) {
        List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
        Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
        List<InstanceMetaData> instanceMetadataList = instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroupId);
        for (InstanceMetaData metaData : instanceMetadataList) {
            Map<String, String> hostInfo = new HashMap<>();
            hostInfo.put(FQDN, metaData.getDiscoveryFQDN());
            hostInfoForHostGroup.add(hostInfo);
        }
        return hostInfoForHostGroup;
    }

}
