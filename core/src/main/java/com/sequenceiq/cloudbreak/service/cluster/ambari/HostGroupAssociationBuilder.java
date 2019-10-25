package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.TopologyRecord;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class HostGroupAssociationBuilder {

    public static final String FQDN = "fqdn";

    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupAssociationBuilder.class);

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public Map<String, List<Map<String, String>>> buildHostGroupAssociations(Iterable<HostGroup> hostGroups) {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            List<Map<String, String>> hostInfoForHostGroup = buildHostGroupAssociation(hostGroup);
            hostGroupMappings.put(hostGroup.getName(), hostInfoForHostGroup);
        }
        LOGGER.info("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    public List<Map<String, String>> buildHostGroupAssociation(HostGroup hostGroup) {
        List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
        Map<String, String> topologyMapping = getTopologyMapping(hostGroup);
        Set<InstanceMetaData> notTerminatedInstanceMetaDataSet = hostGroup.getInstanceGroup().getNotTerminatedInstanceMetaDataSet();

        for (InstanceMetaData instanceMetaData : notTerminatedInstanceMetaDataSet) {
            Map<String, String> hostInfo = new HashMap<>();
            hostInfo.put(FQDN, instanceMetaData.getDiscoveryFQDN());
            String localityIndicator = instanceMetaData.getLocalityIndicator();
            if (localityIndicator != null) {
                if (topologyMapping.isEmpty()) {
                    // Azure
                    if (localityIndicator.startsWith("/")) {
                        hostInfo.put("rack", instanceMetaData.getLocalityIndicator());
                        // Openstack
                    } else {
                        hostInfo.put("rack", '/' + instanceMetaData.getLocalityIndicator());
                    }
                    // With topology mapping
                } else {
                    hostInfo.put("hypervisor", instanceMetaData.getLocalityIndicator());
                    hostInfo.put("rack", topologyMapping.get(instanceMetaData.getLocalityIndicator()));
                }
            }
            hostInfoForHostGroup.add(hostInfo);
        }
        return hostInfoForHostGroup;
    }

    private Map<String, String> getTopologyMapping(HostGroup hg) {
        Map<String, String> result = new HashMap<>();
        LOGGER.info("Computing hypervisor - rack mapping based on topology");
        Topology topology = hg.getCluster().getStack().getCredential().getTopology();
        if (topology == null) {
            return result;
        }
        List<TopologyRecord> records = topology.getRecords();
        if (records != null) {
            for (TopologyRecord t : records) {
                result.put(t.getHypervisor(), t.getRack());
            }
        }
        return result;
    }
}
