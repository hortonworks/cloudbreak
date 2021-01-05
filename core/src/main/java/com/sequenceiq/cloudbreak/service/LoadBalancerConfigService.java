package com.sequenceiq.cloudbreak.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class LoadBalancerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerConfigService.class);

    private static final String ENDPOINT_SUFFIX = "gateway";

    private static final String PUBLIC_SUFFIX = "external";

    private static final Set<Integer> DEFAULT_KNOX_PORTS = Set.of(443);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    public Set<String> getKnoxGatewayGroups(Stack stack) {
        LOGGER.debug("Fetching list of instance groups with Knox gateway installed");
        Set<String> groupNames = new HashSet<>();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            LOGGER.debug("Checking if Knox gateway is explicitly defined");
            CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(cluster.getBlueprint().getBlueprintText());
            groupNames = cmTemplateProcessor.getHostGroupsWithComponent(KnoxRoles.KNOX_GATEWAY);
        }

        if (groupNames.isEmpty()) {
            LOGGER.debug("Knox gateway is not explicitly defined; searching for CM gateway hosts");
            groupNames = stack.getInstanceGroups().stream()
                .filter(i -> InstanceGroupType.isGateway(i.getInstanceGroupType()))
                .map(InstanceGroup::getGroupName)
                .collect(Collectors.toSet());
        }

        if (groupNames.isEmpty()) {
            LOGGER.info("No Knox gateway instance groups found");
        }
        return groupNames;
    }

    private boolean isKnoxGatewayDefinedInServices(Set<ServiceComponent> serviceComponents) {
        return serviceComponents.stream()
            .anyMatch(serviceComponent -> KnoxRoles.KNOX_GATEWAY.equals(serviceComponent.getComponent()));
    }

    public String generateLoadBalancerEndpoint(Stack stack, LoadBalancerType type) {
        StringBuilder name = new StringBuilder()
            .append(stack.getName())
            .append('-')
            .append(ENDPOINT_SUFFIX);
        if (LoadBalancerType.PUBLIC.equals(type)) {
            name.append('-').append(PUBLIC_SUFFIX);
        }
        return name.toString();
    }

    public Set<Integer> getPortsForTargetGroup(TargetGroup targetGroup) {
        switch (targetGroup.getType()) {
            case KNOX:
                return DEFAULT_KNOX_PORTS;
            default:
                return Collections.emptySet();
        }
    }

    public String getLoadBalancerUserFacingFQDN(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackId);
        if (!loadBalancers.isEmpty()) {
            LoadBalancer preferredLB =  loadBalancers.stream().filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType())).findAny()
                .orElse(loadBalancers.iterator().next());
            return preferredLB.getFqdn();
        }

        return null;
    }
}
