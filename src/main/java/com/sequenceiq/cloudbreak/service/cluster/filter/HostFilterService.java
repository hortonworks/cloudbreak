package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;

@Service
public class HostFilterService {

    public static final String RM_WS_PATH = "/ws/v1/cluster";
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);

    @Autowired
    private List<HostFilter> hostFilters;

    @Autowired
    private AmbariConfigurationService configurationService;

    @Autowired
    private AmbariClientService clientService;

    public List<HostMetadata> filterHostsForDecommission(Stack stack, Set<HostMetadata> hosts, String hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        List<HostMetadata> filteredList = new ArrayList<>(hosts);
        try {
            AmbariClient ambariClient = clientService.create(stack);
            Map<String, String> config = configurationService.getConfiguration(ambariClient, hostGroup);
            for (HostFilter hostFilter : hostFilters) {
                try {
                    filteredList = hostFilter.filter(stack.getId(), config, filteredList);
                } catch (HostFilterException e) {
                    LOGGER.warn("Filter didn't succeed, moving to next filter", e);
                }
            }
        } catch (ConnectException e) {
            LOGGER.error("Error retrieving the configuration from Ambari, no host filtering is provided", e);
        }
        return filteredList;
    }
}
