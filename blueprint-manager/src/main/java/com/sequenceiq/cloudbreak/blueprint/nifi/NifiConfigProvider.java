package com.sequenceiq.cloudbreak.blueprint.nifi;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.blueprint.HdfClusterLocator;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

@Component
public class NifiConfigProvider implements BlueprintComponentConfigProvider {

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private HdfClusterLocator hdfClusterLocator;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) {
        Set<String> nifiMasters = blueprintProcessor.getHostGroupsWithComponent(blueprintText, "NIFI_MASTER");
        Set<InstanceGroup> nifiIgs = source.getHostGroups().stream().filter(hg -> nifiMasters.contains(hg.getName())).map(hg -> hg.getConstraint()
                .getInstanceGroup()).collect(Collectors.toSet());
        List<String> nifiFqdns = nifiIgs.stream().flatMap(ig -> source.getFqdns().get(ig.getGroupName()).stream()).collect(Collectors.toList());
        AtomicInteger index = new AtomicInteger(0);
        String nodeIdentities = nifiFqdns.stream()
                .map(fqdn -> String.format("<property name=\"Node Identity %s\">CN=%s, OU=NIFI</property>", index.addAndGet(1), fqdn))
                .collect(Collectors.joining());
        blueprintText = source.getAmbariClient().extendBlueprintGlobalConfiguration(blueprintText, ImmutableMap.of("nifi-ambari-ssl-config", ImmutableMap.of(
                "content", nodeIdentities)));
        blueprintText = source.getAmbariClient().extendBlueprintGlobalConfiguration(blueprintText, ImmutableMap.of("nifi-ambari-ssl-config", ImmutableMap.of(
                "nifi.initial.admin.identity", source.getCluster().getUserName())));
        blueprintText = source.getAmbariClient().extendBlueprintGlobalConfiguration(blueprintText, ImmutableMap.of("ams-grafana-env", ImmutableMap.of(
                "metrics_grafana_username", source.getCluster().getUserName(),
                "metrics_grafana_password", source.getCluster().getPassword())));
        return blueprintText;
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return hdfClusterLocator.hdfCluster(source.getStackRepoDetails());
    }
}
