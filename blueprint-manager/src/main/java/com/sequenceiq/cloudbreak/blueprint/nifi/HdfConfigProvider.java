package com.sequenceiq.cloudbreak.blueprint.nifi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

@Component
public class HdfConfigProvider {

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public HdfConfigs nodeIdentities(Set<HostGroup> hostgroups,  Map<String, List<String>> fqdns, String blueprintText) {
        Set<String> nifiMasters = collectNifiMasters(blueprintText);
        Set<InstanceGroup> nifiIgs = collectInstanceGroupsWhichContainsNifiMasters(hostgroups, nifiMasters);
        List<String> nifiFqdns = collectFqdnsByInstanceGroupName(fqdns, nifiIgs);
        AtomicInteger index = new AtomicInteger(0);
        String nodeIdentities = nifiFqdns.stream()
                .map(fqdn -> String.format("<property name=\"Node Identity %s\">CN=%s, OU=NIFI</property>", index.addAndGet(1), fqdn))
                .collect(Collectors.joining());

        return new HdfConfigs(nodeIdentities);
    }

    private List<String> collectFqdnsByInstanceGroupName(Map<String, List<String>> fqdns, Set<InstanceGroup> nifiIgs) {
        return nifiIgs.stream().flatMap(ig -> fqdns.get(ig.getGroupName()).stream()).collect(Collectors.toList());
    }

    private Set<InstanceGroup> collectInstanceGroupsWhichContainsNifiMasters(Set<HostGroup> hostgroups, Set<String> nifiMasters) {
        return hostgroups.stream().filter(hg -> nifiMasters.contains(hg.getName())).map(hg -> hg.getConstraint()
                .getInstanceGroup()).collect(Collectors.toSet());
    }

    private Set<String> collectNifiMasters(String blueprintText) {
        return blueprintProcessor.getHostGroupsWithComponent(blueprintText, "NIFI_MASTER");
    }
}
