package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;

public class UnhealthyNodeStrategy implements Strategy {
    private final String hostgroup;

    private final int nodeCount;

    public UnhealthyNodeStrategy(String hostgroup, int nodeCount) {
        this.hostgroup = hostgroup;
        this.nodeCount = nodeCount;
    }

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Stack stack = (Stack) entity;
        StackResponse response = Objects.requireNonNull(stack.getResponse(), "Stack response is null; should get it before");
        Long id = Objects.requireNonNull(response.getId());

        InstanceGroupResponse instanceGroup = response.getInstanceGroups().stream()
                .filter(ig -> ig.getGroup().equals(hostgroup)).collect(Collectors.toList()).get(0);
        List<String> nodes = instanceGroup.getMetadata().stream()
                .map(metadata -> metadata.getDiscoveryFQDN()).collect(Collectors.toList()).subList(0, nodeCount);
        ProxyCloudbreakClient client = getAutoscaleProxyCloudbreakClient(integrationTestContext);
        FailureReport failureReport = new FailureReport();
        failureReport.setFailedNodes(nodes);
        client.autoscaleEndpoint().failureReport(id, failureReport);
    }

    private ProxyCloudbreakClient getAutoscaleProxyCloudbreakClient(IntegrationTestContext integrationTestContext) {
        return new ProxyCloudbreakClient(
                integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                integrationTestContext.getContextParam(CloudbreakTest.CAAS_PROTOCOL),
                integrationTestContext.getContextParam(CloudbreakTest.CAAS_ADDRESS),
                integrationTestContext.getContextParam(CloudbreakTest.REFRESH_TOKEN),
                new ConfigKey(false, true, true));
    }
}
