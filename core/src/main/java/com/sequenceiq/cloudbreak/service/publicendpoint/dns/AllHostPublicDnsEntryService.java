package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
public class AllHostPublicDnsEntryService extends BaseDnsEntryService {
    private static final int GROUP_WITH_SINGLE_NODE = 1;

    @Override
    protected Map<String, List<String>> getComponentLocation(Stack stack) {
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        Function<InstanceGroup, List<String>> notTerminatedAndPrimaryGatewayInstances = ig -> ig.getNotTerminatedInstanceMetaDataSet()
                .stream()
                .filter(im -> !im.getDiscoveryFQDN().equals(primaryGatewayInstance.getDiscoveryFQDN()))
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toList());

        return stack.getInstanceGroups()
                .stream()
                .filter(ig -> !(ig.getNotTerminatedInstanceMetaDataSet().contains(primaryGatewayInstance) && ig.getNodeCount() == GROUP_WITH_SINGLE_NODE))
                .collect(Collectors.toMap(InstanceGroup::getGroupName, notTerminatedAndPrimaryGatewayInstances));
    }

    @Override
    protected String logName() {
        return "all nodes except primary gateway node";
    }
}
