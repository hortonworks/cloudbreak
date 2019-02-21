package com.sequenceiq.cloudbreak.ambari.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
public class HostFilterService {

    public static final String RM_WS_PATH = "/ws/v1/cluster";

    private static final Logger LOGGER = LoggerFactory.getLogger(HostFilterService.class);

    @Inject
    private List<HostFilter> hostFilters;

    @Inject
    private AmbariConfigurationService configurationService;

    public List<HostMetadata> filterHostsForDecommission(Cluster cluster, Collection<HostMetadata> hosts, String hostGroup, AmbariClient ambariClient,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        List<HostMetadata> filteredList = new ArrayList<>(hosts);
        LOGGER.debug("Ambari service config, hostGroup: {}, originalList: {}", hostGroup, filteredList);
        Map<String, String> config = configurationService.getConfiguration(ambariClient, hostGroup);
        LOGGER.debug("Ambari service config, hostGroup: {}, config: {}", hostGroup, config);
        for (HostFilter hostFilter : hostFilters) {
            try {
                filteredList = hostFilter.filter(cluster.getId(), config, filteredList, instanceMetaDatasInStack);
                LOGGER.debug("Filtered with hostfilter: {}, filteredList: {}", hostFilter.getClass().getSimpleName(), filteredList);
            } catch (HostFilterException e) {
                LOGGER.debug("Filter didn't succeed, moving to next filter: {}", e.getMessage());
            }
        }
        LOGGER.debug("Returned filtered hosts: {}", filteredList);
        return filteredList;
    }
}
