package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class CmCommandLinkProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmCommandLinkProvider.class);

    @Value("${distrox.gateway.topology.name:cdp-proxy}")
    private String defaultTopologyName;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    public Optional<String> getCmCommandLink(Stack stack, String commandId) {
        try {
            String managerAddress = stackUtil.extractClusterManagerAddress(stack);
            Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies =
                    serviceEndpointCollector.prepareClusterExposedServices(stack.getCluster(), managerAddress);
            if (clusterExposedServicesForTopologies.containsKey(defaultTopologyName)) {
                Predicate<ClusterExposedServiceV4Response> cmUiPredicate = exposedService -> StringUtils.equals(exposedService.getServiceName(),
                        exposedServiceCollector.getClouderaManagerUIService().getServiceName());
                Optional<ClusterExposedServiceV4Response> exposedServiceV4Response = clusterExposedServicesForTopologies.get(defaultTopologyName)
                        .stream().filter(cmUiPredicate).findFirst();
                if (exposedServiceV4Response.isPresent()) {
                    return Optional.of(exposedServiceV4Response.get().getServiceUrl().replace("/home/", "/command/" + commandId + "/details"));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't generate link for command {}, because: {}", commandId, e.getMessage());
        }
        return Optional.empty();
    }
}
