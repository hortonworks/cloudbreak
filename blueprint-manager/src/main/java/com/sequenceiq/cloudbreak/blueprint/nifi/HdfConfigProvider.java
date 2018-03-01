package com.sequenceiq.cloudbreak.blueprint.nifi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

@Component
public class HdfConfigProvider {

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private StackInfoService stackInfoService;

    public HdfConfigs nodeIdentities(Set<HostGroup> hostgroups,  Map<String, List<String>> fqdns, String blueprintText) {
        Set<String> nifiMasters = blueprintProcessor.getHostGroupsWithComponent(blueprintText, "NIFI_MASTER");
        Set<InstanceGroup> nifiIgs = hostgroups.stream().filter(hg -> nifiMasters.contains(hg.getName())).map(hg -> hg.getConstraint()
                .getInstanceGroup()).collect(Collectors.toSet());
        List<String> nifiFqdns = nifiIgs.stream().flatMap(ig -> fqdns.get(ig.getGroupName()).stream()).collect(Collectors.toList());
        AtomicInteger index = new AtomicInteger(0);
        String nodeIdentities = nifiFqdns.stream()
                .map(fqdn -> String.format("<property name=\"Node Identity %s\">CN=%s, OU=NIFI</property>", index.addAndGet(1), fqdn))
                .collect(Collectors.joining());

        return new HdfConfigs(nodeIdentities);
    }
}
