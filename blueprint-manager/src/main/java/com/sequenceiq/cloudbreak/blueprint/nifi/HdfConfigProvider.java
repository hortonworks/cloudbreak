package com.sequenceiq.cloudbreak.blueprint.nifi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class HdfConfigProvider {
    private static final String DEFAULT_NIFI_PORT = "9091";

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    public HdfConfigs createHdfConfig(Set<HostGroup> hostgroups, Map<String, List<InstanceMetaData>> groupInstances, String blueprintText) {
        BlueprintTextProcessor blueprintTextProcessor = createTextProcessor(blueprintText);
        Set<String> nifiMasters = collectNifiMasters(blueprintText);
        Set<InstanceGroup> nifiIgs = collectInstanceGroupsWhichContainsNifiMasters(hostgroups, nifiMasters);
        List<String> nifiFqdns = collectFqdnsByInstanceGroupName(groupInstances, nifiIgs);
        AtomicInteger index = new AtomicInteger(0);
        String nodeEntities = nifiFqdns.stream()
            .map(fqdn -> String.format("<property name=\"Node Identity %s\">CN=%s, OU=NIFI</property>", index.addAndGet(1), fqdn))
            .collect(Collectors.joining());
        String registryNodeEntities = nodeEntities.replaceAll("Node Identity", "NiFi Identity");
        return new HdfConfigs(nodeEntities, registryNodeEntities, getProxyHostsParameter(nifiIgs, blueprintTextProcessor, groupInstances));
    }

    private BlueprintTextProcessor createTextProcessor(String blueprintText) {
        return blueprintProcessorFactory.get(blueprintText);
    }

    private Optional<String> getProxyHostsParameter(Set<InstanceGroup> nifiIgs, BlueprintTextProcessor blueprintTextProcessor, Map<String,
        List<InstanceMetaData>> groupInstances) {
        String port = blueprintTextProcessor.pathValue("configurations", "nifi-ambari-config", "nifi.node.ssl.port").orElse(DEFAULT_NIFI_PORT);
        List<String> publicIps = collectPublicIps(groupInstances, nifiIgs);
        return publicIps.isEmpty() ? Optional.empty() : Optional.of(publicIps.stream().map(ip -> ip + ":" + port).collect(Collectors.joining(",")));
    }

    private List<String> collectFqdnsByInstanceGroupName(Map<String, List<InstanceMetaData>> fqdns, Set<InstanceGroup> nifiIgs) {
        return nifiIgs.stream().flatMap(ig -> fqdns.get(ig.getGroupName()).stream()).map(im -> im.getDiscoveryFQDN()).collect(Collectors.toList());
    }

    private List<String> collectPublicIps(Map<String, List<InstanceMetaData>> groupInstances, Set<InstanceGroup> nifiIgs) {
        return nifiIgs.stream().flatMap(ig -> groupInstances.get(ig.getGroupName()).stream()).filter(im -> StringUtils.hasText(im.getPublicIp()))
            .map(im -> im.getPublicIp()).collect(Collectors.toList());
    }

    private Set<InstanceGroup> collectInstanceGroupsWhichContainsNifiMasters(Set<HostGroup> hostgroups, Set<String> nifiMasters) {
        return hostgroups.stream().filter(hg -> nifiMasters.contains(hg.getName())).map(hg -> hg.getConstraint()
            .getInstanceGroup()).collect(Collectors.toSet());
    }

    private Set<String> collectNifiMasters(String blueprintText) {
        return blueprintProcessorFactory.get(blueprintText).getHostGroupsWithComponent("NIFI_MASTER");
    }
}
