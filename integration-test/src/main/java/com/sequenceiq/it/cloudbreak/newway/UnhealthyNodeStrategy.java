package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
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
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) {
        Stack stack = (Stack) entity;
        var response = Objects.requireNonNull(stack.getResponse(), "Stack response is null; should get it before");
        Long id = Objects.requireNonNull(response.getId());

        var instanceGroup = response.getInstanceGroups().stream()
                .filter(ig -> ig.getName().equals(hostgroup)).collect(Collectors.toList()).get(0);
        List<String> nodes = instanceGroup.getMetadata().stream()
                .map(InstanceMetaDataV4Response::getDiscoveryFQDN).collect(Collectors.toList()).subList(0, nodeCount);
        ProxyCloudbreakClient client = getAutoscaleProxyCloudbreakClient(integrationTestContext);
        FailureReportV4Request failureReport = new FailureReportV4Request();
        failureReport.setFailedNodes(nodes);
        client.autoscaleEndpoint().failureReport(id, failureReport);
    }

    private ProxyCloudbreakClient getAutoscaleProxyCloudbreakClient(IntegrationTestContext integrationTestContext) {
        return new ProxyCloudbreakClient(
                integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                integrationTestContext.getContextParam(CloudbreakTest.CAAS_PROTOCOL),
                integrationTestContext.getContextParam(CloudbreakTest.CAAS_ADDRESS),
                integrationTestContext.getContextParam(CloudbreakTest.REFRESH_TOKEN),
                new ConfigKey(false, true, true),
                integrationTestContext.getContextParam(CloudbreakTest.IDENTITY_URL),
                integrationTestContext.getContextParam(CloudbreakTest.AUTOSCALE_CLIENT_ID),
                integrationTestContext.getContextParam(CloudbreakTest.AUTOSCALE_SECRET));
    }
}
