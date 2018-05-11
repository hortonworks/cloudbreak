package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.TopologyRecord;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class HostGroupAssociationBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupAssociationBuilder.class);

    private static final String FQDN = "fqdn";

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public Map<String, List<Map<String, String>>> buildHostGroupAssociations(Iterable<HostGroup> hostGroups) {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
            if (hostGroup.getConstraint().getInstanceGroup() != null) {
                Map<String, String> topologyMapping = getTopologyMapping(hostGroup);
                Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
                List<InstanceMetaData> metas = instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroupId);
                if (metas.isEmpty()) {
                    for (HostMetadata hostMetadata : hostGroup.getHostMetadata()) {
                        Map<String, String> hostInfo = new HashMap<>();
                        hostInfo.put(FQDN, hostMetadata.getHostName());
                        hostInfoForHostGroup.add(hostInfo);
                    }
                } else {
                    for (InstanceMetaData meta : metas) {
                        Map<String, String> hostInfo = new HashMap<>();
                        hostInfo.put(FQDN, meta.getDiscoveryFQDN());
                        String localityIndicator = meta.getLocalityIndicator();
                        if (localityIndicator != null) {
                            if (topologyMapping.isEmpty()) {
                                // Azure
                                if (localityIndicator.startsWith("/")) {
                                    hostInfo.put("rack", meta.getLocalityIndicator());
                                    // Openstack
                                } else {
                                    hostInfo.put("rack", '/' + meta.getLocalityIndicator());
                                }
                                // With topology mapping
                            } else {
                                hostInfo.put("hypervisor", meta.getLocalityIndicator());
                                hostInfo.put("rack", topologyMapping.get(meta.getLocalityIndicator()));
                            }
                        }
                        hostInfoForHostGroup.add(hostInfo);
                    }
                }
            } else {
                for (HostMetadata hostMetadata : hostGroup.getHostMetadata()) {
                    Map<String, String> hostInfo = new HashMap<>();
                    hostInfo.put(FQDN, hostMetadata.getHostName());
                    hostInfoForHostGroup.add(hostInfo);
                }
            }

            hostGroupMappings.put(hostGroup.getName(), hostInfoForHostGroup);
        }
        LOGGER.info("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
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
