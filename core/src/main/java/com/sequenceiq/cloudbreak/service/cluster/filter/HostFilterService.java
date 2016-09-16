package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;

@Service
public class HostFilterService {

    public static final String RM_WS_PATH = "/ws/v1/cluster";
    private static final Logger LOGGER = LoggerFactory.getLogger(HostFilterService.class);

    @Inject
    private List<HostFilter> hostFilters;

    @Inject
    private AmbariConfigurationService configurationService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<HostMetadata> filterHostsForDecommission(Cluster cluster, Set<HostMetadata> hosts, String hostGroup) throws CloudbreakSecuritySetupException {
        List<HostMetadata> filteredList = new ArrayList<>(hosts);
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(cluster.getStack().getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getStack().getGatewayPort(), cluster.getUserName(),
                cluster.getPassword());
        Map<String, String> config = configurationService.getConfiguration(ambariClient, hostGroup);
        for (HostFilter hostFilter : hostFilters) {
            try {
                filteredList = hostFilter.filter(cluster.getId(), config, filteredList);
            } catch (HostFilterException e) {
                LOGGER.warn("Filter didn't succeed, moving to next filter", e);
            }
        }
        return filteredList;
    }
}
